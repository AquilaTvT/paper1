from pathlib import Path
from time import perf_counter
from typing import Dict, List

from app.config import Settings, settings
from app.models.light_switch_analyzer import analyze_light_switch_video
from app.models.projection_adapter import ProjectionAdapter
from app.models.summary_generator import SummaryGenerator
from app.models.token_compressor import DualTrackTokenCompressor
from app.models.video_preprocessor import VideoPreprocessor
from app.models.video_swin_encoder import VideoSwinEncoder
from app.schemas import InferenceResult, RuntimeMetrics, SampledFrames, TokenMetrics, VideoMetadata
from app.services.metrics_collector import MetricsCollector
from app.services.stream_publisher import StreamPublisher
from app.utils.time_utils import elapsed_ms


class InferencePipeline:
    """Run the local inference pipeline and small explainable demos."""

    def __init__(self, app_settings: Settings = settings, stream_publisher: StreamPublisher | None = None) -> None:
        self.settings = app_settings
        self.preprocessor = VideoPreprocessor(app_settings)
        self.encoder = VideoSwinEncoder(app_settings)
        self.compressor = DualTrackTokenCompressor(app_settings)
        self.adapter = ProjectionAdapter(app_settings)
        self.generator = SummaryGenerator()
        self.stream_publisher = stream_publisher or StreamPublisher()

    def run(self, task_id: str, video_path: str, query_text: str, scenario_type: str = "") -> InferenceResult:
        light_switch_requested = self._is_light_switch_request(video_path, query_text, scenario_type)
        if light_switch_requested and Path(video_path).exists():
            try:
                return self._run_light_switch(task_id, video_path)
            except Exception as exc:  # noqa: BLE001 - lightweight demo should fall back without failing the existing pipeline.
                return self._run_standard(task_id, video_path, query_text, fallback_reason=f"light_switch_analyzer_failed: {exc}")

        fallback_reason = "video_path_not_accessible_for_light_switch_analyzer" if light_switch_requested else ""
        return self._run_standard(task_id, video_path, query_text, fallback_reason=fallback_reason)

    def _run_standard(self, task_id: str, video_path: str, query_text: str, fallback_reason: str = "") -> InferenceResult:
        collector = MetricsCollector()

        started = perf_counter()
        metadata, sampled_frames = self.preprocessor.preprocess(video_path)
        collector.set_stage("preprocess_ms", elapsed_ms(started))
        self.stream_publisher.publish(task_id, "stage", status="running", stage="video_preprocess")

        started = perf_counter()
        patch_features = self.encoder.encode_mock(sampled_frames)
        collector.set_stage("encode_ms", elapsed_ms(started))
        self.stream_publisher.publish(task_id, "stage", status="running", stage="video_swin")

        started = perf_counter()
        compressed_tokens, token_metrics = self.compressor.compress(patch_features, query_text)
        collector.set_stage("compress_ms", elapsed_ms(started))
        self.stream_publisher.publish(task_id, "token_metrics", stage="token_compression", token_metrics=self._token_metrics_event(token_metrics, collector.to_runtime_metrics().total_ms))

        started = perf_counter()
        projected_tokens = self.adapter.project_mock(compressed_tokens)
        collector.set_stage("project_ms", elapsed_ms(started))
        self.stream_publisher.publish(task_id, "stage", status="running", stage="projection_adapter")

        started = perf_counter()
        summary_chunks: List[str] = []
        for chunk in self.generator.stream_generate(metadata, query_text, token_metrics):
            summary_chunks.append(chunk)
            self.stream_publisher.publish(task_id, "summary_delta", status="streaming", stage="summary_generation", summary_delta=chunk)
        collector.set_stage("generate_ms", elapsed_ms(started))

        runtime_metrics: RuntimeMetrics = collector.to_runtime_metrics()
        summary = "".join(summary_chunks)
        key_events = [
            "读取视频文件并提取基础元数据。",
            "按固定间隔采样关键帧。",
            "整理画面主体、动作顺序和场景变化。",
            "生成保守的视频摘要。",
        ]
        result = InferenceResult(
            task_id=task_id,
            run_mode=self.settings.run_mode,
            video_metadata=metadata,
            sampled_frames=sampled_frames,
            token_metrics=token_metrics,
            runtime_metrics=runtime_metrics,
            summary=summary,
            summary_chunks=summary_chunks,
            key_events=key_events,
            model_info={**self._model_info(), **({"fallbackReason": fallback_reason} if fallback_reason else {})},
        )
        self.stream_publisher.publish(task_id, "completed", status="finished", stage="finished", completed=self._completed_event(result))
        return result

    def _run_light_switch(self, task_id: str, video_path: str) -> InferenceResult:
        collector = MetricsCollector()

        started = perf_counter()
        analysis = analyze_light_switch_video(video_path)
        collector.set_stage("preprocess_ms", elapsed_ms(started))
        self.stream_publisher.publish(task_id, "stage", status="running", stage="video_sampling")

        started = perf_counter()
        token_metrics = TokenMetrics(
            sampled_frames=int(analysis["sampledFrames"]),
            raw_patch_tokens_per_frame=196,
            compressed_tokens_per_frame=5,
            raw_visual_tokens=int(analysis["sampledFrames"]) * 196,
            compressed_visual_tokens=int(analysis["sampledFrames"]) * 5,
            compression_ratio=round(196 / 5, 3),
        )
        collector.set_stage("compress_ms", elapsed_ms(started))
        self.stream_publisher.publish(task_id, "token_metrics", stage="token_compression", token_metrics=self._token_metrics_event(token_metrics, collector.to_runtime_metrics().total_ms))

        started = perf_counter()
        summary_chunks = self._light_switch_chunks(analysis)
        for chunk in summary_chunks:
            self.stream_publisher.publish(task_id, "summary_delta", status="streaming", stage="summary_generation", summary_delta=chunk)
        collector.set_stage("generate_ms", elapsed_ms(started))

        metadata = VideoMetadata(
            video_path=video_path,
            duration_seconds=float(analysis["duration"]),
            fps=float(analysis["fps"]),
            total_frames=max(1, int(round(float(analysis["duration"]) * float(analysis["fps"])))),
            width=0,
            height=0,
            source="opencv_light_switch",
        )
        sampled_frames = SampledFrames(
            strategy="opencv_light_switch_4fps",
            sample_fps=4,
            frame_indices=list(range(int(analysis["sampledFrames"]))),
            sampled_count=int(analysis["sampledFrames"]),
        )
        key_events = self._light_switch_key_events(analysis)
        result = InferenceResult(
            task_id=task_id,
            run_mode=self.settings.run_mode,
            video_metadata=metadata,
            sampled_frames=sampled_frames,
            token_metrics=token_metrics,
            runtime_metrics=collector.to_runtime_metrics(),
            summary="".join(summary_chunks),
            summary_chunks=summary_chunks,
            key_events=key_events,
            model_info={**self._model_info(), "scenarioType": "light_switch_demo"},
            light_switch_analysis=analysis,
        )
        self.stream_publisher.publish(task_id, "completed", status="finished", stage="finished", completed=self._completed_event(result))
        return result

    def _is_light_switch_request(self, video_path: str, query_text: str, scenario_type: str = "") -> bool:
        text = f"{video_path} {query_text} {scenario_type}".lower()
        return any(keyword in text for keyword in ("light", "switch", "lamp", "开关", "灯", "按开关"))

    def _light_switch_chunks(self, analysis: Dict[str, object]) -> List[str]:
        action = "检测到一次明显的手部靠近或按压动作。" if analysis.get("motionDetected") else "动作变化较弱，无法稳定确认按压动作。"
        trend = analysis.get("brightnessTrend")
        operation = analysis.get("operationGuess")
        if trend == "brighter":
            brightness = "按压后画面整体亮度上升，结果更接近开灯。"
        elif trend == "darker":
            brightness = "按压后画面整体亮度下降，结果更接近关灯。"
        else:
            brightness = "按压前后亮度变化不明显，不能明确判断开灯或关灯。"
        uncertainty = "该结果基于帧差与平均亮度估计，只适用于灯开关短视频演示场景。"
        if operation == "unknown":
            uncertainty = "画面证据不足，建议结合原视频复核。"
        return [
            f"视频内容摘要：{analysis['summary']}",
            f"动作判断：{action}",
            f"亮度变化：{brightness}",
            f"不确定性说明：{uncertainty}",
        ]

    def _light_switch_key_events(self, analysis: Dict[str, object]) -> List[str]:
        return [
            "读取视频文件并采样关键帧。",
            f"在约 {analysis.get('motionPeakTime', 0)} 秒处观察到最大帧间变化。",
            f"按压前后平均亮度差为 {analysis.get('brightnessDelta', 0)}。",
            f"操作判断：{analysis.get('operationGuess', 'unknown')}，置信度约 {analysis.get('confidence', 0)}。",
        ]

    def _token_metrics_event(self, token_metrics, estimated_latency_ms: float) -> Dict[str, object]:
        return {
            "sampledFrames": token_metrics.sampled_frames,
            "rawPatchTokensPerFrame": token_metrics.raw_patch_tokens_per_frame,
            "compressedTokensPerFrame": token_metrics.compressed_tokens_per_frame,
            "rawVisualTokens": token_metrics.raw_visual_tokens,
            "compressedVisualTokens": token_metrics.compressed_visual_tokens,
            "compressionRatio": token_metrics.compression_ratio,
            "estimatedLatencyMs": int(estimated_latency_ms),
        }

    def _completed_event(self, result: InferenceResult) -> Dict[str, object]:
        event = {
            "summary": result.summary,
            "keyEvents": result.key_events,
            "estimatedLatencyMs": int(result.runtime_metrics.total_ms),
        }
        if result.light_switch_analysis:
            event["scenarioType"] = "light_switch_demo"
            event["lightSwitchAnalysis"] = result.light_switch_analysis
        if result.model_info.get("fallbackReason"):
            event["fallbackReason"] = result.model_info["fallbackReason"]
        return event

    def _model_info(self) -> Dict[str, str]:
        return {
            "video_preprocessor": "mock metadata with optional opencv metadata reader",
            "video_encoder": "local sampled-frame feature placeholder",
            "token_compressor": "local token compression metrics 196 -> 5",
            "projection_adapter": f"numpy projection placeholder to {self.settings.llm_dim}",
            "summary_generator": "conservative Chinese summary generator",
        }

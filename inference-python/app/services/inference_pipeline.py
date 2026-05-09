from time import perf_counter
from typing import Dict, List

from app.config import Settings, settings
from app.models.projection_adapter import ProjectionAdapter
from app.models.summary_generator import SummaryGenerator
from app.models.token_compressor import DualTrackTokenCompressor
from app.models.video_preprocessor import VideoPreprocessor
from app.models.video_swin_encoder import VideoSwinEncoder
from app.schemas import InferenceResult, RuntimeMetrics
from app.services.metrics_collector import MetricsCollector
from app.services.stream_publisher import StreamPublisher
from app.utils.time_utils import elapsed_ms


class InferencePipeline:
    """串联论文第 5 章详细设计中的视频预处理、Video Swin、Token 压缩、Adapter 与摘要生成。"""

    def __init__(self, app_settings: Settings = settings, stream_publisher: StreamPublisher | None = None) -> None:
        self.settings = app_settings
        self.preprocessor = VideoPreprocessor(app_settings)
        self.encoder = VideoSwinEncoder(app_settings)
        self.compressor = DualTrackTokenCompressor(app_settings)
        self.adapter = ProjectionAdapter(app_settings)
        self.generator = SummaryGenerator()
        self.stream_publisher = stream_publisher or StreamPublisher()

    def run(self, task_id: str, video_path: str, query_text: str) -> InferenceResult:
        collector = MetricsCollector()

        started = perf_counter()
        metadata, sampled_frames = self.preprocessor.preprocess(video_path)
        collector.set_stage("preprocess_ms", elapsed_ms(started))
        self.stream_publisher.publish(task_id, "stage", {"stage": "video_preprocess", "sampled": sampled_frames.sampled_count})

        started = perf_counter()
        patch_features = self.encoder.encode_mock(sampled_frames)
        collector.set_stage("encode_ms", elapsed_ms(started))
        self.stream_publisher.publish(task_id, "stage", {"stage": "video_swin", "shape": list(patch_features.shape)})

        started = perf_counter()
        compressed_tokens, token_metrics = self.compressor.compress(patch_features, query_text)
        collector.set_stage("compress_ms", elapsed_ms(started))
        self.stream_publisher.publish(task_id, "token_metrics", token_metrics.model_dump())

        started = perf_counter()
        projected_tokens = self.adapter.project_mock(compressed_tokens)
        collector.set_stage("project_ms", elapsed_ms(started))
        self.stream_publisher.publish(task_id, "stage", {"stage": "projection_adapter", "shape": list(projected_tokens.shape)})

        started = perf_counter()
        summary_chunks: List[str] = []
        for chunk in self.generator.stream_generate(metadata, query_text, token_metrics):
            summary_chunks.append(chunk)
            self.stream_publisher.publish(task_id, "summary_delta", {"text": chunk})
        collector.set_stage("generate_ms", elapsed_ms(started))

        runtime_metrics: RuntimeMetrics = collector.to_runtime_metrics()
        summary = "".join(summary_chunks)
        key_events = [
            "完成视频预处理与均匀抽帧。",
            "生成形状为 [T, 196, C] 的 Video Swin mock 特征。",
            "双轨 Token 压缩输出 [T, 5, C]。",
            "MLP Projection Adapter 输出面向 LLM 的视觉表示。",
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
            model_info=self._model_info(),
        )
        self.stream_publisher.publish(task_id, "completed", result.model_dump())
        return result

    def _model_info(self) -> Dict[str, str]:
        return {
            "video_preprocessor": "mock metadata with optional opencv metadata reader",
            "video_encoder": "mock Video Swin [T,196,768]",
            "token_compressor": "dual-track content/context compressor 196 -> 5",
            "projection_adapter": f"numpy two-layer MLP to {self.settings.llm_dim}",
            "summary_generator": "template-based Chinese mock generator",
        }

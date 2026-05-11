from collections.abc import Iterator

from app.schemas import TokenMetrics, VideoMetadata


class SummaryGenerator:
    """Generate conservative Chinese video-summary text for the local demo pipeline."""

    def stream_generate(
        self,
        metadata: VideoMetadata,
        query_text: str,
        token_metrics: TokenMetrics,
    ) -> Iterator[str]:
        yield f"视频内容摘要：视频时长约 {metadata.duration_seconds:.1f} 秒，画面可按开头、主体动作和结果变化进行整理。"
        yield "动作判断：采样片段显示画面中存在连续变化，摘要优先描述可见主体、物体位置与动作顺序。"
        yield f"关注重点：根据用户指令“{query_text}”，结果会优先保留与问题直接相关的场景和事件。"
        yield "不确定性说明：当前链路只给出轻量分析结果，无法确认的细节会保持保守表述。"

    def generate(self, metadata: VideoMetadata, query_text: str, token_metrics: TokenMetrics) -> tuple[str, list[str]]:
        chunks = list(self.stream_generate(metadata, query_text, token_metrics))
        return "".join(chunks), chunks

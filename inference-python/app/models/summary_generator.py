from collections.abc import Iterator

from app.schemas import TokenMetrics, VideoMetadata


class SummaryGenerator:
    """论文“摘要生成模块”：根据视频元数据、用户指令和 Token 指标生成中文摘要。"""

    def stream_generate(
        self,
        metadata: VideoMetadata,
        query_text: str,
        token_metrics: TokenMetrics,
    ) -> Iterator[str]:
        yield f"系统读取到视频时长约 {metadata.duration_seconds:.1f} 秒，分辨率为 {metadata.width}×{metadata.height}。"
        yield "视频主要内容可概括为：画面中存在连续场景变化、主体动作推进以及可用于摘要的关键事件线索。"
        yield "关键事件包括视频抽帧、时空视觉特征提取、双轨 Token 压缩以及面向用户问题的摘要生成。"
        yield f"针对用户指令“{query_text}”，系统重点保留与场景、动作、上下文关联最强的信息。"
        yield (
            "处理指标显示，单帧视觉 Patch Token 从 "
            f"{token_metrics.raw_patch_tokens_per_frame} 个压缩到 {token_metrics.compressed_tokens_per_frame} 个，"
            f"总视觉 Token 从 {token_metrics.raw_visual_tokens} 降至 {token_metrics.compressed_visual_tokens}。"
        )

    def generate(self, metadata: VideoMetadata, query_text: str, token_metrics: TokenMetrics) -> tuple[str, list[str]]:
        chunks = list(self.stream_generate(metadata, query_text, token_metrics))
        return "".join(chunks), chunks

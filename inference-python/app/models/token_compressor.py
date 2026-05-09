import numpy as np

from app.config import Settings
from app.schemas import TokenMetrics


class DualTrackTokenCompressor:
    """论文“双轨 Token 压缩模块”：Content Token + Context Token，将 [T,196,C] 压缩为 [T,5,C]。"""

    def __init__(self, settings: Settings) -> None:
        self.settings = settings

    def compress(self, patch_features: np.ndarray, query_text: str) -> tuple[np.ndarray, TokenMetrics]:
        if patch_features.ndim != 3:
            raise ValueError("patch_features 必须是 [T, P, C] 三维张量")
        time_steps, patch_count, feature_dim = patch_features.shape
        if patch_count != self.settings.patch_tokens_per_frame:
            raise ValueError(f"每帧 Patch Token 数必须为 {self.settings.patch_tokens_per_frame}")

        content_token = patch_features.mean(axis=1, keepdims=True)
        query_embedding = self._mock_query_embedding(query_text, feature_dim)
        context_tokens = self._build_context_tokens(patch_features, query_embedding)
        compressed = np.concatenate([content_token, context_tokens], axis=1).astype(np.float32)

        metrics = TokenMetrics(
            sampled_frames=time_steps,
            raw_patch_tokens_per_frame=patch_count,
            compressed_tokens_per_frame=compressed.shape[1],
            raw_visual_tokens=time_steps * patch_count,
            compressed_visual_tokens=time_steps * compressed.shape[1],
            compression_ratio=(time_steps * patch_count) / (time_steps * compressed.shape[1]),
        )
        return compressed, metrics

    def _mock_query_embedding(self, query_text: str, feature_dim: int) -> np.ndarray:
        seed = sum(ord(char) for char in query_text) % 100_000
        rng = np.random.default_rng(seed=seed)
        return rng.normal(0, 0.03, size=(feature_dim,)).astype(np.float32)

    def _build_context_tokens(self, patch_features: np.ndarray, query_embedding: np.ndarray) -> np.ndarray:
        time_steps, _, feature_dim = patch_features.shape
        temporal_mean = patch_features.mean(axis=0).mean(axis=0)
        global_context = (temporal_mean + query_embedding).astype(np.float32)
        scales = np.array([0.80, 0.95, 1.05, 1.20], dtype=np.float32).reshape(1, 4, 1)
        base = np.tile(global_context.reshape(1, 1, feature_dim), (time_steps, 4, 1))
        temporal_offsets = np.linspace(0, 0.04, time_steps, dtype=np.float32).reshape(time_steps, 1, 1)
        return base * scales + temporal_offsets

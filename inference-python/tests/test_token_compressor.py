import numpy as np

from app.config import settings
from app.models.token_compressor import DualTrackTokenCompressor


def test_dual_track_token_compressor_reduces_196_to_5() -> None:
    compressor = DualTrackTokenCompressor(settings)
    features = np.ones((4, 196, 768), dtype=np.float32)

    compressed, metrics = compressor.compress(features, "请总结视频内容")

    assert compressed.shape == (4, 5, 768)
    assert metrics.raw_patch_tokens_per_frame == 196
    assert metrics.compressed_tokens_per_frame == 5
    assert metrics.raw_visual_tokens == 4 * 196
    assert metrics.compressed_visual_tokens == 4 * 5
    assert metrics.compression_ratio == 196 / 5

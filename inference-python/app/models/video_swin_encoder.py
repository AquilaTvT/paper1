from importlib import import_module, util

import numpy as np

from app.config import Settings
from app.schemas import SampledFrames


class VideoSwinEncoder:
    """论文“Video Swin 特征提取模块”：mock 输出 [T, 196, C] 时空视觉特征。"""

    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self.real_model = None

    def encode_mock(self, sampled_frames: SampledFrames) -> np.ndarray:
        time_steps = sampled_frames.sampled_count
        rng = np.random.default_rng(seed=20260509 + time_steps)
        features = rng.normal(
            loc=0.0,
            scale=0.02,
            size=(time_steps, self.settings.patch_tokens_per_frame, self.settings.feature_dim),
        ).astype(np.float32)
        temporal_bias = np.linspace(0, 1, time_steps, dtype=np.float32).reshape(time_steps, 1, 1)
        return features + temporal_bias

    def load_real_model(self) -> object:
        if util.find_spec("torch") is None:
            raise RuntimeError("real mode 需要安装 torch 并配置 Video Swin 权重路径")
        torch = import_module("torch")
        self.real_model = {"torch": torch, "path": self.settings.real_video_swin_path}
        return self.real_model

    def encode_real(self, sampled_frames: SampledFrames) -> np.ndarray:
        raise NotImplementedError("真实 Video Swin 推理将在 real mode 阶段接入")

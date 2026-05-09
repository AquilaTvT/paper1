from importlib import import_module, util

import numpy as np

from app.config import Settings


class ProjectionAdapter:
    """论文“MLP Projection Adapter 模块”：mock 两层 MLP 将视觉 Token 投影到 LLM 维度。"""

    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        rng = np.random.default_rng(seed=4096)
        hidden_dim = 1024
        self.weight1 = rng.normal(0, 0.01, size=(settings.feature_dim, hidden_dim)).astype(np.float32)
        self.weight2 = rng.normal(0, 0.01, size=(hidden_dim, settings.llm_dim)).astype(np.float32)

    def project_mock(self, visual_tokens: np.ndarray) -> np.ndarray:
        hidden = np.tanh(visual_tokens @ self.weight1)
        return (hidden @ self.weight2).astype(np.float32)

    def build_torch_adapter(self) -> object:
        if util.find_spec("torch") is None:
            raise RuntimeError("real mode 需要安装 torch 才能构建真实 MLP Adapter")
        torch = import_module("torch")
        return {"torch": torch, "adapter_path": self.settings.real_adapter_path}

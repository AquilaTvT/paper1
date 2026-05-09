from abc import ABC, abstractmethod
from typing import Any

import numpy as np

from app.schemas import SampledFrames, TokenMetrics, VideoMetadata


class RealVideoSwinInterface(ABC):
    """真实 Video Swin 接入接口：第 4 阶段后可由具体模型实现替换 mock encoder。"""

    @abstractmethod
    def load(self, checkpoint_path: str) -> None:
        raise NotImplementedError

    @abstractmethod
    def encode(self, metadata: VideoMetadata, sampled_frames: SampledFrames) -> np.ndarray:
        raise NotImplementedError


class RealProjectionAdapterInterface(ABC):
    """真实 MLP Adapter 接入接口：支持后续加载 PyTorch/QLoRA 训练产物。"""

    @abstractmethod
    def project(self, visual_tokens: np.ndarray) -> Any:
        raise NotImplementedError


class RealLLMGeneratorInterface(ABC):
    """真实 LLM 接入接口：支持后续本地模型或远程模型服务。"""

    @abstractmethod
    def generate(self, projected_tokens: Any, query_text: str, metrics: TokenMetrics) -> str:
        raise NotImplementedError

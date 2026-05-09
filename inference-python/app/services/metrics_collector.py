from time import perf_counter
from typing import Dict

from app.schemas import RuntimeMetrics
from app.utils.time_utils import elapsed_ms


class MetricsCollector:
    """记录论文测试分析需要的各阶段耗时。"""

    def __init__(self) -> None:
        self.started = perf_counter()
        self.stage_metrics: Dict[str, float] = {}

    def set_stage(self, key: str, value_ms: float) -> None:
        self.stage_metrics[key] = value_ms

    def to_runtime_metrics(self) -> RuntimeMetrics:
        return RuntimeMetrics(
            preprocess_ms=self.stage_metrics.get("preprocess_ms", 0),
            encode_ms=self.stage_metrics.get("encode_ms", 0),
            compress_ms=self.stage_metrics.get("compress_ms", 0),
            project_ms=self.stage_metrics.get("project_ms", 0),
            generate_ms=self.stage_metrics.get("generate_ms", 0),
            total_ms=elapsed_ms(self.started),
        )

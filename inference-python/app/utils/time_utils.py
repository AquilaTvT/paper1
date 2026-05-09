from contextlib import contextmanager
from time import perf_counter
from typing import Dict, Iterator


@contextmanager
def record_elapsed_ms(metrics: Dict[str, float], key: str) -> Iterator[None]:
    started = perf_counter()
    yield
    metrics[key] = round((perf_counter() - started) * 1000, 3)


def elapsed_ms(started: float) -> float:
    return round((perf_counter() - started) * 1000, 3)

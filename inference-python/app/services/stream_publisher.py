from typing import Any, Dict, List
import json

from app.config import settings
from app.utils.logger import get_logger


class StreamPublisher:
    """流式消息发布器：mock 写内存；Redis mode 写 stream:task:{taskId} 与 result:{taskId}。"""

    def __init__(self) -> None:
        self.logger = get_logger(self.__class__.__name__)
        self.events: List[Dict[str, Any]] = []
        self.redis_client = None
        if settings.redis_enabled:
            try:
                import redis
                self.redis_client = redis.Redis.from_url(settings.redis_url, decode_responses=True)
                self.redis_client.ping()
            except Exception as exc:
                self.logger.warning("Redis unavailable, fallback to memory events: %s", exc)
                self.redis_client = None

    def publish(self, task_id: str, event_type: str, payload: Dict[str, Any], stage: str | None = None) -> None:
        event = {"task_id": task_id, "event_type": event_type, "stage": stage or event_type, "payload": payload}
        self.events.append(event)
        self.logger.info("publish stream event: %s", event)
        if self.redis_client:
            self.redis_client.xadd(
                f"stream:task:{task_id}",
                {"eventType": event_type, "stage": stage or event_type, "payload": json.dumps(payload, ensure_ascii=False)},
            )

    def save_result(self, task_id: str, result: Dict[str, Any]) -> None:
        if self.redis_client:
            self.redis_client.set(f"result:{task_id}", json.dumps(result, ensure_ascii=False))

import json
from typing import Any, Dict, List, Optional

from app.config import settings
from app.utils.logger import get_logger


class StreamPublisher:
    """发布统一 camelCase 事件到 Redis Stream；mock mode 下保留内存事件列表。"""

    def __init__(self, redis_client: Optional[Any] = None) -> None:
        self.logger = get_logger(self.__class__.__name__)
        self.events: List[Dict[str, Any]] = []
        self.redis_client = redis_client or self._build_redis_client()

    def publish(
        self,
        task_id: str,
        event_type: str,
        payload: Optional[Dict[str, Any]] = None,
        *,
        status: Optional[str] = None,
        stage: Optional[str] = None,
        token_metrics: Optional[Dict[str, Any]] = None,
        summary_delta: Optional[str] = None,
        completed: Optional[Dict[str, Any]] = None,
        error: Optional[str] = None,
    ) -> None:
        payload = payload or {}
        event = {
            "taskId": task_id,
            "eventType": event_type,
            "status": status or payload.get("status") or "",
            "stage": stage or payload.get("stage") or "",
            "tokenMetrics": token_metrics or payload.get("tokenMetrics") or {},
            "summaryDelta": summary_delta or payload.get("summaryDelta") or payload.get("text") or "",
            "completed": completed or payload.get("completed") or {},
            "error": error or payload.get("error") or "",
        }
        self.events.append(event)
        if self.redis_client is not None:
            redis_event = {
                "taskId": event["taskId"],
                "eventType": event["eventType"],
                "status": event["status"],
                "stage": event["stage"],
                "tokenMetrics": json.dumps(event["tokenMetrics"], ensure_ascii=False),
                "summaryDelta": event["summaryDelta"],
                "completed": json.dumps(event["completed"], ensure_ascii=False),
                "error": event["error"],
            }
            self.redis_client.xadd(settings.redis_event_stream, redis_event)
        self.logger.info("publish stream event: %s", event)

    def _build_redis_client(self) -> Optional[Any]:
        if not settings.redis_enabled:
            return None
        import redis

        return redis.Redis.from_url(settings.redis_url, decode_responses=True)

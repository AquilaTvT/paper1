from typing import Any, Dict, List

from app.utils.logger import get_logger


class StreamPublisher:
    """流式消息发布器：当前写入内存事件列表，后续替换为 Redis Stream。"""

    def __init__(self) -> None:
        self.logger = get_logger(self.__class__.__name__)
        self.events: List[Dict[str, Any]] = []

    def publish(self, task_id: str, event_type: str, payload: Dict[str, Any]) -> None:
        event = {"task_id": task_id, "event_type": event_type, "payload": payload}
        self.events.append(event)
        self.logger.info("publish mock stream event: %s", event)

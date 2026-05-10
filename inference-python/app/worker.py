import time
from typing import Any, Dict, Optional

from app.config import settings
from app.schemas import InferenceRequest, InferenceResult
from app.services.inference_pipeline import InferencePipeline
from app.services.stream_publisher import StreamPublisher
from app.utils.logger import get_logger


class InferenceWorker:
    """推理 worker：Redis mode 消费任务 Stream，保留无 Redis 本地 mock task。"""

    def __init__(self) -> None:
        self.logger = get_logger(self.__class__.__name__)
        self.redis_client = self._build_redis_client()
        self.publisher = StreamPublisher(self.redis_client)
        self.pipeline = InferencePipeline(settings, stream_publisher=self.publisher)
        self.last_seen_id = "0-0"

    def run_once(self, request: InferenceRequest | None = None) -> InferenceResult:
        task = request or InferenceRequest(
            video_path="mock://worker-demo.mp4",
            query_text="请总结视频主要内容并输出 Token 压缩指标",
            task_id="task-worker-local",
        )
        self.logger.info("run local mock inference task: %s", task.task_id)
        return self.pipeline.run(task.task_id, task.video_path, task.query_text)

    def run_forever(self) -> None:
        if not settings.redis_enabled:
            self.logger.info("Redis 未启用，执行一次本地 mock task。")
            self.run_once()
            return

        self.logger.info("Redis mode enabled, consuming stream: %s", settings.redis_task_stream)
        while True:
            records = self.redis_client.xread({settings.redis_task_stream: self.last_seen_id}, count=1, block=5000)
            if not records:
                continue
            for _, messages in records:
                for message_id, fields in messages:
                    self.last_seen_id = message_id
                    self._handle_task(fields)

    def _handle_task(self, fields: Dict[str, Any]) -> None:
        task_id = fields.get("taskId", "task-redis-missing")
        video_path = fields.get("videoPath", "mock://redis-worker.mp4")
        query_text = fields.get("queryText", "请总结视频中的关键事件")
        try:
            self.publisher.publish(task_id, "status", status="running", stage="video_preprocess")
            self.pipeline.run(task_id, video_path, query_text)
        except Exception as exc:  # noqa: BLE001 - worker 顶层需兜底发布错误事件。
            self.logger.exception("redis task failed: %s", task_id)
            self.publisher.publish(task_id, "error", status="failed", stage="python_worker", error=str(exc))

    def _build_redis_client(self) -> Optional[Any]:
        if not settings.redis_enabled:
            return None
        import redis

        return redis.Redis.from_url(settings.redis_url, decode_responses=True)


if __name__ == "__main__":
    InferenceWorker().run_forever()

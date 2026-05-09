import json
from typing import Any, Dict

from app.config import settings
from app.schemas import InferenceRequest, InferenceResult
from app.services.inference_pipeline import InferencePipeline
from app.utils.logger import get_logger


class InferenceWorker:
    """推理 worker：Redis mode 阻塞读取 queue:mmvs:tasks；无 Redis 时执行本地 mock task。"""

    def __init__(self) -> None:
        self.logger = get_logger(self.__class__.__name__)
        self.pipeline = InferencePipeline(settings)
        self.redis_client = None
        if settings.redis_enabled:
            import redis
            self.redis_client = redis.Redis.from_url(settings.redis_url, decode_responses=True)
            self.redis_client.ping()

    def run_once(self, request: InferenceRequest | None = None) -> InferenceResult:
        task = request or InferenceRequest(
            video_path="mock://worker-demo.mp4",
            query_text="请总结视频主要内容并输出 Token 压缩指标",
            task_id="task-worker-local",
        )
        self.logger.info("run local mock inference task: %s", task.task_id)
        return self.pipeline.run(task.task_id, task.video_path, task.query_text)

    def run_forever(self) -> None:
        if not self.redis_client:
            self.logger.info("Redis 未启用，执行一次本地 mock task。")
            self.run_once()
            return
        self.logger.info("Redis worker started, blocking on queue:mmvs:tasks")
        last_id = "0-0"
        while True:
            records = self.redis_client.xread({"queue:mmvs:tasks": last_id}, block=5000, count=1)
            for _, messages in records:
                for message_id, fields in messages:
                    last_id = message_id
                    self._handle_redis_task(fields)

    def _handle_redis_task(self, fields: Dict[str, Any]) -> None:
        task_id = fields["taskId"]
        try:
            self.redis_client.hset(f"task:{task_id}", mapping={"status": "running", "currentStage": "python_worker", "updatedAt": fields.get("createdAt", "")})
            self.redis_client.xadd(
                f"stream:task:{task_id}",
                {"taskId": task_id, "eventType": "status", "stage": "python_worker", "status": "running", "payload": json.dumps({"status": "running", "progress": 5}, ensure_ascii=False)},
            )
            self.pipeline.run(task_id, fields.get("videoPath", "mock://redis-task.mp4"), fields.get("queryText", "请总结视频"))
            self.redis_client.hset(f"task:{task_id}", mapping={"status": "finished", "currentStage": "finished", "progress": "100"})
        except Exception as exc:
            payload = {"message": str(exc)}
            self.redis_client.hset(f"task:{task_id}", mapping={"status": "failed", "currentStage": "python_worker", "errorMessage": str(exc)})
            self.redis_client.xadd(f"stream:task:{task_id}", {"taskId": task_id, "eventType": "error", "stage": "python_worker", "error": str(exc), "payload": json.dumps(payload, ensure_ascii=False)})


if __name__ == "__main__":
    InferenceWorker().run_forever()

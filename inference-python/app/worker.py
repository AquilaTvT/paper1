from app.config import settings
from app.schemas import InferenceRequest, InferenceResult
from app.services.inference_pipeline import InferencePipeline
from app.utils.logger import get_logger


class InferenceWorker:
    """推理 worker：预留 Redis 队列消费逻辑；无 Redis 时执行本地 mock task。"""

    def __init__(self) -> None:
        self.logger = get_logger(self.__class__.__name__)
        self.pipeline = InferencePipeline(settings)

    def run_once(self, request: InferenceRequest | None = None) -> InferenceResult:
        task = request or InferenceRequest(
            video_path="mock://worker-demo.mp4",
            query_text="请总结视频主要内容并输出 Token 压缩指标",
            task_id="task-worker-local",
        )
        self.logger.info("run local mock inference task: %s", task.task_id)
        return self.pipeline.run(task.task_id, task.video_path, task.query_text)

    def run_forever(self) -> None:
        if settings.redis_enabled:
            self.logger.info("Redis mode 已预留，下一阶段接入 Redis Stream 消费。")
        else:
            self.logger.info("Redis 未启用，执行一次本地 mock task。")
            self.run_once()

from dataclasses import dataclass
import os


@dataclass(frozen=True)
class Settings:
    service_name: str = "mmvs-inference-python"
    run_mode: str = os.getenv("MMVS_RUN_MODE", "mock")
    default_sample_fps: int = int(os.getenv("MMVS_SAMPLE_FPS", "1"))
    default_sample_frames: int = int(os.getenv("MMVS_SAMPLE_FRAMES", "16"))
    patch_tokens_per_frame: int = int(os.getenv("MMVS_PATCH_TOKENS_PER_FRAME", "196"))
    compressed_tokens_per_frame: int = int(os.getenv("MMVS_COMPRESSED_TOKENS_PER_FRAME", "5"))
    feature_dim: int = int(os.getenv("MMVS_FEATURE_DIM", "768"))
    llm_dim: int = int(os.getenv("MMVS_LLM_DIM", "4096"))
    redis_url: str = os.getenv("MMVS_REDIS_URL", "redis://localhost:6379/0")
    redis_enabled: bool = os.getenv("MMVS_REDIS_ENABLED", "false").lower() == "true"
    redis_task_stream: str = os.getenv("MMVS_REDIS_TASK_STREAM", "mmvs:tasks:requests")
    redis_event_stream: str = os.getenv("MMVS_REDIS_EVENT_STREAM", "mmvs:tasks:events")
    real_video_swin_path: str = os.getenv("MMVS_REAL_VIDEO_SWIN_PATH", "")
    real_llm_path: str = os.getenv("MMVS_REAL_LLM_PATH", "")
    real_adapter_path: str = os.getenv("MMVS_REAL_ADAPTER_PATH", "")


settings = Settings()

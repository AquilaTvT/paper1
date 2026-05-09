from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field


class InferenceRequest(BaseModel):
    video_path: str = Field(default="mock://demo.mp4", description="视频文件路径或 mock 路径")
    query_text: str = Field(default="请总结视频中的关键事件", description="用户摘要或问答指令")
    task_id: str = Field(default="task-local-mock", description="任务 ID")


class VideoMetadata(BaseModel):
    video_path: str
    duration_seconds: float
    fps: float
    total_frames: int
    width: int
    height: int
    source: str = "mock"


class SampledFrames(BaseModel):
    strategy: str
    sample_fps: int
    frame_indices: List[int]
    sampled_count: int


class TokenMetrics(BaseModel):
    sampled_frames: int
    raw_patch_tokens_per_frame: int
    compressed_tokens_per_frame: int
    raw_visual_tokens: int
    compressed_visual_tokens: int
    compression_ratio: float


class RuntimeMetrics(BaseModel):
    preprocess_ms: float = 0
    encode_ms: float = 0
    compress_ms: float = 0
    project_ms: float = 0
    generate_ms: float = 0
    total_ms: float = 0


class InferenceResult(BaseModel):
    task_id: str
    run_mode: str
    video_metadata: VideoMetadata
    sampled_frames: SampledFrames
    token_metrics: TokenMetrics
    runtime_metrics: RuntimeMetrics
    summary: str
    summary_chunks: List[str]
    key_events: List[str]
    model_info: Dict[str, Any]
    error_message: Optional[str] = None

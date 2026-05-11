from typing import Any, Dict, List, Optional
from pydantic import BaseModel, ConfigDict, Field


class InferenceRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    video_path: str = Field(default="mock://demo.mp4", alias="videoPath", description="视频文件路径或 mock 路径")
    query_text: str = Field(default="请总结视频中的关键事件", alias="queryText", description="用户摘要或问答指令")
    task_id: str = Field(default="task-local-mock", alias="taskId", description="任务 ID")
    scenario_type: str = Field(default="", alias="scenarioType", description="可选场景类型，例如 light_switch_demo")


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
    light_switch_analysis: Optional[Dict[str, Any]] = None
    error_message: Optional[str] = None

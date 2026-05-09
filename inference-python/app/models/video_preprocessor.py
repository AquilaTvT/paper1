from app.config import Settings
from app.schemas import SampledFrames, VideoMetadata
from app.utils.video_utils import read_video_metadata, uniform_sample_indices


class VideoPreprocessor:
    """论文“视频预处理模块”：读取视频元数据，并按 1 FPS 或固定帧数均匀抽帧。"""

    def __init__(self, settings: Settings) -> None:
        self.settings = settings

    def preprocess(self, video_path: str) -> tuple[VideoMetadata, SampledFrames]:
        metadata = read_video_metadata(video_path)
        sampled_count = min(
            self.settings.default_sample_frames,
            max(1, int(round(metadata.duration_seconds * self.settings.default_sample_fps))),
        )
        frame_indices = uniform_sample_indices(metadata.total_frames, sampled_count)
        sampled_frames = SampledFrames(
            strategy="uniform_1fps_or_fixed_16",
            sample_fps=self.settings.default_sample_fps,
            frame_indices=frame_indices,
            sampled_count=len(frame_indices),
        )
        return metadata, sampled_frames

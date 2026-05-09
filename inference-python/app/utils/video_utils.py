from importlib import import_module, util
from pathlib import Path
from typing import List

from app.schemas import VideoMetadata


def has_cv2() -> bool:
    return util.find_spec("cv2") is not None


def read_video_metadata(video_path: str) -> VideoMetadata:
    if has_cv2() and Path(video_path).exists():
        cv2 = import_module("cv2")
        capture = cv2.VideoCapture(video_path)
        fps = float(capture.get(cv2.CAP_PROP_FPS) or 25.0)
        total_frames = int(capture.get(cv2.CAP_PROP_FRAME_COUNT) or 0)
        width = int(capture.get(cv2.CAP_PROP_FRAME_WIDTH) or 1280)
        height = int(capture.get(cv2.CAP_PROP_FRAME_HEIGHT) or 720)
        capture.release()
        duration = total_frames / fps if fps > 0 and total_frames > 0 else 16.0
        return VideoMetadata(
            video_path=video_path,
            duration_seconds=round(duration, 3),
            fps=fps,
            total_frames=max(total_frames, 1),
            width=width,
            height=height,
            source="opencv",
        )

    return VideoMetadata(
        video_path=video_path,
        duration_seconds=16.0,
        fps=25.0,
        total_frames=400,
        width=1280,
        height=720,
        source="fallback_mock",
    )


def uniform_sample_indices(total_frames: int, sampled_count: int) -> List[int]:
    if sampled_count <= 1:
        return [0]
    last_index = max(total_frames - 1, 0)
    return [round(i * last_index / (sampled_count - 1)) for i in range(sampled_count)]

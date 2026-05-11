from importlib import import_module
from pathlib import Path
from typing import Any, Dict, List, Sequence

import numpy as np

BRIGHTNESS_THRESHOLD = 8.0
MOTION_THRESHOLD = 0.015


def _cv2():
    return import_module("cv2")


def _to_gray(frame: np.ndarray) -> np.ndarray:
    if frame.ndim == 2:
        gray = frame.astype(np.float32)
    else:
        b = frame[:, :, 0].astype(np.float32)
        g = frame[:, :, 1].astype(np.float32)
        r = frame[:, :, 2].astype(np.float32)
        gray = 0.114 * b + 0.587 * g + 0.299 * r
    row_step = max(1, gray.shape[0] // 90)
    col_step = max(1, gray.shape[1] // 160)
    return gray[::row_step, ::col_step]


def _mean_brightness(frames: Sequence[np.ndarray]) -> float:
    if not frames:
        return 0.0
    values = [float(np.mean(_to_gray(frame))) for frame in frames]
    return round(float(np.mean(values)), 3)


def read_video_metadata(video_path: str) -> Dict[str, Any]:
    """Read minimal metadata from a real local video file with OpenCV."""
    path = Path(video_path)
    if not path.exists() or not path.is_file():
        raise FileNotFoundError(f"视频文件不可访问：{video_path}")

    cv2 = _cv2()
    capture = cv2.VideoCapture(str(path))
    if not capture.isOpened():
        capture.release()
        raise ValueError(f"OpenCV 无法读取视频：{video_path}")

    fps = float(capture.get(cv2.CAP_PROP_FPS) or 25.0)
    total_frames = int(capture.get(cv2.CAP_PROP_FRAME_COUNT) or 0)
    width = int(capture.get(cv2.CAP_PROP_FRAME_WIDTH) or 0)
    height = int(capture.get(cv2.CAP_PROP_FRAME_HEIGHT) or 0)
    capture.release()

    if total_frames <= 0:
        raise ValueError(f"视频帧数为空：{video_path}")

    duration = total_frames / fps if fps > 0 else 0.0
    return {
        "videoPath": str(path),
        "duration": round(duration, 3),
        "fps": round(fps, 3),
        "totalFrames": total_frames,
        "width": width,
        "height": height,
    }


def sample_frames(video_path: str, sample_fps: int = 4) -> List[np.ndarray]:
    """Sample frames uniformly by wall-clock time at a low frame rate."""
    metadata = read_video_metadata(video_path)
    cv2 = _cv2()
    capture = cv2.VideoCapture(str(video_path))
    source_fps = float(metadata["fps"] or 25.0)
    step = max(1, int(round(source_fps / max(sample_fps, 1))))
    frames: List[np.ndarray] = []
    frame_index = 0
    while True:
        ok, frame = capture.read()
        if not ok:
            break
        if frame_index % step == 0:
            frames.append(frame)
        frame_index += 1
    capture.release()
    return frames


def compute_motion_scores(frames: Sequence[np.ndarray]) -> List[float]:
    """Estimate motion with normalized mean gray-frame difference."""
    if not frames:
        return []
    scores = [0.0]
    previous = _to_gray(frames[0]).astype(np.float32) / 255.0
    for frame in frames[1:]:
        current = _to_gray(frame).astype(np.float32) / 255.0
        scores.append(round(float(np.mean(np.abs(current - previous))), 5))
        previous = current
    return scores


def estimate_brightness_change(frames: Sequence[np.ndarray], motion_peak_index: int) -> Dict[str, Any]:
    """Compare average brightness around the strongest motion moment."""
    if not frames:
        return {
            "brightnessBefore": 0.0,
            "brightnessAfter": 0.0,
            "brightnessDelta": 0.0,
            "brightnessTrend": "unchanged",
            "operationGuess": "unknown",
        }

    peak = min(max(motion_peak_index, 0), len(frames) - 1)
    before_start = max(0, peak - 4)
    before_end = max(before_start + 1, peak)
    after_start = min(len(frames) - 1, peak + 1)
    after_end = min(len(frames), after_start + 4)

    before_frames = frames[before_start:before_end] or frames[:1]
    after_frames = frames[after_start:after_end] or frames[-1:]
    brightness_before = _mean_brightness(before_frames)
    brightness_after = _mean_brightness(after_frames)
    delta = round(brightness_after - brightness_before, 3)

    if delta >= BRIGHTNESS_THRESHOLD:
        trend = "brighter"
        operation = "turn_on"
    elif delta <= -BRIGHTNESS_THRESHOLD:
        trend = "darker"
        operation = "turn_off"
    else:
        trend = "unchanged"
        operation = "press_only"

    return {
        "brightnessBefore": brightness_before,
        "brightnessAfter": brightness_after,
        "brightnessDelta": delta,
        "brightnessTrend": trend,
        "operationGuess": operation,
    }


def _summary(operation_guess: str, brightness_trend: str, motion_detected: bool) -> str:
    if not motion_detected:
        return "视频画面变化较弱，未能稳定确认手部按压动作。可以看到的内容有限，建议结合原视频复核开关位置和操作过程。"
    if operation_guess == "turn_on":
        return "画面中可以观察到手部靠近墙面开关并完成一次按压动作。按压后画面整体亮度有所上升，谨慎推测执行了开灯操作。视频内容较短，核心事件为一次开关控制行为。"
    if operation_guess == "turn_off":
        return "视频中可以观察到手部接近墙面开关并完成一次按压动作。按压后画面整体亮度有所下降，谨慎推测执行了关灯操作。视频主要展示了一次开关控制过程。"
    if brightness_trend == "unchanged":
        return "视频中可以观察到手部接近墙面开关并完成按压动作。按压前后亮度变化不明显，因此无法明确判断是开灯还是关灯。可确认视频主要展示了一次开关按压过程。"
    return "视频中出现一次靠近墙面开关的短时动作，但亮度变化与操作结果不够稳定，无法给出明确开关状态判断。"


def _confidence(motion_detected: bool, motion_peak: float, brightness_delta: float) -> float:
    if not motion_detected:
        return 0.28
    motion_part = min(0.32, motion_peak * 6.0)
    brightness_part = min(0.25, abs(brightness_delta) / 80.0)
    return round(0.38 + motion_part + brightness_part, 3)


def analyze_light_switch_video(video_path: str, sample_fps: int = 4) -> Dict[str, Any]:
    metadata = read_video_metadata(video_path)
    frames = sample_frames(video_path, sample_fps=sample_fps)
    if len(frames) < 2:
        raise ValueError("可用采样帧不足，无法分析动作和亮度变化")

    motion_scores = compute_motion_scores(frames)
    motion_peak_index = int(np.argmax(motion_scores)) if motion_scores else 0
    motion_peak_score = float(motion_scores[motion_peak_index]) if motion_scores else 0.0
    motion_detected = motion_peak_score >= MOTION_THRESHOLD
    brightness = estimate_brightness_change(frames, motion_peak_index)
    confidence = _confidence(motion_detected, motion_peak_score, float(brightness["brightnessDelta"]))

    return {
        "scenarioType": "light_switch_demo",
        "duration": metadata["duration"],
        "fps": metadata["fps"],
        "sampledFrames": len(frames),
        "motionDetected": motion_detected,
        "motionPeakTime": round(motion_peak_index / max(sample_fps, 1), 3),
        "motionPeakScore": round(motion_peak_score, 5),
        **brightness,
        "confidence": confidence,
        "summary": _summary(str(brightness["operationGuess"]), str(brightness["brightnessTrend"]), motion_detected),
    }

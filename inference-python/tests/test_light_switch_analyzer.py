import numpy as np

from app.models.light_switch_analyzer import compute_motion_scores, estimate_brightness_change
from app.services.inference_pipeline import InferencePipeline


def solid_frame(value: int) -> np.ndarray:
    return np.full((48, 64, 3), value, dtype=np.uint8)


def test_brightness_change_detects_turn_on() -> None:
    frames = [solid_frame(55), solid_frame(58), solid_frame(62), solid_frame(148), solid_frame(155), solid_frame(158)]

    result = estimate_brightness_change(frames, motion_peak_index=2)

    assert result["brightnessTrend"] == "brighter"
    assert result["operationGuess"] == "turn_on"
    assert result["brightnessDelta"] > 8


def test_brightness_change_detects_unchanged_press_only() -> None:
    frames = [solid_frame(96), solid_frame(97), solid_frame(99), solid_frame(100), solid_frame(101)]

    result = estimate_brightness_change(frames, motion_peak_index=2)

    assert result["brightnessTrend"] == "unchanged"
    assert result["operationGuess"] == "press_only"


def test_motion_scores_increase_on_frame_difference() -> None:
    frames = [solid_frame(20), solid_frame(20), solid_frame(180)]

    scores = compute_motion_scores(frames)

    assert len(scores) == 3
    assert scores[0] == 0.0
    assert scores[1] == 0.0
    assert scores[2] > 0.5


def test_light_switch_request_falls_back_when_video_missing() -> None:
    pipeline = InferencePipeline()

    result = pipeline.run("task-light-missing", "/tmp/not-found-light-switch.mp4", "请分析按开关后灯光变化")

    assert result.light_switch_analysis is None
    assert result.model_info["fallbackReason"] == "video_path_not_accessible_for_light_switch_analyzer"
    assert result.token_metrics.raw_patch_tokens_per_frame == 196
    assert result.token_metrics.compressed_tokens_per_frame == 5

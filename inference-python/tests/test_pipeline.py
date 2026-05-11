from app.services.inference_pipeline import InferencePipeline


def test_mock_pipeline_runs_end_to_end() -> None:
    pipeline = InferencePipeline()

    result = pipeline.run(
        task_id="task-pytest-mock",
        video_path="mock://pytest-demo.mp4",
        query_text="请总结视频中的关键事件",
    )

    assert result.task_id == "task-pytest-mock"
    assert result.token_metrics.raw_patch_tokens_per_frame == 196
    assert result.token_metrics.compressed_tokens_per_frame == 5
    assert result.token_metrics.compression_ratio == 196 / 5
    assert result.runtime_metrics.total_ms >= 0
    assert "视频内容摘要" in result.summary
    assert len(result.summary_chunks) >= 3

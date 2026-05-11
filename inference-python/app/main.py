from fastapi import FastAPI

from app.config import settings
from app.schemas import InferenceRequest, InferenceResult
from app.services.inference_pipeline import InferencePipeline

app = FastAPI(title="MMVS Inference Service", version="0.1.0")
pipeline = InferencePipeline(settings)


@app.get("/health")
def health() -> dict[str, object]:
    return {
        "service": settings.service_name,
        "status": "up",
        "run_mode": settings.run_mode,
        "redis_enabled": settings.redis_enabled,
        "real_model_ready": bool(settings.real_video_swin_path and settings.real_llm_path),
    }


@app.post("/infer", response_model=InferenceResult)
def infer(request: InferenceRequest) -> InferenceResult:
    return pipeline.run(request.task_id, request.video_path, request.query_text, request.scenario_type)


@app.post("/mock-infer", response_model=InferenceResult)
def mock_infer(request: InferenceRequest) -> InferenceResult:
    return pipeline.run(request.task_id, request.video_path, request.query_text, request.scenario_type)

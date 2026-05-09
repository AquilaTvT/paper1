# inference-python 推理服务

本目录是《多模态视频理解与摘要系统》的 Python FastAPI 推理服务。第 3 阶段采用 **mock-first** 设计：默认不依赖 GPU、不依赖真实大模型、不依赖真实 Video Swin 权重，但保留 real mode 的模型接口边界，便于后续接入真实算法实验。

## 技术栈

- Python 3.10+
- FastAPI
- NumPy mock 特征张量
- 可选 OpenCV 元数据读取：如果环境安装 `opencv-python-headless` 且视频路径存在，则读取真实视频元数据；否则使用 fallback mock metadata。
- Pytest 测试 Token 压缩和完整 pipeline。

## 启动方式

```bash
cd inference-python
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

健康检查：

```bash
curl http://localhost:8000/health
```

## 调用 /mock-infer

```bash
curl -X POST http://localhost:8000/mock-infer \
  -H "Content-Type: application/json" \
  -d '{
    "task_id": "task-demo-python",
    "video_path": "mock://demo.mp4",
    "query_text": "请总结视频中的关键事件，并说明 Token 压缩指标"
  }'
```

`/infer` 与 `/mock-infer` 当前都运行 mock pipeline，后续第 4 阶段可由 Java 后端通过 Redis 队列触发 Python worker。

## Mock mode 与 Real mode

### mock mode

默认模式，适合普通电脑运行完整演示：

1. `VideoPreprocessor`：读取视频元数据，默认 1 FPS 或固定 16 帧均匀抽帧。
2. `VideoSwinEncoder`：使用 NumPy 生成 `[T, 196, C]` mock 特征，其中 `196 = 14 × 14` Patch Token。
3. `DualTrackTokenCompressor`：Content Token 均值池化输出 1 个 token，Context Token 根据 query embedding mock 输出 4 个 tokens，最终 `[T, 5, C]`。
4. `ProjectionAdapter`：使用 NumPy 两层 MLP mock，将 `[T, 5, C]` 投影到 `[T, 5, 4096]`。
5. `SummaryGenerator`：根据视频元数据、用户指令和 TokenMetrics 生成中文摘要，并支持逐句流式生成。
6. `MetricsCollector`：记录 `preprocess_ms`、`encode_ms`、`compress_ms`、`project_ms`、`generate_ms`、`total_ms`。

### real mode

real mode 接口已预留，但默认不调用：

- `VideoSwinEncoder.load_real_model()` 与 `encode_real()` 用于后续加载真实 Video Swin Transformer。
- `ProjectionAdapter.build_torch_adapter()` 用于后续加载真实 PyTorch MLP Adapter。
- `real_model_interfaces.py` 定义真实 Video Swin、Projection Adapter 和 LLM Generator 的抽象接口。
- 后续可通过环境变量配置真实模型路径：
  - `MMVS_REAL_VIDEO_SWIN_PATH`
  - `MMVS_REAL_LLM_PATH`
  - `MMVS_REAL_ADAPTER_PATH`

## 测试

```bash
cd inference-python
python -m compileall app
pytest
```

核心测试：

- `tests/test_token_compressor.py`：验证 `196 → 5` 双轨 Token 压缩。
- `tests/test_pipeline.py`：验证完整 mock pipeline 可运行并生成中文摘要。

## 后续与 Java/Redis 接入

`app/worker.py` 已预留 Redis 队列消费逻辑。第 4 阶段计划：

1. Java 后端向 Redis Stream 写入任务。
2. Python worker 消费任务并调用 `InferencePipeline`。
3. `StreamPublisher` 从内存事件列表替换为 Redis Stream 发布。
4. Java 后端读取 Redis 事件并通过 SSE 转发给前端。

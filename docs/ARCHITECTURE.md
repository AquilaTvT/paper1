# 系统架构说明

本文档面向本科毕业设计论文第 4 章“系统总体设计”和第 5 章“系统详细设计”，说明当前代码仓库中各子系统的边界、调用关系和数据流。本文档只描述当前工程已经保留或实现的链路，不额外引入新的接口路径和依赖。

## 1. 总体架构

系统采用前后端分离与异步推理服务拆分的设计。用户在浏览器中操作 Vue 3 前端，Java Spring Boot 后端负责视频上传、任务创建、任务状态管理和 SSE 转发，Python FastAPI / worker 负责 mock 推理 pipeline，Redis Stream 在 Java 与 Python 之间承担任务队列、状态缓存和流式事件中转。

| 子系统 | 目录 | 技术 | 主要职责 |
| --- | --- | --- | --- |
| 前端展示层 | `frontend-vue/` | Vue 3 + Vite + TypeScript | 视频上传、用户指令输入、任务时间线、SSE 摘要展示、Token 指标展示、历史任务展示 |
| 业务后端 | `backend-java/` | Spring Boot | REST API、上传文件管理、任务创建、任务状态维护、Redis Stream 投递、SSE 事件转发 |
| 推理服务 | `inference-python/` | FastAPI / Python worker | 视频预处理、Video Swin mock encoder、Token 压缩、Projection Adapter、摘要生成、Redis 事件发布 |
| 中间件 | Redis | Redis Stream | 任务队列、任务事件流、Java 与 Python 的异步解耦 |
| 展示通道 | SSE | Server-Sent Events | Java 后端向前端推送 `status`、`stage`、`token_metrics`、`summary_delta`、`completed`、`error` 事件 |

## 2. Mermaid 架构图

```mermaid
flowchart LR
    U[用户浏览器] --> FE[frontend-vue\nVue 3 + Vite + TypeScript]
    FE -->|POST /api/videos/upload\nPOST /api/tasks\nGET /api/tasks/{taskId}/events| BE[backend-java\nSpring Boot]
    BE -->|XADD mmvs:tasks:requests| RT[(Redis task stream\n任务队列)]
    RT -->|XREAD| WK[inference-python worker]
    WK --> VP[VideoPreprocessor]
    VP --> VS[Video Swin mock encoder\n输出 T × 196 × C]
    VS --> TC[DualTrackTokenCompressor\n196 → 5]
    TC --> PA[MLP Projection Adapter]
    PA --> SG[SummaryGenerator]
    SG -->|stage/token_metrics/summary_delta/completed| RE[(Redis event stream\n流式事件)]
    RE -->|XREAD| RC[RedisStreamEventConsumer]
    RC --> SSE[Java SSE\nSseEmitterService]
    SSE --> FE
    FE --> UI[前端展示\n时间线 + 流式摘要 + Token 指标]
```

## 3. 核心调用链路

### 3.1 backend mode 全链路

1. 前端通过 `frontend-vue/src/api/taskApi.ts` 上传视频，调用 Java 后端 `POST /api/videos/upload`。
2. 前端通过 `POST /api/tasks` 创建摘要任务，Java 后端生成 `taskId` 并创建等待态任务。
3. Java 后端 `TaskDispatchService` 根据 `MMVS_INFERENCE_MODE` 和 `MMVS_REDIS_ENABLED` 判断是否启用 Redis 模式；Redis 模式下写入 `mmvs:tasks:requests`。
4. Python worker 从 Redis task stream 读取任务，调用 `InferencePipeline`。
5. `InferencePipeline` 依次执行 `VideoPreprocessor`、`VideoSwinEncoder`、`DualTrackTokenCompressor`、`ProjectionAdapter` 和 `SummaryGenerator`。
6. Python 通过 `StreamPublisher` 将阶段、Token 指标、摘要片段和完成事件写入 `mmvs:tasks:events`。
7. Java `RedisStreamEventConsumer` 消费 event stream，更新任务状态，并调用 `SseEmitterService` 推送 SSE。
8. 前端 `frontend-vue/src/api/sseClient.ts` 接收 SSE 事件，在页面中展示任务状态、流式摘要和 Token 压缩指标。

### 3.2 mock mode 与 in-memory mode

当前系统同时保留前端 mock mode 与 Java in-memory mock mode：

- 前端 mock mode：不依赖 Java、Python、Redis，主要用于论文截图和无后端环境演示。
- Java in-memory mode：Java 后端使用 `MockInferenceScheduler` 模拟任务流转，不要求启动 Redis 和 Python worker。
- Redis backend mode：走完整工程链路，适合论文第 7 章联调测试和答辩展示。

## 4. 推理 pipeline 模块

| 模块 | 当前代码位置 | 论文设计含义 |
| --- | --- | --- |
| VideoPreprocessor | `inference-python/app/models/video_preprocessor.py` | 读取视频元数据，按 1 FPS 或固定帧数进行均匀抽帧 |
| Video Swin mock encoder | `inference-python/app/models/video_swin_encoder.py` | mock 生成 `[T, 196, C]` 视觉 Patch Token，保留真实 Video Swin 接口边界 |
| DualTrackTokenCompressor | `inference-python/app/models/token_compressor.py` | 由 Content Token 与 Context Token 组成，将单帧 `196` 个 Patch Token 压缩为 `5` 个视觉 Token |
| MLP Projection Adapter | `inference-python/app/models/projection_adapter.py` | 将压缩后的视觉 Token 投影到面向语言模型的语义空间 |
| SummaryGenerator | `inference-python/app/models/summary_generator.py` | 生成中文摘要片段，支持流式输出和最终摘要拼接 |
| InferencePipeline | `inference-python/app/services/inference_pipeline.py` | 串联全部推理步骤，并发布 Redis 流式事件 |

## 5. Redis 与 SSE 事件设计

Redis 在系统中承担两个角色：

1. `mmvs:tasks:requests`：Java 后端写入任务请求，Python worker 读取后执行推理。
2. `mmvs:tasks:events`：Python worker 写入推理阶段和摘要片段，Java 后端消费后转发给前端。

SSE 由 Java 后端统一暴露为 `GET /api/tasks/{taskId}/events`，前端只需要订阅该接口，不需要直接连接 Redis 或 Python worker。这样可以降低前端复杂度，也便于在论文中说明“业务后端对异步推理服务进行了统一封装”。

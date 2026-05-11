# 论文章节与工程实现映射

本文档说明当前项目如何支撑本科毕业论文第 3 章至第 7 章写作。写论文时建议将本文档作为“工程证据清单”，从中选择接口、模块、截图和测试表格放入正文。

## 第 3 章 需求分析

第 3 章可以围绕“用户希望上传视频并获得可解释摘要”的业务场景展开，将需求拆分为功能需求、非功能需求和异常处理需求。

### 3.1 功能需求映射

| 需求 | 工程对应内容 | 可写入论文的说明 |
| --- | --- | --- |
| 视频上传 | `frontend-vue/src/components/VideoUploadPanel.vue`、`backend-java/src/main/java/com/mmvs/controller/VideoController.java` | 用户选择本地视频，后端保存文件并返回 `videoId` |
| 用户指令输入 | `frontend-vue/src/components/PromptEditor.vue`、`frontend-vue/src/api/taskApi.ts` | 用户可输入“总结关键事件”等自然语言指令，任务创建时一并提交 |
| 任务创建 | `POST /api/tasks`、`TaskController.java`、`TaskDispatchService.java` | 系统创建异步推理任务，返回 `taskId` 并进入等待状态 |
| 状态查询 | `GET /api/tasks`、`GET /api/tasks/{taskId}` | 前端可查询任务列表和单任务状态，用于历史记录与任务详情 |
| SSE 输出 | `GET /api/tasks/{taskId}/events`、`frontend-vue/src/api/sseClient.ts`、`SseEmitterService.java` | 后端以 Server-Sent Events 推送阶段变化、Token 指标和摘要片段 |
| 异常处理 | `GlobalExceptionHandler.java`、前端 `ErrorBanner.vue` | 文件类型错误、任务不存在、服务连接失败等通过统一错误提示展示 |
| 历史记录 | `frontend-vue/src/components/HistoryTable.vue`、`frontend-vue/src/composables/useLocalHistory.ts`、`GET /api/tasks` | 前端展示历史任务，支持论文截图说明用户可回看任务结果 |
| Python pipeline | `inference-python/app/services/inference_pipeline.py` | 完成视频预处理、编码、压缩、投影和摘要生成的核心流程 |

### 3.2 非功能需求映射

- 可演示性：前端 mock mode 和 Java in-memory mode 可在普通电脑上运行。
- 可扩展性：Python 侧保留真实 Video Swin、Projection Adapter 和 LLM 接口边界。
- 可维护性：前端、后端、推理服务、文档分目录管理。
- 可观测性：任务状态、阶段日志、SSE 事件和 Token 指标均可展示。

## 第 4 章 系统总体设计

第 4 章建议描述系统分层架构、模块划分、运行模式和数据流。

| 设计内容 | 工程对应内容 | 论文写作建议 |
| --- | --- | --- |
| 前端层 | `frontend-vue/` | 说明 Vue 3 + Vite + TypeScript 负责交互展示 |
| 业务服务层 | `backend-java/` | 说明 Spring Boot 提供 REST API、任务调度和 SSE 转发 |
| 推理服务层 | `inference-python/` | 说明 FastAPI / worker 负责多模态推理 pipeline |
| 中间件层 | `docker-compose.yml` 中的 Redis | 说明 Redis Stream 解耦 Java 与 Python，支持异步任务和事件流 |
| 配置文件 | `.env.example`、`backend-java/src/main/resources/application.yml` | 说明端口、Redis stream 名称、Token 参数和运行模式 |
| 项目说明 | `README.md` | 作为系统运行方式和阶段成果说明入口 |
| 架构图 | `docs/ARCHITECTURE.md` | 可直接使用 Mermaid 图绘制论文总体架构图 |

总体数据流可写为：

```text
用户浏览器 → Vue 前端 → Java REST API → Redis task stream → Python worker → Redis event stream → Java SSE → Vue 前端展示
```

## 第 5 章 系统详细设计

第 5 章建议按模块展开，重点说明输入、处理逻辑、输出和异常情况。

| 模块 / 类 | 文件路径 | 详细设计要点 |
| --- | --- | --- |
| VideoPreprocessor | `inference-python/app/models/video_preprocessor.py` | 读取视频元数据，采用 1 FPS 或固定帧数策略进行均匀抽帧 |
| VideoSwinEncoder | `inference-python/app/models/video_swin_encoder.py` | mock 输出 `[T, 196, C]`，与 Video Swin Transformer 的 Patch Token 结构保持一致 |
| DualTrackTokenCompressor | `inference-python/app/models/token_compressor.py` | 构造 Content Token 与 Context Token，将单帧 `196` 个视觉 Token 压缩为 `5` 个 |
| ProjectionAdapter | `inference-python/app/models/projection_adapter.py` | 以 MLP Projection Adapter 的形式把视觉表示投影到语言模型输入空间 |
| SummaryGenerator | `inference-python/app/models/summary_generator.py` | 生成中文摘要片段，并支持流式输出 |
| InferencePipeline | `inference-python/app/services/inference_pipeline.py` | 串联预处理、编码、压缩、投影和生成，并发布事件 |
| TaskDispatchService | `backend-java/src/main/java/com/mmvs/service/TaskDispatchService.java` | 根据运行模式决定使用 Redis Stream 还是 in-memory mock scheduler |
| RedisStreamEventConsumer | `backend-java/src/main/java/com/mmvs/service/RedisStreamEventConsumer.java` | 消费 Redis event stream，更新任务状态并转发 SSE |
| SseEmitterService | `backend-java/src/main/java/com/mmvs/service/SseEmitterService.java` | 管理前端 SSE 连接，向指定任务订阅者发送事件 |
| TaskController | `backend-java/src/main/java/com/mmvs/controller/TaskController.java` | 提供任务创建、任务查询和事件订阅接口 |

Token 压缩模块可作为第 5 章重点算法设计：

```text
输入：Video Swin mock encoder 输出的 [T, 196, C]
处理：每帧生成 1 个 Content Token 和 4 个 Context Token
输出：[T, 5, C]
压缩比：196 / 5 = 39.2
```

## 第 6 章 系统代码实现

第 6 章可以选择少量关键代码进行讲解，不建议全文粘贴所有源码。适合展示的关键文件如下。

### 6.1 前端关键代码

| 文件 | 展示重点 |
| --- | --- |
| `frontend-vue/src/components/VideoUploadPanel.vue` | 视频上传 UI、文件选择、上传状态 |
| `frontend-vue/src/components/StreamingSummary.vue` | 摘要片段逐段展示 |
| `frontend-vue/src/components/TokenCompressionCard.vue` | `196 → 5` Token 压缩指标展示 |
| `frontend-vue/src/api/taskApi.ts` | 上传视频、创建任务、查询任务接口封装 |
| `frontend-vue/src/api/sseClient.ts` | EventSource 订阅 SSE 事件 |

### 6.2 Java 后端关键代码

| 文件 | 展示重点 |
| --- | --- |
| `backend-java/src/main/java/com/mmvs/controller/TaskController.java` | `POST /api/tasks`、`GET /api/tasks/{taskId}`、SSE 订阅接口 |
| `backend-java/src/main/java/com/mmvs/service/TaskDispatchService.java` | Redis task stream 投递与 in-memory 模式切换 |
| `backend-java/src/main/java/com/mmvs/service/RedisStreamEventConsumer.java` | Redis event stream 消费、任务状态更新、SSE 转发 |

### 6.3 Python 推理关键代码

| 文件 | 展示重点 |
| --- | --- |
| `inference-python/app/services/inference_pipeline.py` | 推理 pipeline 主流程 |
| `inference-python/app/models/token_compressor.py` | 双轨 Token 压缩核心实现 |
| `inference-python/app/models/video_swin_encoder.py` | Video Swin mock encoder 输出 `[T, 196, C]` |
| `inference-python/app/models/projection_adapter.py` | MLP Projection Adapter mock 实现 |
| `inference-python/app/models/summary_generator.py` | 中文摘要流式生成 |

## 第 7 章 系统测试

第 7 章建议由测试环境、测试方法、测试用例、测试结果和结果分析组成。

### 7.1 测试表格来源

可直接参考 `docs/TEST_PLAN.md`，至少覆盖以下类别：

1. 功能测试。
2. 接口测试。
3. 文件上传异常测试。
4. 任务状态流转测试。
5. SSE 流式输出测试。
6. Token 压缩测试。
7. Python 推理 pipeline 测试。
8. Redis 异步链路测试。
9. 前端 mock mode 测试。
10. backend mode 联调测试。
11. 摘要样例质量测试。
12. 资源估算测试。

### 7.2 截图建议

截图可参考 `docs/SCREENSHOT_GUIDE.md`。第 7 章至少建议放入：

- 系统首页截图。
- 视频上传与用户指令截图。
- 任务状态时间线截图。
- SSE 流式摘要输出截图。
- Token 压缩 `196 → 5` 截图。
- 接口测试截图。
- Redis / Java / Python / Vue 终端启动截图。
- Python 测试通过截图。

### 7.3 Token 压缩结果展示

论文中可以写：系统模拟 Video Swin Transformer 输出每帧 `196` 个 Patch Token，经过双轨 Token 压缩模块后，每帧保留 `5` 个视觉 Token。因此单帧压缩比为：

```text
compressionRatio = rawPatchTokensPerFrame / compressedTokensPerFrame = 196 / 5 = 39.2
```

该结果可在前端 `TokenCompressionCard`、SSE `token_metrics` 事件、Python `/mock-infer` 响应和测试表格中同时展示。

### 7.4 SSE 流式输出展示

论文中可以描述：任务执行期间，Python worker 持续发布阶段事件和摘要片段，Java 后端通过 SSE 转发给前端。前端不需要轮询即可实时展示摘要生成过程，体现了系统的交互性和可观测性。

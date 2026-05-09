# 多模态视频理解与摘要系统工程规格书

## 1. 项目定位

本项目是本科毕业设计《多模态视频理解与摘要系统设计》的工程实现仓库。系统采用前后端分离、业务层与计算层分离的 monorepo 架构，用于完成视频上传、任务创建、异步推理、状态查询、SSE 流式输出、摘要展示、Token 压缩指标展示和测试分析。

本阶段为第 0 阶段，仅建立工程规格、接口契约、数据模型、论文映射和后续实施计划，不编写大规模业务代码。后续实现必须以本文档为工程基线。

## 2. 总体目标

系统需要满足以下目标：

1. 支持用户通过前端上传视频或选择示例视频。
2. 支持用户输入摘要指令，例如“总结视频中的关键事件”“列出人物行为变化”。
3. Java Spring Boot 业务服务负责用户请求、文件元数据、任务生命周期、SSE 转发、结果查询和测试指标展示。
4. Python FastAPI 推理服务负责视频预处理、特征提取、Token 压缩、适配器投影、摘要生成和推理结果回写。
5. Redis 负责异步任务队列、任务状态缓存和流式消息中转。
6. 默认 mock mode 可在普通电脑运行完整演示，不依赖 GPU、真实 LLM 或真实 Video Swin 权重。
7. real mode 保留真实 Video Swin Transformer、LLM、QLoRA 和 MLP Projection Adapter 的接入接口。
8. 测试与展示页面必须体现单帧视觉 Patch Token 从 196 个压缩到 5 个视觉 Token。
9. 工程结构清晰，禁止把所有逻辑堆在一个文件中。
10. 工程产物能够支撑论文第 3 章至第 7 章写作、截图和测试分析。

## 3. Monorepo 目录结构

仓库根目录采用以下结构：

```text
paper1/
├── frontend-vue/
│   ├── src/
│   │   ├── api/
│   │   ├── components/
│   │   ├── views/
│   │   ├── stores/
│   │   ├── router/
│   │   └── styles/
│   ├── public/
│   ├── package.json
│   └── vite.config.ts
├── backend-java/
│   ├── src/main/java/
│   │   └── com/example/paper1/
│   │       ├── controller/
│   │       ├── service/
│   │       ├── domain/
│   │       ├── repository/
│   │       ├── redis/
│   │       ├── sse/
│   │       ├── config/
│   │       └── exception/
│   ├── src/main/resources/
│   └── pom.xml
├── inference-python/
│   ├── app/
│   │   ├── api/
│   │   ├── workers/
│   │   ├── pipeline/
│   │   ├── models/
│   │   ├── adapters/
│   │   ├── schemas/
│   │   ├── config/
│   │   └── utils/
│   ├── tests/
│   ├── requirements.txt
│   └── pyproject.toml
├── docs/
│   ├── PROJECT_SPEC.md
│   ├── IMPLEMENTATION_PLAN.md
│   ├── API_CONTRACT.md
│   ├── DATA_MODEL.md
│   └── THESIS_MAPPING.md
├── scripts/
│   ├── dev-up.sh
│   ├── dev-down.sh
│   └── seed-demo-data.sh
├── docker-compose.yml
├── .env.example
└── README.md
```

> 说明：本阶段文档明确目标结构。后续阶段按需逐步补齐源代码、脚本、Docker 配置和示例数据。

## 4. 架构分层

### 4.1 前端层：`frontend-vue/`

技术栈建议：Vue 3、Vite、TypeScript、Pinia、Vue Router、Element Plus 或 Naive UI。

职责：

- 视频上传与示例视频选择。
- 用户指令输入。
- 任务创建与任务列表展示。
- 任务状态展示。
- SSE 流式摘要渲染。
- 推理结果、Token 压缩指标和测试结果展示。
- 为论文截图提供清晰页面：上传页、任务页、流式摘要页、指标页、历史结果页。

### 4.2 业务服务层：`backend-java/`

技术栈要求：Java Spring Boot。

职责：

- 暴露统一 REST API。
- 接收和保存视频文件。
- 创建推理任务并写入 Redis 队列。
- 维护任务状态、任务日志和结果索引。
- 从 Redis Stream 或 Pub/Sub 读取流式消息并通过 SSE 转发给前端。
- 查询历史任务和推理结果。
- 对外屏蔽 Python 推理服务内部细节。
- 提供异常处理、参数校验、日志记录和健康检查。

### 4.3 推理计算层：`inference-python/`

技术栈要求：Python FastAPI。

职责：

- 提供健康检查和推理服务管理接口。
- 消费 Redis 任务队列。
- 执行视频预处理、特征提取、Token 压缩、适配器投影和摘要生成。
- 将任务状态、流式 token/句子、指标和最终结果写回 Redis。
- 同时支持 mock mode 与 real mode。
- real mode 下保留 Video Swin Transformer、LLM、QLoRA、MLP Projection Adapter 的真实模型接入点。

### 4.4 中间件层：Redis

职责：

- 异步任务队列：Java 后端写入任务，Python worker 消费任务。
- 状态缓存：保存任务当前状态、进度、错误信息和时间戳。
- 流式消息中转：Python 推理服务写入增量摘要消息，Java 后端通过 SSE 转发。
- 结果缓存：保存摘要结果、TokenMetrics 和测试指标，供 Java 后端查询。

## 5. 系统模块

### 5.1 视频上传模块

- 前端提供上传控件，限制文件类型为常见视频格式，如 MP4、MOV、AVI、MKV。
- Java 后端负责 multipart 文件接收、大小校验、存储路径生成、文件元数据登记。
- 上传成功后返回 `videoId`，后续任务创建必须引用该 `videoId`。
- 默认允许使用示例视频，方便论文演示和自动化测试。

### 5.2 任务管理模块

- 根据 `videoId`、用户指令、运行模式和参数创建 `InferenceTask`。
- 任务创建后初始状态为 `waiting`。
- 任务状态必须遵循：`waiting` → `running` → `streaming` → `finished`。
- 异常时进入 `failed`，用户取消时进入 `cancelled`。
- 任务列表支持按创建时间倒序查询。

### 5.3 Redis 队列模块

- 推荐使用 Redis Stream 实现任务队列和流式消息队列。
- 任务队列 key 建议：`paper1:queue:tasks`。
- 状态缓存 key 建议：`paper1:task:{taskId}:state`。
- 流式消息 key 建议：`paper1:task:{taskId}:events`。
- 结果缓存 key 建议：`paper1:task:{taskId}:result`。
- 指标缓存 key 建议：`paper1:task:{taskId}:metrics`。

### 5.4 SSE 流式输出模块

- Java 后端提供 `GET /api/tasks/{taskId}/events`。
- 前端使用 `EventSource` 订阅。
- SSE 事件至少包含：`status`、`delta`、`metrics`、`result`、`error`、`done`。
- 推理服务不直接暴露给浏览器，所有流式消息由 Java 后端转发。

### 5.5 视频预处理模块

- mock mode：读取视频元信息或使用固定示例元信息，生成可复现的帧采样记录。
- real mode：执行视频解码、帧采样、尺寸归一化、归一化张量构建。
- 预处理输出包括：帧数、采样帧索引、采样 FPS、输入分辨率和处理耗时。

### 5.6 Video Swin 特征提取模块

- mock mode：生成固定维度的模拟视觉特征，保证演示输出稳定。
- real mode：加载 Video Swin Transformer 权重，对采样帧提取时空视觉特征。
- 模块输出应记录原始视觉 patch token 数量，单帧默认为 196。

### 5.7 双轨 Token 压缩模块

双轨 Token 压缩由 Content Token 与 Context Token 两部分组成：

- Content Token：保留与视频主体内容、动作、场景变化最相关的视觉信息。
- Context Token：保留时间上下文、场景关联和用户指令相关信息。
- 单帧视觉 Patch Token 默认从 196 个压缩为 5 个视觉 Token。
- 指标必须进入 `TokenMetrics`，用于前端展示和论文测试分析。

### 5.8 MLP Projection Adapter 模块

- 将压缩后的视觉 Token 映射到摘要生成模型可接收的语义空间。
- mock mode：使用确定性规则生成文本侧提示片段或低维模拟向量描述。
- real mode：加载训练好的 MLP Projection Adapter 参数，将视觉特征投影到 LLM embedding 或对齐空间。
- 保留 QLoRA 微调模型加载配置，用于后续真实模型实验。

### 5.9 摘要生成模块

- 输入包括用户指令、视频元信息、压缩视觉 Token 表示和上下文信息。
- mock mode：按固定模板逐句生成摘要，并通过 Redis 发送流式消息。
- real mode：调用真实 LLM 或多模态生成模型，逐 token 或逐句回传。
- 最终输出包括摘要正文、关键事件、时间线、置信度说明和模型运行模式。

### 5.10 结果展示模块

- 前端展示任务状态、流式摘要、最终摘要、关键事件、Token 压缩指标、运行耗时和错误信息。
- 历史结果页面支持查看以往任务。
- 测试指标区域必须显示 `196 → 5`、压缩率、耗时和模式标识。

### 5.11 日志与异常处理模块

- Java 后端记录请求日志、任务状态变更、SSE 连接生命周期和异常。
- Python 推理服务记录 worker 启停、任务消费、流水线阶段耗时和异常。
- 任务失败时必须写入 `TaskLog` 和 `InferenceTask.errorMessage`。
- 前端对失败、取消、连接断开和重试给出明确提示。

## 6. 运行模式

### 6.1 mock mode

默认运行模式，目标是让普通电脑完成完整演示：

- 不依赖 GPU。
- 不依赖真实 LLM。
- 不依赖真实 Video Swin 权重。
- 使用固定或可配置的模拟特征和摘要模板。
- 必须走完整工程链路：前端 → Java 后端 → Redis → Python worker → Redis → Java SSE → 前端。
- 必须产生可展示指标：单帧 `196` patch tokens 压缩到 `5` visual tokens。

### 6.2 real mode

真实实验模式，目标是保留论文算法实现和后续扩展能力：

- 支持加载 Video Swin Transformer 权重。
- 支持加载 LLM 或多模态生成模型。
- 支持 QLoRA 微调权重配置。
- 支持 MLP Projection Adapter 参数加载。
- 允许通过 `.env` 或配置文件切换模型路径、设备、精度和批大小。
- real mode 不作为普通电脑默认验收条件，但接口和模块边界必须保留。

## 7. 任务状态

系统统一使用以下状态字符串：

| 状态 | 含义 | 典型触发方 |
| --- | --- | --- |
| `waiting` | 任务已创建并等待 worker 消费 | Java 后端 |
| `running` | worker 已领取任务并开始预处理或特征提取 | Python 推理服务 |
| `streaming` | 摘要正在通过 SSE 增量输出 | Python 推理服务 |
| `finished` | 任务成功完成，结果可查询 | Python 推理服务 |
| `failed` | 任务执行失败 | Java 后端或 Python 推理服务 |
| `cancelled` | 用户或系统取消任务 | Java 后端 |

## 8. 核心接口

接口契约以 `docs/API_CONTRACT.md` 为准，必须至少包含：

- `POST /api/videos/upload`
- `POST /api/tasks`
- `GET /api/tasks`
- `GET /api/tasks/{taskId}`
- `GET /api/tasks/{taskId}/events`
- `GET /api/results/{taskId}`
- `GET /api/health`

## 9. 核心数据模型

数据模型以 `docs/DATA_MODEL.md` 为准，必须至少包含：

- `VideoFile`
- `InferenceTask`
- `InferenceResult`
- `TaskLog`
- `StreamMessage`
- `TokenMetrics`

## 10. 验收标准

第 0 阶段文档验收：

- 已创建 `docs/PROJECT_SPEC.md`。
- 已创建 `docs/IMPLEMENTATION_PLAN.md`。
- 已创建 `docs/API_CONTRACT.md`。
- 已创建 `docs/DATA_MODEL.md`。
- 已创建 `docs/THESIS_MAPPING.md`。
- 已更新 `README.md`。
- 文档明确 monorepo 结构、模块划分、接口契约、任务状态、数据模型、运行模式和验收标准。

最终系统验收：

- 可以本地启动前端、Java 后端、Python 推理服务和 Redis。
- 可以上传视频或使用示例视频。
- 可以创建任务。
- 可以看到 `waiting → running → streaming → finished` 的状态流转。
- 可以看到 SSE 风格的逐字或逐句摘要输出。
- 可以看到 Token 压缩指标 `196 → 5`。
- 可以看到历史任务和测试结果。
- 可以用于论文截图和第六章代码说明。

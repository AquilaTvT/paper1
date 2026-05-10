# 多模态视频理解与摘要系统

本科毕业设计项目：《多模态视频理解与摘要系统设计》。本仓库采用 monorepo 管理前端、Java 业务后端、Python 推理服务、工程文档和启动脚本。

## 当前阶段

当前完成第 0 阶段：工程规格书与实施文档。此阶段不编写大量业务代码，重点是明确系统边界、目录结构、接口契约、数据模型、运行模式、验收标准和论文映射。

## 系统目标

系统最终需要支持：

- 视频上传或示例视频选择。
- 用户摘要指令输入。
- 异步推理任务创建。
- 任务状态查询。
- SSE 风格流式摘要输出。
- 最终摘要和关键事件展示。
- Token 压缩指标展示，核心指标为单帧视觉 Patch Token `196 → 5`。
- 历史任务和测试结果展示。
- mock mode 普通电脑完整演示。
- real mode 保留真实 Video Swin Transformer、LLM、QLoRA 和 MLP Projection Adapter 接入接口。

## 目标技术栈

| 层级 | 目录 | 技术 | 职责 |
| --- | --- | --- | --- |
| 前端 | `frontend-vue/` | Vue 3、Vite、TypeScript | 上传、任务管理、SSE 展示、指标展示 |
| 业务服务 | `backend-java/` | Java Spring Boot | REST API、文件管理、任务管理、Redis、SSE 转发 |
| 推理服务 | `inference-python/` | Python FastAPI | 视频预处理、特征提取、Token 压缩、摘要生成 |
| 中间件 | Redis | Redis Stream/缓存 | 任务队列、状态缓存、流式消息中转 |
| 文档 | `docs/` | Markdown | 规格、契约、模型、论文映射 |
| 脚本 | `scripts/` | Shell | 本地启动、停止、种子数据 |

## 目标目录结构

```text
paper1/
├── frontend-vue/
├── backend-java/
├── inference-python/
├── docs/
│   ├── PROJECT_SPEC.md
│   ├── IMPLEMENTATION_PLAN.md
│   ├── API_CONTRACT.md
│   ├── DATA_MODEL.md
│   └── THESIS_MAPPING.md
├── scripts/
├── docker-compose.yml
├── .env.example
└── README.md
```

## 核心模块

- 视频上传模块。
- 任务管理模块。
- Redis 队列模块。
- SSE 流式输出模块。
- 视频预处理模块。
- Video Swin 特征提取模块。
- 双轨 Token 压缩模块。
- MLP Projection Adapter 模块。
- 摘要生成模块。
- 结果展示模块。
- 日志与异常处理模块。

## 核心 API

后续 Java Spring Boot 业务服务至少提供以下接口：

- `POST /api/videos/upload`
- `POST /api/tasks`
- `GET /api/tasks`
- `GET /api/tasks/{taskId}`
- `GET /api/tasks/{taskId}/events`
- `GET /api/results/{taskId}`
- `GET /api/health`

详细契约见 [`docs/API_CONTRACT.md`](docs/API_CONTRACT.md)。

## 任务状态

系统统一使用以下任务状态：

- `waiting`
- `running`
- `streaming`
- `finished`
- `failed`
- `cancelled`

标准成功流转为：

```text
waiting → running → streaming → finished
```

## 运行模式

### mock mode

默认演示模式：

- 不依赖 GPU。
- 不依赖真实 LLM。
- 不依赖真实 Video Swin 权重。
- 普通电脑可以运行完整演示。
- 必须走完整链路：前端 → Java 后端 → Redis → Python worker → Redis → Java SSE → 前端。
- 必须展示 Token 压缩指标 `196 → 5`。

### real mode

真实模型模式：

- 保留真实 Video Swin Transformer 接入接口。
- 保留真实 LLM 接入接口。
- 保留 QLoRA 权重加载配置。
- 保留 MLP Projection Adapter 参数加载接口。
- 通过 `.env` 或配置文件切换模型路径、设备、精度和批大小。

## 文档入口

- [工程规格书](docs/PROJECT_SPEC.md)
- [实施计划](docs/IMPLEMENTATION_PLAN.md)
- [API 契约](docs/API_CONTRACT.md)
- [数据模型设计](docs/DATA_MODEL.md)
- [论文章节映射](docs/THESIS_MAPPING.md)

## 最终验收目标

项目完成后应满足：

- 可以本地启动前端、Java 后端、Python 推理服务和 Redis。
- 可以上传视频或使用示例视频。
- 可以创建任务。
- 可以看到 `waiting → running → streaming → finished` 的状态流转。
- 可以看到 SSE 风格的逐字或逐句摘要输出。
- 可以看到 Token 压缩指标 `196 → 5`。
- 可以看到历史任务和测试结果。
- 可以用于论文截图和第六章代码说明。

## 前端本地启动

第 1 阶段已创建 `frontend-vue/`，用于演示 Vue 3 + Vite + TypeScript 的 mock 前端闭环。启动步骤：

```bash
cd frontend-vue
npm install
npm run dev
npm run build
```

说明：

- `npm run dev` 启动 Vite 开发服务器，默认端口为 `5173`。
- `npm run build` 执行 TypeScript 类型检查并生成生产构建。
- 当前前端默认使用 mock mode，不依赖 Java 后端、Python 推理服务、Redis、GPU 或真实模型权重。
- API 层已经预留 `taskApi.ts` 和 `sseClient.ts`，后续可对接 Java Spring Boot 的 REST/SSE 接口。

## Testing

当前 Codex 云环境在访问 npm registry 时，对 `@vitejs/plugin-vue` 返回 `403 Forbidden`，因此云端未能完成依赖安装，`npm run build` 也因 `vue-tsc` 未安装而无法真正执行完成。本次已通过静态方式检查 `package.json` 精确版本、前端文件结构、相对 import 路径和 mock 流程关键字段。

本地最终验证请运行：

```bash
cd frontend-vue
npm install
npm run build
```

## 后端本地启动

第 2 阶段已创建 `backend-java/`，使用 Java 17、Spring Boot 3.x 和 Maven 实现 in-memory mock mode 后端服务。当前后端不依赖 Redis、数据库或 Python 推理服务即可运行完整 mock 任务流转。

启动与构建：

```bash
cd backend-java
mvn spring-boot:run
mvn package
```

核心接口：

- `GET /api/health`：服务健康检查。
- `POST /api/videos/upload`：上传视频，支持 `mp4`、`mov`、`avi`、`mkv`。
- `POST /api/tasks`：基于 `videoId` 和 `queryText` 创建摘要任务。
- `GET /api/tasks`：查询任务列表。
- `GET /api/tasks/{taskId}`：查询任务详情、日志和 Token 指标。
- `GET /api/tasks/{taskId}/events`：订阅 SSE 任务事件。
- `GET /api/results/{taskId}`：查询最终摘要和测试指标。

与 `frontend-vue` 联调：

1. 启动后端：`cd backend-java && mvn spring-boot:run`。
2. 启动前端：`cd frontend-vue && npm run dev`。
3. Vite 已将 `/api` 代理到 `http://localhost:8080`，后续可将前端 mock API 切换为真实 `taskApi.ts` 和 `sseClient.ts`。

当前说明：

- 后端默认是 in-memory mock mode，上传文件元数据、任务、日志和结果保存在内存中。
- `MockInferenceScheduler` 暂不调用 Python 服务，会自动模拟 `waiting → running → streaming → finished`。
- 下一阶段再接入 Redis 队列、Python FastAPI 推理服务以及可选 H2/JPA 持久化。

## 推理服务本地启动

第 3 阶段已创建 `inference-python/`，使用 Python 3.10+、FastAPI 和 NumPy 实现 mock-first 推理链路。默认 mock mode 不依赖 GPU、真实大模型或真实 Video Swin 权重。

启动与测试：

```bash
cd inference-python
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
python -m compileall app
pytest
```

核心接口：

- `GET /health`：推理服务健康检查。
- `POST /infer`：输入 `video_path`、`query_text`、`task_id`，返回完整推理结果。
- `POST /mock-infer`：直接运行 mock 推理流程，便于本地测试。

当前说明：

- `InferencePipeline` 串联视频预处理、Video Swin mock 编码、双轨 Token 压缩、MLP Projection Adapter 和中文摘要生成。
- `DualTrackTokenCompressor` 保证单帧 `196` 个 Patch Token 压缩为 `5` 个视觉 Token。
- `worker.py` 和 `StreamPublisher` 已预留 Redis 队列与流式事件发布边界，下一阶段与 Java 后端和 Redis 接入。

## 第 4 阶段：Redis 全链路联调

本阶段在既有前端、Java 后端和 Python 推理服务基础上做增量接入，保留原有 in-memory/mock mode，同时新增 Redis Stream 异步链路：

```text
Vue backend mode → Java REST → Redis task stream → Python worker → Redis event stream → Java SSE → Vue
```

新增统一事件字段：`taskId`、`eventType`、`status`、`stage`、`tokenMetrics`、`summaryDelta`、`completed`、`error`。Python 内部仍可使用 snake_case，但发布到 Redis Stream 的事件字段使用 camelCase，Java 读取后按原 `GET /api/tasks/{taskId}/events` SSE 接口转发给前端。

### Redis 联调启动顺序

1. 启动 Redis：

```bash
docker compose up -d redis
```

2. 启动 Java 后端 Redis mode：

```bash
cd backend-java
MMVS_INFERENCE_MODE=redis MMVS_REDIS_ENABLED=true mvn spring-boot:run
```

3. 启动 Python worker：

```bash
cd inference-python
pip install -r requirements.txt
MMVS_REDIS_ENABLED=true python -m app.worker
```

4. 启动 Vue backend mode：

```bash
cd frontend-vue
npm install
VITE_API_MODE=backend VITE_API_BASE_URL=http://localhost:8080/api npm run dev
```

Windows 可参考 `scripts/start-dev.bat`，macOS/Linux 可参考 `scripts/start-dev.sh` 中的分步说明。

### 第 4 阶段测试步骤

- 打开前端页面，选择本地视频上传；backend mode 会先调用 `POST /api/videos/upload` 获取 Java 后端生成的 `videoId`。
- 输入摘要指令并创建任务；Java 调用 `POST /api/tasks` 后写入 Redis task stream。
- Python worker 消费任务并发布 `stage`、`token_metrics`、`summary_delta`、`completed` 或 `error` 事件到 Redis event stream。
- Java 后端消费 Redis event stream，更新内存任务状态，并通过 `GET /api/tasks/{taskId}/events` SSE 转发给 Vue。
- 前端应看到 `waiting → running → streaming → finished` 流转、摘要增量输出和 `196 → 5` Token 压缩指标。

保留原有模式：不设置 `MMVS_INFERENCE_MODE=redis` 时 Java 继续使用内存 mock scheduler；不设置 `VITE_API_MODE=backend` 时 Vue 继续使用浏览器 mock mode。

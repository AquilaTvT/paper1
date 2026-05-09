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

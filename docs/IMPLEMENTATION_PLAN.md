# 多模态视频理解与摘要系统实施计划

## 1. 阶段划分

项目按“先工程骨架、再业务闭环、后算法增强”的顺序实施。每一阶段都必须保证代码结构清晰、接口稳定、可运行、可测试，并能沉淀论文截图或说明材料。

## 2. 第 0 阶段：工程规格阶段

### 目标

建立项目工程基线，明确系统边界、目录结构、接口契约、数据模型、运行模式、验收标准和论文映射。

### 交付物

- `docs/PROJECT_SPEC.md`：总体工程规格书。
- `docs/IMPLEMENTATION_PLAN.md`：实施计划。
- `docs/API_CONTRACT.md`：REST API 与 SSE 契约。
- `docs/DATA_MODEL.md`：核心数据模型。
- `docs/THESIS_MAPPING.md`：论文章节映射。
- `README.md`：项目入口说明。

### 验收标准

- 文档能够指导后续编码。
- 明确 `frontend-vue/`、`backend-java/`、`inference-python/`、`docs/`、`scripts/`、`docker-compose.yml`、`.env.example` 的定位。
- 明确 mock mode 与 real mode。
- 明确核心指标 `196 → 5`。

## 3. 第 1 阶段：工程骨架与本地启动

### 目标

创建可启动的三端工程骨架和 Redis 编排环境。

### 主要任务

1. 在 `frontend-vue/` 初始化 Vue 3 + Vite + TypeScript 项目。
2. 在 `backend-java/` 初始化 Spring Boot 项目，包含 Web、Validation、Redis、SSE、测试依赖。
3. 在 `inference-python/` 初始化 FastAPI 项目，包含 Redis 客户端、配置管理、日志、测试依赖。
4. 创建 `docker-compose.yml`，至少包含 Redis 服务，后续可加入 Java、Python 和前端服务。
5. 创建 `.env.example`，覆盖端口、Redis 地址、上传目录、运行模式和模型路径。
6. 创建 `scripts/dev-up.sh`、`scripts/dev-down.sh` 和基础检查脚本。

### 验收标准

- Redis 可通过 Docker Compose 启动。
- Java 后端 `GET /api/health` 返回正常。
- Python 推理服务健康检查返回正常。
- 前端开发服务器可以打开首页。
- README 提供本地启动步骤。

## 4. 第 2 阶段：视频上传与任务管理闭环

### 目标

实现视频上传、任务创建、任务列表和任务详情查询。

### 主要任务

1. Java 后端实现 `POST /api/videos/upload`。
2. Java 后端实现 `POST /api/tasks`，创建任务并写入 Redis 队列。
3. Java 后端实现 `GET /api/tasks` 和 `GET /api/tasks/{taskId}`。
4. 前端实现上传页面、任务创建表单、任务列表页和任务详情页。
5. 后端记录 `VideoFile`、`InferenceTask` 和 `TaskLog`。
6. 使用 mock 数据或内存存储完成早期闭环，随后可替换为数据库。

### 验收标准

- 用户可以上传视频并获得 `videoId`。
- 用户可以基于 `videoId` 创建任务。
- 新任务状态为 `waiting`。
- 任务列表可看到新建任务。

## 5. 第 3 阶段：Redis 队列与 Python Worker

### 目标

实现 Java 后端与 Python 推理服务通过 Redis 解耦的异步处理链路。

### 主要任务

1. Java 后端向 `paper1:queue:tasks` 写入任务消息。
2. Python worker 消费任务队列。
3. Python worker 将状态写入 `paper1:task:{taskId}:state`。
4. Java 后端读取 Redis 状态并更新任务视图。
5. 实现任务失败处理和错误日志回写。

### 验收标准

- 任务可以从 `waiting` 进入 `running`。
- worker 异常时任务进入 `failed`。
- 任务日志可以记录关键状态变更。

## 6. 第 4 阶段：SSE 流式摘要闭环

### 目标

实现流式消息从 Python 推理服务到前端页面的完整链路。

### 主要任务

1. Python 推理服务将流式摘要消息写入 Redis。
2. Java 后端实现 `GET /api/tasks/{taskId}/events`。
3. 前端使用 `EventSource` 订阅任务事件。
4. 页面实时展示状态、摘要增量、指标和结束事件。
5. 处理 SSE 断开、重连和任务结束后的资源清理。

### 验收标准

- 页面可以看到 `waiting → running → streaming → finished`。
- 页面可以看到逐字或逐句摘要输出。
- 连接结束时显示最终摘要入口。

## 7. 第 5 阶段：mock mode 算法流水线

### 目标

实现不依赖 GPU 和真实权重的完整演示流水线。

### 主要任务

1. 实现视频预处理 mock 模块，输出帧采样信息。
2. 实现 Video Swin 特征提取 mock 模块，输出模拟视觉 token 统计。
3. 实现双轨 Token 压缩 mock 模块，稳定输出 `196 → 5`。
4. 实现 MLP Projection Adapter mock 模块，输出可解释的投影摘要输入。
5. 实现摘要生成 mock 模块，通过 Redis 逐句输出。
6. 将耗时、压缩率和模式信息写入 `TokenMetrics`。

### 验收标准

- 普通电脑无需 GPU 即可跑完整流程。
- 前端指标区域显示单帧 Patch Token `196`、压缩视觉 Token `5` 和压缩率。
- 最终结果包含摘要、关键事件和测试指标。

## 8. 第 6 阶段：real mode 接口与模型接入点

### 目标

保留并验证真实模型接入接口，使论文算法流程具备工程可扩展性。

### 主要任务

1. 定义 Video Swin 特征提取器接口，并提供 mock 与 real 两种实现。
2. 定义 Token 压缩器接口，并提供参数化配置。
3. 定义 MLP Projection Adapter 加载接口。
4. 定义 LLM 摘要生成器接口，支持本地模型或远程模型服务。
5. 在 `.env.example` 中提供模型路径、设备、精度、批大小、QLoRA 路径配置。
6. 在文档中说明 real mode 的环境要求和降级策略。

### 验收标准

- `RUN_MODE=mock` 不加载真实模型。
- `RUN_MODE=real` 会检查模型路径和设备配置。
- real mode 入口存在清晰错误提示，不影响 mock mode 演示。

## 9. 第 7 阶段：测试分析与论文材料整理

### 目标

为论文第 6 章和第 7 章准备截图、测试用例、测试结果和分析材料。

### 主要任务

1. 编写接口测试、单元测试和关键链路集成测试。
2. 增加测试指标页面，展示任务耗时、状态流转和 Token 压缩结果。
3. 准备演示视频和种子数据。
4. 记录典型测试用例：上传、创建任务、SSE 输出、失败任务、历史查询。
5. 整理论文章节截图清单。

### 验收标准

- README 包含演示步骤。
- 测试页可以展示历史任务和测试结果。
- 论文可引用系统页面、接口契约、数据模型和关键代码结构。

## 10. 风险与约束

| 风险 | 影响 | 应对策略 |
| --- | --- | --- |
| 本地无 GPU | 无法运行真实模型 | mock mode 作为默认验收模式 |
| 真实模型权重大 | 下载和部署困难 | 保留接口，后续按需接入 |
| SSE 连接不稳定 | 前端流式展示中断 | 支持重连和结果兜底查询 |
| Redis 消息丢失 | 状态不一致 | 关键状态写入缓存和任务日志 |
| 工期有限 | 难以完成复杂训练 | 优先完成工程闭环和可解释 mock 算法 |

## 11. 后续编码原则

- 控制器、服务、模型、队列、SSE、算法模块分层实现。
- 避免单文件堆叠业务逻辑。
- 每个核心模块必须有清晰输入输出和错误处理。
- mock mode 与 real mode 通过接口抽象隔离。
- 文档、接口契约和数据模型变更必须同步更新。

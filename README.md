# 多模态视频理解与摘要系统

本科毕业设计项目：《多模态视频理解与摘要系统设计》。本仓库采用 monorepo 管理 Vue 前端、Java Spring Boot 业务后端、Python FastAPI 推理服务、Redis 异步链路、工程文档和便捷检查脚本。

## 当前阶段

当前完成第 6 阶段：最终工程化自查、运行稳定性修复与答辩发布版本整理。项目定位为 release candidate，优先保证本地稳定运行、论文截图、接口测试和答辩演示；不改变既有系统架构，不引入重型依赖。

## 项目结构

```text
paper1/
├── frontend-vue/          # Vue 3 + Vite + TypeScript 前端
├── backend-java/          # Java 17 + Spring Boot 后端
├── inference-python/      # FastAPI mock-first 推理服务与 worker
├── docs/                  # API、部署、测试、论文映射、截图与发布清单
├── scripts/               # 本地启动提示与 release candidate 检查脚本
├── docker-compose.yml     # Redis 本地联调服务
├── .env.example           # 环境变量示例
└── README.md
```

## 核心能力

- 视频上传或示例视频选择。
- 用户摘要指令输入。
- 异步推理任务创建。
- `waiting → running → streaming → finished` 状态流转。
- SSE 风格流式摘要输出。
- Token 压缩指标展示，核心指标为单帧视觉 Patch Token `196 → 5`。
- 历史任务展示。
- 前端 mock mode、Java in-memory mode 和 Redis backend mode。
- 真实 Video Swin、Projection Adapter、LLM、QLoRA 接入边界保留，但默认演示使用 mock-first 流程。

## 技术栈

| 层级 | 目录 | 技术 | 职责 |
| --- | --- | --- | --- |
| 前端 | `frontend-vue/` | Vue 3、Vite、TypeScript | 上传、任务管理、SSE 展示、指标展示 |
| 业务服务 | `backend-java/` | Java 17、Spring Boot 3 | REST API、文件管理、任务管理、Redis、SSE 转发 |
| 推理服务 | `inference-python/` | Python 3.10+、FastAPI、NumPy | 视频预处理、特征提取、Token 压缩、摘要生成 |
| 中间件 | Redis | Redis Stream | 任务队列、状态缓存、流式消息中转 |
| 文档 | `docs/` | Markdown | API、部署、测试、论文映射、截图与发布检查 |

## 运行模式

### 1. 前端 mock mode（答辩兜底）

不依赖 Java、Python、Redis、GPU 或真实模型权重，适合论文第六章截图和答辩现场兜底演示。

```bash
cd frontend-vue
npm install
npm run dev
```

浏览器访问 `http://localhost:5173`，选择示例视频或本地视频后创建任务。mock mode 只读取浏览器可获得的文件元数据，摘要来自样例场景、用户指令和系统指标，不代表真实画面识别。

构建检查：

```bash
cd frontend-vue
npm run build
```

### 2. Java in-memory backend mode

默认后端模式，不需要 Redis 或 Python worker。Java 后端自动模拟任务状态、SSE 摘要和 Token 指标。

```bash
cd backend-java
mvn spring-boot:run
```

另开终端启动前端 backend mode：

```bash
cd frontend-vue
VITE_API_MODE=backend VITE_API_BASE_URL=http://localhost:8080/api npm run dev
```

健康检查：

```bash
curl http://localhost:8080/api/health
```

打包检查：

```bash
cd backend-java
mvn -q -DskipTests package
```

### 3. Python 推理服务独立运行

```bash
cd inference-python
python -m pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

独立检查：

```bash
cd inference-python
python -m compileall app
python -m pytest -q
```

核心接口：

- `GET /health`
- `POST /infer`
- `POST /mock-infer`

### 4. Redis backend mode（完整工程链路）

完整链路为：

```text
Vue backend mode → Java REST → Redis task stream → Python worker → Redis event stream → Java SSE → Vue
```

启动顺序：

```bash
# 1. Redis
docker compose up -d redis

# 2. Python worker
cd inference-python
python -m pip install -r requirements.txt
MMVS_REDIS_ENABLED=true python -m app.worker

# 3. Java 后端 Redis mode
cd backend-java
MMVS_INFERENCE_MODE=redis MMVS_REDIS_ENABLED=true mvn spring-boot:run

# 4. Vue 前端 backend mode
cd frontend-vue
npm install
VITE_API_MODE=backend VITE_API_BASE_URL=http://localhost:8080/api npm run dev
```

## 核心 API

Java 后端基础路径为 `http://localhost:8080`：

- `GET /api/health`
- `POST /api/videos/upload`
- `POST /api/tasks`
- `GET /api/tasks`
- `GET /api/tasks/{taskId}`
- `GET /api/tasks/{taskId}/events`
- `GET /api/results/{taskId}`

Python 推理服务基础路径为 `http://localhost:8000`：

- `GET /health`
- `POST /infer`
- `POST /mock-infer`

详细说明见 [`docs/API.md`](docs/API.md)。旧版契约归档见 [`docs/API_CONTRACT.md`](docs/API_CONTRACT.md)。

## 便捷检查脚本

release candidate 推荐使用脚本统一验证。脚本只依赖 npm、Maven、Python 等基础工具，缺失环境时会输出明确提示。

```bash
./scripts/check-frontend.sh
./scripts/check-backend.sh
./scripts/check-python.sh
./scripts/check-all.sh
```

脚本行为：

- `check-frontend.sh`：进入 `frontend-vue/`，执行 `npm install` 和 `npm run build`。
- `check-backend.sh`：进入 `backend-java/`，执行 `mvn -q -DskipTests package`。
- `check-python.sh`：进入 `inference-python/`，执行 `python -m compileall app` 和 `python -m pytest -q`。
- `check-all.sh`：按前端、后端、Python 顺序依次运行上述检查。

## 文档入口

- [系统架构说明](docs/ARCHITECTURE.md)：说明前端、Java 后端、Python 推理服务、Redis 和 SSE 的总体关系。
- [API 接口说明](docs/API.md)：整理 Java REST API、SSE 事件和 Python FastAPI 接口。
- [本地部署与联调说明](docs/DEPLOYMENT.md)：给出 mock mode、backend mode 和 Redis mode 的启动顺序。
- [系统测试计划](docs/TEST_PLAN.md)：面向论文第 7 章，提供功能测试、接口测试、SSE、Redis、Token 压缩等测试表格。
- [论文章节与工程实现映射](docs/THESIS_MAPPING.md)：说明第 3 章至第 7 章如何对应当前代码与文档。
- [论文与答辩截图指南](docs/SCREENSHOT_GUIDE.md)：列出论文和答辩建议截图、截图目的和操作方法。
- [常见问题排查](docs/TROUBLESHOOTING.md)：整理 npm、Maven、Python、Redis、SSE、mock/backend mode 常见问题。
- [Release Candidate 发布检查清单](docs/RELEASE_CHECKLIST.md)：整理最终发布前检查项、已知限制和真实模型接入说明。

## 论文各章节入口

- 第 3 章需求分析：见 [论文章节与工程实现映射](docs/THESIS_MAPPING.md#第-3-章-需求分析)。
- 第 4 章系统总体设计：见 [系统架构说明](docs/ARCHITECTURE.md) 和 [论文章节映射第 4 章](docs/THESIS_MAPPING.md#第-4-章-系统总体设计)。
- 第 5 章系统详细设计：见 [论文章节映射第 5 章](docs/THESIS_MAPPING.md#第-5-章-系统详细设计)。
- 第 6 章系统代码实现：见 [论文章节映射第 6 章](docs/THESIS_MAPPING.md#第-6-章-系统代码实现) 和 [截图指南](docs/SCREENSHOT_GUIDE.md)。
- 第 7 章系统测试：见 [系统测试计划](docs/TEST_PLAN.md) 和 [发布检查清单](docs/RELEASE_CHECKLIST.md)。

## 答辩演示建议

1. 优先演示前端 mock mode，确认页面、状态流、摘要和 `196 → 5` 指标稳定可见。
2. 如果现场环境允许，再演示 Java in-memory backend mode，展示真实 REST/SSE 接口。
3. Redis backend mode 适合展示完整工程链路，但应提前启动 Redis、Python worker、Java 后端和前端四个终端。
4. 截图时保留黑白灰学术风格 UI。
5. 讲解时明确说明默认演示模式是 mock-first 工程闭环，真实模型接入属于后续扩展路径。

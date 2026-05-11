# 多模态视频理解与摘要系统

这是一个面向毕业设计演示的一体化视频分析系统。前端网页是统一入口：上传视频、浏览器本地预览、填写问题、开始分析，并显示进度与摘要结果。正式分析链路为：

```text
Vue 页面 → Java 后端 → Python 轻量视频分析器 → Java 返回结果 → Vue 展示
```

Redis 只作为工程扩展选项，不是灯开关识别演示的必要条件。

## 三种使用方式

### 1. 最简单查看页面

只查看页面交互与本地演示结果：

```bash
cd frontend-vue
npm run dev
```

打开 `http://localhost:5173`。

### 2. 正式分析演示

一键启动前端、Java 后端与 Python 分析服务：

```bash
./scripts/start-demo.sh
```

打开：

```text
http://localhost:5173
```

停止演示：

```bash
./scripts/stop-demo.sh
```

启动脚本会检查 Node/npm、Java/Maven 和 Python3.11，并把日志写入 `.demo-logs/`。如果页面显示服务离线，稍等几秒后点击“重新检测”，或查看对应日志文件。

### 3. 分服务调试

适合需要单独观察某一层日志时使用。

#### Python 分析服务（8000）

```bash
cd inference-python
python -m pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

健康检查：`http://localhost:8000/health`

#### Java 后端（8080）

```bash
cd backend-java
MMVS_INFERENCE_MODE=python MMVS_PYTHON_BASE_URL=http://localhost:8000 mvn spring-boot:run
```

健康检查：`http://localhost:8080/api/health`

#### 前端页面（5173）

```bash
cd frontend-vue
VITE_API_MODE=backend VITE_API_BASE_URL=http://localhost:8080/api VITE_PYTHON_HEALTH_URL=http://localhost:8000/health npm run dev
```

## 页面中的两种分析方式

- **本地演示**：不提交视频到服务，适合快速查看上传、预览、进度和摘要展示效果。
- **正式分析**：上传视频到 Java 后端，由 Python 轻量分析器读取采样帧，并返回按压动作、亮度变化和不确定性说明。

正式分析前，页面会自动检测：

1. 前端：已运行；
2. Java 后端：`http://localhost:8080/api/health`；
3. Python 分析服务：`http://localhost:8000/health`；
4. Redis：可选扩展，不作为默认要求。

如果 Java 或 Python 未启动，页面会禁用“开始分析”，并提示先运行一键启动脚本或对应服务命令。

## 项目结构

```text
paper1/
├── frontend-vue/          # Vue 3 + Vite + TypeScript 前端
├── backend-java/          # Java 17 + Spring Boot 后端
├── inference-python/      # FastAPI 轻量分析服务
├── docs/                  # API、部署、测试等文档
├── scripts/               # 启动、停止与检查脚本
├── docker-compose.yml     # Redis 可选联调服务
└── README.md
```

## 常用检查命令

```bash
cd frontend-vue && npm run build
cd inference-python && python -m compileall app
cd inference-python && python -m pytest -q
cd backend-java && mvn -q -DskipTests package
git diff --check
```

## 主要接口

Java 后端：

- `GET /api/health`
- `POST /api/videos/upload`
- `POST /api/tasks`
- `GET /api/tasks/{taskId}/events`
- `GET /api/results/{taskId}`

Python 分析服务：

- `GET /health`
- `POST /infer`

更多接口细节见 [`docs/API.md`](docs/API.md)。

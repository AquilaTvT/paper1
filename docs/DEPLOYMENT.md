# 本地部署与联调说明

本文档说明如何在本地启动 Vue 前端、Java 后端、Python 推理服务、Python worker 和 Redis，适用于论文答辩演示、第 7 章系统测试和日常联调。

## 1. 环境要求

| 组件 | 推荐版本 | 说明 |
| --- | --- | --- |
| Node.js | 18 或更高 | 用于 `frontend-vue` Vite 开发服务器 |
| npm | 随 Node.js 安装 | 安装前端依赖并执行构建 |
| JDK | 17 | Spring Boot 后端运行环境 |
| Maven | 3.8 或更高 | 构建和运行 `backend-java` |
| Python | 3.11 推荐，3.10+ 可用 | 运行 FastAPI、worker、pytest |
| Docker / Docker Compose | Docker Desktop 或 Linux Docker | 启动 Redis |
| Redis | 7.x | 任务队列、状态缓存和流式事件 |

常见端口：

| 服务 | 端口 |
| --- | --- |
| frontend-vue | `5173` |
| backend-java | `8080` |
| inference-python | `8000` |
| Redis | `6379` |

## 2. 安装依赖

### 2.1 前端依赖

```bash
cd frontend-vue
npm install
```

### 2.2 Java 后端依赖

Maven 会在首次运行时自动下载依赖：

```bash
cd backend-java
mvn -version
mvn package
```

### 2.3 Python 依赖

```bash
cd inference-python
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

Windows PowerShell 可使用：

```powershell
cd inference-python
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

## 3. 启动 Redis

项目根目录已经提供 `docker-compose.yml`，可直接启动 Redis：

```bash
docker compose up -d redis
```

检查 Redis 容器：

```bash
docker compose ps
```

可选检查：

```bash
docker exec -it paper1-redis redis-cli ping
```

期望输出为：

```text
PONG
```

## 4. 启动 Python FastAPI 服务

该服务提供 `/health`、`/infer` 和 `/mock-infer`，便于独立测试推理 pipeline。

```bash
cd inference-python
source .venv/bin/activate
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

健康检查：

```bash
curl http://localhost:8000/health
```

## 5. 启动 Python worker

Python worker 用于从 Redis task stream 消费 Java 后端投递的异步任务，并把推理过程事件写回 Redis event stream。

```bash
cd inference-python
source .venv/bin/activate
MMVS_REDIS_ENABLED=true python -m app.worker
```

如需指定 Redis 地址：

```bash
MMVS_REDIS_ENABLED=true MMVS_REDIS_URL=redis://localhost:6379/0 python -m app.worker
```

## 6. 启动 Spring Boot 后端

### 6.1 in-memory mock mode

不依赖 Redis 和 Python worker，适合快速检查后端 API：

```bash
cd backend-java
mvn spring-boot:run
```

### 6.2 Redis backend mode

走完整链路：Java → Redis task stream → Python worker → Redis event stream → Java SSE。

```bash
cd backend-java
MMVS_INFERENCE_MODE=redis MMVS_REDIS_ENABLED=true mvn spring-boot:run
```

健康检查：

```bash
curl http://localhost:8080/api/health
```

## 7. 启动 Vue 前端

### 7.1 mock mode

mock mode 不依赖 Java、Python、Redis，适合答辩时兜底演示。

```bash
cd frontend-vue
npm install
npm run dev
```

浏览器访问：

```text
http://localhost:5173
```

### 7.2 backend mode

backend mode 调用 Java REST API 与 SSE 接口。

```bash
cd frontend-vue
VITE_API_MODE=backend VITE_API_BASE_URL=http://localhost:8080/api npm run dev
```

若通过 Vite 代理访问，也可保留默认 `/api` 基础路径；答辩时建议使用显式 `VITE_API_BASE_URL=http://localhost:8080/api`，便于排查。

## 8. 推荐完整启动顺序

1. 启动 Redis。
2. 启动 Python FastAPI 服务，用于独立健康检查。
3. 启动 Python worker。
4. 启动 Java 后端 Redis backend mode。
5. 启动 Vue 前端 backend mode。
6. 浏览器访问 `http://localhost:5173`，上传视频或选择 mock 场景，创建摘要任务，观察时间线、SSE 摘要和 Token 指标。

## 9. mock mode 与 backend mode 切换

| 模式 | 启动方式 | 适用场景 |
| --- | --- | --- |
| 前端 mock mode | 不设置 `VITE_API_MODE=backend` | 无后端环境、答辩兜底、前端截图 |
| Java in-memory mode | 不设置 `MMVS_INFERENCE_MODE=redis` | 后端接口测试，不启动 Redis 和 Python |
| Redis backend mode | `MMVS_INFERENCE_MODE=redis MMVS_REDIS_ENABLED=true`，前端设置 `VITE_API_MODE=backend` | 完整工程链路、论文第 7 章联调测试 |

## 10. 构建与测试命令汇总

```bash
cd frontend-vue
npm run build
```

```bash
cd backend-java
mvn test
mvn package
```

```bash
cd inference-python
python -m compileall app
pytest
```

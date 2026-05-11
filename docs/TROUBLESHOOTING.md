# 常见问题排查

本文档整理本地开发、论文截图和答辩演示中最常见的问题。建议答辩前至少完整演练一次 Redis backend mode，并保留前端 mock mode 作为兜底方案。

## 1. npm install 失败

**现象**：在 `frontend-vue` 下执行 `npm install` 报网络错误、registry 超时或权限错误。

**排查步骤**：

1. 确认 Node.js 版本满足 `>=18.0.0`。
2. 检查网络是否能访问 npm registry。
3. 清理 npm 缓存后重试。

```bash
node -v
npm -v
npm cache verify
npm install
```

如网络较慢，可切换到学校或家庭稳定网络后再安装。

## 2. npm run build 失败

**现象**：`vue-tsc`、`vite` 未找到，或 TypeScript 类型检查失败。

**排查步骤**：

1. 先确认已经执行 `npm install`。
2. 再执行构建命令。
3. 若提示依赖缺失，删除 `node_modules` 和 lock 文件后重新安装。

```bash
cd frontend-vue
npm install
npm run build
```

注意：不要为修复构建临时引入新依赖，除非确认项目本身确实缺失依赖声明。

## 3. Maven 下载依赖失败

**现象**：`mvn package` 或 `mvn spring-boot:run` 卡在下载依赖，或提示仓库不可达。

**排查步骤**：

```bash
mvn -version
cd backend-java
mvn -U package
```

若仍失败，检查网络代理或 Maven 本地仓库配置。答辩前建议提前完成一次依赖下载，避免现场网络影响。

## 4. Java 版本不对

**现象**：启动后端时报 `Unsupported class file major version`，或 Maven 编译提示 Java 版本不匹配。

**解决方法**：安装并切换到 JDK 17。

```bash
java -version
javac -version
```

期望主版本为 17。若系统有多个 JDK，请设置 `JAVA_HOME` 指向 JDK 17。

## 5. Python 依赖安装失败

**现象**：`pip install -r requirements.txt` 下载失败，或 NumPy、FastAPI、Redis 依赖安装失败。

**排查步骤**：

```bash
cd inference-python
python --version
python -m venv .venv
source .venv/bin/activate
python -m pip install --upgrade pip
pip install -r requirements.txt
```

建议使用 Python 3.11。Windows 下注意先激活 `.venv`。

## 6. Redis 未启动

**现象**：Java 后端或 Python worker 报 Redis 连接失败，前端任务一直停留在 `waiting`。

**解决方法**：

```bash
docker compose up -d redis
docker compose ps
docker exec -it paper1-redis redis-cli ping
```

若 `ping` 没有返回 `PONG`，说明 Redis 容器未正常启动，需要查看 Docker 日志或检查 6379 端口是否被占用。

## 7. SSE 无输出

**现象**：任务创建成功，但摘要区没有流式输出。

**排查步骤**：

1. 浏览器 Network 面板检查 `GET /api/tasks/{taskId}/events` 是否为 `event-stream`。
2. 确认 Java 后端正在运行。
3. Redis backend mode 下确认 Python worker 正在运行。
4. 查看 Java `RedisStreamEventConsumer` 日志是否消费到事件。
5. 查看 Python worker 是否发布 `summary_delta` 和 `completed` 事件。

可先切换到 Java in-memory mode 验证 SSE 基础能力，再切换 Redis backend mode 验证完整链路。

## 8. 前端无法连接后端

**现象**：前端提示接口请求失败，浏览器控制台出现 `Failed to fetch` 或 CORS 问题。

**排查步骤**：

1. 确认 Java 后端端口为 `8080`。
2. 使用 curl 直接访问后端健康检查。
3. backend mode 启动前端时显式设置 API 地址。

```bash
curl http://localhost:8080/api/health
cd frontend-vue
VITE_API_MODE=backend VITE_API_BASE_URL=http://localhost:8080/api npm run dev
```

若使用 Vite 代理，确认请求路径以 `/api` 开头。

## 9. mock mode 与 backend mode 混淆

**现象**：以为已经联调后端，但 Java、Redis 或 Python 日志没有任何请求。

**解释**：前端 mock mode 会在浏览器本地模拟任务，不会访问 Java 后端。只有设置 `VITE_API_MODE=backend` 后，前端才会走 `taskApi.ts` 和 `sseClient.ts` 调用后端。

**建议**：

- 论文截图可使用 mock mode。
- 第 7 章完整链路测试应使用 backend mode。
- 答辩现场先演示 backend mode，如环境异常再切换 mock mode 兜底。

## 10. 上传视频没有真实语义识别

**现象**：上传不同视频后，摘要主要描述 pipeline 和关键事件流程，而不是精确识别视频内容。

**解释**：当前系统为 mock-first 毕设工程，Video Swin encoder、Projection Adapter 和 SummaryGenerator 均为 mock 实现，重点展示系统架构、异步链路、Token 压缩和流式摘要能力。真实 Video Swin、LLM、QLoRA 权重加载接口已保留，但未接入真实模型权重。

论文中应明确说明：当前演示模式用于普通电脑可运行的工程闭环，真实语义识别属于 real mode 扩展方向。

## 11. Token 指标显示异常

**现象**：前端没有显示 `196 → 5`，或压缩比不是 `39.2`。

**排查步骤**：

1. 确认 Python 环境变量 `MMVS_PATCH_TOKENS_PER_FRAME=196`。
2. 确认 Python 环境变量 `MMVS_COMPRESSED_TOKENS_PER_FRAME=5`。
3. Redis backend mode 下确认 `token_metrics` 事件已经发布并被 Java 消费。
4. Java in-memory mode 下确认 `application.yml` 中对应配置为 `196` 和 `5`。

核心期望值为：

```text
rawPatchTokensPerFrame = 196
compressedTokensPerFrame = 5
compressionRatio = 196 / 5 = 39.2
```

## 12. 端口被占用

**现象**：启动服务时报 `Address already in use`。

**常见端口**：

| 服务 | 端口 |
| --- | --- |
| Vue 前端 | `5173` |
| Java 后端 | `8080` |
| Python FastAPI | `8000` |
| Redis | `6379` |

**排查命令**：

```bash
lsof -i :5173
lsof -i :8080
lsof -i :8000
lsof -i :6379
```

找到占用进程后关闭对应服务，或修改启动端口。论文答辩建议使用默认端口，方便口头说明系统架构。

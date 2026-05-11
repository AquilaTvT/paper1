# Release Candidate 发布检查清单

本文档用于第 6 阶段最终工程化自查，目标是确认项目在答辩前可以稳定启动、构建、测试和截图。建议每次提交发布候选版本前按顺序执行。

## 1. 前端构建检查

- 进入目录：`cd frontend-vue`。
- 安装依赖：`npm install`。
- 执行构建：`npm run build`。
- 预期结果：`vue-tsc --noEmit` 类型检查通过，Vite 生成 `dist/` 构建产物。
- 便捷脚本：`./scripts/check-frontend.sh`。

## 2. Java 后端构建检查

- 确认 JDK 版本为 17：`java -version`。
- 进入目录：`cd backend-java`。
- 执行打包：`mvn -q -DskipTests package`。
- 预期结果：Spring Boot 工程完成编译与打包；默认配置仍为 in-memory mock mode，不要求 Redis 启动。
- 便捷脚本：`./scripts/check-backend.sh`。

## 3. Python 测试检查

- 建议 Python 3.10+。
- 进入目录：`cd inference-python`。
- 安装依赖：`python -m pip install -r requirements.txt`。
- 语法检查：`python -m compileall app`。
- 单元测试：`python -m pytest -q`。
- 预期结果：`test_token_compressor.py` 验证 `196 → 5`，`test_pipeline.py` 验证完整 mock pipeline。
- 便捷脚本：`./scripts/check-python.sh`。

## 4. mock mode 演示检查

- 启动前端：`cd frontend-vue && npm run dev`。
- 浏览器访问：`http://localhost:5173`。
- 操作顺序：使用示例视频或选择本地视频 → 输入摘要指令 → 创建任务。
- 预期结果：页面出现 `waiting → running → streaming → finished`，SSE 风格摘要逐句输出，Token 指标显示 `196 → 5`，历史记录新增完成任务。
- 边界要求：mock mode 必须明确说明摘要来自样例场景和系统指标，不暗示真实读取视频画面内容。

## 5. backend mode 联调检查

- 启动 Java 后端默认模式：`cd backend-java && mvn spring-boot:run`。
- 启动前端 backend mode：`cd frontend-vue && VITE_API_MODE=backend VITE_API_BASE_URL=http://localhost:8080/api npm run dev`。
- 访问 Java 健康检查：`curl http://localhost:8080/api/health`。
- 在页面上传本地视频，确认 `POST /api/videos/upload` 返回 `videoId`。
- 创建任务后确认 Java in-memory scheduler 能推动状态、SSE 摘要和 Token 指标。

## 6. Redis mode 检查

- 启动 Redis：`docker compose up -d redis`。
- 启动 Python worker：`cd inference-python && MMVS_REDIS_ENABLED=true python -m app.worker`。
- 启动 Java Redis mode：`cd backend-java && MMVS_INFERENCE_MODE=redis MMVS_REDIS_ENABLED=true mvn spring-boot:run`。
- 启动 Vue backend mode：`cd frontend-vue && VITE_API_MODE=backend VITE_API_BASE_URL=http://localhost:8080/api npm run dev`。
- 预期结果：任务请求写入 `mmvs:tasks:requests`，Python worker 发布事件到 `mmvs:tasks:events`，Java 通过 `/api/tasks/{taskId}/events` 转发到前端。

## 7. 论文截图检查

- 首页整体截图：保留黑白灰学术风格。
- 视频上传模块截图：包含 mock/backend mode 边界说明。
- 状态时间线截图：最好截取运行中状态，体现阶段推进。
- SSE 摘要截图：包含多段摘要片段。
- Token 压缩截图：突出 `196 Patch Tokens / frame`、`5 Visual Tokens / frame` 和压缩倍数。
- 历史记录截图：至少包含 1 条 finished 任务。

## 8. 答辩演示前检查

- 提前完成 `./scripts/check-all.sh` 或分别执行三个检查脚本。
- 提前启动并验证前端 mock mode，作为无网络、无 Redis 或后端异常时的兜底方案。
- 若演示 Redis backend mode，至少提前 10 分钟启动 Redis、Java、Python worker 和前端四个终端。
- 浏览器建议打开开发者工具 Network 面板，便于展示 `event-stream`。
- 准备一份小体积 `mp4` 样例视频，避免大文件上传影响节奏。

## 9. 已知限制

- 默认 mock mode 和 Java in-memory mode 不进行真实视频语义识别。
- Redis mode 依赖本机 Redis、Python worker 和 Java 后端同时运行。
- 当前任务和结果默认保存在内存中，服务重启后会丢失。
- Maven、npm、pip 首次安装依赖需要访问外部仓库，答辩前应提前完成依赖下载。
- 真实模型权重、GPU 推理和 QLoRA 参数加载接口已保留，但不是默认演示路径。

## 10. 后续真实模型接入说明

- 在 `inference-python/app/models/real_model_interfaces.py` 中实现真实 Video Swin、Projection Adapter 和 LLM 适配器。
- 在配置中补充真实模型路径、设备、精度和批大小。
- 保持 `/infer`、Redis worker 输入输出和 Java SSE 事件字段不变，避免影响前端与论文接口说明。
- 接入真实模型后仍需保留 mock fallback，保证普通电脑可演示和测试。

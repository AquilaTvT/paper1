# 论文与答辩截图指南

本文档用于指导论文正文、系统测试章节和答辩 PPT 截图。截图应尽量保持窗口整洁，建议使用 Chrome 或 Edge，浏览器缩放保持 90% 至 100%，终端使用统一字体与深色背景。

## 截图清单

| 序号 | 截图名称 | 对应论文位置 | 截图目的 | 截图时应如何操作 |
| --- | --- | --- | --- | --- |
| 1 | 系统首页 | 第 6 章系统实现 / 第 7 章功能测试 | 展示系统整体界面、黑白灰学术风格和主要功能区域 | 启动前端，访问 `http://localhost:5173`，截取完整首页 |
| 2 | 视频上传模块 | 第 3 章需求分析 / 第 6 章代码实现 | 证明系统支持本地视频上传或样例视频选择 | 点击上传区域，选择 `mp4` 文件，等待显示文件名和大小后截图 |
| 3 | 用户指令输入模块 | 第 3 章功能需求 | 展示用户可输入自然语言摘要需求 | 在指令框输入“请总结视频中的关键事件”，保留光标或输入内容截图 |
| 4 | 任务状态时间线 | 第 5 章详细设计 / 第 7 章任务状态流转测试 | 展示 `waiting → running → streaming → finished` 状态流转 | 创建任务后，在任务执行过程中截取时间线区域，最好包含当前 stage |
| 5 | SSE 流式摘要输出 | 第 5 章 SSE 设计 / 第 7 章 SSE 测试 | 展示摘要按片段逐步生成，而非一次性返回 | backend mode 创建任务，等待摘要区出现多段文字时截图 |
| 6 | Token 压缩 196 → 5 | 第 5 章 Token 压缩设计 / 第 7 章 Token 压缩测试 | 展示论文核心指标 `rawPatchTokensPerFrame=196`、`compressedTokensPerFrame=5` | 任务执行到 `token_compression` 后截取 Token 指标卡片 |
| 7 | 系统架构流程图 | 第 4 章系统总体设计 | 展示浏览器、Java、Redis、Python worker 和 SSE 的完整链路 | 使用 `docs/ARCHITECTURE.md` 中 Mermaid 图导出，或截取 Markdown 预览 |
| 8 | 历史任务 | 第 3 章需求分析 / 第 7 章功能测试 | 证明系统支持历史任务展示 | 连续创建 2 至 3 个任务后截取历史任务表格 |
| 9 | backend mode / mock mode 说明 | 第 4 章运行模式设计 / 答辩 PPT | 说明系统既可完整联调，也可无后端演示 | 截取 README 或前端模式说明区域；答辩时可放一页模式对比 |
| 10 | 终端启动 Redis | 第 7 章测试环境 | 证明 Redis 中间件正常启动 | 执行 `docker compose up -d redis` 和 `docker compose ps` 后截图 |
| 11 | 终端启动 Java | 第 7 章测试环境 | 证明 Spring Boot 后端正常运行 | 执行 `MMVS_INFERENCE_MODE=redis MMVS_REDIS_ENABLED=true mvn spring-boot:run`，截取启动成功日志 |
| 12 | 终端启动 Python FastAPI | 第 7 章测试环境 | 证明推理服务可独立启动 | 执行 `uvicorn app.main:app --reload --host 0.0.0.0 --port 8000`，截取 Uvicorn 启动日志 |
| 13 | 终端启动 Python worker | 第 7 章 Redis 异步链路测试 | 证明 worker 正在等待和消费任务 | 执行 `MMVS_REDIS_ENABLED=true python -m app.worker`，任务执行时截取 worker 日志 |
| 14 | 终端启动 Vue | 第 7 章测试环境 | 证明前端开发服务器正常启动 | 执行 `VITE_API_MODE=backend VITE_API_BASE_URL=http://localhost:8080/api npm run dev`，截取 Vite 本地地址 |
| 15 | 接口测试截图 | 第 7 章接口测试 | 证明 REST API 可调用 | 使用 curl、Postman 或浏览器访问 `/api/health`、`/health`、`POST /api/tasks`，截取响应 JSON |
| 16 | Python 测试通过截图 | 第 7 章 Python pipeline 测试 | 证明 Python 代码与测试用例通过 | 在 `inference-python` 下执行 `python -m compileall app` 和 `pytest` 后截图 |

## 推荐截图顺序

答辩演示建议按以下顺序截图和讲解：

1. 系统架构流程图，先说明整体链路。
2. 系统首页，展示界面风格。
3. 视频上传与用户指令输入，说明用户操作。
4. 任务状态时间线，说明异步任务。
5. SSE 流式摘要输出，说明实时展示。
6. Token 压缩 `196 → 5`，突出论文核心设计。
7. 历史任务，说明可回看结果。
8. 终端启动截图，证明系统真实运行。
9. 接口测试和 Python 测试截图，支撑第 7 章测试结论。

## 截图命名建议

建议将截图保存到论文素材目录，文件名使用中文或英文均可，但要保持编号清晰：

```text
01_system_home.png
02_video_upload.png
03_prompt_input.png
04_task_timeline.png
05_sse_streaming_summary.png
06_token_compression_196_to_5.png
07_architecture_flow.png
08_history_tasks.png
09_mode_switch.png
10_redis_terminal.png
11_java_terminal.png
12_python_fastapi_terminal.png
13_python_worker_terminal.png
14_vue_terminal.png
15_api_test.png
16_python_tests.png
```

## 截图注意事项

- 截图中不要暴露个人隐私路径、真实密钥或不必要的后台程序。
- 如果端口冲突或服务报错，不要作为正式截图；先参考 `docs/TROUBLESHOOTING.md` 修复。
- Token 压缩截图必须清晰包含 `196`、`5` 和压缩比，便于论文审阅老师直接看到核心结果。
- SSE 截图最好选择摘要正在生成时，而不是任务完全结束后，这样更能体现“流式输出”。

# 系统测试计划

本文档服务论文第 7 章“系统测试”。测试目标是验证系统在 mock mode、Java in-memory mode 和 Redis backend mode 下的功能正确性、接口可用性、异步链路完整性、SSE 流式输出效果以及 Token 压缩指标展示。

## 1. 测试环境

| 项目 | 配置 |
| --- | --- |
| 前端 | Vue 3 + Vite，端口 `5173` |
| Java 后端 | Spring Boot，端口 `8080` |
| Python 推理服务 | FastAPI / worker，端口 `8000` |
| Redis | Redis 7.x，端口 `6379` |
| 浏览器 | Chrome / Edge 最新稳定版 |
| 测试视频 | 任意短视频文件，建议 5 秒至 30 秒，格式 `mp4` |

## 2. 关键指标说明

Token 压缩测试必须记录以下固定指标：

```text
rawPatchTokensPerFrame = 196
compressedTokensPerFrame = 5
compressionRatio = 196 / 5 = 39.2
```

若抽取 `T` 帧，则总 Token 指标为：

```text
rawVisualTokens = T × 196
compressedVisualTokens = T × 5
```

## 3. 功能测试

| 测试编号 | 测试目标 | 测试步骤 | 输入数据 | 预期结果 | 实际结果填写位置 | 是否通过 |
| --- | --- | --- | --- | --- | --- | --- |
| FT-01 | 验证系统首页可正常访问 | 启动前端后访问 `http://localhost:5173` | 无 | 页面显示上传区、指令输入区、任务状态区、摘要区和 Token 指标区 | 论文第 7 章表格“实际结果” | 待填写 |
| FT-02 | 验证视频上传模块 | 点击上传区域选择 `mp4` 文件 | `demo.mp4` | 前端显示文件名和大小，backend mode 下返回 `videoId` | 论文第 7 章表格“实际结果” | 待填写 |
| FT-03 | 验证用户指令输入 | 在指令框输入摘要需求 | “请总结视频关键事件” | 输入内容能够随任务创建请求提交 | 论文第 7 章表格“实际结果” | 待填写 |
| FT-04 | 验证任务创建 | 点击创建任务按钮 | 已上传视频和摘要指令 | 页面出现新任务，状态初始为 `waiting` | 论文第 7 章表格“实际结果” | 待填写 |
| FT-05 | 验证历史记录 | 创建多个任务后查看历史任务区 | 多个任务 | 历史任务按时间显示，能够查看状态和摘要概览 | 论文第 7 章表格“实际结果” | 待填写 |

## 4. 接口测试

| 测试编号 | 测试目标 | 测试步骤 | 输入数据 | 预期结果 | 实际结果填写位置 | 是否通过 |
| --- | --- | --- | --- | --- | --- | --- |
| API-01 | 验证 Java 健康检查 | 执行 `curl http://localhost:8080/api/health` | 无 | 返回 `success=true`、`service=backend-java` | 接口测试截图或响应 JSON | 待填写 |
| API-02 | 验证 Python 健康检查 | 执行 `curl http://localhost:8000/health` | 无 | 返回 `status=up`、`run_mode=mock` | 接口测试截图或响应 JSON | 待填写 |
| API-03 | 验证上传接口 | 执行 `curl -F "file=@demo.mp4" http://localhost:8080/api/videos/upload` | `demo.mp4` | 返回 `videoId` 和文件元数据 | 接口测试截图或响应 JSON | 待填写 |
| API-04 | 验证创建任务接口 | 调用 `POST /api/tasks` | `videoId`、`instruction`、`runMode`、`stream` | 返回 `taskId`，任务状态为 `waiting` 或 `running` | 接口测试截图或响应 JSON | 待填写 |
| API-05 | 验证任务查询接口 | 调用 `GET /api/tasks/{taskId}` | `taskId` | 返回任务详情、当前阶段和日志 | 接口测试截图或响应 JSON | 待填写 |
| API-06 | 验证结果查询接口 | 任务完成后调用 `GET /api/results/{taskId}` | `taskId` | 返回最终摘要、关键事件和 Token 指标 | 接口测试截图或响应 JSON | 待填写 |

## 5. 文件上传异常测试

| 测试编号 | 测试目标 | 测试步骤 | 输入数据 | 预期结果 | 实际结果填写位置 | 是否通过 |
| --- | --- | --- | --- | --- | --- | --- |
| UP-01 | 验证空文件处理 | 不选择文件直接提交 | 空文件 | 前端或后端提示文件不能为空 | 异常测试表格 | 待填写 |
| UP-02 | 验证非法扩展名处理 | 上传 `.txt` 或 `.jpg` 文件 | `test.txt` | 后端返回不支持的文件类型错误 | 异常测试表格 | 待填写 |
| UP-03 | 验证超大文件处理 | 上传超过配置限制的视频 | 大于 200MB 文件 | 后端拒绝请求并返回文件过大提示 | 异常测试表格 | 待填写 |
| UP-04 | 验证中文文件名 | 上传中文命名视频 | `校园场景.mp4` | 后端安全存储文件并返回原始文件名 | 异常测试表格 | 待填写 |

## 6. 任务状态流转测试

| 测试编号 | 测试目标 | 测试步骤 | 输入数据 | 预期结果 | 实际结果填写位置 | 是否通过 |
| --- | --- | --- | --- | --- | --- | --- |
| ST-01 | 验证正常状态流转 | 创建任务并观察时间线 | 短视频与摘要指令 | 状态按 `waiting → running → streaming → finished` 流转 | 时间线截图 | 待填写 |
| ST-02 | 验证阶段名称展示 | 在任务执行过程中观察 stage | Redis backend mode | 页面显示 `video_preprocess` / `video_sampling`、`video_swin_feature`、`token_compression`、`projection_adapter` / `mlp_adapter`、`summary_generation` | 时间线截图 | 待填写 |
| ST-03 | 验证失败状态 | 停止 Python worker 后创建任务 | Redis backend mode | 任务最终进入 `failed` 或保持等待并有错误提示 | 异常截图 | 待填写 |

## 7. SSE 流式输出测试

| 测试编号 | 测试目标 | 测试步骤 | 输入数据 | 预期结果 | 实际结果填写位置 | 是否通过 |
| --- | --- | --- | --- | --- | --- | --- |
| SSE-01 | 验证 SSE 连接建立 | 创建任务后订阅 `GET /api/tasks/{taskId}/events` | `taskId` | 浏览器 Network 中出现 `event-stream` 连接 | Network 截图 | 待填写 |
| SSE-02 | 验证摘要片段增量输出 | 观察摘要展示区 | 短视频与摘要指令 | 摘要按片段逐步追加，而不是一次性全部出现 | 摘要区截图 | 待填写 |
| SSE-03 | 验证完成事件 | 等待任务完成 | 正常任务 | 收到 `completed` 事件，任务状态变为 `finished` | SSE 响应截图 | 待填写 |
| SSE-04 | 验证错误事件 | 制造异常或停止 worker | Redis backend mode | 前端显示错误提示，SSE 收到 `error` 事件 | 异常截图 | 待填写 |

## 8. Token 压缩测试

| 测试编号 | 测试目标 | 测试步骤 | 输入数据 | 预期结果 | 实际结果填写位置 | 是否通过 |
| --- | --- | --- | --- | --- | --- | --- |
| TK-01 | 验证单帧压缩比例 | 创建任务并查看 Token 指标卡片 | 任意短视频 | `rawPatchTokensPerFrame = 196`，`compressedTokensPerFrame = 5`，`compressionRatio = 196 / 5 = 39.2` | Token 指标截图 | 待填写 |
| TK-02 | 验证总 Token 计算 | 记录抽帧数 `T` | 任意短视频 | `rawVisualTokens = T × 196`，`compressedVisualTokens = T × 5` | Token 指标截图 | 待填写 |
| TK-03 | 验证前端指标展示 | 查看 `TokenCompressionCard` | 完成任务 | 页面突出显示 `196 → 5` | 指标卡片截图 | 待填写 |
| TK-04 | 验证 Python compressor | 运行 Python pipeline 或 pytest | mock 特征 | compressor 输出形状 `[T, 5, C]` | pytest 截图 | 待填写 |

## 9. Python 推理 pipeline 测试

| 测试编号 | 测试目标 | 测试步骤 | 输入数据 | 预期结果 | 实际结果填写位置 | 是否通过 |
| --- | --- | --- | --- | --- | --- | --- |
| PY-01 | 验证 Python 代码可编译 | 执行 `python -m compileall app` | Python 源码 | 无语法错误 | 终端截图 | 待填写 |
| PY-02 | 验证单元测试 | 执行 `python -m pytest -q` | 测试用例 | 所有测试通过 | 终端截图 | 待填写 |
| PY-03 | 验证 mock infer 接口 | 调用 `POST /mock-infer` | `task_id`、`video_path`、`query_text` | 返回摘要、关键事件和 Token 指标 | 接口响应截图 | 待填写 |
| PY-04 | 验证 pipeline 阶段顺序 | 查看 worker 日志 | Redis backend mode | 日志依次出现预处理、编码、压缩、投影、摘要生成 | 终端截图 | 待填写 |

## 10. Redis 异步链路测试

| 测试编号 | 测试目标 | 测试步骤 | 输入数据 | 预期结果 | 实际结果填写位置 | 是否通过 |
| --- | --- | --- | --- | --- | --- | --- |
| RD-01 | 验证 Redis 启动 | 执行 `docker compose up -d redis` 与 `redis-cli ping` | 无 | 返回 `PONG` | 终端截图 | 待填写 |
| RD-02 | 验证任务写入 stream | backend mode 创建任务 | 任务请求 | Redis 中出现 `mmvs:tasks:requests` 记录 | Redis 查询截图 | 待填写 |
| RD-03 | 验证 worker 消费任务 | 观察 Python worker 日志 | Redis task stream | worker 读取任务并执行 pipeline | worker 日志截图 | 待填写 |
| RD-04 | 验证事件写回 stream | 观察 Redis event stream | 推理事件 | Redis 中出现 `stage`、`token_metrics`、`summary_delta`、`completed` | Redis 查询截图 | 待填写 |

## 11. 前端 mock mode 测试

| 测试编号 | 测试目标 | 测试步骤 | 输入数据 | 预期结果 | 实际结果填写位置 | 是否通过 |
| --- | --- | --- | --- | --- | --- | --- |
| FM-01 | 验证无后端演示 | 仅启动 `npm run dev` | mock 场景 | 页面可完整展示上传、任务、摘要、Token 指标 | 前端截图 | 待填写 |
| FM-02 | 验证 mock 视频场景 | 切换示例视频或 mock 场景 | 样例场景 | 摘要文本与场景描述一致 | 前端截图 | 待填写 |
| FM-03 | 验证 mock 历史记录 | 多次创建 mock 任务 | mock 任务 | 历史任务可见 | 前端截图 | 待填写 |

## 12. backend mode 联调测试

| 测试编号 | 测试目标 | 测试步骤 | 输入数据 | 预期结果 | 实际结果填写位置 | 是否通过 |
| --- | --- | --- | --- | --- | --- | --- |
| BM-01 | 验证完整链路启动 | 按 Redis、Python worker、Java、Vue 顺序启动 | 无 | 四个服务均正常运行 | 终端截图 | 待填写 |
| BM-02 | 验证上传到摘要全流程 | 前端上传视频并创建任务 | `demo.mp4` | 前端显示完成摘要与 `196 → 5` 指标 | 演示截图 | 待填写 |
| BM-03 | 验证后端异常提示 | 关闭 Java 后端后刷新前端 | 无 | 前端提示连接失败或请求失败 | 异常截图 | 待填写 |

## 13. 摘要样例质量测试

| 测试编号 | 测试目标 | 测试步骤 | 输入数据 | 预期结果 | 实际结果填写位置 | 是否通过 |
| --- | --- | --- | --- | --- | --- | --- |
| SQ-01 | 验证摘要完整性 | 使用短视频创建任务 | “请概括主要事件” | 摘要包含预处理、视觉特征、Token 压缩和关键事件说明 | 质量评价表 | 待填写 |
| SQ-02 | 验证指令相关性 | 输入不同用户指令 | “请关注人物动作” | 摘要中体现用户指令文本或关注点 | 质量评价表 | 待填写 |
| SQ-03 | 验证流式可读性 | 观察摘要片段 | 正常任务 | 片段语言连贯，能够用于答辩展示 | 质量评价表 | 待填写 |

## 14. 资源估算测试

| 测试编号 | 测试目标 | 测试步骤 | 输入数据 | 预期结果 | 实际结果填写位置 | 是否通过 |
| --- | --- | --- | --- | --- | --- | --- |
| RS-01 | 验证普通电脑可运行 mock mode | 不使用 GPU 启动系统 | mock mode | 前端、Java、Python、Redis 可在 CPU 环境运行 | 资源记录表 | 待填写 |
| RS-02 | 估算任务耗时 | 记录创建任务到完成的时间 | 短视频 | mock pipeline 在可接受时间内完成 | 资源记录表 | 待填写 |
| RS-03 | 估算 Token 压缩收益 | 对比压缩前后 Token 数量 | 任意任务 | 单帧 Token 数从 `196` 降为 `5`，理论压缩比 `39.2` | 资源记录表 | 待填写 |
| RS-04 | 验证端口占用情况 | 启动全部服务后查看端口 | 四个服务 | `5173`、`8080`、`8000`、`6379` 均被对应服务占用 | 终端截图 | 待填写 |

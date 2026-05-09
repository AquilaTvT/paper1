# API 契约

## 1. 通用约定

### 1.1 基础路径

Java Spring Boot 业务服务对前端暴露统一前缀：

```text
/api
```

Python FastAPI 推理服务默认不直接暴露给前端。前端仅访问 Java 后端。Java 后端通过 Redis 与 Python 推理服务解耦。

### 1.2 数据格式

- 普通接口请求和响应使用 JSON。
- 视频上传使用 `multipart/form-data`。
- 流式输出使用 Server-Sent Events，响应 `Content-Type` 为 `text/event-stream`。
- 时间字段使用 ISO-8601 字符串，例如 `2026-05-09T10:30:00Z`。
- ID 字段建议使用 UUID 字符串。

### 1.3 通用响应结构

成功响应：

```json
{
  "success": true,
  "data": {},
  "message": "ok",
  "requestId": "req-uuid"
}
```

失败响应：

```json
{
  "success": false,
  "data": null,
  "message": "invalid video file",
  "errorCode": "VIDEO_INVALID_FORMAT",
  "requestId": "req-uuid"
}
```

### 1.4 任务状态枚举

任务状态必须使用以下字符串：

- `waiting`
- `running`
- `streaming`
- `finished`
- `failed`
- `cancelled`

## 2. POST /api/videos/upload

上传视频文件，返回视频元数据。

### 请求

`Content-Type: multipart/form-data`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `file` | file | 是 | 视频文件，建议支持 MP4、MOV、AVI、MKV |
| `source` | string | 否 | `upload` 或 `sample`，默认 `upload` |
| `remark` | string | 否 | 视频备注 |

### 响应数据

```json
{
  "videoId": "video-uuid",
  "originalName": "demo.mp4",
  "storagePath": "uploads/2026/05/video-uuid.mp4",
  "mimeType": "video/mp4",
  "sizeBytes": 10485760,
  "durationSeconds": 32.5,
  "status": "available",
  "createdAt": "2026-05-09T10:30:00Z"
}
```

### 业务规则

- 上传成功后生成 `VideoFile` 记录。
- 文件校验失败返回 `VIDEO_INVALID_FORMAT` 或 `VIDEO_TOO_LARGE`。
- 该接口不创建推理任务，只返回 `videoId`。

## 3. POST /api/tasks

创建视频理解与摘要任务。

### 请求体

```json
{
  "videoId": "video-uuid",
  "instruction": "请总结视频中的关键事件",
  "runMode": "mock",
  "stream": true,
  "parameters": {
    "sampleFps": 1,
    "maxSummaryTokens": 512,
    "language": "zh-CN",
    "enableTokenMetrics": true
  }
}
```

### 字段说明

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `videoId` | string | 是 | 已上传或示例视频 ID |
| `instruction` | string | 是 | 用户摘要指令 |
| `runMode` | string | 否 | `mock` 或 `real`，默认 `mock` |
| `stream` | boolean | 否 | 是否启用流式输出，默认 `true` |
| `parameters` | object | 否 | 采样、摘要长度、语言和指标开关 |

### 响应数据

```json
{
  "taskId": "task-uuid",
  "videoId": "video-uuid",
  "status": "waiting",
  "instruction": "请总结视频中的关键事件",
  "runMode": "mock",
  "createdAt": "2026-05-09T10:31:00Z",
  "eventUrl": "/api/tasks/task-uuid/events"
}
```

### 业务规则

- Java 后端创建 `InferenceTask`。
- 初始状态为 `waiting`。
- Java 后端向 Redis 任务队列写入任务消息。
- 若 `videoId` 不存在，返回 `VIDEO_NOT_FOUND`。

## 4. GET /api/tasks

查询任务列表。

### 查询参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `status` | string | 否 | 空 | 按任务状态过滤 |
| `page` | number | 否 | 1 | 页码 |
| `pageSize` | number | 否 | 20 | 每页数量 |
| `sort` | string | 否 | `createdAt,desc` | 排序规则 |

### 响应数据

```json
{
  "items": [
    {
      "taskId": "task-uuid",
      "videoId": "video-uuid",
      "videoName": "demo.mp4",
      "status": "streaming",
      "runMode": "mock",
      "instruction": "请总结视频中的关键事件",
      "createdAt": "2026-05-09T10:31:00Z",
      "updatedAt": "2026-05-09T10:31:12Z"
    }
  ],
  "page": 1,
  "pageSize": 20,
  "total": 1
}
```

## 5. GET /api/tasks/{taskId}

查询任务详情。

### 路径参数

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `taskId` | string | 任务 ID |

### 响应数据

```json
{
  "taskId": "task-uuid",
  "videoId": "video-uuid",
  "videoName": "demo.mp4",
  "status": "streaming",
  "instruction": "请总结视频中的关键事件",
  "runMode": "mock",
  "progress": 65,
  "currentStage": "summary_generation",
  "errorMessage": null,
  "createdAt": "2026-05-09T10:31:00Z",
  "startedAt": "2026-05-09T10:31:03Z",
  "finishedAt": null,
  "logs": [
    {
      "level": "INFO",
      "message": "task moved to running",
      "createdAt": "2026-05-09T10:31:03Z"
    }
  ]
}
```

## 6. GET /api/tasks/{taskId}/events

订阅任务 SSE 流式事件。

### 请求

```http
GET /api/tasks/task-uuid/events
Accept: text/event-stream
```

### SSE 事件类型

| 事件名 | 说明 |
| --- | --- |
| `status` | 任务状态变更 |
| `delta` | 摘要增量文本 |
| `metrics` | Token 压缩指标或阶段耗时 |
| `result` | 最终结果摘要 |
| `error` | 错误信息 |
| `done` | 流结束 |

### 示例事件

```text
event: status
data: {"taskId":"task-uuid","status":"running","stage":"video_preprocess","progress":15}

event: metrics
data: {"taskId":"task-uuid","patchTokensPerFrame":196,"visualTokensPerFrame":5,"compressionRatio":0.0255}

event: delta
data: {"taskId":"task-uuid","text":"视频开头展示了主要场景。"}

event: done
data: {"taskId":"task-uuid","status":"finished"}
```

### 业务规则

- Java 后端负责 SSE 连接管理。
- Python 推理服务只向 Redis 写入流式消息。
- 任务已经完成时，该接口应返回已有结果和 `done` 事件，避免前端空等。
- 任务失败时发送 `error` 和 `done`。

## 7. GET /api/results/{taskId}

查询任务最终结果。

### 响应数据

```json
{
  "taskId": "task-uuid",
  "status": "finished",
  "summary": "视频展示了一个完整事件过程，包括场景建立、主体行动和结果呈现。",
  "keyEvents": [
    {
      "timeRange": "00:00-00:05",
      "description": "视频开头出现主要场景"
    }
  ],
  "tokenMetrics": {
    "patchTokensPerFrame": 196,
    "visualTokensPerFrame": 5,
    "contentTokensPerFrame": 3,
    "contextTokensPerFrame": 2,
    "compressionRatio": 0.0255,
    "compressionText": "196 → 5"
  },
  "runtimeMetrics": {
    "runMode": "mock",
    "preprocessMs": 120,
    "featureExtractMs": 80,
    "tokenCompressMs": 20,
    "generationMs": 1500,
    "totalMs": 1720
  },
  "createdAt": "2026-05-09T10:31:00Z",
  "finishedAt": "2026-05-09T10:31:20Z"
}
```

### 业务规则

- 任务未完成时返回当前状态和空结果，或返回 `RESULT_NOT_READY`。
- 任务失败时返回错误信息和失败日志摘要。

## 8. GET /api/health

查询 Java 业务服务健康状态。

### 响应数据

```json
{
  "service": "backend-java",
  "status": "up",
  "time": "2026-05-09T10:30:00Z",
  "dependencies": {
    "redis": "up",
    "inferenceService": "up"
  }
}
```

## 9. 推理服务内部接口

Python FastAPI 推理服务可提供内部健康检查：

```text
GET /health
```

该接口供 Java 后端或运维脚本检查，不直接暴露给前端页面。

## 10. 错误码建议

| 错误码 | 说明 |
| --- | --- |
| `VIDEO_NOT_FOUND` | 视频不存在 |
| `VIDEO_INVALID_FORMAT` | 视频格式不支持 |
| `VIDEO_TOO_LARGE` | 视频过大 |
| `TASK_NOT_FOUND` | 任务不存在 |
| `TASK_ALREADY_FINISHED` | 任务已完成，不能重复操作 |
| `RESULT_NOT_READY` | 结果尚未生成 |
| `REDIS_UNAVAILABLE` | Redis 不可用 |
| `INFERENCE_UNAVAILABLE` | 推理服务不可用 |
| `MODEL_NOT_CONFIGURED` | real mode 模型路径未配置 |
| `INTERNAL_ERROR` | 系统内部错误 |

## 11. 第 4 阶段 Redis 异步链路约定

第 4 阶段在保留 in-memory mode 的同时增加 Redis mode。Java 后端通过 `app.mode=redis` 切换为 Redis 异步任务链路，Python worker 通过 `MMVS_REDIS_ENABLED=true` 消费任务。

### Redis Key

| Key | 类型 | 说明 |
| --- | --- | --- |
| `queue:mmvs:tasks` | Redis Stream | Java 后端写入待推理任务，Python worker 阻塞读取 |
| `task:{taskId}` | Hash | 保存任务状态、`videoPath`、`queryText`、`createdAt`、`updatedAt` |
| `stream:task:{taskId}` | Redis Stream | 保存任务流式事件，Java SSE 接口读取并转发 |
| `result:{taskId}` | String JSON | 保存最终摘要、Token 指标和耗时 |

### Stream Event 类型

`stream:task:{taskId}` 中事件字段为：

- `status`
- `stage`
- `token_metrics`
- `summary_delta`
- `completed`
- `error`

每条 Redis Stream 消息至少包含：

```json
{
  "eventType": "summary_delta",
  "stage": "summary_generation",
  "payload": "{\"text\":\"视频摘要片段\"}"
}
```

### 启动顺序

```bash
docker compose up -d redis
cd inference-python && MMVS_REDIS_ENABLED=true python -m app.worker
cd backend-java && APP_MODE=redis mvn spring-boot:run
cd frontend-vue && VITE_APP_MODE=backend npm run dev
```

### 字段命名合并约定

为解决前后端与 Redis worker 的字段命名差异，第 4 阶段统一对外保留以下语义字段：

- `taskId`：任务 ID。
- `status`：任务状态。
- `stage`：当前推理阶段。
- `tokenMetrics`：Token 指标对象；Redis Stream 中同时允许 `token_metrics` 事件名。
- `summaryDelta`：摘要增量文本；Redis Stream 中同时允许 `summary_delta` 事件名。
- `completed`：任务完成标记，对应 `completed` 事件。
- `error`：错误信息，对应 `error` 事件。

Java SSE 转发层需要兼容 Redis Stream 中的 snake_case 事件名和 camelCase payload 字段，Vue 前端也需要同时兼容 `summary_delta` / `summaryDelta` 与 `token_metrics` / `tokenMetrics`。

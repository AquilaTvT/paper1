# API 接口说明

本文档整理当前项目主要 REST API、SSE 事件和 Python 推理接口，用于论文第 3 章需求分析、第 5 章详细设计和第 7 章接口测试。Java 后端基础路径为 `http://localhost:8080`，Python 推理服务基础路径为 `http://localhost:8000`。

## 1. 通用响应格式

Java 后端接口统一返回：

```json
{
  "success": true,
  "data": {},
  "message": "ok",
  "errorCode": null,
  "requestId": "req_202605110001"
}
```

错误响应示例：

```json
{
  "success": false,
  "data": null,
  "message": "任务不存在",
  "errorCode": "TASK_NOT_FOUND",
  "requestId": "req_202605110002"
}
```

## 2. Java 后端接口

### 2.1 GET /api/health

| 字段 | 说明 |
| --- | --- |
| 接口用途 | 检查 Java 后端是否启动，并返回当前推理模式与 Redis 开关 |
| 请求方法 | `GET` |
| URL | `/api/health` |
| 请求参数 | 无 |
| 对应前端调用文件 | 可由浏览器或接口测试工具直接访问；前端业务接口集中在 `frontend-vue/src/api/taskApi.ts` |
| 对应后端 Controller | `backend-java/src/main/java/com/mmvs/controller/HealthController.java` |

响应示例：

```json
{
  "success": true,
  "data": {
    "service": "backend-java",
    "status": "up",
    "mode": "redis",
    "redisEnabled": true,
    "h2JpaEnabled": false,
    "time": "2026-05-11T08:00:00Z"
  },
  "message": "ok",
  "errorCode": null,
  "requestId": "req_demo"
}
```

错误示例：

```json
{
  "success": false,
  "data": null,
  "message": "后端服务未启动或端口不可达",
  "errorCode": "CONNECTION_REFUSED",
  "requestId": "manual-test"
}
```

### 2.2 POST /api/videos/upload

| 字段 | 说明 |
| --- | --- |
| 接口用途 | 上传本地视频文件，生成后续创建任务所需的 `videoId` |
| 请求方法 | `POST` |
| URL | `/api/videos/upload` |
| 请求参数 | `multipart/form-data`，字段名为 `file`；支持 `mp4`、`mov`、`avi`、`mkv` |
| 对应前端调用文件 | `frontend-vue/src/api/taskApi.ts` 中的 `uploadVideo` |
| 对应后端 Controller | `backend-java/src/main/java/com/mmvs/controller/VideoController.java` |

请求示例：

```bash
curl -F "file=@demo.mp4" http://localhost:8080/api/videos/upload
```

响应示例：

```json
{
  "success": true,
  "data": {
    "videoId": "video_202605110001",
    "originalFileName": "demo.mp4",
    "storedPath": "uploads/videos/video_202605110001.mp4",
    "fileSize": 1048576,
    "contentType": "video/mp4",
    "createdAt": "2026-05-11T08:00:00Z"
  },
  "message": "ok",
  "errorCode": null,
  "requestId": "req_demo"
}
```

错误示例：

```json
{
  "success": false,
  "data": null,
  "message": "仅支持 mp4、mov、avi、mkv 视频格式",
  "errorCode": "VIDEO_INVALID_FORMAT",
  "requestId": "req_demo"
}
```

### 2.3 POST /api/tasks

| 字段 | 说明 |
| --- | --- |
| 接口用途 | 根据 `videoId` 和用户指令创建摘要任务 |
| 请求方法 | `POST` |
| URL | `/api/tasks` |
| 请求参数 | JSON：必填 `videoId`，可传 `instruction` 或 `queryText`；前端会附带 `runMode`、`stream` 作为兼容字段，Java DTO 会使用有效查询文本 |
| 对应前端调用文件 | `frontend-vue/src/api/taskApi.ts` 中的 `createTask` |
| 对应后端 Controller | `backend-java/src/main/java/com/mmvs/controller/TaskController.java`；任务投递由 `TaskDispatchService` 执行 |

请求示例：

```json
{
  "videoId": "video_202605110001",
  "instruction": "请概括视频中的关键事件",
  "runMode": "mock",
  "stream": true
}
```

响应示例：

```json
{
  "success": true,
  "data": {
    "taskId": "task_202605110001",
    "videoId": "video_202605110001",
    "queryText": "请概括视频中的关键事件",
    "status": "waiting",
    "currentStage": "waiting",
    "progress": 0,
    "tokenMetrics": null,
    "logs": []
  },
  "message": "ok",
  "errorCode": null,
  "requestId": "req_demo"
}
```

错误示例：

```json
{
  "success": false,
  "data": null,
  "message": "videoId 不能为空或视频不存在",
  "errorCode": "BAD_REQUEST",
  "requestId": "req_demo"
}
```

### 2.4 GET /api/tasks

| 字段 | 说明 |
| --- | --- |
| 接口用途 | 查询任务列表，用于历史任务展示 |
| 请求方法 | `GET` |
| URL | `/api/tasks` |
| 请求参数 | 无 |
| 对应前端调用文件 | `frontend-vue/src/api/taskApi.ts` 中的 `getTasks` |
| 对应后端 Controller | `backend-java/src/main/java/com/mmvs/controller/TaskController.java` |

响应示例：

```json
{
  "success": true,
  "data": [
    {
      "taskId": "task_202605110001",
      "videoId": "video_202605110001",
      "status": "finished",
      "currentStage": "finished",
      "progress": 100
    }
  ],
  "message": "ok",
  "errorCode": null,
  "requestId": "req_demo"
}
```

错误示例：

```json
{
  "success": false,
  "data": null,
  "message": "任务列表查询失败",
  "errorCode": "TASK_LIST_ERROR",
  "requestId": "req_demo"
}
```

### 2.5 GET /api/tasks/{taskId}

| 字段 | 说明 |
| --- | --- |
| 接口用途 | 查询单个任务详情、状态、日志和 Token 指标 |
| 请求方法 | `GET` |
| URL | `/api/tasks/{taskId}` |
| 请求参数 | 路径参数：`taskId` |
| 对应前端调用文件 | `frontend-vue/src/api/taskApi.ts` 中的 `getTask` |
| 对应后端 Controller | `backend-java/src/main/java/com/mmvs/controller/TaskController.java` |

响应示例：

```json
{
  "success": true,
  "data": {
    "taskId": "task_202605110001",
    "status": "streaming",
    "currentStage": "summary_generation",
    "progress": 85,
    "tokenMetrics": {
      "rawPatchTokensPerFrame": 196,
      "compressedTokensPerFrame": 5,
      "compressionRatio": 39.2
    },
    "streamChunks": ["系统完成视频预处理。"]
  },
  "message": "ok",
  "errorCode": null,
  "requestId": "req_demo"
}
```

错误示例：

```json
{
  "success": false,
  "data": null,
  "message": "任务不存在：task_unknown",
  "errorCode": "TASK_NOT_FOUND",
  "requestId": "req_demo"
}
```

### 2.6 GET /api/tasks/{taskId}/events

| 字段 | 说明 |
| --- | --- |
| 接口用途 | 订阅任务 SSE 事件，接收状态、阶段、Token 指标、摘要片段和完成事件 |
| 请求方法 | `GET` |
| URL | `/api/tasks/{taskId}/events` |
| 请求参数 | 路径参数：`taskId` |
| 对应前端调用文件 | `frontend-vue/src/api/sseClient.ts` 中的 `connectTaskEvents` |
| 对应后端 Controller / Service | `TaskController.java`、`SseEmitterService.java`、Redis 模式下的 `RedisStreamEventConsumer.java` |

SSE 响应示例：

```text
event: status
data: {"taskId":"task_202605110001","eventType":"status","status":"running","stage":"video_preprocess"}

event: token_metrics
data: {"taskId":"task_202605110001","eventType":"token_metrics","stage":"token_compression","tokenMetrics":{"rawPatchTokensPerFrame":196,"compressedTokensPerFrame":5,"compressionRatio":39.2}}

event: summary_delta
data: {"taskId":"task_202605110001","eventType":"summary_delta","status":"streaming","stage":"summary_generation","summaryDelta":"系统正在生成摘要片段。"}
```

错误示例：

```text
event: error
data: {"taskId":"task_202605110001","eventType":"error","status":"failed","stage":"redis_worker","error":"推理任务执行失败"}
```

### 2.7 GET /api/results/{taskId}

| 字段 | 说明 |
| --- | --- |
| 接口用途 | 查询任务最终摘要、关键事件和运行指标 |
| 请求方法 | `GET` |
| URL | `/api/results/{taskId}` |
| 请求参数 | 路径参数：`taskId` |
| 对应前端调用文件 | 当前前端以 SSE 完成事件为主要展示来源，后续结果页可调用 `frontend-vue/src/api/taskApi.ts` 扩展结果接口 |
| 对应后端 Controller | `backend-java/src/main/java/com/mmvs/controller/ResultController.java` |

响应示例：

```json
{
  "success": true,
  "data": {
    "taskId": "task_202605110001",
    "summary": "视频展示了若干关键事件，系统完成了抽帧、编码、压缩与摘要生成。",
    "keyEvents": ["完成视频预处理", "完成 Token 压缩"],
    "tokenMetrics": {
      "rawPatchTokensPerFrame": 196,
      "compressedTokensPerFrame": 5,
      "compressionRatio": 39.2
    }
  },
  "message": "ok",
  "errorCode": null,
  "requestId": "req_demo"
}
```

错误示例：

```json
{
  "success": false,
  "data": null,
  "message": "结果尚未生成",
  "errorCode": "RESULT_NOT_READY",
  "requestId": "req_demo"
}
```

## 3. Python 推理服务接口

### 3.1 GET /health

| 字段 | 说明 |
| --- | --- |
| 接口用途 | 检查 Python FastAPI 推理服务是否启动 |
| 请求方法 | `GET` |
| URL | `/health` |
| 请求参数 | 无 |
| 对应前端调用文件 | 前端不直接调用，由联调或接口测试工具访问 |
| 对应 Python module | `inference-python/app/main.py` |

响应示例：

```json
{
  "service": "mmvs-inference-python",
  "status": "up",
  "run_mode": "mock",
  "redis_enabled": true,
  "real_model_ready": false
}
```

错误示例：

```json
{
  "detail": "Python 推理服务未启动或端口 8000 不可达"
}
```

### 3.2 POST /infer

| 字段 | 说明 |
| --- | --- |
| 接口用途 | 执行推理 pipeline，返回完整推理结果；当前 mock-first，可为 real mode 保留入口 |
| 请求方法 | `POST` |
| URL | `/infer` |
| 请求参数 | JSON：`task_id`、`video_path`、`query_text` |
| 对应前端调用文件 | 前端不直接调用；Redis backend mode 由 Python worker 调用 pipeline |
| 对应 Python module | `inference-python/app/main.py`、`inference-python/app/services/inference_pipeline.py` |

请求示例：

```json
{
  "task_id": "task_202605110001",
  "video_path": "uploads/videos/demo.mp4",
  "query_text": "请总结视频关键事件"
}
```

响应示例：

```json
{
  "task_id": "task_202605110001",
  "run_mode": "mock",
  "token_metrics": {
    "sampled_frames": 8,
    "raw_patch_tokens_per_frame": 196,
    "compressed_tokens_per_frame": 5,
    "raw_visual_tokens": 1568,
    "compressed_visual_tokens": 40,
    "compression_ratio": 39.2
  },
  "summary": "系统完成视频预处理、视觉编码、Token 压缩和摘要生成。"
}
```

错误示例：

```json
{
  "detail": "patch_features 必须是 [T, P, C] 三维张量"
}
```

### 3.3 POST /mock-infer

| 字段 | 说明 |
| --- | --- |
| 接口用途 | 直接运行 mock 推理流程，适合本地接口测试和论文第 7 章截图 |
| 请求方法 | `POST` |
| URL | `/mock-infer` |
| 请求参数 | JSON：`task_id`、`video_path`、`query_text` |
| 对应前端调用文件 | 前端不直接调用；主要用于 Python 服务独立测试 |
| 对应 Python module | `inference-python/app/main.py`、`inference-python/app/services/inference_pipeline.py` |

响应示例与 `/infer` 一致。错误示例：

```json
{
  "detail": "请求体缺少 task_id、video_path 或 query_text"
}
```

## 4. SSE 事件字段说明

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `taskId` | string | 任务唯一标识 |
| `eventType` | string | 事件类型：`status`、`stage`、`token_metrics`、`summary_delta`、`completed`、`error` |
| `status` | string | 当前任务状态：`waiting`、`running`、`streaming`、`finished`、`failed`、`cancelled` |
| `stage` | string | 当前处理阶段，例如 `video_preprocess`、`video_swin`、`token_compression`、`projection_adapter`、`summary_generation` |
| `tokenMetrics` | object | Token 压缩指标，核心字段为 `rawPatchTokensPerFrame=196`、`compressedTokensPerFrame=5`、`compressionRatio=39.2` |
| `summaryDelta` | string | 本次新增摘要片段，前端逐段追加展示 |
| `completed` | object | 任务完成时的最终摘要、关键事件和耗时信息 |
| `error` | string | 失败原因，用于前端错误提示和论文异常测试 |

# 数据模型设计

## 1. 设计原则

系统数据模型围绕视频文件、推理任务、推理结果、任务日志、流式消息和 Token 指标展开。第 0 阶段先定义稳定字段，后续实现时可选择内存存储、文件存储、关系型数据库或 Redis 组合实现。

通用约定：

- 主键 ID 使用 UUID 字符串。
- 时间字段使用 ISO-8601 格式。
- 状态字段使用小写字符串。
- 运行模式使用 `mock` 或 `real`。
- Token 压缩核心指标固定体现 `196 → 5`。

## 2. VideoFile

视频文件模型，记录上传视频或示例视频的元数据。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `videoId` | string | 是 | 视频 ID |
| `originalName` | string | 是 | 原始文件名 |
| `storagePath` | string | 是 | 后端存储路径或示例文件路径 |
| `mimeType` | string | 是 | MIME 类型 |
| `sizeBytes` | number | 是 | 文件大小 |
| `durationSeconds` | number | 否 | 视频时长 |
| `width` | number | 否 | 视频宽度 |
| `height` | number | 否 | 视频高度 |
| `fps` | number | 否 | 原始帧率 |
| `source` | string | 是 | `upload` 或 `sample` |
| `status` | string | 是 | `available`、`deleted` 或 `invalid` |
| `remark` | string | 否 | 备注 |
| `createdAt` | string | 是 | 创建时间 |
| `updatedAt` | string | 是 | 更新时间 |

示例：

```json
{
  "videoId": "video-uuid",
  "originalName": "demo.mp4",
  "storagePath": "uploads/2026/05/video-uuid.mp4",
  "mimeType": "video/mp4",
  "sizeBytes": 10485760,
  "durationSeconds": 32.5,
  "width": 1280,
  "height": 720,
  "fps": 25,
  "source": "upload",
  "status": "available",
  "remark": "毕业设计演示视频",
  "createdAt": "2026-05-09T10:30:00Z",
  "updatedAt": "2026-05-09T10:30:00Z"
}
```

## 3. InferenceTask

推理任务模型，描述一次视频理解与摘要请求的生命周期。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `taskId` | string | 是 | 任务 ID |
| `videoId` | string | 是 | 关联视频 ID |
| `instruction` | string | 是 | 用户指令 |
| `runMode` | string | 是 | `mock` 或 `real` |
| `status` | string | 是 | `waiting`、`running`、`streaming`、`finished`、`failed`、`cancelled` |
| `progress` | number | 是 | 进度百分比，0 到 100 |
| `currentStage` | string | 否 | 当前阶段 |
| `parameters` | object | 否 | 任务参数 |
| `errorMessage` | string | 否 | 错误信息 |
| `createdAt` | string | 是 | 创建时间 |
| `startedAt` | string | 否 | 开始时间 |
| `updatedAt` | string | 是 | 更新时间 |
| `finishedAt` | string | 否 | 结束时间 |

`currentStage` 建议值：

- `queued`
- `video_preprocess`
- `feature_extraction`
- `token_compression`
- `projection_adapter`
- `summary_generation`
- `result_persist`

示例：

```json
{
  "taskId": "task-uuid",
  "videoId": "video-uuid",
  "instruction": "请总结视频中的关键事件",
  "runMode": "mock",
  "status": "streaming",
  "progress": 70,
  "currentStage": "summary_generation",
  "parameters": {
    "sampleFps": 1,
    "maxSummaryTokens": 512,
    "language": "zh-CN"
  },
  "errorMessage": null,
  "createdAt": "2026-05-09T10:31:00Z",
  "startedAt": "2026-05-09T10:31:03Z",
  "updatedAt": "2026-05-09T10:31:12Z",
  "finishedAt": null
}
```

## 4. InferenceResult

推理结果模型，记录最终摘要、关键事件和指标。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `resultId` | string | 是 | 结果 ID |
| `taskId` | string | 是 | 任务 ID |
| `videoId` | string | 是 | 视频 ID |
| `summary` | string | 是 | 最终摘要 |
| `keyEvents` | array | 是 | 关键事件列表 |
| `timeline` | array | 否 | 时间线描述 |
| `tokenMetrics` | TokenMetrics | 是 | Token 压缩指标 |
| `runtimeMetrics` | object | 是 | 阶段耗时指标 |
| `runMode` | string | 是 | `mock` 或 `real` |
| `modelInfo` | object | 否 | 模型信息 |
| `createdAt` | string | 是 | 创建时间 |

`keyEvents` 子项字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `timeRange` | string | 否 | 时间范围，例如 `00:00-00:05` |
| `description` | string | 是 | 事件描述 |
| `confidence` | number | 否 | 置信度 |

示例：

```json
{
  "resultId": "result-uuid",
  "taskId": "task-uuid",
  "videoId": "video-uuid",
  "summary": "视频展示了主要场景、主体动作和结果变化。",
  "keyEvents": [
    {
      "timeRange": "00:00-00:05",
      "description": "视频开头出现主要场景",
      "confidence": 0.91
    }
  ],
  "timeline": [
    {
      "timestamp": "00:00",
      "text": "场景建立"
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
    "preprocessMs": 120,
    "featureExtractMs": 80,
    "tokenCompressMs": 20,
    "generationMs": 1500,
    "totalMs": 1720
  },
  "runMode": "mock",
  "modelInfo": {
    "featureExtractor": "mock-video-swin",
    "generator": "mock-summary-generator"
  },
  "createdAt": "2026-05-09T10:31:20Z"
}
```

## 5. TaskLog

任务日志模型，记录任务状态变更、异常和关键阶段信息。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `logId` | string | 是 | 日志 ID |
| `taskId` | string | 是 | 任务 ID |
| `level` | string | 是 | `INFO`、`WARN`、`ERROR` |
| `stage` | string | 否 | 所属阶段 |
| `message` | string | 是 | 日志内容 |
| `detail` | object | 否 | 结构化详情 |
| `createdAt` | string | 是 | 创建时间 |

示例：

```json
{
  "logId": "log-uuid",
  "taskId": "task-uuid",
  "level": "INFO",
  "stage": "token_compression",
  "message": "patch tokens compressed from 196 to 5 per frame",
  "detail": {
    "patchTokensPerFrame": 196,
    "visualTokensPerFrame": 5
  },
  "createdAt": "2026-05-09T10:31:10Z"
}
```

## 6. StreamMessage

流式消息模型，Python 推理服务写入 Redis，Java 后端通过 SSE 转发给前端。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `messageId` | string | 是 | 消息 ID |
| `taskId` | string | 是 | 任务 ID |
| `eventType` | string | 是 | `status`、`delta`、`metrics`、`result`、`error`、`done` |
| `sequence` | number | 是 | 单任务内递增序号 |
| `payload` | object | 是 | 事件负载 |
| `createdAt` | string | 是 | 创建时间 |

示例：

```json
{
  "messageId": "msg-uuid",
  "taskId": "task-uuid",
  "eventType": "delta",
  "sequence": 5,
  "payload": {
    "text": "随后主体开始移动。"
  },
  "createdAt": "2026-05-09T10:31:14Z"
}
```

## 7. TokenMetrics

Token 指标模型，用于展示算法核心压缩效果和论文测试分析。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `patchTokensPerFrame` | number | 是 | 单帧原始视觉 Patch Token 数，默认 196 |
| `visualTokensPerFrame` | number | 是 | 单帧压缩后视觉 Token 数，默认 5 |
| `contentTokensPerFrame` | number | 是 | Content Token 数，建议 3 |
| `contextTokensPerFrame` | number | 是 | Context Token 数，建议 2 |
| `totalSampledFrames` | number | 否 | 采样帧数 |
| `totalPatchTokens` | number | 否 | 总原始 Patch Token 数 |
| `totalVisualTokens` | number | 否 | 总压缩 Token 数 |
| `compressionRatio` | number | 是 | 压缩后数量 / 原始数量，默认 5 / 196 = 0.0255 |
| `compressionText` | string | 是 | 展示文本，固定体现 `196 → 5` |
| `method` | string | 是 | 压缩方法说明 |

示例：

```json
{
  "patchTokensPerFrame": 196,
  "visualTokensPerFrame": 5,
  "contentTokensPerFrame": 3,
  "contextTokensPerFrame": 2,
  "totalSampledFrames": 16,
  "totalPatchTokens": 3136,
  "totalVisualTokens": 80,
  "compressionRatio": 0.0255,
  "compressionText": "196 → 5",
  "method": "dual-track content/context token compression"
}
```

## 8. Redis Key 设计

| Key | 类型 | 说明 |
| --- | --- | --- |
| `paper1:queue:tasks` | Stream | 异步任务队列 |
| `paper1:task:{taskId}:state` | Hash/String | 任务状态缓存 |
| `paper1:task:{taskId}:events` | Stream | 任务流式消息 |
| `paper1:task:{taskId}:result` | String | 最终结果 JSON |
| `paper1:task:{taskId}:metrics` | String | TokenMetrics JSON |
| `paper1:task:{taskId}:logs` | List/Stream | 任务日志 |

## 9. 存储演进建议

- 早期阶段可使用 Redis 和本地 JSON 文件完成演示。
- 中期阶段可引入 H2、SQLite 或 MySQL 保存任务历史。
- 论文演示优先保证链路完整和数据可解释。
- 若引入数据库，表结构应与本文档字段保持一致。

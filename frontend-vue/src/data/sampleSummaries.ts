export const summaryTemplates = [
  '系统首先完成视频抽帧，并按照 1 FPS 的采样率构建视觉输入序列。',
  'Video Swin Transformer 模块提取了时空视觉特征，用于描述场景、主体动作与事件变化。',
  '双轨 Token 压缩模块将单帧 196 个 Patch Token 压缩为 5 个视觉 Token，其中包含 Content Token 与 Context Token。',
  'MLP Projection Adapter 将压缩后的视觉表示映射到文本生成模型可理解的语义空间。',
  '根据用户指令，系统生成了面向论文演示的结构化摘要，并保留 Token 压缩指标用于测试分析。',
];

export const keyEventTemplates = [
  '00:00-00:05：完成视频载入与关键帧采样。',
  '00:05-00:12：提取场景、动作和上下文视觉特征。',
  '00:12-00:18：执行 Content Token 与 Context Token 双轨压缩。',
  '00:18-00:25：通过 MLP Adapter 对齐视觉与文本语义空间。',
  '00:25 之后：生成摘要并输出测试指标。',
];

<template>
  <section class="card architecture-card">
    <div class="card-heading">
      <div>
        <h2>处理流程</h2>
        <p>从视频上传到摘要输出，各环节保持清晰可追踪。</p>
      </div>
    </div>

    <div class="flow-grid">
      <div class="flow-node frontend">
        <strong>前端页面</strong>
        <span>上传、预览、输入指令</span>
      </div>
      <div class="flow-arrow">任务提交</div>
      <div class="flow-node backend">
        <strong>任务服务</strong>
        <span>创建任务、维护状态</span>
      </div>
      <div class="flow-arrow">队列</div>
      <div class="flow-node redis">
        <strong>状态与消息</strong>
        <span>进度记录、结果缓存</span>
      </div>
      <div class="flow-arrow">回传</div>
      <div class="flow-node inference">
        <strong>摘要生成</strong>
        <span>采样、分析、组织文本</span>
      </div>
    </div>
  </section>
</template>

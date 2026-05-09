<template>
  <section class="card architecture-card">
    <div class="card-heading">
      <div>
        <h2>系统架构流程图</h2>
        <p>对应论文总体设计：前后端分离、业务层与计算层分离。</p>
      </div>
    </div>

    <div class="flow-grid">
      <div class="flow-node frontend">
        <strong>Vue 前端</strong>
        <span>上传、指令、SSE 展示</span>
      </div>
      <div class="flow-arrow">REST / SSE</div>
      <div class="flow-node backend">
        <strong>Java Spring Boot</strong>
        <span>任务管理、状态查询、SSE 转发</span>
      </div>
      <div class="flow-arrow">Redis Stream</div>
      <div class="flow-node redis">
        <strong>Redis</strong>
        <span>队列、状态缓存、流式消息中转</span>
      </div>
      <div class="flow-arrow">异步消费</div>
      <div class="flow-node inference">
        <strong>Python FastAPI</strong>
        <span>Video Swin、Token 压缩、MLP Adapter、LLM</span>
      </div>
    </div>
  </section>
</template>

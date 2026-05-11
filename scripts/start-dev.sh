#!/usr/bin/env bash
set -euo pipefail

cat <<'HELP'
Release candidate 本地联调启动顺序：

1. 启动 Redis：
   docker compose up -d redis

2. 启动 Java 后端（Redis mode）：
   cd backend-java
   MMVS_INFERENCE_MODE=redis MMVS_REDIS_ENABLED=true mvn spring-boot:run

3. 启动 Python worker（另开终端）：
   cd inference-python
   pip install -r requirements.txt
   MMVS_REDIS_ENABLED=true python -m app.worker

4. 启动 Vue 前端 backend mode（另开终端）：
   cd frontend-vue
   npm install
   VITE_API_MODE=backend VITE_API_BASE_URL=http://localhost:8080/api npm run dev

保留 in-memory/mock mode：
- Java 不设置 MMVS_INFERENCE_MODE=redis 时使用内存 mock scheduler。
- Vue 不设置 VITE_API_MODE=backend 时使用浏览器 mock 流程。
HELP

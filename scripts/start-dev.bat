@echo off
echo 第 4 阶段本地联调启动顺序：
echo.
echo 1. 启动 Redis：
echo    docker compose up -d redis
echo.
echo 2. 启动 Java 后端（Redis mode）：
echo    cd backend-java
echo    set MMVS_INFERENCE_MODE=redis
echo    set MMVS_REDIS_ENABLED=true
echo    mvn spring-boot:run
echo.
echo 3. 启动 Python worker（另开终端）：
echo    cd inference-python
echo    pip install -r requirements.txt
echo    set MMVS_REDIS_ENABLED=true
echo    python -m app.worker
echo.
echo 4. 启动 Vue 前端 backend mode（另开终端）：
echo    cd frontend-vue
echo    npm install
echo    set VITE_API_MODE=backend
echo    set VITE_API_BASE_URL=http://localhost:8080/api
echo    npm run dev
echo.
echo 保留 in-memory/mock mode：Java 不设置 redis 模式时走内存 mock；Vue 不设置 backend 模式时走浏览器 mock。

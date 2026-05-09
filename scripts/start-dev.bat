@echo off
echo [1/4] Starting Redis...
docker compose up -d redis
echo [2/4] Start Python worker: cd inference-python ^&^& set MMVS_REDIS_ENABLED=true ^&^& python -m app.worker
echo [3/4] Start Java backend: cd backend-java ^&^& set APP_MODE=redis ^&^& mvn spring-boot:run
echo [4/4] Start Vue frontend: cd frontend-vue ^&^& set VITE_APP_MODE=backend ^&^& npm run dev

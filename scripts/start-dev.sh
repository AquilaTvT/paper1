#!/usr/bin/env bash
set -euo pipefail

echo "[1/4] Starting Redis..."
docker compose up -d redis

echo "[2/4] Start Python worker in another terminal:"
echo "cd inference-python && MMVS_REDIS_ENABLED=true python -m app.worker"

echo "[3/4] Start Java backend in another terminal:"
echo "cd backend-java && APP_MODE=redis mvn spring-boot:run"

echo "[4/4] Start Vue frontend in another terminal:"
echo "cd frontend-vue && VITE_APP_MODE=backend npm run dev"

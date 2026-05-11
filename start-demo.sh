#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$ROOT_DIR/.demo-logs"
PID_DIR="$ROOT_DIR/.demo-pids"
mkdir -p "$LOG_DIR" "$PID_DIR"

start_service() {
  local name="$1"
  local workdir="$2"
  local command="$3"
  local pid_file="$PID_DIR/$name.pid"
  local log_file="$LOG_DIR/$name.log"

  if [[ -f "$pid_file" ]] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
    echo "$name 已在运行，PID $(cat "$pid_file")"
    return
  fi

  echo "启动 $name ..."
  (cd "$workdir" && bash -lc "$command") >"$log_file" 2>&1 &
  echo $! >"$pid_file"
  echo "$name 日志：$log_file"
}

start_service "python" "$ROOT_DIR/inference-python" "uvicorn app.main:app --reload --port 8000"
start_service "java" "$ROOT_DIR/backend-java" "mvn spring-boot:run"
start_service "frontend" "$ROOT_DIR/frontend-vue" "npm run dev"

echo "已启动。前端地址：http://localhost:5173"
echo "停止请运行：./stop-demo.sh"

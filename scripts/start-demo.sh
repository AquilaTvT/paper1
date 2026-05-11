#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="$ROOT_DIR/.demo-logs"
PID_DIR="$LOG_DIR/pids"
mkdir -p "$PID_DIR"

need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "缺少命令：$1"
    exit 1
  fi
}

need_cmd node
need_cmd npm
need_cmd java
need_cmd mvn

if command -v python3.11 >/dev/null 2>&1; then
  PYTHON_BIN="python3.11"
else
  echo "缺少 Python3.11。请先安装 Python 3.11 后再启动正式分析演示。"
  exit 1
fi

start_service() {
  local name="$1"
  local port="$2"
  local workdir="$3"
  local logfile="$4"
  shift 4

  if command -v lsof >/dev/null 2>&1 && lsof -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
    echo "$name 已有进程监听端口 $port，跳过启动。"
    return
  fi

  echo "启动 $name（端口 $port），日志：$logfile"
  (
    cd "$workdir"
    "$@"
  ) >"$logfile" 2>&1 &
  echo $! > "$PID_DIR/$name.pid"
}

start_service "python" 8000 "$ROOT_DIR/inference-python" "$LOG_DIR/python.log" \
  "$PYTHON_BIN" -m uvicorn app.main:app --host 0.0.0.0 --port 8000

start_service "java" 8080 "$ROOT_DIR/backend-java" "$LOG_DIR/java.log" \
  env MMVS_INFERENCE_MODE=python MMVS_PYTHON_BASE_URL=http://localhost:8000 mvn spring-boot:run

start_service "frontend" 5173 "$ROOT_DIR/frontend-vue" "$LOG_DIR/frontend.log" \
  env VITE_API_MODE=backend VITE_API_BASE_URL=http://localhost:8080/api VITE_PYTHON_HEALTH_URL=http://localhost:8000/health npm run dev -- --host 0.0.0.0

echo
echo "一体化演示正在启动。"
echo "访问地址：http://localhost:5173"
echo "日志目录：$LOG_DIR"
echo "进程 PID：$PID_DIR"
echo "停止演示：./scripts/stop-demo.sh"
echo
echo "如果页面显示服务离线，请等待数秒后点击“重新检测”，或查看对应日志文件。"

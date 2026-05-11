#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="$ROOT_DIR/.demo-logs"
PID_DIR="$LOG_DIR/pids"

stop_pid_file() {
  local file="$1"
  [ -f "$file" ] || return 0
  local pid
  pid="$(cat "$file")"
  if [ -n "$pid" ] && kill -0 "$pid" >/dev/null 2>&1; then
    echo "停止进程 $pid（$(basename "$file" .pid)）"
    kill "$pid" >/dev/null 2>&1 || true
  fi
  rm -f "$file"
}

if [ -d "$PID_DIR" ]; then
  for file in "$PID_DIR"/*.pid; do
    [ -e "$file" ] || continue
    stop_pid_file "$file"
  done
fi

for port in 5173 8080 8000; do
  if command -v lsof >/dev/null 2>&1; then
    pids="$(lsof -tiTCP:"$port" -sTCP:LISTEN || true)"
    if [ -n "$pids" ]; then
      echo "停止端口 $port：$pids"
      kill $pids >/dev/null 2>&1 || true
    fi
  elif command -v fuser >/dev/null 2>&1; then
    echo "尝试停止端口 $port"
    fuser -k "$port/tcp" >/dev/null 2>&1 || true
  fi
done

echo "演示服务已停止。"

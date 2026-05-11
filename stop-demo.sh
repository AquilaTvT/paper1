#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_DIR="$ROOT_DIR/.demo-pids"

if [[ ! -d "$PID_DIR" ]]; then
  echo "没有发现运行中的演示服务。"
  exit 0
fi

for pid_file in "$PID_DIR"/*.pid; do
  [[ -e "$pid_file" ]] || continue
  name="$(basename "$pid_file" .pid)"
  pid="$(cat "$pid_file")"
  if kill -0 "$pid" 2>/dev/null; then
    echo "停止 $name，PID $pid"
    kill "$pid" 2>/dev/null || true
  else
    echo "$name 未在运行。"
  fi
  rm -f "$pid_file"
done

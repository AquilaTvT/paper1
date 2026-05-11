#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PYTHON_DIR="$ROOT_DIR/inference-python"
PYTHON_BIN="${PYTHON:-python}"

if ! command -v "$PYTHON_BIN" >/dev/null 2>&1; then
  echo "[python] 未找到 Python。可设置 PYTHON=/path/to/python，建议 Python 3.10+。" >&2
  exit 1
fi

echo "[python] Python: $($PYTHON_BIN --version)"
echo "[python] 检查语法并运行 pytest。"
cd "$PYTHON_DIR"
"$PYTHON_BIN" -m compileall app
"$PYTHON_BIN" -m pytest -q

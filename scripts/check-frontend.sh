#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND_DIR="$ROOT_DIR/frontend-vue"

if ! command -v npm >/dev/null 2>&1; then
  echo "[frontend] npm 未安装。请先安装 Node.js >= 18 和 npm。" >&2
  exit 1
fi

if ! command -v node >/dev/null 2>&1; then
  echo "[frontend] node 未安装。请先安装 Node.js >= 18。" >&2
  exit 1
fi

echo "[frontend] Node: $(node -v)"
echo "[frontend] npm: $(npm -v)"
echo "[frontend] 安装依赖并执行生产构建。"
cd "$FRONTEND_DIR"
npm install
npm run build

#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/backend-java"

if ! command -v mvn >/dev/null 2>&1; then
  echo "[backend] Maven 未安装。请先安装 Maven，并确认使用 JDK 17。" >&2
  exit 1
fi

if ! command -v java >/dev/null 2>&1; then
  echo "[backend] Java 未安装。请先安装 JDK 17。" >&2
  exit 1
fi

echo "[backend] Java: $(java -version 2>&1 | head -n 1)"
echo "[backend] Maven: $(mvn -version | head -n 1)"
echo "[backend] 执行 Maven package（跳过测试）。"
cd "$BACKEND_DIR"
mvn -q -DskipTests package

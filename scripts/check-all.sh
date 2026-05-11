#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "== MMVS release candidate check =="
echo "1/3 frontend-vue"
"$ROOT_DIR/scripts/check-frontend.sh"

echo "2/3 backend-java"
"$ROOT_DIR/scripts/check-backend.sh"

echo "3/3 inference-python"
"$ROOT_DIR/scripts/check-python.sh"

echo "所有检查已完成。"

#!/usr/bin/env bash
# 编辑后验证脚本：按变更文件类型分发到对应检查
# 用法: scripts/validate_edit.sh <变更文件的相对路径>
# 可接入任意支持"编辑后钩子"的 AI 工具，也可手动执行。
set -u -o pipefail
cd "$(dirname "$0")/.."

file="${1:-}"
case "$file" in
  src/main/java/com/hmdp/*.java)
    name="$(basename "$file" .java)"
    mvn test -q -Dtest="${name}*" 2>&1 | tail -20
    ;;
  frontend/src/*.vue|frontend/src/*.js)
    (cd frontend && npx vite build 2>&1 | tail -15)
    ;;
  src/main/resources/*.yaml|src/main/resources/*.yml)
    mvn compile -q 2>&1 | tail -10
    ;;
  *)
    echo "无匹配的验证规则: $file"
    ;;
esac

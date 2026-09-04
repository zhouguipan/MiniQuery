#!/usr/bin/env bash
# 从服务端下载表情图集，覆盖 App 内置的 assets/emoticon.png
# 用法：在项目根目录执行  bash scripts/fetch_assets.sh
set -euo pipefail

BASE="${BASE:-http://81.71.23.66:18088}"
DEST="app/src/main/assets/emoticon.png"

echo "==> 下载 $BASE/emoticon.png"
curl -fsSL --max-time 60 "$BASE/emoticon.png" -o "$DEST.tmp"

# 简单校验：PNG 文件头
if head -c 4 "$DEST.tmp" | grep -q $'\x89PNG'; then
  mv "$DEST.tmp" "$DEST"
  echo "==> 已更新 $DEST ($(wc -c < "$DEST") bytes)"
else
  rm -f "$DEST.tmp"
  echo "!! 下载内容不是 PNG，已放弃（可能是无权访问或被拦截）" >&2
  exit 1
fi

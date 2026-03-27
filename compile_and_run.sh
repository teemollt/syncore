#!/bin/bash
set -e

ROOT="$(cd "$(dirname "$0")" && pwd)"
SRC="$ROOT/src"
OUT="$ROOT/out"

echo "=== 컴파일 중... ==="
mkdir -p "$OUT"
find "$SRC" -name "*.java" | xargs javac -d "$OUT" -encoding UTF-8

echo "=== 실행 ==="
java -cp "$OUT" SyncoreMain

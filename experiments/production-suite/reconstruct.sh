#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="${1:-$ROOT/extracted}"
PAYLOAD="${RUNNER_TEMP:-/tmp}/production-suite-source.tar.xz"
mkdir -p "$OUT"
cat "$ROOT"/experiments/production-suite/parts/part-* | base64 -d > "$PAYLOAD"
echo "4c051448ba81d035217ba8ca31b42cd5ed4a1620071a428b4bb6146f46740f6a  $PAYLOAD" | sha256sum -c -
tar -xJf "$PAYLOAD" -C "$OUT"
test "$(find "$OUT/springboot-vue-nine-projects" -mindepth 1 -maxdepth 1 -type d -name '[0-9][0-9]-*' | wc -l)" -eq 9

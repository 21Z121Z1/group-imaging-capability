#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="${1:-$ROOT/extracted}"
PAYLOAD="${RUNNER_TEMP:-/tmp}/production-suite-source.tar.xz"
PATCH_B64="${RUNNER_TEMP:-/tmp}/production-suite-v2.patch.xz.b64"
PATCH_XZ="${RUNNER_TEMP:-/tmp}/production-suite-v2.patch.xz"
mkdir -p "$OUT"
cat "$ROOT"/experiments/production-suite/parts/part-* | base64 -d > "$PAYLOAD"
echo "4c051448ba81d035217ba8ca31b42cd5ed4a1620071a428b4bb6146f46740f6a  $PAYLOAD" | sha256sum -c -
tar -xJf "$PAYLOAD" -C "$OUT"
cp "$ROOT/experiments/production-suite/v2.patch.xz.b64" "$PATCH_B64"
echo "4059f17cc42cd71d4dd10bb3e3fe2987c18d1448140840d484a1d035bc26224a  $PATCH_B64" | sha256sum -c -
base64 -d "$PATCH_B64" > "$PATCH_XZ"
echo "9656746fd5cc82e37c64d87e8b4d56e6ce69615fe4775b63c3519a9765478578  $PATCH_XZ" | sha256sum -c -
(
  cd "$OUT/springboot-vue-nine-projects"
  xz -dc "$PATCH_XZ" | patch -p1 --batch --fuzz=0
)
test "$(find "$OUT/springboot-vue-nine-projects" -mindepth 1 -maxdepth 1 -type d -name '[0-9][0-9]-*' | wc -l)" -eq 9

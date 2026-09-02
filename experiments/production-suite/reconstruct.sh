#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="${1:-$ROOT/extracted}"
PAYLOAD="${RUNNER_TEMP:-/tmp}/production-suite-source.tar.xz"
V2_B64="${RUNNER_TEMP:-/tmp}/production-suite-v2.patch.xz.b64"
V2_XZ="${RUNNER_TEMP:-/tmp}/production-suite-v2.patch.xz"
V3_B64="${RUNNER_TEMP:-/tmp}/production-suite-v3.patch.xz.b64"
V3_XZ="${RUNNER_TEMP:-/tmp}/production-suite-v3.patch.xz"
V4_B64="${RUNNER_TEMP:-/tmp}/production-suite-v4.patch.xz.b64"
V4_XZ="${RUNNER_TEMP:-/tmp}/production-suite-v4.patch.xz"
mkdir -p "$OUT"
cat "$ROOT"/experiments/production-suite/parts/part-* | base64 -d > "$PAYLOAD"
echo "4c051448ba81d035217ba8ca31b42cd5ed4a1620071a428b4bb6146f46740f6a  $PAYLOAD" | sha256sum -c -
tar -xJf "$PAYLOAD" -C "$OUT"
cp "$ROOT/experiments/production-suite/v2.patch.xz.b64" "$V2_B64"
echo "4059f17cc42cd71d4dd10bb3e3fe2987c18d1448140840d484a1d035bc26224a  $V2_B64" | sha256sum -c -
base64 -d "$V2_B64" > "$V2_XZ"
echo "9656746fd5cc82e37c64d87e8b4d56e6ce69615fe4775b63c3519a9765478578  $V2_XZ" | sha256sum -c -
cp "$ROOT/experiments/production-suite/v3.patch.xz.b64" "$V3_B64"
echo "c6e6edd1426c254f5100c825f1f2dd647211482d198f4d1779b4a2b596d42a74  $V3_B64" | sha256sum -c -
base64 -d "$V3_B64" > "$V3_XZ"
echo "e6194712b57431960e20f7a98c02882725fff5c2183c9a6f07276b17e2620452  $V3_XZ" | sha256sum -c -
cp "$ROOT/experiments/production-suite/v4.patch.xz.b64" "$V4_B64"
echo "64d2fc41a39788a9576b6c3fb2139b213bd674741c48bd39e3dfaabe97adc75c  $V4_B64" | sha256sum -c -
base64 -d "$V4_B64" > "$V4_XZ"
echo "f1df0d406b16ae7cba9707c6f7db52b4b7e79eefe601743cf28ed1e6477153e4  $V4_XZ" | sha256sum -c -
(
  cd "$OUT/springboot-vue-nine-projects"
  xz -dc "$V2_XZ" | patch -p1 --batch --fuzz=0
  xz -dc "$V3_XZ" | patch -p1 --batch --fuzz=0
  xz -dc "$V4_XZ" | patch -p1 --batch --fuzz=0
)
test "$(find "$OUT/springboot-vue-nine-projects" -mindepth 1 -maxdepth 1 -type d -name '[0-9][0-9]-*' | wc -l)" -eq 9

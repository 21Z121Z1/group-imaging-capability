#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
for p in "$ROOT"/[0-9][0-9]-*; do
  echo "==> Backend: $(basename "$p")"
  (cd "$p/backend" && mvn -q test)
  echo "==> Frontend: $(basename "$p")"
  (cd "$p/frontend" && npm install --silent && npm run build --silent)
done
echo "All 18 build/test stages passed."

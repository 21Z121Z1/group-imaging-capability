#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
python3 "$ROOT/tools/static_validate.py"
python3 "$ROOT/tools/deep_validate.py"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
javac -d "$TMP" "$ROOT/tools/ParseCheck.java"
java -cp "$TMP" ParseCheck "$ROOT"
python3 - "$ROOT" <<'PY'
from pathlib import Path
import re, subprocess, sys, tempfile
root=Path(sys.argv[1]); checked=0
for p in sorted(root.glob('[0-9][0-9]-*')):
    app=(p/'frontend/src/App.vue').read_text(); m=re.search(r'<script setup>(.*?)</script>',app,re.S)
    if not m: raise SystemExit(f'Missing script setup: {p}')
    temp=Path(tempfile.gettempdir())/(p.name+'-App.mjs');temp.write_text(m.group(1))
    for f in [temp,p/'frontend/src/api.js',p/'frontend/src/main.js',p/'frontend/vite.config.js']:
        subprocess.run(['node','--check',str(f)],check=True); checked+=1
print(f'NODE SYNTAX PASSED: {checked} modules')
PY

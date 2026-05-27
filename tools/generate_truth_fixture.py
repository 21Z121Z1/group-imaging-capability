#!/usr/bin/env python3
from __future__ import annotations

import importlib.util
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE_DIR = Path("/Volumes/42APFS/x9")
OUTPUTS = [
    ROOT / "fixtures" / "oplus_truth_fixture.json",
    ROOT / "app" / "src" / "main" / "assets" / "oplus_truth_fixture.json",
]

DECODER_PATH = ROOT.parent / "scripts" / "decode_oplus_usercomment.py"
SPEC = importlib.util.spec_from_file_location("decode_oplus_usercomment", DECODER_PATH)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC is not None and SPEC.loader is not None
sys.modules[SPEC.name] = MODULE
SPEC.loader.exec_module(MODULE)

SAMPLES = [
    "IMG20260306113440.jpg",
    "IMG20260304155953_BURST043.jpg",
    "IMG20260203165233_preview.jpg",
    "IMG20260218152415.jpg",
]


def read_exif(path: Path) -> dict:
    out = subprocess.check_output(
        [
            "exiftool",
            "-q",
            "-q",
            "-json",
            "-Make",
            "-Model",
            "-UserComment",
            "-FocalLength",
            "-FocalLengthIn35mmFormat",
            "-MIMEType",
            str(path),
        ],
        text=True,
    )
    return json.loads(out)[0]


def main() -> int:
    rows = []
    for name in SAMPLES:
        path = SOURCE_DIR / name
        exif = read_exif(path)
        decoded = MODULE.decode_value_payload(exif["UserComment"], [0, 1, 2, 3])
        rows.append(
            {
                "file_name": name,
                "make": exif.get("Make"),
                "model": exif.get("Model"),
                "user_comment": exif.get("UserComment"),
                "focal_length": exif.get("FocalLength"),
                "focal_length_eq": exif.get("FocalLengthIn35mmFormat"),
                "mime_type": exif.get("MIMEType"),
                "decoded": decoded,
            }
        )

    payload = {
        "source_dir": str(SOURCE_DIR),
        "sample_count": len(rows),
        "samples": rows,
    }
    for output in OUTPUTS:
        output.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

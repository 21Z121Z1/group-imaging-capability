#!/usr/bin/env python3

import argparse
import sqlite3
import tempfile
from pathlib import Path


ASSET_SCHEMA = """
CREATE TABLE media_assets (
    assetId TEXT PRIMARY KEY,
    mediaStoreId INTEGER NOT NULL,
    path TEXT,
    relativePath TEXT,
    uri TEXT NOT NULL,
    fileName TEXT NOT NULL,
    mimeType TEXT NOT NULL,
    size INTEGER NOT NULL,
    createdAt INTEGER NOT NULL,
    modifiedAt INTEGER NOT NULL,
    isOplusOriginal INTEGER NOT NULL,
    isLivePhoto INTEGER NOT NULL,
    isRaw INTEGER NOT NULL,
    pairedCaptureId TEXT,
    deviceModel TEXT,
    focalLength REAL,
    focalLengthEq INTEGER,
    lensClass TEXT NOT NULL,
    captureModeLabel TEXT,
    userCommentRaw TEXT,
    userCommentDigest TEXT,
    parseStatus TEXT NOT NULL,
    parseVersion INTEGER NOT NULL,
    sourceConfidence REAL NOT NULL,
    capturePairStatus TEXT NOT NULL,
    contentSignature TEXT NOT NULL
);
CREATE INDEX index_media_assets_mediaStoreId ON media_assets(mediaStoreId);
"""

SESSION_SCHEMA = """
CREATE TABLE capture_sessions (
    captureId TEXT PRIMARY KEY,
    primaryAssetId TEXT NOT NULL,
    pairedRawAssetId TEXT,
    captureTime INTEGER NOT NULL,
    deviceModel TEXT,
    lensClass TEXT NOT NULL,
    focalLengthEq INTEGER,
    isLivePhoto INTEGER NOT NULL,
    isRawCapture INTEGER NOT NULL,
    captureModeLabel TEXT
);
"""


def repeated_text(seed: str, target_size: int) -> str:
    repeat = (target_size // len(seed)) + 1
    return (seed * repeat)[:target_size]


def build_asset_row(index: int, keep_raw_comment: bool, raw_ratio: float):
    is_raw = 1 if (index % 10) < int(raw_ratio * 10) else 0
    user_comment_raw = repeated_text("oplus_comment_payload_", 768) if keep_raw_comment else None
    user_comment_digest = None if keep_raw_comment else f"digest-{index:08d}-" + ("a" * 48)
    return (
        f"asset-{index:08d}",
        index,
        f"/storage/emulated/0/DCIM/Camera/IMG_{index:08d}.JPG",
        "DCIM/Camera/",
        f"content://media/external/images/media/{index}",
        f"IMG_{index:08d}.JPG",
        "image/jpeg",
        4_500_000 + (index % 1024),
        1_700_000_000_000 + index * 1000,
        1_700_000_000_000 + index * 1000,
        1,
        0,
        is_raw,
        None,
        "PKJ110",
        5.58,
        23,
        "MAIN",
        "夜景",
        user_comment_raw,
        user_comment_digest,
        "PARSED",
        2,
        0.95,
        "PRIMARY",
        f"sig-{index:08d}",
    )


def build_session_row(index: int):
    return (
        f"capture-{index:08d}",
        f"asset-{index:08d}",
        None,
        1_700_000_000_000 + index * 1000,
        "PKJ110",
        "MAIN",
        23,
        0,
        0,
        "夜景",
    )


def populate_database(db_path: Path, count: int, keep_raw_comment: bool, raw_ratio: float):
    conn = sqlite3.connect(db_path)
    conn.executescript(ASSET_SCHEMA)
    conn.executescript(SESSION_SCHEMA)
    asset_sql = """
        INSERT INTO media_assets (
            assetId, mediaStoreId, path, relativePath, uri, fileName, mimeType, size,
            createdAt, modifiedAt, isOplusOriginal, isLivePhoto, isRaw, pairedCaptureId,
            deviceModel, focalLength, focalLengthEq, lensClass, captureModeLabel,
            userCommentRaw, userCommentDigest, parseStatus, parseVersion, sourceConfidence,
            capturePairStatus, contentSignature
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """
    session_sql = """
        INSERT INTO capture_sessions (
            captureId, primaryAssetId, pairedRawAssetId, captureTime, deviceModel,
            lensClass, focalLengthEq, isLivePhoto, isRawCapture, captureModeLabel
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """
    chunk_size = 1000
    for start in range(0, count, chunk_size):
        end = min(start + chunk_size, count)
        conn.executemany(
            asset_sql,
            [build_asset_row(index, keep_raw_comment, raw_ratio) for index in range(start, end)],
        )
        conn.executemany(
            session_sql,
            [build_session_row(index) for index in range(start, end)],
        )
        conn.commit()
    conn.execute("VACUUM")
    conn.close()


def measure_scenario(output_dir: Path, count: int, keep_raw_comment: bool, raw_ratio: float):
    scenario = "current" if keep_raw_comment else "recommended"
    db_path = output_dir / f"{scenario}_{count}.sqlite"
    if db_path.exists():
        db_path.unlink()
    populate_database(db_path, count, keep_raw_comment, raw_ratio)
    size_bytes = db_path.stat().st_size
    return db_path, size_bytes


def format_size(size_bytes: int) -> str:
    mib = size_bytes / (1024 * 1024)
    return f"{mib:.2f} MiB"


def main():
    parser = argparse.ArgumentParser(description="Estimate SQLite size for media_assets and capture_sessions.")
    parser.add_argument(
        "--counts",
        nargs="+",
        type=int,
        default=[1000, 10000, 100000],
        help="Photo counts to synthesize.",
    )
    parser.add_argument(
        "--raw-ratio",
        type=float,
        default=0.1,
        help="Fraction of rows treated as RAW companions.",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=Path(tempfile.gettempdir()) / "group_imaging_db_size",
        help="Directory for generated SQLite files.",
    )
    args = parser.parse_args()

    args.output_dir.mkdir(parents=True, exist_ok=True)
    print(f"Writing sample databases to {args.output_dir}")
    print("scenario,count,size,file")
    for count in args.counts:
        for keep_raw_comment in (True, False):
            db_path, size_bytes = measure_scenario(args.output_dir, count, keep_raw_comment, args.raw_ratio)
            scenario = "current" if keep_raw_comment else "recommended"
            print(f"{scenario},{count},{format_size(size_bytes)},{db_path}")


if __name__ == "__main__":
    main()

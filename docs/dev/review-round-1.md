# Code Review Round 1

Date: 2026-05-27
Scope: All 48 Kotlin source files under `app/src/main/java/`

## Findings

### 1. Regex recompiled on every call in hot path
- **File**: `core/CaptureSessionMatcher.kt:75`
- **Severity**: Medium
- `Regex("(_RAW|_JPG)$")` is compiled inside `stemsMatch()`, which is called O(n) times during session building. Should be a pre-compiled companion val.

### 2. Duplicated `sha256Hex` utility
- **Files**: `core/OplusClassifier.kt:72`, `data/GroupImagingRepository.kt:721`
- **Severity**: Low
- Identical SHA-256 hex digest logic duplicated in two classes. Should be extracted to a shared utility.

### 3. ~~Missing `InsightDimension.MODE` case~~ (False positive — MODE is handled at line 345)

### 4. `captureModes` not exposed in Insight UI
- **File**: `ui/insight/InsightFeature.kt`
- **Severity**: Low (design gap)
- `FilterSpec` supports `captureModes` but `InsightFiltersUi` has no field for it. The UI does not expose capture mode filtering. Not a bug — the field is simply unused in the Insight feature.

### 5. ScanWorker swallows exception details
- **File**: `data/scan/ScanWorker.kt:38`
- **Severity**: Medium
- `Result.failure()` is returned without logging the error. The exception is silently lost, making production debugging difficult.

### 6. Unused `scanStageLabel` function
- **File**: `ui/scan/progress/ScanProgressFeature.kt:160`
- **Severity**: Low
- `scanStageLabel` is defined but never called. The screen uses `scanStageLabel(state.stage)` in the TopBar subtitle — wait, actually it IS used on line 201. False positive on re-check.

### 7. O(n²) asset lookup in CaptureSessionMatcher
- **File**: `core/CaptureSessionMatcher.kt:42,66`
- **Severity**: Medium
- `updated.indexOfFirst { it.assetId == rawMatch.assetId }` is O(n) per iteration. For large libraries, this compounds to O(n²). Should use a map lookup.

### 8. `filteredSessions` loads all sessions into memory
- **File**: `data/GroupImagingRepository.kt:596`
- **Severity**: Medium
- Every filter query loads the entire capture_sessions table into memory then filters in Kotlin. For large libraries this is wasteful. Could use Room DAO queries with WHERE clauses.

### 9. Hardcoded Chinese strings in domain layer
- **File**: `core/OplusCommentDecoder.kt:40-46`, `data/GroupImagingRepository.kt` (various)
- **Severity**: Low
- Chinese strings like "大师", "全景", "未知机型" are hardcoded in domain/data layers. Should be in string resources for i18n.

### 10. `MediaScanner.getLongOrNull` treats 0 as null
- **File**: `core/MediaScanner.kt:143`
- **Severity**: Low
- `.takeIf { it > 0L }` means epoch timestamp 0 (1970-01-01) is treated as null. While unlikely for real photos, this is semantically incorrect.

## Priority Fixes (Round 1)

1. Fix #1 — Pre-compile regex in CaptureSessionMatcher ✓
2. Fix #2 — Extract shared sha256Hex to core/CryptoUtil.kt ✓
3. Fix #5 — Log exception in ScanWorker ✓
4. Fix #7 — Map-based assetId lookup in CaptureSessionMatcher ✓
5. #3 was false positive (MODE already handled)
6. #4 was false positive (design gap, not a bug)

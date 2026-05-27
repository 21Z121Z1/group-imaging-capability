# Code Review Round 3

Date: 2026-05-27
Scope: All Kotlin source files (re-review after Round 2 fixes)

## Findings

### 1. MediaScanner.getLongOrNull treats epoch 0 as null
- **File**: `core/MediaScanner.kt:143`
- **Severity**: Low (semantic bug)
- `.takeIf { it > 0L }` discards valid timestamp 0 (1970-01-01). While unlikely for real photos, this is semantically wrong. The check should only guard against the cursor returning 0 for missing values.

### 2. Fully qualified FocalEqRange references in OplusClassifier
- **File**: `core/OplusClassifier.kt:93-106`
- **Severity**: Low (readability)
- `com.oplus.groupimaging.domain.FocalEqRange` is used with full path instead of importing. Should use import.

### 3. Fully qualified CapturePairStatus in CaptureSessionMatcher
- **File**: `core/CaptureSessionMatcher.kt:47`
- **Severity**: Low (readability)
- `com.oplus.groupimaging.domain.CapturePairStatus.RAW_COMPANION` used inline. Should import.

### 4. GroupImagingRepository.loadRuleGroups uses getAll() then filters in Kotlin
- **File**: `data/GroupImagingRepository.kt:349-358`
- **Severity**: Low (performance)
- Loads all capture sessions just to count by lens class. Could use COUNT queries.

### 5. `@OptIn(ExperimentalCoroutinesApi::class)` may be unnecessary
- **File**: `data/GroupImagingRepository.kt:67`
- **Severity**: Low
- `limitedParallelism` was promoted to stable in kotlinx-coroutines 1.7+. If the project uses 1.7+, this annotation is unnecessary.

### 6. ScanProgressViewModel.applyScanJob uses state.errorMessage instead of job error
- **File**: `ui/scan/progress/ScanProgressFeature.kt:148,154`
- **Severity**: Low (potential stale data)
- When a FAILED ScanJob arrives, `errorMessage` reads from the current state rather than the job. If `startScan` hasn't completed yet, the error message may be null.

### 7. defaultDeviceProfiles FocalEqRange repeated across profiles
- **File**: `core/OplusClassifier.kt:88-117`
- **Severity**: Low (duplication)
- Both device profiles define identical focal ranges (0-18, 19-34, 35+). Could extract shared ranges.

## Priority Fixes (Round 3)

1. Fix #2 — Import FocalEqRange in OplusClassifier
2. Fix #3 — Import CapturePairStatus in CaptureSessionMatcher
3. Fix #1 — Fix getLongOrNull semantic issue
4. Fix #7 — Extract shared focal ranges

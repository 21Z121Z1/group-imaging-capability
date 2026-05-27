# Code Review Round 2

Date: 2026-05-27
Scope: All Kotlin source files (re-review after Round 1 fixes)

## Findings

### 1. InsightViewModel.load() makes 7 sequential suspend calls
- **File**: `ui/insight/InsightFeature.kt:186-192`
- **Severity**: Medium (performance)
- Seven `observeInsight()` calls run sequentially. Each hits the database and does in-memory filtering. Should use `coroutineScope { async {} }` to run concurrently.

### 2. GroupImagingRepository.refreshLibrary is 180+ lines
- **File**: `data/GroupImagingRepository.kt:91-273`
- **Severity**: Medium (maintainability)
- Single method handles scan setup, metadata extraction loop, session building, cursor management, and error recovery. Should be decomposed.

### 3. OplusClassifier.resolveLens lowercases deviceModel per profile iteration
- **File**: `core/OplusClassifier.kt:74`
- **Severity**: Low (performance)
- `deviceModel?.lowercase(Locale.ROOT).orEmpty()` is computed inside the `firstOrNull` lambda, repeated for each profile. Should be hoisted.

### 4. AppRoot Saver uses unsafe casts
- **File**: `navigation/AppRoot.kt:270-276`
- **Severity**: Medium (crash risk)
- `saved.getValue("progress") as Float` will ClassCastException if the value is a Double (JSON deserialization often produces Doubles). Should use safe casts with fallbacks.

### 5. CaptureSessionMatcher raw matching still O(n) per non-raw asset
- **File**: `core/CaptureSessionMatcher.kt:19`
- **Severity**: Low (performance)
- `sorted.firstOrNull { candidate -> candidate.isRaw && ... }` iterates all assets for each non-raw asset. Could pre-group raw assets by device+path.

### 6. GroupImagingRepository.executeMove deeply nested
- **File**: `data/GroupImagingRepository.kt:417-516`
- **Severity**: Medium (maintainability)
- 100-line method with nested forEach, when, runCatching. Hard to follow control flow.

### 7. InsightFiltersUi saver restores with getOrNull + unsafe casts
- **File**: `ui/insight/InsightFeature.kt:446-456`
- **Severity**: Low (crash risk)
- `restored.getOrNull(5) as? Boolean ?: false` is fine, but the pattern is inconsistent with the rest of the codebase.

### 8. CalendarViewModel emits effect inside updateState
- **File**: `ui/calendar/CalendarFeature.kt:97-100`
- **Severity**: Low
- `onAction(CalendarAction.OnViewDayStatsClick)` calls `emitEffect` after `currentState()`. This is correct but the pattern of reading state after an update could be racy if Compose recomposes between.

## Priority Fixes (Round 2)

1. Fix #1 — Parallelize InsightViewModel.load() calls
2. Fix #3 — Hoist lowercase in resolveLens
3. Fix #4 — Safe casts in AppRoot Saver
4. Fix #5 — Pre-group raw assets in CaptureSessionMatcher

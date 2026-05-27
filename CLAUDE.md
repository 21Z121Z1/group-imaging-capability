# Group Imaging Capability

Android offline OPPO/Oplus photo library categorization & archival app. Scans MediaStore images on-device, parses EXIF + Oplus `UserComment`, builds CaptureSession index, categorizes by device/lens/focal-length/date/RAW/Live/shooting-mode, and supports previewing matched files + RAW companions before moving originals.

## Architecture

- **Language**: Kotlin, compileSdk 35, minSdk 29
- **DI**: Hilt (Dagger)
- **DB**: Room with generation-protected full-scan
- **UI**: Jetpack Compose, Material 3
- **Background**: WorkManager foreground scan
- **Testing**: JUnit, Roborazzi screenshot regression, Compose test rules
- **Build**: Gradle KSP, no Groovy

## Key Modules

- `core/` — MediaScanner, CaptureSessionMatcher, MetadataExtractor, OplusClassifier, OplusCommentDecoder
- `data/` — Room DB, DAOs, Entities, ScanScheduler, ScanWorker, GroupImagingRepository
- `domain/` — Use cases (observe/execute patterns), Models, Repository interface
- `ui/` — Feature screens (Home, Calendar, Insight, Albums, Settings, Failed, Scan), BaseViewModel, UiContracts, Theme
- `navigation/` — AppRoute, AppRoot, RootChrome, InsightFilterSeed
- `di/` — AppModule (Hilt)
- `sharedTest/` — FakeInsightRepository, HarnessScenario, UiStateFixtures, ComposeHarness
- `tools/` — generate_truth_fixture.py, estimate_db_size.py

## Build & Test Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run JVM unit tests
./gradlew testDebugUnitTest

# Run Roborazzi screenshot tests
./gradlew recordRoborazziDebug

# Verify screenshot regression
./gradlew verifyRoborazziDebug
```

**NOTE**: Android SDK 35 + JDK 17 required. No Android emulator/SDK on this server — cannot run `./gradlew` tests. Code review and refactoring only.

## Code Conventions

- Kotlin coroutines + Flow for async
- Compose state hoisting — screens are pure functions, ViewModels hold state
- Use cases are single-responsibility, injected via Hilt
- Room migrations use explicit version steps
- Test fixtures in `sharedTest/` serve both unit and instrumentation tests
- `TestTags.kt` for stable Compose test selectors (no text-based selectors)

## Current Constraints

- `release` build uses debug-initialized `releaseLike` signing (local/alpha only)
- Android 14+ may only grant Selected Photos Access
- Android 11+ moving non-app media needs system write authorization
- No root required — uses MediaStore URI + authorization

# Verification scope

The Android application is validated on every pull request with Android API 37 / Android 17 tooling.

Required cloud gates:

- `:app:testDebugUnitTest`
- `:app:lintDebug`
- `:app:assembleDebug`
- `:app:assembleDebugAndroidTest`
- `:app:assembleRelease`

The debug APK is uploaded as the `unmark-debug-apk` workflow artifact. Device-specific ColorOS screenshot rendering and OEM Ultra HDR behavior remain device acceptance checks; the app therefore provides both standard and HDR calibration flows instead of hard-coding one OEM compositor model.

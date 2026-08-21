# ColorOS Predictive Back Lab

A small Android 17 test app for separating **app-owned predictive back animation** from **system-owned ColorOS seamless transitions**.

The implementation is adapted from the idea demonstrated by [`JacobHu0723/PredictiveBackGesture`](https://github.com/JacobHu0723/PredictiveBackGesture) (MIT): bind visual state to predictive-back progress. This repository adds an explicit commit/cancel continuation model and, critically, stops consuming Back at navigation roots so ColorOS Shell/Launcher can own cross-activity and back-to-home transitions.

## Why this shape

ColorOS 17's private Shell continuation machinery is system/signature-only. A normal APK should not reflectively call `ShellContinuousTransitionController`, `AdaptiveSmoothShellAnimManager`, or Launcher internals. The useful compatibility contract is instead ownership discipline:

1. While an app-internal detail layer owns Back, `BackEvent.progress` drives its transform.
2. On commit, the completion animator starts from the exact last gesture progress; there is no reset frame.
3. Once internal navigation is complete, the app unregisters its callback.
4. Activity-to-Activity and root-to-Home Back are left untouched so Android/ColorOS Shell can run predictive system animations and launcher icon morphs.

## Test matrix

### A. In-app seamless handoff

Open **In-app handoff demo** and swipe back slowly.

- During the gesture, telemetry reads `phase=gesture`.
- Release to commit: it changes to `phase=commit-continuation` and continues from the current geometry.
- Abort the gesture: it uses `phase=cancel-settle` and returns to the original state.
- After a successful internal back, the callback is unregistered. A second edge-back is system-owned.

### B. System cross-activity predictive back

Open **System-owned Activity**, then edge-swipe back.

`SystemBackActivity` contains no `OnBackInvokedCallback`, no `OnBackAnimationCallback`, no `overridePendingTransition()`, and no remote animation. This is intentionally the clean path for comparing ColorOS Shell behavior.

### C. ColorOS back-to-home / Launcher handoff

Launch the app from the home screen and, while on `MainActivity`, edge-swipe back to Home. `MainActivity` does not consume Back. On a ColorOS build/package that passes the vendor eligibility gates, Launcher is free to run its predictive-continuous close/icon-morph path.

## Build

The project targets Android 17 / API 37 and uses AGP 9.3 with Gradle 9.5, matching the repository CI.

```bash
gradle :app:assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Upstream

See `THIRD_PARTY_NOTICES.md` for the upstream MIT attribution.

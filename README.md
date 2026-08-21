# ColorOS Predictive Back Lab

A small Android 17 test app for separating **content-level predictive shared-element morphs**, **app-owned predictive-back continuation**, and **system-owned ColorOS seamless transitions**.

The implementation is adapted from the idea demonstrated by [`JacobHu0723/PredictiveBackGesture`](https://github.com/JacobHu0723/PredictiveBackGesture) (MIT): bind visual state to predictive-back progress. This repository adds explicit commit/cancel continuation, a measured shared-element destination lab, and—critically—stops consuming Back at navigation roots so ColorOS Shell/Launcher can own cross-activity and back-to-home transitions.

## Why this shape

There are two different things that are often both called "seamless":

1. **Timeline continuity**: the frame at gesture release is handed directly to the completion animation without resetting.
2. **Content continuity**: a visual object on the detail screen has a known destination object on the previous screen, so its geometry can morph back into that exact image/card/title.

The first does not require corresponding content. The second does.

ColorOS 17's private Shell continuation machinery is system/signature-only. A normal APK should not reflectively call `ShellContinuousTransitionController`, `AdaptiveSmoothShellAnimManager`, or Launcher internals. The useful compatibility contract is instead ownership discipline:

1. While an app-internal detail layer owns Back, `BackEvent.progress` drives its transform.
2. On commit, the completion animator starts from the exact last gesture progress; there is no reset frame.
3. For a shared-element path, the list destination stays composed underneath and its source bounds are measured before the detail transition.
4. Once internal navigation is complete, the app unregisters its callback.
5. Activity-to-Activity and root-to-Home Back are left untouched so Android/ColorOS Shell can run predictive system animations and launcher icon morphs.

## Test matrix

### A. Content-level shared-element predictive morph

Open **Shared-element lab**.

The screen has two rows:

- **MATCHED DESTINATION / Azure Ridge**: the detail hero image and title use the exact measured geometry of the thumbnail and title as their predictive-back destination.
- **NO MATCH / Unmatched Detail**: the detail page uses only a generic scale/fade continuation. There is deliberately no destination element.

For the matched path:

- Forward navigation visibly expands the thumbnail into the detail hero.
- During edge-back, `BackEvent.progress` directly controls hero scale/translation, title scale/translation, corner radius, destination reveal, and non-shared detail content alpha.
- The list remains rendered underneath.
- At `progress=1`, the moving hero/title are at the exact measured source geometry.
- Commit continues from the last progress and then atomically reveals the original list elements while removing the overlay.
- Cancellation continues from the last progress back to the detail geometry.

This is the demo to use when evaluating whether "the previous page needs corresponding content" for a real content morph.

### B. In-app seamless handoff

Open **In-app handoff demo** and swipe back slowly.

- During the gesture, telemetry reads `phase=gesture`.
- Release to commit: it changes to `phase=commit-continuation` and continues from the current geometry.
- Abort the gesture: it uses `phase=cancel-settle` and returns to the original state.
- After a successful internal back, the callback is unregistered. A second edge-back is system-owned.

### C. System cross-activity predictive back

Open **System-owned Activity**, then edge-swipe back.

`SystemBackActivity` contains no `OnBackInvokedCallback`, no `OnBackAnimationCallback`, no `overridePendingTransition()`, and no remote animation. This is intentionally the clean path for comparing ColorOS Shell behavior.

### D. ColorOS back-to-home / Launcher handoff

Launch the app from the home screen and, while on `MainActivity`, edge-swipe back to Home. `MainActivity` does not consume Back. On a ColorOS build/package that passes the vendor eligibility gates, Launcher is free to run its predictive-continuous close/icon-morph path.

## Implementation note

The shared-element lab intentionally uses framework Views and GPU transforms rather than bringing in a navigation framework. The matched hero keeps a fixed 4:3 backing size and interpolates scale/translation to the measured 4:3 list thumbnail, which avoids per-frame relayout. The title is treated as a second shared element. Only corner-radius redraw and ordinary property changes occur during progress.

This mirrors the current Android guidance conceptually: shared content has stable identities/destinations, predictive back controls a pre-commit/deferred phase, and the completion transition takes over without a reset.

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

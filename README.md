# ColorOS OEM Predictive Back / Seamless Lab

This Android 17 test app now separates **real ColorOS-owned animation paths** from **app/framework comparison implementations**.

The important correction is that ColorOS has more than one mechanism commonly described as "seamless":

1. **SystemUI/Shell predictive continuous** — task/window SurfaceControl leash state is handed from predictive Back into the next default/remote transition.
2. **ColorOS Launcher predictive close** — the task/window is continued into Launcher-owned app-close/icon morph logic.
3. **ColorOS Gallery content seamless** — Gallery keeps a trigger-view protocol with a measured source Rect, drawing bitmap, and corner radius and performs its own grid/photo morph.
4. **Android/app shared-element comparison** — useful as a control only; it is not the ColorOS OEM implementation.

## Reverse-engineered ColorOS 17 evidence

From the supplied ColorOS 17 APKs:

### SystemUI / Shell

`com.android.systemui` contains:

- `BackAnimationController`
- `BackAnimationController.BackTransitionHandler`
- `AdaptiveSmoothShellAnimManager`
- `com.oplus.transition.ShellContinuousTransitionController`
- `com.oplus.transition.SpringSlideAnimationController`

The OEM controller contains explicit predictive-continuous operations including:

- `matchPredictiveContinuous(...)`
- `prepareBackAnimContinueIfNeed(...)`
- `mergeContinuousBackTransitionIfNeed(...)`
- `setBackAnimContinuous(...)`
- `setBackPredictiveToRemoteContinuous(...)`
- `setRemoteToBackAnimContinuous(...)`
- `adjustFinishTransactionForPredictive(...)`

It carries transformation/alpha/corner-radius state across transition boundaries and applies it to real `SurfaceControl` leashes.

### Oplus WCT / Transition extension markers

ColorOS also ships `com.oplus.transition.SharedReflectionHelper`, which accesses ColorOS framework extension objects such as:

- `android.window.OplusWCTExtendInfo`
- `android.window.OplusTransitionExtendedInfo`

and methods including:

- `setContinuousTransition(WindowContainerTransaction, boolean)`
- `setBackAnimContinuous(WindowContainerTransaction, boolean)`
- `setIsPredictiveBackAnimation(...)`

These are OEM extensions, not Android SDK shared-element APIs. A normal third-party Activity does not own the Shell `WindowContainerTransaction`/transition leash, so the lab does not copy these classes and pretend it is invoking the system controller. Instead, the real Shell test leaves transition ownership with SystemUI.

### Launcher

ColorOS Launcher contains:

- `LauncherBackAnimationController`
- `PredictiveBackCloseAnimatorCreator`
- `PredictiveBackAnimRequest`
- `OplusLauncherAppTransitionHelper.startAppCloseWindowAnim(...)`

and explicit predictive-continuous feature gating through `persist.wm.enable.predictive.continuous` plus seamless eligibility/fallback logic.

### Gallery content seamless

The supplied ColorOS Gallery contains its own `SeamlessTransitionAnimation`. Its transition setup passes:

- `seamless_trigger_view_getter`
- `seamless_trigger_view_radius`
- `seamless_trigger_view_drawing_bitmap`

`TriggerViewRectGetter` provides the real source-view screen geometry. The animation code also uses COUI interpolators. This is the ColorOS content-level "corresponding element" mechanism, separate from Android shared-element transitions.

## Test matrix

### A · REAL ColorOS Shell predictive continuous

Open **REAL ColorOS Shell probe** and edge-swipe Back.

`SystemBackActivity` intentionally installs no `OnBackAnimationCallback`, no `overridePendingTransition()`, no RemoteAnimation and no app-owned shared-element animator. On ColorOS 17, SystemUI/Shell owns the task leash and can execute:

```text
BackAnimationController
  → BackTransitionHandler
  → AdaptiveSmoothShellAnimManager
  → ShellContinuousTransitionController
  → SpringSlideAnimationController / default continuation
```

This is the real OEM system path.

### B · REAL ColorOS Gallery content seamless

Tap **Open ColorOS Gallery → tap a photo → swipe back**.

The app launches the installed `com.coloros.gallery3d`. Inside Gallery, tap a thumbnail to enter the photo page, then edge-swipe Back. The grid/photo transition is Gallery's own content seamless implementation, so this is the cleanest way to observe ColorOS's trigger-view Rect/bitmap/radius mechanism together with Gallery's predictive-back handling.

### C · REAL ColorOS Launcher predictive close

Launch this lab from the home screen. While `MainActivity` is the root Activity, edge-swipe Back to Home.

`MainActivity` does not intercept root Back. If the package/build passes ColorOS eligibility gates, Launcher can run its real predictive-close path and continue the task toward the matching launcher icon.

### D · ANDROID COMPARISON · shared-element morph

This is the previous app-side matched image/title demo. It remains only as a visual comparison. It is **not** the ColorOS OEM seamless implementation.

### E · APP COMPARISON · gesture/commit handoff

This demonstrates only temporal continuity from the last `BackEvent.progress` into a completion animator. It is also **not** the ColorOS OEM controller.

## Capability probe

The main screen reports whether the device exposes:

- `oplus.software.adaptive_smooth_animation`
- `android.window.OplusWCTExtendInfo`
- `android.window.OplusTransitionExtendedInfo`

This does not mutate hidden framework state; it only confirms whether the ColorOS framework extension surface exists on the running build.

## Why the lab does not directly instantiate ShellContinuousTransitionController

`ShellContinuousTransitionController` lives in the SystemUI/Shell process and operates on real system transition objects and SurfaceControl leashes. Copying or reflectively loading the class into a normal app process would not give the app ownership of those objects and would not exercise the real ColorOS transition pipeline.

The correct third-party test is therefore to create a transition scene and leave ownership with ColorOS SystemUI/Launcher, while using an actual ColorOS app (Gallery) for the content-level seamless path.

## Build

The project targets Android 17 / API 37 and uses AGP 9.3 with Gradle 9.5.

```bash
gradle :app:assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Upstream comparison sample

The Android/app-owned comparison path was originally adapted from the MIT-licensed `JacobHu0723/PredictiveBackGesture` idea. See `THIRD_PARTY_NOTICES.md`.

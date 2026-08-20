# ColorOS Material Playground

A diagnostic/catalog APK for exploring the ColorOS 17 material stack with the real vendor implementation installed on the device.

## What it renders

The catalog is derived from `com.oplus.uxdesign` 17.0.11 and attempts to load the installed package at runtime. When runtime loading succeeds, the app enumerates the firmware's own enums instead of relying on a frozen list.

- `COUIMaterialBlurEffect.BlurEffectType`
- `AppBarBlurHelper` gradient blur and `updateGradientBlurFraction`
- `COUIMaterialStrokeEffect.StrokeEffectType`
- `COUISpotLightEffect.SpotLightType`
- `ToolbarMaterialEffectDelegate.ViewCategory`
- Toolbar material composition: Blur + Stroke + SpotLight + Caustic Shadow

The preview deliberately does **not** ship a custom Liquid Glass shader as a visual fallback. If ColorOS blocks loading or invoking its private/vendor implementation from a third-party UID, the affected preview reports `UNAVAILABLE` and the Runtime Probe lists the missing layer.

## Background and HDR playground

The background can be changed between:

1. a generated high-contrast color pattern for blur inspection;
2. a user-selected image through Android Photo Picker (no broad media permission);
3. an extended-sRGB HDR UI test pattern with values above SDR reference white.

For Ultra HDR images the app checks `Bitmap.hasGainmap()`. It requests an HDR window when a gain map is present, when the HDR test pattern is selected, or when the force-HDR switch is enabled. On Android 15+ the Desired HDR Headroom slider drives `Window.setDesiredHdrHeadroom`.

The runtime panel also reports screen wide-gamut capability, current window wide-gamut state, supported HDR types, and hardware acceleration.

## Important runtime limitation

GitHub Actions can compile, test, lint, and package this APK, but it cannot validate ColorOS private APIs or HDR luminance because the runner is not a ColorOS 17 device. Runtime success therefore remains intentionally visible in the app itself:

- installed `com.oplus.uxdesign` version;
- whether a foreign package code context could be created;
- availability of COUI material classes;
- availability of `OplusMaterialUtil`, `OplusRenderEffect`, and `OplusViewBackgroundRenderEffect`;
- per-preview invocation failures.

This distinction is deliberate: a generic Android implementation that merely resembles ColorOS would defeat the purpose of this playground.

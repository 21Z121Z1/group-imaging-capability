# Oplus Private API Probe

A minimal translucent Android app for isolating ColorOS/Oplus blur call-chain behavior on a real device.

It compares three independent paths:

1. raw `com.oplus.view.ViewRootManager` + `getBackgroundBlurDrawable()` + `setBlurRadius(150)`;
2. `ViewRootManager` + `com.oplus.graphics.OplusBlurParam` with blur type 2 and smooth-corner parameters;
3. a control path that code-loads `com.oplus.uxdesign` and applies `COUIMaterialBlurEffect.TYPE_FRAMEWORK_TOP_BAR_BLUR`, matching the strategy used by the Material Playground.

The runtime report also prints the relevant private method signatures exposed by the device, including `setMaterialParams`, so the exact ColorOS 17 firmware API surface can be compared against the WeType smali injection.

Run the probe over a moving/video app. Press **Copy** and send the complete report together with which of RAW / PARAM / COUI visibly blurred the app underneath.

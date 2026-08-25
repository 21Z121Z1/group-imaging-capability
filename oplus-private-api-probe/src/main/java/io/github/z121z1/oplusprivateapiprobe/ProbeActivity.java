package io.github.z121z1.oplusprivateapiprobe;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * A deliberately small runtime probe for the ColorOS/Oplus private blur stack.
 *
 * It tests the exact low-level ViewRootManager/OplusBlurParam chain separately
 * from the already-known-good com.oplus.uxdesign COUI material path, so a
 * device report can identify the first failing layer without relying on an IME.
 */
public final class ProbeActivity extends Activity {
    private static final String UX_PACKAGE = "com.oplus.uxdesign";
    private static final String VIEW_ROOT_MANAGER = "com.oplus.view.ViewRootManager";
    private static final String BLUR_PARAM = "com.oplus.graphics.OplusBlurParam";
    private static final String COUI_BLUR = "com.coui.appcompat.COUIMaterialBlurEffect";
    private static final String COUI_BLUR_ENUM = "com.coui.appcompat.COUIMaterialBlurEffect$BlurEffectType";

    private final StringBuilder report = new StringBuilder();
    private TextView reportView;
    private TextView rawPanel;
    private TextView paramPanel;
    private TextView couiPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        setContentView(buildUi());

        // ViewRootManager needs an attached ViewRootImpl. Run only after the
        // translucent activity and all probe panels are attached to a window.
        getWindow().getDecorView().post(this::runAllProbes);
    }

    private View buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.TRANSPARENT);

        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(dp(12), dp(12), dp(12), dp(12));
        FrameLayout.LayoutParams sheetLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        sheetLp.gravity = Gravity.CENTER;
        root.addView(sheet, sheetLp);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12), dp(8), dp(8), dp(8));
        header.setBackground(roundRect(0xD91A1A1A, 20));

        TextView title = text("Oplus private blur probe", 17, Color.WHITE, true);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1f));

        Button rerun = new Button(this);
        rerun.setText("Run");
        rerun.setAllCaps(false);
        rerun.setOnClickListener(v -> runAllProbes());
        header.addView(rerun, new LinearLayout.LayoutParams(dp(76), dp(48)));

        Button copy = new Button(this);
        copy.setText("Copy");
        copy.setAllCaps(false);
        copy.setOnClickListener(v -> copyReport());
        header.addView(copy, new LinearLayout.LayoutParams(dp(84), dp(48)));
        sheet.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView instruction = text(
                "Leave a moving/video app behind this translucent window. RAW and PARAM should visibly blur that app if the low-level chain works; COUI is the known-good control.",
                13, 0xFFE8EAED, false);
        instruction.setPadding(dp(12), dp(10), dp(12), dp(10));
        instruction.setBackground(roundRect(0xB01F1F1F, 16));
        LinearLayout.LayoutParams instructionLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        instructionLp.topMargin = dp(8);
        sheet.addView(instruction, instructionLp);

        LinearLayout previews = new LinearLayout(this);
        previews.setOrientation(LinearLayout.HORIZONTAL);
        previews.setGravity(Gravity.CENTER);
        previews.setPadding(0, dp(10), 0, dp(10));
        rawPanel = makePanel("RAW\nViewRootManager");
        paramPanel = makePanel("PARAM\nBlurType 2 + smooth");
        couiPanel = makePanel("COUI\nTOP_BAR_BLUR");
        previews.addView(rawPanel, panelLayoutParams());
        previews.addView(paramPanel, panelLayoutParams());
        previews.addView(couiPanel, panelLayoutParams());
        sheet.addView(previews, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(150)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackground(roundRect(0xE6131313, 18));
        reportView = text("Waiting for attach…", 12, 0xFFF1F3F4, false);
        reportView.setTextIsSelectable(true);
        reportView.setPadding(dp(12), dp(12), dp(12), dp(20));
        reportView.setTypeface(android.graphics.Typeface.MONOSPACE);
        scroll.addView(reportView, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        scrollLp.topMargin = dp(4);
        sheet.addView(scroll, scrollLp);

        return root;
    }

    private LinearLayout.LayoutParams panelLayoutParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(132), 1f);
        lp.setMargins(dp(4), 0, dp(4), 0);
        return lp;
    }

    private TextView makePanel(String label) {
        TextView panel = text(label, 13, Color.WHITE, true);
        panel.setGravity(Gravity.CENTER);
        panel.setPadding(dp(8), dp(8), dp(8), dp(8));
        panel.setBackground(roundRect(0x8A2C2C2C, 24));
        panel.setElevation(dp(4));
        return panel;
    }

    private void resetPanel(TextView panel, String text) {
        panel.setText(text);
        panel.setTextColor(Color.WHITE);
        panel.setBackground(roundRect(0x8A2C2C2C, 24));
    }

    private void runAllProbes() {
        report.setLength(0);
        resetPanel(rawPanel, "RAW\nViewRootManager");
        resetPanel(paramPanel, "PARAM\nBlurType 2 + smooth");
        resetPanel(couiPanel, "COUI\nTOP_BAR_BLUR");

        line("=== Oplus private API probe v0.1 ===");
        line("device=" + Build.MANUFACTURER + " / " + Build.BRAND + " / " + Build.MODEL);
        line("display=" + Build.DISPLAY);
        line("sdk=" + Build.VERSION.SDK_INT + " targetSdk=" + getApplicationInfo().targetSdkVersion);
        line("package=" + getPackageName());
        line("signature=ordinary debug/release app; no Oplus platform signature assumed");
        line("");

        probeSystemGates();
        probeClassVisibility();
        probeDirectRaw(rawPanel);
        probeDirectParam(paramPanel);
        probeCouiControl(couiPanel);
        line("");
        line("=== END REPORT ===");
        publish();
    }

    private void probeSystemGates() {
        section("SYSTEM GATES");
        try {
            int enabled = Settings.System.getInt(getContentResolver(), "system_material_blur_enable", -1);
            pass("Settings.System system_material_blur_enable", String.valueOf(enabled));
        } catch (Throwable t) {
            fail("Settings.System system_material_blur_enable", t);
        }

        try {
            WindowManager wm = getSystemService(WindowManager.class);
            if (wm == null) {
                line("[FAIL] WindowManager service = null");
            } else {
                pass("WindowManager.isCrossWindowBlurEnabled", String.valueOf(wm.isCrossWindowBlurEnabled()));
            }
        } catch (Throwable t) {
            fail("WindowManager.isCrossWindowBlurEnabled", t);
        }

        try {
            PackageInfo info = getPackageManager().getPackageInfo(UX_PACKAGE, 0);
            pass("com.oplus.uxdesign installed", (info.versionName == null ? "?" : info.versionName)
                    + " (" + info.getLongVersionCode() + ")");
        } catch (Throwable t) {
            fail("com.oplus.uxdesign installed", t);
        }
    }

    private void probeClassVisibility() {
        section("CLASS / METHOD VISIBILITY");
        Class<?> vrm = loadHost(VIEW_ROOT_MANAGER, "host Class.forName ViewRootManager");
        Class<?> param = loadHost(BLUR_PARAM, "host Class.forName OplusBlurParam");
        loadHost("com.oplus.view.material.OplusMaterialUtil", "host Class.forName OplusMaterialUtil");
        loadHost("com.oplus.graphics.OplusRenderEffect", "host Class.forName OplusRenderEffect");
        loadHost("com.oplus.view.OplusViewBackgroundRenderEffect", "host Class.forName OplusViewBackgroundRenderEffect");

        if (vrm != null) {
            dumpMethods(vrm, "ViewRootManager", new String[]{
                    "getBackgroundBlurDrawable", "setBlurParams", "setBlurRadius", "setColor", "setCornerRadius"
            });
        }
        if (param != null) {
            dumpMethods(param, "OplusBlurParam", new String[]{
                    "setBlurType", "setMaterialParams", "setArcylicParams", "setSmoothCornerType", "setSmoothCornerWeight"
            });
        }

        try {
            Context vendor = createPackageContext(
                    UX_PACKAGE, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            pass("createPackageContext(INCLUDE_CODE|IGNORE_SECURITY)",
                    vendor.getPackageName() + " loader=" + vendor.getClassLoader().getClass().getName());
            Class<?> c = vendor.getClassLoader().loadClass(COUI_BLUR);
            pass("vendor loader COUIMaterialBlurEffect", c.getName());
            Class<?> e = vendor.getClassLoader().loadClass(COUI_BLUR_ENUM);
            pass("vendor loader BlurEffectType", Arrays.toString(e.getEnumConstants()));
        } catch (Throwable t) {
            fail("vendor package code loading", t);
        }
    }

    /** Raw chain: no OplusBlurParam at all. */
    private void probeDirectRaw(TextView panel) {
        section("DIRECT RAW BACKGROUND BLUR");
        try {
            Class<?> vrmClass = Class.forName(VIEW_ROOT_MANAGER);
            Constructor<?> ctor = vrmClass.getDeclaredConstructor(View.class);
            ctor.setAccessible(true);
            Object manager = ctor.newInstance(panel);
            pass("RAW new ViewRootManager(attached panel)", manager.getClass().getName());

            Drawable drawable = (Drawable) invoke(manager, "getBackgroundBlurDrawable", new Class<?>[]{});
            if (drawable == null) {
                line("[FAIL] RAW getBackgroundBlurDrawable returned null");
                panel.setText("RAW\nNULL DRAWABLE");
                panel.setTextColor(0xFFFF8A80);
                return;
            }
            pass("RAW getBackgroundBlurDrawable", drawable.getClass().getName());

            // Deliberately attach the vendor drawable before changing radius.
            panel.setBackground(drawable);
            pass("RAW View.setBackground(drawable)", "ok");

            invoke(manager, "setBlurRadius", new Class<?>[]{int.class}, 150);
            pass("RAW setBlurRadius(150)", "ok");

            try {
                invoke(manager, "setColor", new Class<?>[]{int.class}, 0x30000000);
                pass("RAW setColor(0x30000000)", "ok");
            } catch (Throwable t) {
                fail("RAW setColor", t);
            }

            try {
                float r = dpF(24);
                invoke(manager, "setCornerRadius",
                        new Class<?>[]{float.class, float.class, float.class, float.class}, r, r, r, r);
                pass("RAW setCornerRadius(24dp x4)", "ok");
            } catch (Throwable t) {
                fail("RAW setCornerRadius", t);
            }

            panel.setText("RAW\nCALLS PASS\nShould blur behind");
        } catch (Throwable t) {
            fail("RAW chain", t);
            panel.setText("RAW\nFAILED\n" + shortError(t));
            panel.setTextColor(0xFFFF8A80);
        }
    }

    /** Exact family used by the WeType experiment: ViewRootManager + OplusBlurParam type 2. */
    private void probeDirectParam(TextView panel) {
        section("DIRECT OPLUS BLUR PARAM");
        try {
            Class<?> vrmClass = Class.forName(VIEW_ROOT_MANAGER);
            Class<?> paramClass = Class.forName(BLUR_PARAM);
            Object manager = vrmClass.getDeclaredConstructor(View.class).newInstance(panel);
            pass("PARAM new ViewRootManager(attached panel)", manager.getClass().getName());

            Drawable drawable = (Drawable) invoke(manager, "getBackgroundBlurDrawable", new Class<?>[]{});
            if (drawable == null) {
                line("[FAIL] PARAM getBackgroundBlurDrawable returned null");
                panel.setText("PARAM\nNULL DRAWABLE");
                panel.setTextColor(0xFFFF8A80);
                return;
            }
            pass("PARAM getBackgroundBlurDrawable", drawable.getClass().getName());
            panel.setBackground(drawable);
            pass("PARAM View.setBackground(drawable) BEFORE params", "ok");

            Object param = paramClass.getDeclaredConstructor().newInstance();
            pass("PARAM new OplusBlurParam", param.getClass().getName());

            invoke(param, "setBlurType", new Class<?>[]{int.class}, 2);
            pass("PARAM setBlurType(2 / FAST_KAWASE)", "ok");

            invoke(param, "setSmoothCornerType", new Class<?>[]{int.class}, 1);
            pass("PARAM setSmoothCornerType(1)", "ok");

            invoke(param, "setSmoothCornerWeight", new Class<?>[]{float.class}, 3.0f);
            pass("PARAM setSmoothCornerWeight(3.0)", "ok");

            invoke(manager, "setBlurParams", new Class<?>[]{paramClass}, param);
            pass("PARAM ViewRootManager.setBlurParams(param)", "ok");

            invoke(manager, "setBlurRadius", new Class<?>[]{int.class}, 150);
            pass("PARAM setBlurRadius(150)", "ok");

            try {
                invoke(manager, "setColor", new Class<?>[]{int.class}, 0x30000000);
                pass("PARAM setColor(0x30000000)", "ok");
            } catch (Throwable t) {
                fail("PARAM setColor", t);
            }

            float r = dpF(24);
            invoke(manager, "setCornerRadius",
                    new Class<?>[]{float.class, float.class, float.class, float.class}, r, r, r, r);
            pass("PARAM setCornerRadius(24dp x4)", "ok");

            panel.setText("PARAM\nCALLS PASS\nShould blur behind");
        } catch (Throwable t) {
            fail("PARAM chain", t);
            panel.setText("PARAM\nFAILED\n" + shortError(t));
            panel.setTextColor(0xFFFF8A80);
        }
    }

    /** Control path copied from ColorOS Material Playground. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void probeCouiControl(TextView panel) {
        section("COUI CONTROL (KNOWN-GOOD FAMILY)");
        try {
            Context vendor = createPackageContext(
                    UX_PACKAGE, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            ClassLoader loader = vendor.getClassLoader();
            Class<?> effectClass = loader.loadClass(COUI_BLUR);
            Class<?> enumClass = loader.loadClass(COUI_BLUR_ENUM);
            pass("COUI package/classes load", "ok");

            Object constant = null;
            for (Object item : enumClass.getEnumConstants()) {
                Enum<?> e = (Enum<?>) item;
                if ("TYPE_FRAMEWORK_TOP_BAR_BLUR".equals(e.name())) {
                    constant = item;
                    break;
                }
            }
            if (constant == null) {
                throw new IllegalStateException("TYPE_FRAMEWORK_TOP_BAR_BLUR missing: "
                        + Arrays.toString(enumClass.getEnumConstants()));
            }
            pass("COUI preset", ((Enum<?>) constant).name());

            Field companionField = effectClass.getDeclaredField("Companion");
            companionField.setAccessible(true);
            Object companion = companionField.get(null);
            if (companion == null) {
                throw new IllegalStateException("COUIMaterialBlurEffect.Companion is null");
            }

            Method apply = null;
            for (Method method : companion.getClass().getDeclaredMethods()) {
                Class<?>[] p = method.getParameterTypes();
                if (p.length == 2 && View.class.isAssignableFrom(p[0]) && p[1] == enumClass
                        && method.getReturnType() == Void.TYPE) {
                    apply = method;
                    break;
                }
            }
            if (apply == null) {
                throw new NoSuchMethodException("COUI preset apply(View, BlurEffectType)");
            }
            apply.setAccessible(true);
            apply.invoke(companion, panel, constant);
            pass("COUI preset invocation", methodSignature(apply));
            panel.setText("COUI\nCALL PASS\nKnown-good control");
        } catch (Throwable t) {
            fail("COUI control", t);
            panel.setText("COUI\nFAILED\n" + shortError(t));
            panel.setTextColor(0xFFFF8A80);
        }
    }

    private Class<?> loadHost(String name, String label) {
        try {
            Class<?> c = Class.forName(name);
            pass(label, c.getName() + " loader=" + String.valueOf(c.getClassLoader()));
            return c;
        } catch (Throwable t) {
            fail(label, t);
            return null;
        }
    }

    private void dumpMethods(Class<?> clazz, String label, String[] wanted) {
        List<String> names = Arrays.asList(wanted);
        try {
            int found = 0;
            for (Method m : clazz.getDeclaredMethods()) {
                if (!names.contains(m.getName())) {
                    continue;
                }
                found++;
                line("  [SIG] " + label + "." + methodSignature(m));
            }
            pass(label + " reflected target methods", String.valueOf(found));
        } catch (Throwable t) {
            fail(label + " getDeclaredMethods", t);
        }
    }

    private Object invoke(Object receiver, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = receiver.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        try {
            return method.invoke(receiver, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw e;
        }
    }

    private static String methodSignature(Method m) {
        StringBuilder b = new StringBuilder();
        b.append(Modifier.toString(m.getModifiers())).append(' ')
                .append(m.getReturnType().getTypeName()).append(' ')
                .append(m.getName()).append('(');
        Class<?>[] params = m.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            if (i > 0) b.append(", ");
            b.append(params[i].getTypeName());
        }
        return b.append(')').toString().trim();
    }

    private void section(String title) {
        line("");
        line("--- " + title + " ---");
    }

    private void pass(String name, String detail) {
        line("[PASS] " + name + " :: " + detail);
    }

    private void fail(String name, Throwable throwable) {
        Throwable root = rootCause(throwable);
        String msg = root.getMessage();
        line("[FAIL] " + name + " :: " + root.getClass().getName()
                + (msg == null || msg.isEmpty() ? "" : ": " + msg));
        for (StackTraceElement element : root.getStackTrace()) {
            String cls = element.getClassName();
            if (cls.startsWith("com.oplus") || cls.startsWith("android.")
                    || cls.startsWith("io.github.z121z1")) {
                line("       at " + element);
            }
        }
    }

    private static Throwable rootCause(Throwable t) {
        Throwable current = t;
        while (current instanceof InvocationTargetException
                && ((InvocationTargetException) current).getCause() != null) {
            current = ((InvocationTargetException) current).getCause();
        }
        return current;
    }

    private static String shortError(Throwable t) {
        Throwable root = rootCause(t);
        return root.getClass().getSimpleName();
    }

    private void line(String value) {
        report.append(value).append('\n');
    }

    private void publish() {
        reportView.setText(report.toString());
    }

    private void copyReport() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("Oplus private API probe", report.toString()));
            Toast.makeText(this, "Probe report copied", Toast.LENGTH_SHORT).show();
        }
    }

    private TextView text(String value, float sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        if (bold) view.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return view;
    }

    private GradientDrawable roundRect(int color, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dpF(radiusDp));
        return d;
    }

    private int dp(float value) {
        return Math.round(dpF(value));
    }

    private float dpF(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}

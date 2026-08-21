package com.example.colorospredictiveback;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final String COLOROS_GALLERY_PACKAGE = "com.coloros.gallery3d";
    private static final String ADAPTIVE_SMOOTH_FEATURE =
            "oplus.software.adaptive_smooth_animation";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(246, 247, 249));
        getWindow().setNavigationBarColor(Color.rgb(246, 247, 249));
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int p = dp(24);
        root.setPadding(p, dp(42), p, dp(36));
        root.setBackgroundColor(Color.rgb(246, 247, 249));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        root.addView(text("ColorOS OEM Animation Lab", 28, true), matchWrap());
        root.addView(spacer(10));
        root.addView(text(
                "The first three paths deliberately use ColorOS-owned components. The Android/framework recreations are kept only as comparisons.",
                16, false), matchWrap());
        root.addView(spacer(18));

        PackageManager pm = getPackageManager();
        boolean adaptiveSmooth = pm.hasSystemFeature(ADAPTIVE_SMOOTH_FEATURE);
        boolean oplusWct = classExists("android.window.OplusWCTExtendInfo");
        boolean oplusTransition = classExists("android.window.OplusTransitionExtendedInfo");
        String probe = "ColorOS framework probe\n"
                + ADAPTIVE_SMOOTH_FEATURE + " = " + adaptiveSmooth + "\n"
                + "android.window.OplusWCTExtendInfo = " + oplusWct + "\n"
                + "android.window.OplusTransitionExtendedInfo = " + oplusTransition;
        root.addView(statusBox(probe));
        root.addView(spacer(28));

        root.addView(section("A · REAL ColorOS Shell predictive continuous",
                "No app-owned Back callback and no custom transition. Swipe back from the next Activity. On ColorOS 17 this is the path handled inside SystemUI by BackAnimationController → AdaptiveSmoothShellAnimManager → ShellContinuousTransitionController. The app does not reproduce the transform itself."));
        Button system = button("Open real ColorOS Shell probe");
        system.setOnClickListener(v -> startActivity(
                new Intent(this, SystemBackActivity.class)));
        root.addView(system, buttonParams());
        root.addView(spacer(28));

        root.addView(section("B · REAL ColorOS Gallery content seamless",
                "This launches the installed ColorOS Gallery itself. In Gallery, tap any thumbnail to enter the photo page, then edge-swipe back. That grid ↔ photo morph is Gallery's own SeamlessTransitionAnimation using its trigger-view Rect/bitmap/radius protocol, not Android shared elements."));
        Button gallery = button("Open ColorOS Gallery → tap a photo → swipe back");
        gallery.setOnClickListener(v -> openColorOsGallery());
        root.addView(gallery, buttonParams());
        root.addView(spacer(28));

        root.addView(section("C · REAL ColorOS Launcher predictive close",
                "Return to this Activity, then edge-swipe all the way back to Home. MainActivity never intercepts root Back. ColorOS Launcher can therefore run LauncherBackAnimationController → PredictiveBackCloseAnimatorCreator → OplusLauncherAppTransitionHelper and morph the task toward the matching launcher icon when eligible."));
        root.addView(spacer(28));

        root.addView(section("D · ANDROID COMPARISON · shared-element morph",
                "This is our app-side framework recreation only. It is intentionally kept as a visual control and must not be confused with ColorOS Shell or Gallery seamless animation."));
        Button shared = button("Open Android comparison lab");
        shared.setOnClickListener(v -> startActivity(
                new Intent(this, SharedElementMorphActivity.class)));
        root.addView(shared, buttonParams());
        root.addView(spacer(28));

        root.addView(section("E · APP COMPARISON · gesture/commit handoff",
                "Another control path: the app owns Back while an internal layer is visible and simply continues the last BackEvent.progress after commit. This demonstrates temporal continuity only; it is not the ColorOS OEM controller."));
        Button inApp = button("Open app-owned handoff comparison");
        inApp.setOnClickListener(v -> startActivity(
                new Intent(this, InAppSeamlessActivity.class)));
        root.addView(inApp, buttonParams());

        setContentView(scroll);
    }

    private void openColorOsGallery() {
        Intent launch = getPackageManager().getLaunchIntentForPackage(COLOROS_GALLERY_PACKAGE);
        if (launch == null) {
            Toast.makeText(this,
                    "ColorOS Gallery package not found: " + COLOROS_GALLERY_PACKAGE,
                    Toast.LENGTH_LONG).show();
            return;
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        startActivity(launch);
    }

    private boolean classExists(String className) {
        try {
            Class.forName(className, false, getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private LinearLayout statusBox(String body) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.rgb(234, 241, 255));
        bg.setCornerRadius(dp(18));
        box.setBackground(bg);
        TextView t = text(body, 13, false);
        t.setTextColor(Color.rgb(44, 69, 118));
        box.addView(t, matchWrap());
        return box;
    }

    private LinearLayout section(String heading, String body) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(18), dp(18), dp(18));
        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(22));
        box.setBackground(bg);
        box.addView(text(heading, 19, true), matchWrap());
        box.addView(spacer(8));
        box.addView(text(body, 15, false), matchWrap());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        box.setLayoutParams(lp);
        return box;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(15);
        b.setAllCaps(false);
        b.setMinHeight(dp(54));
        return b;
    }

    private TextView text(String value, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(Color.rgb(28, 31, 36));
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        t.setLineSpacing(0f, 1.12f);
        return t;
    }

    private View spacer(int valueDp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(valueDp)));
        return v;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams lp = matchWrap();
        lp.topMargin = dp(12);
        return lp;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

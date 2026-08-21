package com.example.colorospredictiveback;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class MainActivity extends Activity {
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

        TextView title = text("ColorOS Predictive Back Lab", 28, true);
        root.addView(title, matchWrap());
        root.addView(spacer(10));
        root.addView(text(
                "Three paths isolate app-owned continuation from system-owned seamless transitions.",
                16, false), matchWrap());
        root.addView(spacer(30));

        root.addView(section("A · In-app seamless handoff",
                "The app consumes Back only while an internal detail layer is visible. Gesture progress owns the transform; after commit, the animator continues from the exact last progress instead of resetting."));
        Button inApp = button("Open in-app handoff demo");
        inApp.setOnClickListener(v -> startActivity(new Intent(this, InAppSeamlessActivity.class)));
        root.addView(inApp, buttonParams());
        root.addView(spacer(28));

        root.addView(section("B · System cross-activity",
                "This screen registers no back callback and overrides no transition. Swipe back from the next Activity so ColorOS / Android Shell can own the predictive transition end to end."));
        Button system = button("Open system-owned Activity");
        system.setOnClickListener(v -> startActivity(new Intent(this, SystemBackActivity.class)));
        root.addView(system, buttonParams());
        root.addView(spacer(28));

        root.addView(section("C · Back to Home",
                "Return to this launcher Activity, then swipe back to the desktop. MainActivity deliberately does not intercept Back. If ColorOS marks the package eligible, its Launcher predictive-continuous / icon-morph path remains unobstructed."));

        setContentView(scroll);
    }

    private LinearLayout section(String heading, String body) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(18), dp(18), dp(18));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(22));
        box.setBackground(bg);
        box.addView(text(heading, 19, true), matchWrap());
        box.addView(spacer(8));
        box.addView(text(body, 15, false), matchWrap());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
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

    private View spacer(int dp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(dp)));
        return v;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
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

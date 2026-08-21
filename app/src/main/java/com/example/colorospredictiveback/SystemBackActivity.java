package com.example.colorospredictiveback;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Intentionally contains no OnBackInvoked/OnBackAnimation callback and no custom Activity transition.
 * This leaves the cross-activity predictive transition under Android/ColorOS Shell ownership.
 */
public final class SystemBackActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(232, 239, 255));
        getWindow().setNavigationBarColor(Color.rgb(232, 239, 255));
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(28), dp(28), dp(28));
        root.setBackgroundColor(Color.rgb(232, 239, 255));

        TextView title = new TextView(this);
        title.setText("System-owned predictive back");
        title.setTextSize(26);
        title.setTextColor(Color.rgb(24, 40, 72));
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView body = new TextView(this);
        body.setText("Swipe from either edge now.\n\nNo app callback is registered here. No overridePendingTransition(), RemoteAnimation, or custom window animation is installed. The system can therefore preview MainActivity and finish through ColorOS Shell without competing Surface ownership.");
        body.setTextSize(16);
        body.setTextColor(Color.rgb(50, 63, 91));
        body.setGravity(Gravity.CENTER);
        body.setLineSpacing(0f, 1.18f);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bodyLp.topMargin = dp(20);
        root.addView(body, bodyLp);

        setContentView(root);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

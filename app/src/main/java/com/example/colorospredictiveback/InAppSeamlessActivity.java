package com.example.colorospredictiveback;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Outline;
import android.os.Bundle;
import android.view.BackEvent;
import android.view.Gravity;
import android.view.OnBackAnimationCallback;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.PathInterpolator;
import android.window.OnBackInvokedDispatcher;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * App-owned predictive back demo with an explicit continuation handoff.
 *
 * Gesture phase: BackEvent.progress drives every visual property directly.
 * Commit phase: the same progress value is handed to a short completion animator.
 * Cancellation: the same state animates back to zero.
 *
 * The callback is unregistered as soon as the internal detail layer is gone, so the next Back
 * belongs to the system again and cross-activity / launcher seamless transitions are not blocked.
 */
public final class InAppSeamlessActivity extends Activity {
    private FrameLayout root;
    private LinearLayout underlay;
    private RoundFrameLayout detail;
    private TextView telemetry;
    private boolean callbackRegistered;
    private boolean detailVisible;
    private float progress;
    private int swipeEdge = BackEvent.EDGE_LEFT;
    private ValueAnimator settleAnimator;

    private final PathInterpolator commitInterpolator = new PathInterpolator(0.16f, 1f, 0.30f, 1f);
    private final PathInterpolator cancelInterpolator = new PathInterpolator(0.20f, 0f, 0f, 1f);

    private final OnBackAnimationCallback backCallback = new OnBackAnimationCallback() {
        @Override
        public void onBackStarted(BackEvent backEvent) {
            cancelSettle();
            swipeEdge = backEvent.getSwipeEdge();
            progress = clamp(backEvent.getProgress());
            applyProgress(progress, "gesture-start");
        }

        @Override
        public void onBackProgressed(BackEvent backEvent) {
            swipeEdge = backEvent.getSwipeEdge();
            progress = clamp(backEvent.getProgress());
            applyProgress(progress, "gesture");
        }

        @Override
        public void onBackCancelled() {
            animateProgress(progress, 0f, false);
        }

        @Override
        public void onBackInvoked() {
            // Crucial handoff: do NOT reset matrices/alpha before finishing.
            // Continue from the exact last gesture-owned state to the terminal state.
            animateProgress(progress, 1f, true);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(241, 244, 249));
        getWindow().setNavigationBarColor(Color.rgb(241, 244, 249));
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(241, 244, 249));
        underlay = createUnderlay();
        detail = createDetail();
        root.addView(underlay, full());
        root.addView(detail, full());
        setContentView(root);

        showDetail(false);
    }

    @Override
    protected void onDestroy() {
        cancelSettle();
        unregisterCallback();
        super.onDestroy();
    }

    private LinearLayout createUnderlay() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setPadding(dp(24), dp(54), dp(24), dp(28));
        page.setBackgroundColor(Color.rgb(241, 244, 249));

        TextView title = label("Underlying destination", 26, true);
        page.addView(title, wrap());

        TextView body = label(
                "When the detail layer is active, this destination is already composed underneath it. Pull from the edge: it moves from its staged state to its final state continuously.",
                16, false);
        LinearLayout.LayoutParams bp = wrap();
        bp.topMargin = dp(14);
        page.addView(body, bp);

        Button show = new Button(this);
        show.setText("Show detail layer again");
        show.setAllCaps(false);
        show.setOnClickListener(v -> showDetail(true));
        LinearLayout.LayoutParams sp = wrap();
        sp.topMargin = dp(28);
        page.addView(show, sp);

        TextView note = label(
                "With the detail layer hidden, this Activity no longer owns Back. Swipe back once more and ColorOS can run its normal cross-activity transition to MainActivity.",
                14, false);
        LinearLayout.LayoutParams np = wrap();
        np.topMargin = dp(24);
        page.addView(note, np);
        return page;
    }

    private RoundFrameLayout createDetail() {
        RoundFrameLayout card = new RoundFrameLayout(this);
        card.setBackgroundColor(Color.rgb(252, 252, 253));
        card.setElevation(dp(12));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(24), dp(54), dp(24), dp(28));
        card.addView(content, full());

        content.addView(label("App-owned predictive layer", 26, true), wrap());

        TextView body = label(
                "Pull slowly, stop halfway, continue, or cancel. On commit the visual state is not recreated: the terminal animation starts from the exact last BackEvent.progress state.",
                16, false);
        LinearLayout.LayoutParams bp = wrap();
        bp.topMargin = dp(14);
        content.addView(body, bp);

        telemetry = label("phase=idle  progress=0.000", 15, true);
        telemetry.setTextColor(Color.rgb(48, 91, 188));
        LinearLayout.LayoutParams tp = wrap();
        tp.topMargin = dp(26);
        content.addView(telemetry, tp);

        TextView detailText = label(
                "Commit path: gesture → continuation → remove layer\nCancel path: gesture → settle-to-zero\nOwnership after removal: system", 14, false);
        LinearLayout.LayoutParams dp = wrap();
        dp.topMargin = this.dp(16);
        content.addView(detailText, dp);
        return card;
    }

    private void showDetail(boolean animateIn) {
        cancelSettle();
        detailVisible = true;
        detail.setVisibility(View.VISIBLE);
        progress = 0f;
        registerCallback();
        applyProgress(0f, animateIn ? "detail-restored" : "idle");
    }

    private void finishInternalBack() {
        detailVisible = false;
        detail.setVisibility(View.GONE);
        unregisterCallback();
        resetUnderlay();
        progress = 0f;
    }

    private void registerCallback() {
        if (callbackRegistered) return;
        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT, backCallback);
        callbackRegistered = true;
    }

    private void unregisterCallback() {
        if (!callbackRegistered) return;
        getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backCallback);
        callbackRegistered = false;
    }

    private void animateProgress(float from, float to, boolean commit) {
        cancelSettle();
        final float safeFrom = clamp(from);
        final float distance = Math.abs(to - safeFrom);
        long duration = Math.max(90L, Math.round(210L * distance));
        settleAnimator = ValueAnimator.ofFloat(safeFrom, to);
        settleAnimator.setDuration(duration);
        settleAnimator.setInterpolator(commit ? commitInterpolator : cancelInterpolator);
        settleAnimator.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            applyProgress(progress, commit ? "commit-continuation" : "cancel-settle");
        });
        settleAnimator.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (cancelled) return;
                settleAnimator = null;
                if (commit) {
                    finishInternalBack();
                } else {
                    progress = 0f;
                    applyProgress(0f, "idle");
                }
            }
        });
        settleAnimator.start();
    }

    private void applyProgress(float p, String phase) {
        if (!detailVisible) return;
        float width = root.getWidth() > 0
                ? root.getWidth()
                : getResources().getDisplayMetrics().widthPixels;
        float direction = swipeEdge == BackEvent.EDGE_RIGHT ? -1f : 1f;

        // Detail leaves the screen by p=1, so removing it after continuation is visually lossless.
        detail.setTranslationX(direction * width * p);
        float detailScale = 1f - 0.045f * p;
        detail.setScaleX(detailScale);
        detail.setScaleY(detailScale);
        detail.setAlpha(1f - 0.08f * p);
        detail.setCornerRadius(dp(30) * p);

        // Destination starts slightly staged and reaches its canonical geometry at exactly p=1.
        underlay.setTranslationX(-direction * width * 0.055f * (1f - p));
        float underScale = 0.955f + 0.045f * p;
        underlay.setScaleX(underScale);
        underlay.setScaleY(underScale);
        underlay.setAlpha(0.72f + 0.28f * p);

        telemetry.setText(String.format(java.util.Locale.US,
                "phase=%s  progress=%.3f", phase, p));
    }

    private void resetUnderlay() {
        underlay.setTranslationX(0f);
        underlay.setScaleX(1f);
        underlay.setScaleY(1f);
        underlay.setAlpha(1f);
    }

    private void cancelSettle() {
        if (settleAnimator != null) {
            settleAnimator.cancel();
            settleAnimator = null;
        }
    }

    private float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private TextView label(String text, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(Color.rgb(31, 35, 43));
        t.setLineSpacing(0f, 1.15f);
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return t;
    }

    private FrameLayout.LayoutParams full() {
        return new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class RoundFrameLayout extends FrameLayout {
        private float cornerRadius;

        RoundFrameLayout(android.content.Context context) {
            super(context);
            setClipToOutline(true);
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), cornerRadius);
                }
            });
        }

        void setCornerRadius(float radius) {
            cornerRadius = radius;
            invalidateOutline();
        }
    }
}

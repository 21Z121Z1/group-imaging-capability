package com.example.colorospredictiveback;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import android.window.BackEvent;
import android.window.OnBackAnimationCallback;
import android.window.OnBackInvokedDispatcher;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

/**
 * Content-level predictive back lab.
 *
 * The list destination stays composed under the detail UI. For the matched case, the detail hero
 * image and title are the moving shared elements: BackEvent.progress maps them directly from their
 * detail geometry to the exact measured geometry of the source row. Commit/cancel animations start
 * at the last gesture progress, so there is no reset frame between gesture ownership and settle.
 *
 * A second unmatched case intentionally has no destination element. It uses a generic scale/fade,
 * making the visual difference between "continuous timing" and a true shared-element morph obvious.
 */
public final class SharedElementMorphActivity extends Activity {
    private static final float DETAIL_CORNER_DP = 28f;
    private static final float SOURCE_CORNER_DP = 18f;

    private FrameLayout root;
    private View listPage;

    private HeroArtworkView sourceHero;
    private TextView sourceTitle;

    private View detailBackground;
    private HeroArtworkView heroOverlay;
    private TextView titleOverlay;
    private LinearLayout detailBody;
    private TextView modeText;
    private TextView telemetry;

    private final RectF sourceHeroBounds = new RectF();
    private final RectF sourceTitleBounds = new RectF();
    private final RectF detailHeroBounds = new RectF();

    private float detailTitleX;
    private float detailTitleY;
    private float sourceTitleScale = 0.72f;

    private boolean detailVisible;
    private boolean matchedMode;
    private boolean callbackRegistered;
    private float progress;
    private int swipeEdge = BackEvent.EDGE_LEFT;
    private ValueAnimator animator;

    private final PathInterpolator settleInterpolator =
            new PathInterpolator(0.20f, 0f, 0f, 1f);
    private final PathInterpolator openInterpolator =
            new PathInterpolator(0.16f, 1f, 0.30f, 1f);

    private final OnBackAnimationCallback backCallback = new OnBackAnimationCallback() {
        @Override
        public void onBackStarted(BackEvent backEvent) {
            cancelAnimator();
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
            animateProgress(progress, 0f, "cancel-settle", null);
        }

        @Override
        public void onBackInvoked() {
            // Continue from the exact gesture-owned state. Do not restore detail geometry first.
            animateProgress(progress, 1f, "commit-to-target", SharedElementMorphActivity.this::finishBack);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(244, 246, 249));
        getWindow().setNavigationBarColor(Color.rgb(244, 246, 249));
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(244, 246, 249));

        listPage = createListPage();
        root.addView(listPage, full());

        createDetailLayers();
        setContentView(root);
    }

    @Override
    protected void onDestroy() {
        cancelAnimator();
        unregisterCallback();
        super.onDestroy();
    }

    private View createListPage() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(36), dp(20), dp(36));
        page.setBackgroundColor(Color.rgb(244, 246, 249));
        scroll.addView(page, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        page.addView(text("Shared Element Predictive Back", 28, true), matchWrap());
        TextView intro = text(
                "Two detail pages look similar at rest. Only the first has a measured destination image + title to morph back into.",
                15, false);
        LinearLayout.LayoutParams introLp = matchWrap();
        introLp.topMargin = dp(10);
        page.addView(intro, introLp);

        TextView matchedHeading = text("MATCHED DESTINATION", 12, true);
        matchedHeading.setTextColor(Color.rgb(81, 101, 132));
        LinearLayout.LayoutParams mh = matchWrap();
        mh.topMargin = dp(30);
        page.addView(matchedHeading, mh);

        LinearLayout matchedRow = cardRow();
        sourceHero = new HeroArtworkView(this);
        sourceHero.setVariant(0);
        sourceHero.setCornerRadius(dp(SOURCE_CORNER_DP));
        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(dp(120), dp(90));
        matchedRow.addView(sourceHero, heroLp);

        LinearLayout matchedCopy = new LinearLayout(this);
        matchedCopy.setOrientation(LinearLayout.VERTICAL);
        matchedCopy.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams copyLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        copyLp.leftMargin = dp(16);
        matchedRow.addView(matchedCopy, copyLp);

        sourceTitle = text("Azure Ridge", 20, true);
        matchedCopy.addView(sourceTitle, matchWrap());
        TextView matchedSubtitle = text("Image + title have stable destination geometry", 13, false);
        matchedSubtitle.setTextColor(Color.rgb(91, 96, 106));
        LinearLayout.LayoutParams ms = matchWrap();
        ms.topMargin = dp(5);
        matchedCopy.addView(matchedSubtitle, ms);

        matchedRow.setOnClickListener(v -> openDetail(true));
        LinearLayout.LayoutParams rowLp = matchWrap();
        rowLp.topMargin = dp(10);
        page.addView(matchedRow, rowLp);

        TextView unmatchedHeading = text("NO MATCH", 12, true);
        unmatchedHeading.setTextColor(Color.rgb(131, 91, 91));
        LinearLayout.LayoutParams uh = matchWrap();
        uh.topMargin = dp(26);
        page.addView(unmatchedHeading, uh);

        LinearLayout unmatchedRow = cardRow();
        HeroArtworkView otherHero = new HeroArtworkView(this);
        otherHero.setVariant(1);
        otherHero.setCornerRadius(dp(SOURCE_CORNER_DP));
        unmatchedRow.addView(otherHero, new LinearLayout.LayoutParams(dp(120), dp(90)));

        LinearLayout unmatchedCopy = new LinearLayout(this);
        unmatchedCopy.setOrientation(LinearLayout.VERTICAL);
        unmatchedCopy.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams otherCopyLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        otherCopyLp.leftMargin = dp(16);
        unmatchedRow.addView(unmatchedCopy, otherCopyLp);

        unmatchedCopy.addView(text("Unmatched Detail", 20, true), matchWrap());
        TextView unmatchedSubtitle = text(
                "Back can stay continuous, but there is no element to land on",
                13, false);
        unmatchedSubtitle.setTextColor(Color.rgb(91, 96, 106));
        LinearLayout.LayoutParams us = matchWrap();
        us.topMargin = dp(5);
        unmatchedCopy.addView(unmatchedSubtitle, us);

        unmatchedRow.setOnClickListener(v -> openDetail(false));
        LinearLayout.LayoutParams otherRowLp = matchWrap();
        otherRowLp.topMargin = dp(10);
        page.addView(unmatchedRow, otherRowLp);

        TextView note = text(
                "After either detail page returns, this Activity unregisters its back callback. A second edge-back is again owned by Android / ColorOS Shell.",
                14, false);
        note.setTextColor(Color.rgb(79, 84, 94));
        LinearLayout.LayoutParams noteLp = matchWrap();
        noteLp.topMargin = dp(28);
        page.addView(note, noteLp);
        return scroll;
    }

    private LinearLayout cardRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(14), dp(14), dp(14));
        row.setMinimumHeight(dp(118));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(24));
        row.setBackground(bg);
        row.setElevation(dp(2));
        return row;
    }

    private void createDetailLayers() {
        detailBackground = new View(this);
        detailBackground.setBackgroundColor(Color.rgb(250, 250, 252));
        // Full-screen touch blocker: keep the measured destination list geometrically stable
        // while detail content owns navigation. Edge navigation still belongs to the system.
        detailBackground.setClickable(true);
        detailBackground.setVisibility(View.GONE);
        root.addView(detailBackground, full());

        heroOverlay = new HeroArtworkView(this);
        heroOverlay.setPivotX(0f);
        heroOverlay.setPivotY(0f);
        heroOverlay.setVisibility(View.GONE);
        root.addView(heroOverlay);

        titleOverlay = text("", 28, true);
        titleOverlay.setPivotX(0f);
        titleOverlay.setPivotY(0f);
        titleOverlay.setVisibility(View.GONE);
        root.addView(titleOverlay);

        detailBody = new LinearLayout(this);
        detailBody.setOrientation(LinearLayout.VERTICAL);
        detailBody.setPadding(dp(24), 0, dp(24), dp(30));
        detailBody.setVisibility(View.GONE);

        modeText = text("", 12, true);
        modeText.setTextColor(Color.rgb(64, 100, 171));
        detailBody.addView(modeText, matchWrap());

        TextView body = text(
                "The destination list remains composed underneath. In matched mode the hero image and title are not recreated during Back; their transform is derived from the measured source bounds every frame.",
                15, false);
        LinearLayout.LayoutParams bodyLp = matchWrap();
        bodyLp.topMargin = dp(10);
        detailBody.addView(body, bodyLp);

        telemetry = text("phase=idle  progress=0.000", 14, true);
        telemetry.setTextColor(Color.rgb(64, 100, 171));
        LinearLayout.LayoutParams telemetryLp = matchWrap();
        telemetryLp.topMargin = dp(20);
        detailBody.addView(telemetry, telemetryLp);

        TextView hint = text(
                "Try: drag to ~50%, hold, cancel, then commit. Compare the matched row against the no-match row.",
                14, false);
        hint.setTextColor(Color.rgb(83, 88, 98));
        LinearLayout.LayoutParams hintLp = matchWrap();
        hintLp.topMargin = dp(12);
        detailBody.addView(hint, hintLp);

        root.addView(detailBody);
    }

    private void openDetail(boolean matched) {
        if (detailVisible) return;

        matchedMode = matched;
        detailVisible = true;
        cancelAnimator();

        if (matchedMode) {
            copyBoundsInRoot(sourceHero, sourceHeroBounds);
            copyBoundsInRoot(sourceTitle, sourceTitleBounds);
            sourceTitleScale = sourceTitle.getTextSize() / titleOverlay.getTextSize();
            sourceHero.setAlpha(0f);
            sourceTitle.setAlpha(0f);
        }

        computeDetailGeometry();

        heroOverlay.setVariant(matchedMode ? 0 : 1);
        heroOverlay.setCornerRadius(dp(DETAIL_CORNER_DP));
        titleOverlay.setText(matchedMode ? "Azure Ridge" : "Unmatched Detail");
        modeText.setText(matchedMode
                ? "MATCHED · image + title → measured list targets"
                : "UNMATCHED · generic continuity only");

        detailBackground.setVisibility(View.VISIBLE);
        detailBody.setVisibility(View.VISIBLE);
        heroOverlay.setVisibility(View.VISIBLE);
        titleOverlay.setVisibility(View.VISIBLE);

        registerCallback();

        progress = 1f;
        applyProgress(progress, "open-start");
        animateProgress(1f, 0f, "open-morph", null);
    }

    private void computeDetailGeometry() {
        int width = root.getWidth();
        if (width <= 0) {
            width = getResources().getDisplayMetrics().widthPixels;
        }

        float left = dp(16);
        float top = dp(42);
        float heroWidth = width - dp(32);
        float heroHeight = heroWidth * 0.75f;
        detailHeroBounds.set(left, top, left + heroWidth, top + heroHeight);

        FrameLayout.LayoutParams heroLp = new FrameLayout.LayoutParams(
                Math.max(1, Math.round(heroWidth)),
                Math.max(1, Math.round(heroHeight)));
        heroLp.leftMargin = Math.round(left);
        heroLp.topMargin = Math.round(top);
        heroOverlay.setLayoutParams(heroLp);

        detailTitleX = dp(24);
        detailTitleY = detailHeroBounds.bottom + dp(18);
        FrameLayout.LayoutParams titleLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.leftMargin = Math.round(detailTitleX);
        titleLp.topMargin = Math.round(detailTitleY);
        titleOverlay.setLayoutParams(titleLp);

        FrameLayout.LayoutParams bodyLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bodyLp.topMargin = Math.round(detailHeroBounds.bottom + dp(72));
        detailBody.setLayoutParams(bodyLp);
    }

    private void applyProgress(float rawProgress, String phase) {
        if (!detailVisible) return;

        float p = clamp(rawProgress);
        float direction = swipeEdge == BackEvent.EDGE_RIGHT ? -1f : 1f;

        detailBackground.setAlpha(1f - p);
        detailBody.setAlpha(clamp(1f - p * 1.25f));
        detailBody.setTranslationY(dp(18) * p);

        listPage.setAlpha(0.68f + 0.32f * p);
        float listScale = 0.975f + 0.025f * p;
        listPage.setScaleX(listScale);
        listPage.setScaleY(listScale);

        if (matchedMode) {
            float targetScale = sourceHeroBounds.width() / detailHeroBounds.width();
            float scale = lerp(1f, targetScale, p);

            // A small bell-shaped edge offset makes the surface feel directly manipulated while
            // preserving exact source/detail geometry at p=0 and p=1.
            float arc = 4f * p * (1f - p);
            float tx = (sourceHeroBounds.left - detailHeroBounds.left) * p
                    + direction * dp(12) * arc;
            float ty = (sourceHeroBounds.top - detailHeroBounds.top) * p
                    - dp(8) * arc;

            heroOverlay.setScaleX(scale);
            heroOverlay.setScaleY(scale);
            heroOverlay.setTranslationX(tx);
            heroOverlay.setTranslationY(ty);
            heroOverlay.setAlpha(1f);
            heroOverlay.setCornerRadius(dp(lerp(DETAIL_CORNER_DP, SOURCE_CORNER_DP, p)));

            titleOverlay.setScaleX(lerp(1f, sourceTitleScale, p));
            titleOverlay.setScaleY(lerp(1f, sourceTitleScale, p));
            titleOverlay.setTranslationX((sourceTitleBounds.left - detailTitleX) * p
                    + direction * dp(6) * arc);
            titleOverlay.setTranslationY((sourceTitleBounds.top - detailTitleY) * p);
            titleOverlay.setAlpha(1f);
        } else {
            float scale = 1f - 0.14f * p;
            heroOverlay.setScaleX(scale);
            heroOverlay.setScaleY(scale);
            heroOverlay.setTranslationX(direction * root.getWidth() * 0.18f * p);
            heroOverlay.setTranslationY(dp(24) * p);
            heroOverlay.setAlpha(1f - 0.72f * p);
            heroOverlay.setCornerRadius(dp(DETAIL_CORNER_DP));

            titleOverlay.setScaleX(1f - 0.08f * p);
            titleOverlay.setScaleY(1f - 0.08f * p);
            titleOverlay.setTranslationX(direction * root.getWidth() * 0.12f * p);
            titleOverlay.setTranslationY(dp(24) * p);
            titleOverlay.setAlpha(1f - 0.8f * p);
        }

        telemetry.setText(String.format(
                Locale.US,
                "phase=%s  progress=%.3f  target=%s",
                phase,
                p,
                matchedMode ? "shared-element" : "none"));
    }

    private void animateProgress(float from, float to, String phase, Runnable endAction) {
        cancelAnimator();

        float safeFrom = clamp(from);
        float distance = Math.abs(to - safeFrom);
        long duration = Math.max(90L, Math.round(260L * distance));

        animator = ValueAnimator.ofFloat(safeFrom, to);
        animator.setDuration(duration);
        animator.setInterpolator(to == 0f && safeFrom == 1f ? openInterpolator : settleInterpolator);
        animator.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            applyProgress(progress, phase);
        });
        animator.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (cancelled) return;
                animator = null;
                progress = to;
                applyProgress(progress, phase);
                if (endAction != null) endAction.run();
            }
        });
        animator.start();
    }

    private void finishBack() {
        // UI changes happen in one choreographer turn: reveal the measured source underneath, then
        // remove the overlay that is already at exactly the same geometry.
        if (matchedMode) {
            sourceHero.setAlpha(1f);
            sourceTitle.setAlpha(1f);
        }

        detailBackground.setVisibility(View.GONE);
        heroOverlay.setVisibility(View.GONE);
        titleOverlay.setVisibility(View.GONE);
        detailBody.setVisibility(View.GONE);

        listPage.setAlpha(1f);
        listPage.setScaleX(1f);
        listPage.setScaleY(1f);

        detailVisible = false;
        progress = 0f;
        unregisterCallback();
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

    private void cancelAnimator() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    private void copyBoundsInRoot(View view, RectF out) {
        int[] viewLocation = new int[2];
        int[] rootLocation = new int[2];
        view.getLocationOnScreen(viewLocation);
        root.getLocationOnScreen(rootLocation);
        float left = viewLocation[0] - rootLocation[0];
        float top = viewLocation[1] - rootLocation[1];
        out.set(left, top, left + view.getWidth(), top + view.getHeight());
    }

    private TextView text(String value, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(Color.rgb(28, 31, 36));
        t.setLineSpacing(0f, 1.12f);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private FrameLayout.LayoutParams full() {
        return new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static float lerp(float start, float end, float t) {
        return start + (end - start) * t;
    }

    /**
     * Procedural artwork so the exact same content can be rendered at thumbnail and detail sizes
     * without shipping an image asset. The matched source/overlay use the same variant.
     */
    private static final class HeroArtworkView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path clipPath = new Path();
        private final Path mountainPath = new Path();
        private float cornerRadius;
        private int variant;

        HeroArtworkView(android.content.Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        void setVariant(int value) {
            variant = value;
            invalidate();
        }

        void setCornerRadius(float radius) {
            cornerRadius = radius;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            if (w <= 0f || h <= 0f) return;

            clipPath.reset();
            clipPath.addRoundRect(0f, 0f, w, h, cornerRadius, cornerRadius, Path.Direction.CW);

            int save = canvas.save();
            canvas.clipPath(clipPath);

            int start = variant == 0 ? Color.rgb(53, 91, 151) : Color.rgb(132, 74, 104);
            int mid = variant == 0 ? Color.rgb(105, 159, 191) : Color.rgb(205, 117, 114);
            int end = variant == 0 ? Color.rgb(225, 192, 139) : Color.rgb(236, 189, 140);
            paint.setShader(new LinearGradient(0f, 0f, w, h, start, mid, Shader.TileMode.CLAMP));
            canvas.drawRect(0f, 0f, w, h, paint);
            paint.setShader(null);

            paint.setColor(end);
            canvas.drawCircle(w * 0.80f, h * 0.22f, Math.min(w, h) * 0.12f, paint);

            mountainPath.reset();
            mountainPath.moveTo(0f, h * 0.78f);
            mountainPath.lineTo(w * 0.25f, h * 0.46f);
            mountainPath.lineTo(w * 0.43f, h * 0.68f);
            mountainPath.lineTo(w * 0.62f, h * 0.34f);
            mountainPath.lineTo(w, h * 0.72f);
            mountainPath.lineTo(w, h);
            mountainPath.lineTo(0f, h);
            mountainPath.close();

            paint.setColor(variant == 0
                    ? Color.argb(225, 31, 61, 88)
                    : Color.argb(225, 76, 42, 63));
            canvas.drawPath(mountainPath, paint);

            paint.setColor(Color.argb(145, 245, 249, 252));
            paint.setStrokeWidth(Math.max(2f, w * 0.008f));
            canvas.drawLine(w * 0.12f, h * 0.18f, w * 0.43f, h * 0.18f, paint);

            canvas.restoreToCount(save);
        }
    }
}

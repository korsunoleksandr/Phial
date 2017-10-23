package com.mindcoders.phial.internal.overlay;

import com.mindcoders.phial.R;
import com.mindcoders.phial.internal.keyvalue.KeyValueView;
import com.mindcoders.phial.internal.overlay.OverlayView.OnPageSelectedListener;
import com.mindcoders.phial.internal.share.ShareView;
import com.mindcoders.phial.internal.util.SimpleAnimatorListener;

import java.io.File;
import java.util.Collections;

import android.animation.Animator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import static com.mindcoders.phial.internal.util.UiUtils.dpToPx;

public final class Overlay {

    private static final int BUTTON_SIZE = 53; //dp

    private static final int STATUSBAR_HEIGHT = 25; //dp

    private final Context context;

    private final WindowManager windowManager;

    private final OverlayView overlayView;

    private FrameLayout pageContainerView;

    private int overlayViewX, overlayViewY;

    private final Point displaySize = new Point();

    private final int btnSizePx;

    public Overlay(final Context context) {
        this.context = context;

        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getSize(displaySize);

        btnSizePx = dpToPx(context, BUTTON_SIZE);

        overlayView = new OverlayView(context, btnSizePx);
        overlayView.setOnHandleMoveListener(onHandleMoveListener);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                btnSizePx,
                getType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        windowManager.addView(overlayView, params);

        overlayView.setOnPageSelectedListener(onPageSelectedListener);

        overlayView.addPage(new OverlayView.Page(
                R.drawable.ic_keyvalue,
                new PageViewFactory<KeyValueView>() {
                    @Override
                    public KeyValueView onPageCreate() {
                        return new KeyValueView(context);
                    }

                    @Override
                    public void onPageDestroy(KeyValueView view) {
                        view.onDestroy();
                    }
                }
        ));

        overlayView.addPage(new OverlayView.Page(
                R.drawable.ic_share,
                new PageViewFactory<ShareView>() {
                    @Override
                    public ShareView onPageCreate() {
                        ShareView shareView = new ShareView(context);
                        shareView.setFiles(Collections.<File>emptyList());
                        return shareView;
                    }

                    @Override
                    public void onPageDestroy(ShareView view) {

                    }
                }
        ));
    }

    private int getType() {
        final int type;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        return type;
    }

    void show() {
        overlayView.setVisibility(View.VISIBLE);
        if (pageContainerView != null) {
            pageContainerView.setVisibility(View.VISIBLE);
        }
    }

    void hide() {
        overlayView.setVisibility(View.GONE);
        if (pageContainerView != null) {
            pageContainerView.setVisibility(View.GONE);
        }
    }

    private FrameLayout createPageContainerView() {
        FrameLayout pageContainterView = new FrameLayout(context);

        int height = displaySize.y - btnSizePx - dpToPx(context, STATUSBAR_HEIGHT);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                height,
                getType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.y = displaySize.y / 2;

        windowManager.addView(pageContainterView, params);

        return pageContainterView;
    }

    private final OverlayView.OnHandleMoveListener onHandleMoveListener = new OverlayView.OnHandleMoveListener() {

        private int initialX, initialY;

        @Override
        public void onMoveStart(float x, float y) {

            WindowManager.LayoutParams params = (WindowManager.LayoutParams) overlayView.getLayoutParams();
            initialX = params.x;
            initialY = params.y;
        }

        @Override
        public void onMove(float dx, float dy) {
            WindowManager.LayoutParams params = (WindowManager.LayoutParams) overlayView.getLayoutParams();
            params.x = initialX + (int) dx;
            params.y = initialY + (int) dy;
            windowManager.updateViewLayout(overlayView, params);
        }

        @Override
        public void onMoveEnd() {
            WindowManager.LayoutParams params = (WindowManager.LayoutParams) overlayView.getLayoutParams();
            overlayViewX = params.x;
            overlayViewY = params.y;
        }
    };

    private final OnPageSelectedListener onPageSelectedListener = new OnPageSelectedListener() {

        @Override
        public void onFirstPageSelected(OverlayView.Page page) {
            animateForward(page);
        }

        @Override
        public void onPageSelectionChanged(OverlayView.Page page) {
            pageContainerView.removeAllViews();
            pageContainerView.addView(page.pageViewFactory.onPageCreate());
        }

        @Override
        public void onNothingSelected() {
            animateBackward();
        }

        private void animateForward(final OverlayView.Page page) {
            WindowManager.LayoutParams params = (WindowManager.LayoutParams) overlayView.getLayoutParams();
            int startX = params.x;
            int endX = displaySize.x / 2;

            int startY = params.y;
            int endY = -displaySize.y / 2;
            animate(startX, endX, startY, endY, overlayView, params,
                    new SimpleAnimatorListener() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            pageContainerView = createPageContainerView();
                            pageContainerView.addView(page.pageViewFactory.onPageCreate());
                        }
                    }
                   );
        }

        private void animateBackward() {
            WindowManager.LayoutParams params = (WindowManager.LayoutParams) overlayView.getLayoutParams();
            int startX = params.x;
            int endX = overlayViewX;

            int startY = params.y;
            int endY = overlayViewY;
            animate(startX, endX, startY, endY, overlayView, params,
                    new SimpleAnimatorListener() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            windowManager.removeView(pageContainerView);
                            pageContainerView = null;
                        }
                    }
                   );
        }

        private void animate(
                int startX, int endX, int startY, int endY, final View view, final WindowManager.LayoutParams params,
                Animator.AnimatorListener listener
                            ) {
            PropertyValuesHolder x = PropertyValuesHolder.ofInt("x", startX, endX);
            PropertyValuesHolder y = PropertyValuesHolder.ofInt("y", startY, endY);
            ValueAnimator valueAnimator = ValueAnimator.ofPropertyValuesHolder(x, y);
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    params.x = (int) animation.getAnimatedValue("x");
                    params.y = (int) animation.getAnimatedValue("y");
                    windowManager.updateViewLayout(view, params);
                }
            });
            valueAnimator.setDuration(200);
            valueAnimator.addListener(listener);
            valueAnimator.start();
        }

    };

}

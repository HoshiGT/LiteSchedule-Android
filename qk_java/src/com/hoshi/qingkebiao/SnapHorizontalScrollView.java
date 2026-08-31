package com.hoshi.qingkebiao;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.HorizontalScrollView;

/**
 * 强吸附横向翻页：手指拖多远都只用切到相邻一周。
 * 没滑过一半就停在当前周，滑过一半就切到下一周/上一周。
 */
public class SnapHorizontalScrollView extends HorizontalScrollView {
    private int pageWidth = 1;
    private int startPage = 0;
    private int startScrollX = 0;
    private float downX;
    private float downY;
    private int touchSlop;
    private VelocityTracker velocityTracker;
    private GestureDetector gestureDetector;
    private int flingDirection; // 1 下一周, -1 上一周, 0 无

    public SnapHorizontalScrollView(Context context) {
        super(context);
        init();
    }

    public SnapHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                if (Math.abs(velocityX) < 250) return false;
                if (Math.abs(velocityX) <= Math.abs(velocityY) * 1.2f) return false;
                if (velocityX < 0) {
                    flingDirection = 1;
                } else {
                    flingDirection = -1;
                }
                return true;
            }
        });
    }

    public void setPageWidth(int pageWidth) {
        this.pageWidth = pageWidth;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downY = ev.getY();
                startPage = Math.round(getScrollX() / (float) pageWidth);
                startScrollX = startPage * pageWidth;
                return false;
            case MotionEvent.ACTION_MOVE: {
                float dx = ev.getX() - downX;
                float dy = ev.getY() - downY;
                if (Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy)) {
                    return true;
                }
                return false;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                return false;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startPage = Math.round(getScrollX() / (float) pageWidth);
                startScrollX = startPage * pageWidth;
                flingDirection = 0;
                velocityTracker = VelocityTracker.obtain();
                if (velocityTracker != null) velocityTracker.addMovement(ev);
                if (gestureDetector != null) gestureDetector.onTouchEvent(ev);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (velocityTracker != null) velocityTracker.addMovement(ev);
                if (gestureDetector != null) gestureDetector.onTouchEvent(ev);
                int newScroll = (int) (startScrollX - (ev.getX() - downX));
                int maxScroll = (19) * pageWidth;
                if (newScroll < 0) newScroll = 0;
                if (newScroll > maxScroll) newScroll = maxScroll;
                scrollTo(newScroll, 0);
                return true;
            case MotionEvent.ACTION_UP: {
                if (gestureDetector != null) gestureDetector.onTouchEvent(ev);
                if (velocityTracker != null) {
                    velocityTracker.addMovement(ev);
                    velocityTracker.computeCurrentVelocity(1000);
                }
                float vx = velocityTracker != null ? velocityTracker.getXVelocity() : 0f;
                if (velocityTracker != null) {
                    velocityTracker.recycle();
                    velocityTracker = null;
                }

                int delta = getScrollX() - startScrollX;
                int targetPage;
                if (flingDirection != 0) {
                    targetPage = startPage + flingDirection;
                } else if (delta > pageWidth / 3) {
                    targetPage = startPage + 1;
                } else if (delta < -pageWidth / 3) {
                    targetPage = startPage - 1;
                } else {
                    targetPage = startPage;
                }

                if (targetPage < 0) targetPage = 0;
                int maxPage = 19;
                if (targetPage > maxPage) targetPage = maxPage;
                smoothScrollTo(targetPage * pageWidth, 0);
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
                if (velocityTracker != null) {
                    velocityTracker.recycle();
                    velocityTracker = null;
                }
                flingDirection = 0;
                return true;
        }
        return true;
    }
}

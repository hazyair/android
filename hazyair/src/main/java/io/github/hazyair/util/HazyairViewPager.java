package io.github.hazyair.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
//import android.view.animation.Interpolator;
//import android.widget.Scroller;

//import java.lang.reflect.Field;

public class HazyairViewPager extends ViewPager {
/*
    public class HazyairScroller extends Scroller {

        private double mScrollFactor = 1;

        public HazyairScroller(Context context) {
            super(context);
        }

        public HazyairScroller(Context context, Interpolator interpolator) {
            super(context, interpolator);
        }

        @SuppressLint("NewApi")
        public HazyairScroller(Context context, Interpolator interpolator, boolean flywheel) {
            super(context, interpolator, flywheel);
        }

        void setScrollDurationFactor(double scrollFactor) {
            mScrollFactor = scrollFactor;
        }

        @Override
        public void startScroll(int startX, int startY, int dx, int dy, int duration) {
            super.startScroll(startX, startY, dx, dy, (int) (duration * mScrollFactor));
        }

    }
*/
    private boolean mSwipeEnable = true;

//    private HazyairScroller mScroller = null;

    public HazyairViewPager(Context context) {
        super(context);
    }

    public HazyairViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setSwipeEnable(boolean swipeEnable) {
        mSwipeEnable = swipeEnable;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return mSwipeEnable && super.onInterceptTouchEvent(event);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mSwipeEnable && super.onTouchEvent(event);
    }

    /*public void setScrollDurationFactor(double scrollFactor) {
        if (mScroller == null) {
            try {
                Field scroller = ViewPager.class.getDeclaredField("mScroller");
                scroller.setAccessible(true);
                Field interpolator = ViewPager.class.getDeclaredField("sInterpolator");
                interpolator.setAccessible(true);
                mScroller = new HazyairScroller(getContext(),
                        (Interpolator) interpolator.get(null));
                scroller.set(this, mScroller);
            } catch (Exception ignore) { }
        }
        if (mScroller != null) mScroller.setScrollDurationFactor(scrollFactor);
    }*/

}

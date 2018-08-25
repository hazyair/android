package io.github.hazyair.gui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.gms.maps.SupportMapFragment;

public class StationMapFragment extends SupportMapFragment {

    private View mOriginalView;

    private class TouchableWrapperView extends FrameLayout {
        public TouchableWrapperView(@NonNull Context context) {
            super(context);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (mOriginalView != null)
                        mOriginalView.getParent().requestDisallowInterceptTouchEvent(true);
                    break;
                case MotionEvent.ACTION_UP:
                    if (mOriginalView != null)
                        mOriginalView.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
            }

            return super.dispatchTouchEvent(ev);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        mOriginalView = super.onCreateView(inflater, parent, savedInstanceState);

        Context context = getContext();
        if (context != null) {
            TouchableWrapperView touchableWrapperView = new TouchableWrapperView(context);
            touchableWrapperView.addView(mOriginalView);
            return touchableWrapperView;
        }

        return mOriginalView;
    }

    @Override
    public View getView() {
        return mOriginalView;
    }
}

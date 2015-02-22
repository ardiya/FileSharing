package com.ay.filesharing.view;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;

import com.fortysevendeg.swipelistview.SwipeListView;

public class CustomViewPager extends ViewPager {

    public CustomViewPager(Context context) {
        super(context);
    }

    public CustomViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
        if (v instanceof SwipeListView && dx > 0) {
            return true;
        }
        return super.canScroll(v, checkV, dx, x, y);
    }
}
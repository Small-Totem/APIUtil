package com.zjh.apiutil.view;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;

//用于实现上滑隐藏底栏
//指定于activity_main.xml的 app:layout_behavior=".view.BottomFragmentBehavior"
public class BottomFragmentBehavior extends CoordinatorLayout.Behavior<View> {
    ObjectAnimator outAnimator,inAnimator;

    int curr_height=-1;//为了适应高度改变
    public BottomFragmentBehavior(Context context, AttributeSet attrs) {
        super(context,attrs);
    }

    // 垂直滑动
    @Override
    public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull View child, @NonNull View directTargetChild, @NonNull View target, int axes, int type) {
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL;
    }

    @Override
    public void onNestedPreScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull View child, @NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
        if(curr_height!=child.getHeight()){//如果高度改变/对象改变,则new动画
            outAnimator = ObjectAnimator.ofFloat(child,"translationY",child.getHeight());
            outAnimator.setDuration(200);
            inAnimator = ObjectAnimator.ofFloat(child, "translationY",0);
            inAnimator.setDuration(200);
            curr_height=child.getHeight();
        }


        if (dy > 0) {// 上滑隐藏
            if (!outAnimator.isRunning() && child.getTranslationY() <= 0) {
                outAnimator.start();
            }
        } else if (dy < 0) {// 下滑显示
            if (!inAnimator.isRunning() && child.getTranslationY() >= child.getHeight()) {
                inAnimator.start();
            }
        }
    }
}


package com.zjh.apiutil.view;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;

import androidx.annotation.NonNull;

public class AnimationForView {
    public static void load_view(@NonNull final View v, int time, float end_alpha){
        AlphaAnimation aa = new AlphaAnimation(0f,end_alpha);
        aa.setDuration(time);//动画持续时间
        v.setVisibility(View.VISIBLE);
        v.startAnimation(aa);
    }

    public static void close_view(@NonNull final View v, int time, float start_alpha, int end_visibility){
        close_view(v,time,start_alpha,new Animation.AnimationListener() {
            public void onAnimationEnd(Animation arg0) {
                v.setVisibility(end_visibility);
            }
            public void onAnimationRepeat(Animation animation) {}
            public void onAnimationStart(Animation animation) {}
        });
    }
    public static void close_view(@NonNull final View v, int time, float start_alpha,Animation.AnimationListener listener){
        AlphaAnimation aa = new AlphaAnimation(start_alpha,0f);
        aa.setDuration(time);
        aa.setAnimationListener(listener);
        v.startAnimation(aa);
    }

    public static void close_text(@NonNull final TextView t, int time, float alpha, int visibility){
        AlphaAnimation aa = new AlphaAnimation(alpha,0f);
        aa.setDuration(time);
        aa.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationEnd(Animation arg0) {
                t.setVisibility(visibility);
                t.setText("");
            }
            public void onAnimationRepeat(Animation animation) {}
            public void onAnimationStart(Animation animation) {}
        });
        t.startAnimation(aa);
    }

    public static class ObjAnim{
        //互斥(不会在执行过程中再执行)的属性动画
        //仅淡入和弹出
        public View v;
        private final ObjectAnimator close_animator, load_animator;
        public ObjAnim(View _v,int in_time,int out_time,int close_visibility){
            v=_v;
            load_animator = ObjectAnimator.ofFloat(v, "alpha",1);
            load_animator.setDuration(in_time);
            load_animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    v.setVisibility(View.VISIBLE);
                }
            });

            close_animator = ObjectAnimator.ofFloat(v,"alpha",0);
            close_animator.setDuration(out_time);
            close_animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    v.setVisibility(close_visibility);
                }
            });
        }
        public void load(){
            if (load_animator.isRunning())
                load_animator.cancel();
            if(close_animator.isRunning())
                close_animator.cancel();
            load_animator.start();
        }
        public void close(){
            if (load_animator.isRunning())
                load_animator.cancel();
            if(close_animator.isRunning())
                close_animator.cancel();
            close_animator.start();
        }
    }
}

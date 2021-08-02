package com.zjh.apiutil.view;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;

import java.util.HashMap;

import static com.zjh.apiutil.view.AnimationForView.close_view;
import static com.zjh.apiutil.view.AnimationForView.load_view;

public class ZLogView {
    public final static short status_normal = 0;
    public final static short status_hint = 1;
    public final static short status_warning = 2;
    public final static short status_error = 3;
    public final static short status_gray = 4;

    public boolean hint_enabled = true;
    public boolean warning_enabled = true;
    public boolean error_enabled = true;
    public boolean gray_enabled = true;

    public boolean flag_if_working = false;
    private boolean flag_if_continue_task = false;

    /**
     * normal 浅蓝箭头 bitmap 紫色箭头 editable 黄色箭头 clickable 青色箭头
     **/
    public final static int R_color_light_blue = Color.rgb(15, 129, 218);
    public final static int R_color_purple = Color.rgb(220, 12, 255);
    public final static int R_color_clan = Color.CYAN;
    public final static int R_color_gray = Color.rgb(126, 126, 126);

    private final ForegroundColorSpan span_light_blue;
    private final ForegroundColorSpan span_red;
    private final ForegroundColorSpan span_light_red;
    private final ForegroundColorSpan span_yellow;
    private final ForegroundColorSpan span_white;

    private final ViewTreeObserver.OnGlobalLayoutListener listener;
    private boolean OnGlobalLayoutListener_enabled = false;// 标识现在到底有没有启用 只能通过scroll_down_enabled()改

    public final AppCompatActivity activity;
    public final LinearLayout linearLayout;
    public final NestedScrollView scrollView;
    public final ProgressBar doing_task_ProgressBar;

    // private final LinkedList<byte[]> bitmap_bytearray_LinkedList;
    private final HashMap<Integer, SpannableStringBuilder> SpannableStringBuilder_HashMap_for_editable;
    public final HashMap<String, TextView> TextView_HashMap_for_editable;// 用于保存需要多次修改的editable_text(就是那种等用户响应的)
                                                                         // 通过一个唯一的String来标识

    public Thread curr_thread = null;
    //这个东西也许不该存在,但是如果内部的interruptedIOException被主动catch了,外部就不会停下来
    //所以用这个boolean来标识下外部是否应该结束
    //eg.连续下载图片时,interrupt只会停止当前那张,这时就要这个来标识结束外层 下载请求的for循环
    public boolean should_interrupt_curr_thread = false;
    private boolean interrupt_next_whole_thread = false;

    // ProgressBar可为null
    public ZLogView(AppCompatActivity a, NestedScrollView sv, ProgressBar pb) {
        activity = a;
        span_light_blue = new ForegroundColorSpan(R_color_light_blue);
        span_red = new ForegroundColorSpan(Color.RED);
        span_light_red = new ForegroundColorSpan(Color.rgb(255, 94, 94));
        span_yellow = new ForegroundColorSpan(Color.YELLOW);
        span_white = new ForegroundColorSpan(Color.WHITE);
        linearLayout = new LinearLayout(a);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        scrollView = sv;
        scrollView.addView(linearLayout);

        doing_task_ProgressBar = pb;
        if (pb != null)
            pb.setOnClickListener(view -> cancel_task());

        // 这里是两个lambda
        listener = () -> scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));

        // scroll_down_enabled(true);

        // bitmap_bytearray_LinkedList = new LinkedList<>();
        SpannableStringBuilder_HashMap_for_editable = new HashMap<>();
        TextView_HashMap_for_editable = new HashMap<>();
    }

    // 使用start_task和close_task来避免同时开多个任务的情况(互斥)
    // 使用start_thread之后,就可以通过cancel_task中途取消任务
    public void start_task(){
        start_task(true,null,false);
    }
    public boolean start_task(boolean clear_log, String name, boolean using_scroll_down) {
        // 返回值是 是否该取消掉这个请求
        if (flag_if_working && !flag_if_continue_task) {
            info_add(ZLogView.status_warning, "由于有正在进行的任务，取消了一次请求");
            return true;
        }

        if (clear_log)
            clear();

        if (doing_task_ProgressBar != null && !flag_if_continue_task)
            load_view(doing_task_ProgressBar, 300, 1);

        flag_if_continue_task = false;

        if (using_scroll_down)
            scroll_down_enabled(true);
        if (name != null)
            info_add(Color.GREEN, name);
        flag_if_working = true;
        return false;
    }
    public void close_task() {
        close_task(false);
    }
    public void close_task(boolean success) {
        if (success)
            info_add(Color.GREEN, "完成");

        if (OnGlobalLayoutListener_enabled) {
            scroll_down();// 不然的话直接关掉scroll_down_enabled他最后一下不会滑下去
        }
        scroll_down_enabled(false);

        if (doing_task_ProgressBar != null)
            close_view(doing_task_ProgressBar, 300, 1, View.INVISIBLE);

        flag_if_working = false;
    }
    public void start_thread(@NonNull Thread t) {
        // 开启线程并把他记录为当前任务线程
        curr_thread = t;
        t.start();
    }
    public void cancel_task() {
        if (curr_thread == null) {
            info_add(status_error, "任务线程获取失败");
            return;
        }
        // 线程还没dead且没被interrupt才执行下面这个
        if (curr_thread.isAlive() && !curr_thread.isInterrupted()) {
            curr_thread.interrupt();
            info_add(status_hint, "已请求取消当前任务");
        }
        if(interrupt_next_whole_thread) {
            should_interrupt_curr_thread = true;
            interrupt_next_whole_thread = false;
        }
    }
    public void notify_continue_another_task() {
        // 告诉ZLogView 要接着进行一个新的任务
        // 必须在下一个任务开始前的那句调用这个函数!
        flag_if_continue_task = true;
    }
    public void notify_interrupt_whole_task(){
        //参见should_interrupt_curr_thread处的注释
        //需要自己实现interrupt外层thread,并把这个值改为false
        interrupt_next_whole_thread=true;
    }

    public boolean if_mode_not_enabled(short mode) {
        // 如果return true 则是不输出
        switch (mode) {
            case status_normal:
                return false;
            case status_hint:
                return !hint_enabled;
            case status_warning:
                return !warning_enabled;
            case status_error:
                return !error_enabled;
            case status_gray:
                return !gray_enabled;
            default:
                return true;
        }
    }

    public void scroll_down() {
        // *这里其实还挺复杂的
        // 关于这里为什么要runOnUiThread:
        // 由于info_add的实现用到了runOnUiThread 实际的view添加过程是在UI线程中完成的而非此处
        // 所以这里也要runOnUiThread 保证下滑操作在log成功添加后执行
        // 然后 不能直接scrollview.fullScroll(ScrollView.FOCUS_DOWN) 还要带个post
        // 具体可参考https://blog.csdn.net/hanjieson/article/details/10312861
        activity.runOnUiThread(() -> scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN)));
    }
    public void scroll_down_enabled(boolean enabled) {
        if (OnGlobalLayoutListener_enabled && !enabled) {
            OnGlobalLayoutListener_enabled = false;
            scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
        } else if (!OnGlobalLayoutListener_enabled && enabled) {
            OnGlobalLayoutListener_enabled = true;
            scrollView.getViewTreeObserver().addOnGlobalLayoutListener(listener);
        }
    }

    public void clear() {
        // 不知道为啥可以这样写。。
        // 原:activity.runOnUiThread(() -> linearLayout.removeAllViewsInLayout());
        activity.runOnUiThread(linearLayout::removeAllViews);
        // bitmap_bytearray_LinkedList.clear();
        SpannableStringBuilder_HashMap_for_editable.clear();
        TextView_HashMap_for_editable.clear();
        curr_thread = null;
        should_interrupt_curr_thread=false;
        interrupt_next_whole_thread=false;
    }

    // 普通text
    public void info_add(String text) {
        info_add(status_normal, text);
    }
    public void info_add(String text, LinearLayout layout, int view_index) {
        info_add(status_normal, text, layout, view_index);
    }
    public void info_add(int text_color, String text) {
        info_add(R_color_light_blue, text_color, text);
    }
    public void info_add(int text_color, String text, LinearLayout layout, int view_index) {
        info_add(R_color_light_blue, text_color, text, layout, view_index);
    }
    public void info_add(int arrow_color, int text_color, String text) {
        info_add(arrow_color, text_color, text, linearLayout, -1);
    }
    public void info_add(int arrow_color, int text_color, String text, LinearLayout layout, int view_index) {
        final ForegroundColorSpan span_arrow_color = new ForegroundColorSpan(arrow_color);
        final ForegroundColorSpan span_text_color = new ForegroundColorSpan(text_color);
        SpannableStringBuilder builder = new SpannableStringBuilder("> ");
        builder.setSpan(span_arrow_color, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        int start = builder.length();
        builder.append(text);
        builder.setSpan(span_text_color, start, start + text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        final TextView t = new TextView(activity);
        t.setTextIsSelectable(true);
        t.setVisibility(View.INVISIBLE);
        // 参数含义:宽,高
        t.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        t.setText(builder);

        activity.runOnUiThread(() -> {
            if (view_index == -1) {
                layout.addView(t);
            } else {
                try {
                    layout.addView(t, view_index);
                } catch (Exception e) {
                    info_add(status_error, "index:" + view_index + " " + "text:" + text + " " + e.toString());
                }

            }
            load_view(t, 300, 1);
        });
    }

    // 带状态的text
    public void info_add(short mode, String text) {
        info_add(mode, text, linearLayout, -1);
    }
    public void info_add(short mode, String text, LinearLayout layout, int view_index) {
        // 改变textview的部分颜色
        // 参考自https://blog.csdn.net/qq_21036939/article/details/50239543
        // 其中的span颜色在initial_span_colors中初始化
        if (if_mode_not_enabled(mode))
            return;

        if (mode == status_gray) {
            info_add(R_color_gray, text, layout, view_index);
            return;
        }

        SpannableStringBuilder builder = new SpannableStringBuilder("> ");
        builder.setSpan(span_light_blue, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        switch (mode) {
            case status_normal:
                break;
            case status_hint:
                builder.append("提示:");
                builder.setSpan(span_yellow, 2, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                break;
            case status_warning:
                builder.append("警告:");
                builder.setSpan(span_light_red, 2, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                break;
            case status_error:
                builder.append("错误:");
                builder.setSpan(span_red, 2, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                break;
            // 由于if_mode_enabled(mode),这里不会有default
        }

        int start = builder.length();
        builder.append(text);
        builder.setSpan(span_white, start, start + text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        final TextView t = new TextView(activity);
        t.setTextIsSelectable(true);
        t.setVisibility(View.INVISIBLE);
        t.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        t.setText(builder);

        activity.runOnUiThread(() -> {
            if (view_index == -1)
                layout.addView(t);

            else
                layout.addView(t, view_index);
            load_view(t, 300, 1);
        });
    }

    // 图片
    public void info_add(final Bitmap b) {
        info_add(b, null, null, linearLayout, -1);
    }
    public void info_add(final Bitmap b, String img_name, String img_description) {
        info_add(b, img_name, img_description, linearLayout, -1);
    }
    public void info_add(Bitmap b, String img_name, String img_description, LinearLayout layout, int view_index) {
        // 这里实在是经历了太多
        // 不再使用链表存储图片的数据,要保存的时候直接从imageview里面拿
        // 用系统自带的 MediaStore.Images.Media.insertImage 保存图片好像只能保存.jpg(有损压缩)
        // 但是肉眼确实没看出区别,而且分辨率没变
        // 还行,就这样吧
        // **imageview里面应该是没缩放过的原图,如果分辨率太大估计会oom,maybe可以优化下?

        // 如果图片太大(实测6216x7560的时候会(pid=89800046,p=2)),由于硬件加速的限制,会↓
        // java.lang.RuntimeException: Canvas: trying to draw too large(187971840bytes) bitmap.
        // 目前没找到可行的解决办法
        // 暂时设定图片尺寸最大值为5000x5000

        // final int index;

        if (img_name == null)
            img_name = Long.toString(System.currentTimeMillis());
        if(img_description==null)
            img_description="null";

        // byte[] data=bitmap_to_byte_array(b);
        // bitmap_bytearray_LinkedList.add(data);
        // index= bitmap_bytearray_LinkedList.indexOf(data);

        final ImageView i = new ImageView(activity);
        i.setVisibility(View.INVISIBLE);
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getRealMetrics(dm);

        int old_w = b.getWidth();
        int old_h = b.getHeight();

        int new_w = dm.widthPixels;
        int new_h = (int) ((double) dm.widthPixels * (double) old_h / (double) old_w);

        if (b.getWidth() * b.getHeight() >= 5000 * 5000) {
            b = scale_bitmap(b, new_w, new_h, true);
            info_add(status_warning, "图像尺寸过大(" + old_w + "x" + old_h + "),已进行强制缩放(保存图片将只能获得缩放后的尺寸)");
        }

        // Bitmap b_new=scale_bitmap(b,new_w,new_h,true);
        // 原来这个缩放的方法弃用,直接设置imageview的setLayoutParams效果更好
        i.setImageBitmap(b);
        i.setLayoutParams(new LinearLayout.LayoutParams(new_w, new_h));

        /*
         * info_add(ZLogView.status_gray,"@info_add/bitmap:RowBytes="+b_new.getRowBytes(
         * ));
         * info_add(ZLogView.status_gray,"@info_add/bitmap:AllocationByteCount="+b_new.
         * getAllocationByteCount()+"("+(b_new.getAllocationByteCount()/1024)+"kb)");
         * info_add(ZLogView.status_gray,"@info_add/bitmap:ByteCount="+b_new.
         * getByteCount());
         */

        String finalImg_name = img_name;
        String finalImg_description = img_description;
        i.setOnClickListener(v -> {
            // 如果尺寸不合适就缩放(旋转后那种),否则弹保存对话框
            Bitmap _b = ((BitmapDrawable) i.getDrawable()).getBitmap();
            DisplayMetrics _dm = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getRealMetrics(_dm);

            int _image_view_w = i.getWidth();
            int _new_w = _dm.widthPixels;

            if (_new_w != _image_view_w) {
                int _old_w = _b.getWidth();
                int _old_h = _b.getHeight();
                int _new_h = (int) ((double) _dm.widthPixels * (double) _old_h / (double) _old_w);

                i.setLayoutParams(new LinearLayout.LayoutParams(_new_w, _new_h));
           } else {
                AlertDialog.Builder dialog1 = new AlertDialog.Builder(activity);
                dialog1.setTitle("保存到相册?");
                dialog1.setCancelable(true);
                dialog1.setPositiveButton("阔以", (dialogInterface, i1) -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        // byte[] bitmap_data=bitmap_bytearray_LinkedList.get(index);
                        String uriString = MediaStore.Images.Media.insertImage(
                                activity.getApplicationContext().getContentResolver(),
                                // BitmapFactory.decodeByteArray(bitmap_data,0,bitmap_data.length)
                                _b, finalImg_name, finalImg_description);

                        info_add(status_gray, "uri:" + uriString);
                        info_add("保存路径:Pictures/" + finalImg_name + ".jpg");
                        scroll_down();

                        Uri uri = Uri.parse(uriString);
                        intent.setDataAndType(uri, "image/*");
                        activity.startActivity(intent);
                    } catch (Exception e) {
                        info_add(status_error, e.toString());
                    }
                });
                dialog1.setNegativeButton("不要", null);
                dialog1.show();
            }
        });
        activity.runOnUiThread(() -> {
            if (view_index == -1) {
                info_add(R_color_purple, R_color_gray, "原图尺寸:" + old_w + "x" + old_h + "  缩放后尺寸:" + new_w + "x" + new_h,
                        layout, -1);

                layout.addView(i);
            } else {
                info_add(R_color_purple, R_color_gray, "原图尺寸:" + old_w + "x" + old_h + "  缩放后尺寸:" + new_w + "x" + new_h,
                        layout, view_index);
                layout.addView(i, view_index + 1);
            }
            load_view(i, 300, 1);
        });
    }

    // 可点击的text 一些别的东西比如点击后消失、点击后滑到最下面请在onclick里面实现
    public void info_add_clickable(String text, ClickableSpan clickableSpan) {
        info_add_clickable(text, R_color_light_blue, clickableSpan);
    }
    public void info_add_clickable(String text, int text_color, ClickableSpan clickableSpan) {
        info_add_clickable(text,text_color,clickableSpan,linearLayout,-1);
    }
    public void info_add_clickable(String text, int text_color, ClickableSpan clickableSpan, @NonNull LinearLayout layout, int view_index) {
        /*
         * 一些实用的例子 ((LinearLayout)view.getParent()).removeView(view); //删掉view自己
         */

        SpannableStringBuilder builder = new SpannableStringBuilder("> ");
        ForegroundColorSpan clan = new ForegroundColorSpan(R_color_clan);
        builder.setSpan(clan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        UnderlineSpan u = new UnderlineSpan();

        // 这里append(" ") 是为了让可点文字之后的空白部分不可点
        int start = builder.length();
        builder.append(text);
        builder.append(" ");
        builder.setSpan(clickableSpan, start, start + text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ForegroundColorSpan text_color_Span = new ForegroundColorSpan(text_color);
        builder.setSpan(text_color_Span, 2, start + text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        builder.setSpan(u, start, start + text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        final TextView t = new TextView(activity);
        // t.setTextIsSelectable(true);
        t.setMovementMethod(LinkMovementMethod.getInstance());
        t.setVisibility(View.INVISIBLE);
        t.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        t.setText(builder);

        activity.runOnUiThread(() -> {
            if (view_index == -1)
                layout.addView(t);
            else
                layout.addView(t, view_index);
            load_view(t, 300, 1);
        });
    }



    // 可编辑的text
    public TextView info_add_editable(String text) {
        return info_add_editable(text, linearLayout, -1);
    }
    public TextView info_add_editable(String text, LinearLayout layout, int view_index) {
        return info_add_editable(Color.WHITE, text, layout, view_index);
    }
    public TextView info_add_editable(int text_color, String text) {
        return info_add_editable(text_color, text, linearLayout, -1);
    }
    public TextView info_add_editable(int text_color, String text, LinearLayout layout, int view_index) {
        final ForegroundColorSpan temp_color_span = new ForegroundColorSpan(text_color);
        SpannableStringBuilder builder = new SpannableStringBuilder("> ");
        builder.setSpan(span_yellow, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        int start = builder.length();
        builder.append(text);
        builder.setSpan(temp_color_span, start, start + text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        final TextView t = new TextView(activity);

        int id = ViewCompat.generateViewId();
        t.setId(id);
        t.setTextIsSelectable(true);
        t.setVisibility(View.INVISIBLE);
        t.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        t.setText(builder);

        // 通过id来找这个SpannableStringBuilder
        SpannableStringBuilder_HashMap_for_editable.put(id, builder);

        //这里需要等到在ui线程添加好这个view之后再返回,不然之后调用update_editable有可能空指针(获得不到父布局)
        Object lock=new Object();

        //这个玩意是为了防止在lock.wait()之前就已经lock.notify()
        //感觉应该有更好的实现,暂时先这样吧
        boolean[] notified=new boolean[1];
        activity.runOnUiThread(() -> {
            if (view_index == -1)
                layout.addView(t);
            else
                layout.addView(t, view_index);
            load_view(t, 300, 1);
            synchronized (lock) {
                notified[0]=true;
                lock.notify();
            }
        });

        //这里的这个玩意参考自
        //https://www.cnblogs.com/zhoushihui/p/12766451.html
        try {
            synchronized (lock) {
                if(!notified[0])
                    lock.wait();
            }
        } catch (InterruptedException ignored) {}

        return t;
    }

    public void update_editable(@NonNull TextView t, int R_color, String text) {
        final ForegroundColorSpan temp_color_span = new ForegroundColorSpan(R_color);
        SpannableStringBuilder builder = new SpannableStringBuilder("> ");
        builder.setSpan(span_yellow, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        int start = builder.length();
        builder.append(text);
        builder.setSpan(temp_color_span, start, start + text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        activity.runOnUiThread(() -> t.setText(builder));
        SpannableStringBuilder_HashMap_for_editable.replace(t.getId(), builder);
    }
    public void update_editable_with_close(@NonNull TextView child, short mode, String text) {
        LinearLayout layout = (LinearLayout) child.getParent();
        int view_index = layout.indexOfChild(child);
        activity.runOnUiThread(() -> {
            layout.removeViewAt(view_index);
            info_add(mode, text, layout, view_index);
        });
    }
    public void update_editable_with_close(@NonNull TextView child, Bitmap b, String name, String description) {
        LinearLayout layout = (LinearLayout) child.getParent();
        int view_index = layout.indexOfChild(child);
        activity.runOnUiThread(() -> {
            layout.removeViewAt(view_index);
            info_add(b, name, description, layout, view_index);
        });
    }

    // 这个函数 把前面的箭头改成浅蓝色
    // 并删除SpannableStringBuilder_HashMap_for_editable里面的对应值，但是view的id没有被删除
    public void close_editable(@NonNull TextView t) {
        SpannableStringBuilder builder;

        builder = SpannableStringBuilder_HashMap_for_editable.get(t.getId());
        if (builder == null) {
            // 说明这个editable已经被关掉了
            info_add(status_error, "ERR@close_editable:builder==null");
            return;
        }
        builder.setSpan(span_light_blue, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        activity.runOnUiThread(() -> t.setText(builder));
        SpannableStringBuilder_HashMap_for_editable.remove(t.getId());
    }

    /*
     * group 因为线程声明的时候就把view_index塞进去了，动态更新就会出错 所以需要搞group indent==缩进
     */
    public LinearLayout info_add_group(int indent_px) {
        int group_id = ViewCompat.generateViewId();
        LinearLayout group = new LinearLayout(activity);
        group.setId(group_id);
        group.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        if (indent_px > 0)
            param.setMarginStart(indent_px);
        group.setLayoutParams(param);

        //lock详见info_add_editable()
        Object lock=new Object();
        boolean[] notified=new boolean[1];
        activity.runOnUiThread(() -> {
            linearLayout.addView(group);
            synchronized (lock) {
                notified[0]=true;
                lock.notify();
            }
        });
        try {
            synchronized (lock) {
                if(!notified[0])
                    lock.wait();
            }
        } catch (InterruptedException ignored) {}

        return group;
    }

    public void info_add_view(View v) {
        linearLayout.addView(v);
        load_view(v, 300, 1);
    }
    public void info_add_view(View v, @NonNull LinearLayout layout, int view_index) {
        layout.addView(v, view_index + 1);
        load_view(v, 300, 1);
    }

    //add到目标view的前一个(after=false) 或者后一个(after=true)
    //注意:添加到前一个可能有bug,尚未测试
    public void info_add_by_view(int text_color, String text, @NonNull TextView target, boolean after) {
        LinearLayout layout = (LinearLayout) target.getParent();

        if (layout == null) {
            info_add(text_color, text);
            return;
        }

        int index = layout.indexOfChild(target);
        if (after)
            index++;

        final ForegroundColorSpan span_text_color = new ForegroundColorSpan(text_color);
        SpannableStringBuilder builder = new SpannableStringBuilder("> ");
        builder.setSpan(span_light_blue, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        int start = builder.length();
        builder.append(text);
        builder.setSpan(span_text_color, start, start + text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        final TextView t = new TextView(activity);
        t.setTextIsSelectable(true);
        t.setVisibility(View.INVISIBLE);
        // 参数含义:宽,高
        t.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        int finalIndex = index;
        activity.runOnUiThread(() -> {
            t.setText(builder);
            layout.addView(t, finalIndex);
            load_view(t, 300, 1);
        });

    }
    public void info_add_by_view(short mode, String text, TextView target, boolean after) {
        if (if_mode_not_enabled(mode))
            return;

        if (mode == status_gray) {
            info_add_by_view(R_color_gray, text, target, after);
            return;
        }

        LinearLayout layout = (LinearLayout) target.getParent();
        // 这里究极踩坑
        // (妥协)当获取不到parent 就不要info_add_by_view了,改用普通的info_add
        if (layout == null) {
            info_add(mode, text);
            return;
        }

        int index = layout.indexOfChild(target);
        if (after)
            index++;

        SpannableStringBuilder builder = new SpannableStringBuilder("> ");
        builder.setSpan(span_light_blue, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        switch (mode) {
            case status_normal:
                break;
            case status_hint:
                builder.append("提示:");
                builder.setSpan(span_yellow, 2, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                break;
            case status_warning:
                builder.append("警告:");
                builder.setSpan(span_light_red, 2, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                break;
            case status_error:
                builder.append("错误:");
                builder.setSpan(span_red, 2, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                break;
            default:
                break;
        }

        int start = builder.length();
        builder.append(text);
        builder.setSpan(span_white, start, start + text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        final TextView t = new TextView(activity);
        t.setTextIsSelectable(true);
        t.setVisibility(View.INVISIBLE);
        t.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        int finalIndex = index;

        activity.runOnUiThread(() -> {
            t.setText(builder);
            layout.addView(t, finalIndex);
            load_view(t, 300, 1);
        });
    }

    public void add_split_line(int margin_top,int margin_bottom){
        View v=new View(activity);
        LinearLayout.LayoutParams param=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,1);
        param.setMargins(0,margin_top,0,margin_bottom);
        v.setLayoutParams(param);
        v.setVisibility(View.INVISIBLE);
        v.setBackgroundColor(Color.WHITE);
        activity.runOnUiThread(() -> {
            linearLayout.addView(v);
            load_view(v, 300, 1);
        });
    }

    // 删除textview
    public void delete_text(TextView text, boolean use_animation) {
        if (use_animation) {
            AlphaAnimation aa = new AlphaAnimation(1, 0f);
            aa.setDuration(300);
            aa.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationEnd(Animation arg0) {
                    if (text.getParent() != null)
                        ((LinearLayout) text.getParent()).removeView(text);
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationStart(Animation animation) {
                }
            });
            text.startAnimation(aa);
        } else if (text.getParent() != null)
            ((LinearLayout) text.getParent()).removeView(text);
    }

    public static Bitmap scale_bitmap(Bitmap origin, int newWidth, int newHeight, boolean recycle_origin) {
        /*
         * 轮子 根据给定的宽和高进行拉伸
         *
         * @param origin 原图
         * 
         * @param newWidth 新图的宽
         * 
         * @param newHeight 新图的高
         * 
         * @return new Bitmap
         */

        if (origin == null) {
            return null;
        }
        int height = origin.getHeight();
        int width = origin.getWidth();

        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);// 使用后乘
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (!origin.isRecycled() && newBM != origin && recycle_origin) {
            origin.recycle();
        }
        return newBM;
    }
}
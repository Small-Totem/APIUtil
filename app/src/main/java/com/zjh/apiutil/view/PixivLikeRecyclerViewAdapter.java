package com.zjh.apiutil.view;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.alibaba.fastjson.JSONObject;
import com.zjh.apiutil.R;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Objects;


import at.grabner.circleprogress.CircleProgressView;


import static com.zjh.apiutil.netutil.Api.get_bitmap_with_CircleProgressView;
import static com.zjh.apiutil.netutil.PixivApi.get_pixiv_error_info;
import static com.zjh.apiutil.netutil.PixivApi.get_pixiv_error_info_str;
import static com.zjh.apiutil.netutil.PixivApi.get_pixiv_origin_pic_path;
import static com.zjh.apiutil.netutil.PixivApi.require_pixiv_square_1200;
import static com.zjh.apiutil.view.AnimationForView.close_view;

public class PixivLikeRecyclerViewAdapter extends RecyclerView.Adapter<PixivLikeRecyclerViewAdapter.PixivLikeHolder> {
    private final LayoutInflater inflater;
    private final AppCompatActivity activity;

    public LinkedList<Integer> pid_LinkedList;//这个也是用来区别每个item的主标识符
    public LinkedList<Integer> p_LinkedList;
    public LinkedList<String> json_LinkedList;
    public LinkedList<String> add_date_LinkedList;

    public boolean staggered;
    public boolean display_sequence;//true==正序显示 false==反序显示
    public boolean auto_load=true;
    public boolean show_r18=false;
    //public boolean scroll_state_down=true;//用来标识上滑还是下滑,这个值在MainActivity的
                                            //get_pixiv_like_RecyclerView的recycler_view.addOnScrollListener里面更新
                                            //(已弃用,因为偶尔会动画错误,用a_visible_view_position来实现目标功能)
    public int a_visible_view_position=0;
    public boolean unmoved=true;//用来标识还没移动过,此时加载所有显示了的view的图片(如果开了自动加载)

    public PixivLikeRecyclerViewAdapter(AppCompatActivity _activity, LinkedList<Integer> _pid, LinkedList<Integer> _p,
                                        LinkedList<String> _add_date, LinkedList<String> _json, boolean _staggered, boolean _display_sequence) {
        pid_LinkedList = _pid;
        p_LinkedList = _p;
        add_date_LinkedList = _add_date;
        json_LinkedList = _json;

        activity = _activity;
        inflater = LayoutInflater.from(activity);

        staggered =_staggered;
        display_sequence = _display_sequence;
    }

    @NonNull
    @Override
    public PixivLikeHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v;
        if(staggered)
            v= inflater.inflate(R.layout.recyclerview_pixiv_like_content_staggered, parent, false);
        else
            v= inflater.inflate(R.layout.recyclerview_pixiv_like_content, parent, false);

        return new PixivLikeHolder(v,staggered, get_r18_status(json_LinkedList.get(pid_LinkedList.indexOf(viewType))));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull PixivLikeHolder holder,int position) {
        holder.activity = this.activity;
        int final_position = get_true_position(position);

        if(holder.r18){
            if(show_r18) {
                holder.pid_and_p.setVisibility(View.VISIBLE);
                holder.add_date.setVisibility(View.VISIBLE);
                holder.image.setVisibility(View.VISIBLE);
                holder.itemView.setEnabled(true);
            } else {
                holder.pid_and_p.setVisibility(View.GONE);
                holder.add_date.setVisibility(View.GONE);
                holder.image.setVisibility(View.GONE);
                holder.json.setText("R-18项已隐藏,如需显示请更改设置");
                holder.itemView.setEnabled(false);
                return;
            }
        }

        String p_and_pid ="#" + final_position + " pid=" + pid_LinkedList.get(final_position) + " p=" + p_LinkedList.get(final_position);
        holder.curr_pid = pid_LinkedList.get(final_position);
        holder.curr_p = p_LinkedList.get(final_position);
        holder.pid_and_p.setText(p_and_pid);

        holder.add_date.setText(add_date_LinkedList.get(final_position));
        holder.json.setText(analyze_json_to_pixiv_like(json_LinkedList.get(final_position)));

        //这里的tag是为了传值给长按的那个菜单(在mainactivity的onContextItemSelected())实现
        holder.itemView.setTag(holder);
        activity.registerForContextMenu(holder.itemView);
    }


    //为了更新view的局部
    //参见https://blog.csdn.net/jdsjlzx/article/details/52893469/
    //调用notifyItemChanged(int position, Object payload)来实现局部刷新
    //
    //这里有几把巨坑,草
    //    notifyItemRemoved(position);
    //    notifyItemRangeChanged(position, getItemCount() - position,"test");
    //这样写的话"test"是没法传进下面这个函数的
    //x了哈士奇了
/*
    @Override
    public void onBindViewHolder(@NonNull PixivLikeHolder holder, int position, List<Object> payloads) {
        onBindViewHolder(holder, position);
        if(payloads.isEmpty()){

        }
        else {

        }
    }
*/

    @Override
    public int getItemCount() {
        return pid_LinkedList == null ? 0 : pid_LinkedList.size();
    }

    //不Override的话会复用错乱,参考自https://www.jianshu.com/p/4ae396554694
    //这里其实就是由position获取pid
    @Override
    public int getItemViewType(int position) {
        return pid_LinkedList.get(get_true_position(position));
    }

    @Override
    public void onViewAttachedToWindow(@NonNull PixivLikeHolder holder) {
        super.onViewAttachedToWindow(holder);

        //只要不是加载中 都隐藏progress_bar
        //这个好像注释了也不影响来着...
        //if(holder.status!=1)
            //holder.progress_bar.setVisibility(View.INVISIBLE);

        //第一次进入的加载
        if(unmoved&&auto_load&&holder.itemView.isEnabled())
            holder.load(false);

        //判断上滑还是下滑,并设置动画
        if(!unmoved){
            if(holder.getAdapterPosition()>a_visible_view_position)
                holder.itemView.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.pixiv_like_slide_in_bottom));
            else
                holder.itemView.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.pixiv_like_slide_in_top));
        }
    }

    public static class PixivLikeHolder extends RecyclerView.ViewHolder {
        public AppCompatActivity activity;
        public int curr_pid;
        public int curr_p;
        public TextView pid_and_p;
        public TextView add_date;
        public TextView json;
        public TextView image_hint;
        public ImageView image;
        public CircleProgressView circle_progress_view;
        public boolean if_staggered;
        public boolean r18;

        /* -3=加载失败且不用再加载 -2=加载失败且正在获取原因 -1=加载失败且应该重新加载
           0=未加载 1=加载中 2=加载完成*/
        public short status = 0;

        @SuppressLint("SetTextI18n")
        PixivLikeHolder(View view, boolean _if_staggered, boolean _r18) {
            super(view);

            pid_and_p = view.findViewById(R.id.recyclerview_pixiv_like_TextView_pid_and_p);
            add_date = view.findViewById(R.id.recyclerview_pixiv_like_TextView_add_date);
            json = view.findViewById(R.id.recyclerview_pixiv_like_TextView_json);
            image_hint = view.findViewById(R.id.recyclerview_pixiv_like_TextView_image_hint);
            image = view.findViewById(R.id.recyclerview_pixiv_like_ImageView);


            circle_progress_view = view.findViewById(R.id.recyclerview_pixiv_like_CircleProgressView);
            circle_progress_view.setText("");


            if_staggered=_if_staggered;
            r18=_r18;

            view.setOnClickListener(v -> load(false));
        }

        public void load(boolean force_load){
            if(!force_load)
                if (status == 1 || status == 2 || status == -2|| status ==-3){
                    return;
                }

            image_hint.setVisibility(View.INVISIBLE);

            if(image.getVisibility()==View.VISIBLE){
                close_view(image,300,1,View.GONE);
            }

            activity.runOnUiThread(()->{
                AnimationForView.load_view(circle_progress_view, 500, 1);
                if(!if_staggered)
                    AnimationForView.load_view(json, 500, 1);
            });

            new Thread(() -> {
                status = 1;
                Bitmap b = null;
                Bundle bundle = null;
                try {
                    bundle= get_bitmap_with_CircleProgressView(require_pixiv_square_1200(get_pixiv_origin_pic_path(curr_pid,curr_p)),circle_progress_view);
                    if(bundle==null){
                        throw new IOException();
                    }
                    byte[] b_array= bundle.getByteArray("byte_array");
                    b = BitmapFactory.decodeByteArray(b_array,0, b_array.length) ;
                } catch (Exception ignored) {
                }

                Bitmap finalB = b;

                close_CircleProgressView();

                if (finalB == null) {
                    status = -2;
                    activity.runOnUiThread(()->{
                        image_hint.setText("加载失败,正在获取原因");
                        AnimationForView.load_view(image_hint, 300, 1);
                    });
                    int error_id=get_pixiv_error_info(curr_pid,curr_p,null);
                    String s=get_pixiv_error_info_str(error_id);
                    activity.runOnUiThread(()->{
                        image_hint.setText(s);
                        AnimationForView.load_view(image_hint, 300, 1);
                        close_CircleProgressView();
                        if(error_id==-114514)
                            status = -1;
                        else
                            status = -3;
                    });
                }
                else{
                    if(!bundle.getBoolean("success"))
                        status = -1;
                    else
                        status = 2;
                    activity.runOnUiThread(()->{
                        image.setImageBitmap(finalB);
                        if(if_staggered)
                            image.setLayoutParams(new FrameLayout.LayoutParams
                                    (itemView.getWidth(), itemView.getWidth()));
                        AnimationForView.load_view(image, 300, 1);
                    });
                    image_hint.setVisibility(View.INVISIBLE);
                }
            }).start();
        }

        private void close_CircleProgressView(){
            activity.runOnUiThread(()->{
                circle_progress_view.setVisibility(View.INVISIBLE);
                circle_progress_view.setValue(0);
                circle_progress_view.spin();
            });
        }
    }

    public static String analyze_json_to_pixiv_like(String json_str){
        if (json_str.equals("null")) {
            return "没有详细数据(由pid获取的图片)";
        }
        try{
            JSONObject json = JSONObject.parseObject(json_str);
            String author = Objects.requireNonNull(json.get("author")).toString();
            String title = Objects.requireNonNull(json.get("title")).toString();
            String tags = Objects.requireNonNull(json.get("tags")).toString();
            String r18 = Objects.requireNonNull(json.get("r18")).toString();
            String width = Objects.requireNonNull(json.get("width")).toString();
            String height = Objects.requireNonNull(json.get("height")).toString();
            return "标题:"+title+"   作者:"+author+"\n"
                    +"源分辨率:"+width+"x"+height+"   r18:"+r18+"\n"
                    +"tags:"+tags;
        }
        catch (Exception e){
            return "json格式错误";
        }
    }

    public static boolean get_r18_status(String json_str){
        try{
            JSONObject json = JSONObject.parseObject(json_str);
            return Boolean.parseBoolean(Objects.requireNonNull(json.get("r18")).toString());
        }
        catch (Exception e){
            return false;
        }
    }

    public void remove_item(int position){
        //鬼知道我折腾这个几把玩意折腾了多久哈哈哈哈哈哈哈
        int true_position=get_true_position(position);

        pid_LinkedList.remove(true_position);
        p_LinkedList.remove(true_position);
        json_LinkedList.remove(true_position);
        add_date_LinkedList.remove(true_position);

        notifyItemRemoved(position);
        notifyItemRangeChanged(position, getItemCount() - position);
    }

    public int get_true_position(int position){
        //由于有正向和反向显示,所以有这个函数
        if(display_sequence)
            return position;
        return pid_LinkedList.size()-position-1;
    }
}

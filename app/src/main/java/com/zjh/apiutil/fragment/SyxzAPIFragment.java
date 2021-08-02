package com.zjh.apiutil.fragment;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zjh.apiutil.MainActivity;
import com.zjh.apiutil.R;
import com.zjh.apiutil.databinding.FragmentSyxzApiBinding;
import com.zjh.apiutil.view.ZLogView;

import java.io.InterruptedIOException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.zjh.apiutil.fragment.LoliconAPIFragment.add_api_info;
import static com.zjh.apiutil.fragment.LoliconAPIFragment.update_pixiv_like_text;
import static com.zjh.apiutil.fragment.TestAPIFragment.print_api_image;
import static com.zjh.apiutil.netutil.Api.api_util_get;
import static com.zjh.apiutil.tools.StaticTools.stamp_to_time;

public class SyxzAPIFragment extends Fragment {
    private ZLogView log_view;
    private int curr_pid=-1;
    private int curr_p=-1;
    private boolean like=false;

    private FragmentSyxzApiBinding syxz_binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        syxz_binding=FragmentSyxzApiBinding.inflate(inflater, container, false);

        log_view=((MainActivity)requireActivity()).log_view;
        SQLiteDatabase db =((MainActivity)requireActivity()).database.getWritableDatabase();


        syxz_binding.syxzApi.setOnClickListener(view -> syxz_api());
        syxz_binding.syxzApiLike.setOnClickListener(view->{
            if(like){
                change_like_status(false);
                update_pixiv_like_text("已取消收藏",log_view);

                db.delete("pixiv_like_table","pid=?",new String[]{Integer.toString(curr_pid)});
            }
            else {
                if(!change_like_status(true))
                    return;

                Cursor cursor = db.query("pixiv_like_table", new String[]{"pid", "add_date"}, null, null, null, null, null);
                int count=cursor.getCount();
                for(int i=0;i<count;i++){
                    cursor.moveToNext();
                    if(cursor.getInt(cursor.getColumnIndex("pid"))==curr_pid){
                        update_pixiv_like_text("这幅作品之前收藏过了(#"+i+",于"+stamp_to_time(cursor.getString(cursor.getColumnIndex("add_date")))+")",log_view);
                        cursor.close();
                        return;
                    }
                }
                cursor.close();

                ContentValues cv = new ContentValues();//实例化一个ContentValues用来装载待插入的数据
                cv.put("pid", curr_pid);
                cv.put("p", curr_p);
                cv.put("json", "null");
                cv.put("add_date",Long.toString(System.currentTimeMillis()));
                db.insert("pixiv_like_table", null, cv);

                update_pixiv_like_text("已收藏(pid="+curr_pid+",p="+curr_p+")",log_view);
            }
        });

        return syxz_binding.getRoot();
    }

    public void syxz_api(){
        if(log_view.start_task(true,null,false))
            return;
        change_like_status(false);

        String path="https://img.xjh.me/random_img.php?return=json";
        log_view.info_add("正在连接");
        log_view.start_thread(new Thread(() -> {
            long time = System.currentTimeMillis();
            String json_str;
            String image_url;
            try{
                json_str=api_util_get(path,log_view);
                JSONObject json= JSON.parseObject(json_str);

                image_url= Objects.requireNonNull(json.get("img")).toString();

                image_url="https:"+image_url;


                try{
                    // eg.
                    // {被删掉的部分}
                    // {img.xjh.me/desktop/img/}51273780{_p0_master1200.jpg}
                    Pattern pattern_pid=Pattern.compile("[\\s\\S]*img/|_p[\\s\\S]*");
                    Matcher m1=pattern_pid.matcher(image_url);
                    curr_pid= Integer.parseInt(m1.replaceAll("").trim());

                    // {img.xjh.me/desktop/img/51273780_p}0{_master1200.jpg}
                    //                                             [^0-9][\s\S] 从第一个非数字字符开始,匹配后面所有的
                    Pattern pattern_p=Pattern.compile("[\\s\\S]*_p|[^0-9][\\s\\S]*");
                    Matcher m2=pattern_p.matcher(image_url);
                    curr_p= Integer.parseInt(m2.replaceAll("").trim());
                }
                catch (NumberFormatException e){
                    log_view.info_add(ZLogView.status_gray,"获取pid失败");
                    curr_pid=-1;
                    curr_p=-1;
                }


                time = System.currentTimeMillis() - time;
                log_view.info_add(ZLogView.status_gray,"请求花费:"+(time / 1000.0)+"s");

                log_view.notify_continue_another_task();
                print_api_image(log_view ,image_url,false);
            }
            catch (Exception e){
                if(e instanceof InterruptedIOException)
                    log_view.info_add(ZLogView.status_hint,"任务被取消");
                else
                    log_view.info_add(ZLogView.status_error,e.toString());
                log_view.close_task();
            }
        }));
    }


    private boolean change_like_status(boolean _like){
        //return 是否change成功
        if(!like&&_like){
            if (curr_pid == -1 || curr_p == -1) {
                log_view.info_add(ZLogView.status_hint, "没有有效数据,收藏失败");
                log_view.scroll_down();
                return false;
            }

            syxz_binding.syxzApiLike.getDrawable().setTint(ContextCompat.getColor(requireContext(), R.color.light_red));
            like = true;
            return true;
        }
        else if(like&&!_like){
            syxz_binding.syxzApiLike.getDrawable().setTint(ContextCompat.getColor(requireContext(),R.color.white_for_text));
            like=false;
            return true;
        }
        return false;
    }

    public static void add_tips(ZLogView log_view, Context c){
        log_view.info_add_clickable("tips:岁月小筑API", new ClickableSpan() {
            @Override
            public void onClick(@NonNull View view) {
                ((LinearLayout) view.getParent()).removeView(view);
                add_api_info(log_view, "img.xjh.me", c);
            }
        });
    }
}

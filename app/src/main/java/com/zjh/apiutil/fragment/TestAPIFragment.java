package com.zjh.apiutil.fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zjh.apiutil.MainActivity;
import com.zjh.apiutil.databinding.FragmentTestApiBinding;
import com.zjh.apiutil.view.ZLogView;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Objects;

import static com.zjh.apiutil.netutil.Api.api_util_get;
import static com.zjh.apiutil.netutil.Api.get_bitmap;

public class TestAPIFragment extends Fragment {
    private ZLogView log_view;
    com.zjh.apiutil.databinding.FragmentTestApiBinding test_api_binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable  ViewGroup container, @Nullable  Bundle savedInstanceState) {
        test_api_binding= FragmentTestApiBinding.inflate(inflater, container, false);

        test_api_binding.testApiAppCompatButton1.setOnClickListener(view -> print_api_str(log_view,"https://chp.shadiao.app/api.php"));
        test_api_binding.testApiAppCompatButton2.setOnClickListener(view -> print_api_str(log_view,"https://nmsl.shadiao.app/api.php?level=min&lang=zh_cn"));
        test_api_binding.testApiAppCompatButton3.setOnClickListener(view -> print_api_str(log_view,"https://api.ixiaowai.cn/ylapi/index.php"));
        test_api_binding.testApiAppCompatButton4.setOnClickListener(view -> print_api_str(log_view,"https://httpbin.org/get?show_env=1"));
        test_api_binding.testApiAppCompatButtonAcgImage1.setOnClickListener(view -> xiaowai_api());
        test_api_binding.testApiAppCompatButtonAcgImage2.setOnClickListener(view -> print_api_image(log_view,"https://www.dmoe.cc/random.php",true));
        test_api_binding.testApiAppCompatButtonAcgImage3.setOnClickListener(view -> print_api_image(log_view,"https://acg.yanwz.cn/api.php",true));

        log_view=((MainActivity)requireActivity()).log_view;



        return test_api_binding.getRoot();
    }

    public static void print_api_str(ZLogView log_view,String path){
        if(log_view.start_task(true,null,false))
            return;

        log_view.info_add("正在连接");
        log_view.info_add(ZLogView.status_gray,"url:"+ path);

        log_view.start_thread(new Thread(() -> {
            long time = System.currentTimeMillis();
            String s;
            try{
                s=api_util_get(path,log_view);
            } catch (InterruptedIOException e){
                log_view.info_add(ZLogView.status_hint,"任务被取消");
                log_view.close_task();
                return;
            }
                //这里待优化,暂时没想到更好的办法

            if (s==null){
                log_view.close_task();
                return;
            }
            s = s.replace("    ", "###a###");
            s = s.replace("   ", "###b###");
            s = s.replace("  ", "###c###");


            s = s.replace("###a###", "\n        ");
            s = s.replace("###b###", "\n    ");
            s = s.replace("###c###", "\n");

            log_view.info_add(s);

            time = System.currentTimeMillis() - time;
            log_view.info_add(ZLogView.status_gray,"请求花费:"+(time / 1000.0)+"s");
            log_view.close_task();
        }));
    }

    public static void print_api_image(ZLogView log_view,String path, boolean clear_log){
        if(log_view.start_task(clear_log,null,false))
            return;

        log_view.info_add("正在加载图片");
        log_view.info_add(ZLogView.status_gray,"url:"+ path);

        log_view.start_thread(new Thread(() -> {
            long time = System.currentTimeMillis();
            try{
                Bitmap b=get_bitmap(path,log_view);
                if(b==null)
                    throw new IOException("图片获取失败");
                log_view.info_add(b);
            }
            catch (IOException e){
                if (e instanceof InterruptedIOException)
                    log_view.info_add(ZLogView.status_hint,"任务被取消");
                else log_view.info_add(ZLogView.status_error,e.toString());
            }
            time = System.currentTimeMillis() - time;
            log_view.info_add(ZLogView.status_gray,"加载花费:"+(time / 1000.0)+"s");
            log_view.close_task();
        }));
    }

    public void xiaowai_api(){
        if(log_view.start_task(true,null,false))
            return;

        String path="https://api.ixiaowai.cn/api/api.php?return=json";
        log_view.info_add("正在连接");
        log_view.info_add(ZLogView.status_gray,"url:"+ path);

        log_view.start_thread(new Thread(() -> {
            long time = System.currentTimeMillis();
            String json_str;
            String image_url;
            try{
                json_str=api_util_get(path,log_view);
                JSONObject json=JSON.parseObject(json_str);

                image_url= Objects.requireNonNull(json.get("imgurl")).toString();
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

    private static void add_api_info(String[] url,ZLogView log_view, Context c){
        //url不要带https
        log_view.info_add("API地址:");
        LinearLayout l0=log_view.info_add_group(26);
        for (String s : url)
            log_view.info_add_clickable(s, ZLogView.R_color_light_blue, new ClickableSpan() {
                        @Override
                        public void onClick(@NonNull View view) {
                            Uri uri = Uri.parse("https://" + s);
                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            c.startActivity(Intent.createChooser(intent, "请选择浏览器"));
                        }
                    }, l0, -1);
    }

    public static void add_tips(ZLogView log_view, Context c){
        String[] urls={"chp.shadiao.app","nmsl.shadiao.app","httpbin.org","api.ixiaowai.cn","www.dmoe.cc","acg.yanwz.cn"};

        log_view.info_add_clickable("API地址", new ClickableSpan() {
            @Override
            public void onClick(@NonNull View view) {
                ((LinearLayout) view.getParent()).removeView(view);
                add_api_info(urls,log_view,c);
            }
        });
    }
}

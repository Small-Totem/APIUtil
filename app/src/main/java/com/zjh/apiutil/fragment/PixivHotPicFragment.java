package com.zjh.apiutil.fragment;
import android.content.Context;
import android.os.Bundle;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zjh.apiutil.MainActivity;
import com.zjh.apiutil.R;
import com.zjh.apiutil.databinding.FragmentPixivHotPicBinding;
import com.zjh.apiutil.view.ZLogView;

import java.io.InterruptedIOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import static com.zjh.apiutil.fragment.GetPixivPicFragment.get_pixiv_pic_from_pid_without_thread;
import static com.zjh.apiutil.fragment.LoliconAPIFragment.add_api_info;
import static com.zjh.apiutil.netutil.Api.add_get_request;
import static com.zjh.apiutil.netutil.Api.api_util_get;

public class PixivHotPicFragment extends Fragment {
    private ZLogView log_view;
    private String curr_mode="daily";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        FragmentPixivHotPicBinding binding = FragmentPixivHotPicBinding.inflate(inflater, container, false);
        log_view = ((MainActivity) requireActivity()).log_view;


        binding.pixivHotPicAppCompatSpinnerMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                String[] modes=getResources().getStringArray(R.array.pixiv_hot_pic_mode);
                curr_mode=translate_pixiv_hot_pic_mode(modes[pos]);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
        binding.pixivHotPic.setOnClickListener(view -> random_pixiv_hot_pic());



        return binding.getRoot();
    }

    private void random_pixiv_hot_pic() {
        if (log_view.start_task(true, null, false))
            return;

        String path = "https://api.acgmx.com/public/ranking?ranking_type=illust&per_page=10&page=1";

        String date;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        long day_before_yesterday = System.currentTimeMillis() - 172800000;// 减两天
        date = sdf.format(new Date(day_before_yesterday));

        path=add_get_request(path,"date",date,false);
        path=add_get_request(path,"mode",curr_mode,false);


        log_view.info_add("正在连接");
        log_view.info_add(ZLogView.status_gray,"mode:"+curr_mode+",date:" + date);

        String finalPath = path;
        log_view.start_thread(new Thread(() -> {
            long time = System.currentTimeMillis();
            String json_str;
            try {
                json_str = api_util_get(finalPath, null);
            } catch (InterruptedIOException e) {
                log_view.info_add(ZLogView.status_hint, "任务被取消");
                log_view.close_task();
                return;
            }
            try {
                JSONObject json = JSON.parseObject(json_str);

                log_view.info_add(ZLogView.status_gray,"status:"+ Objects.requireNonNull(json.get("status")).toString());

                JSONArray response_array = JSONObject
                        .parseArray(Objects.requireNonNull(json.get("response")).toString());
                JSONObject response=JSON.parseObject(response_array.get(0).toString());
                JSONArray works_array = JSONObject
                        .parseArray(Objects.requireNonNull(response.get("works")).toString());

                time = System.currentTimeMillis() - time;
                log_view.info_add(ZLogView.status_gray, "请求花费:" + (time / 1000.0) + "s");

                log_view.notify_interrupt_whole_task();
                for (int i = 0; i < 10; i++) {
                    log_view.add_split_line(0,0);
                    JSONObject work_outer=JSON.parseObject(works_array.get(i).toString());
                    JSONObject work=JSON.parseObject(Objects.requireNonNull(work_outer.get("work")).toString());
                    int curr_pid = (int) Objects.requireNonNull(work.get("id"));
                    get_pixiv_pic_from_pid_without_thread(log_view, curr_pid, 0, false, null, false);
                    if(log_view.should_interrupt_curr_thread){
                        log_view.should_interrupt_curr_thread=false;
                        throw new InterruptedIOException();
                    }
                }
            } catch (Exception e) {
                if (e instanceof InterruptedIOException)
                    log_view.info_add(ZLogView.status_hint, "任务被取消");
                else
                    log_view.info_add(ZLogView.status_error, e.toString());
            }
            log_view.close_task();
        }));
    }

    private String translate_pixiv_hot_pic_mode(String a){
        switch (a){
            case "每日":
                return "daily";
            case "每周":
                return "weekly";
            case "每月":
                return "monthly";
            case "每日/r18":
                return "daily_r18";
            case "每周/r18":
                return "weekly_r18";
        }
        return null;
    }

    public static void add_tips(ZLogView log_view, Context c){
        log_view.info_add_clickable("tips", new ClickableSpan() {
            @Override
            public void onClick(@NonNull View view) {
                ((LinearLayout) view.getParent()).removeView(view);
                add_api_info(log_view, "api.hcyacg.com", c);
                log_view.info_add("由于今天/昨天的数据不一定能获取到,默认获取日期为2天前");
                log_view.info_add("有时候会获取不到/空指针，懒得修了");
            }
        });
    }
}

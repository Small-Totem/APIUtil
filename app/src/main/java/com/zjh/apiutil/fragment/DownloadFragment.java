package com.zjh.apiutil.fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.zjh.apiutil.MainActivity;
import com.zjh.apiutil.databinding.FragmentDownloadBinding;
import com.zjh.apiutil.view.ZLogView;

import java.io.InterruptedIOException;

import static com.zjh.apiutil.netutil.Api.api_util_get;
import static com.zjh.apiutil.netutil.Api.api_util_post;
import static com.zjh.apiutil.netutil.Api.require_https;

public class DownloadFragment extends Fragment {
    private ZLogView log_view;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentDownloadBinding download_binding=FragmentDownloadBinding.inflate(inflater, container, false);

        log_view=((MainActivity)requireActivity()).log_view;
        download_binding.downloadAppCompatButtonGetAny.setOnClickListener(view -> get_get_str(
                download_binding.downloadEditTextGetAny.getText().toString()));
        download_binding.downloadAppCompatButtonPost.setOnClickListener(view ->get_post_str(
                download_binding.downloadEditTextPostUrl.getText().toString(),
                download_binding.downloadEditTextPostData.getText().toString()
        ));
        return download_binding.getRoot();
    }

    public void get_get_str(String path){
        if(log_view.start_task(true,null,false))
            return;

        path=require_https(path,true);
        log_view.info_add("正在连接");
        String finalPath = path;
        log_view.info_add(ZLogView.status_gray,"url:"+finalPath);

        log_view.start_thread(new Thread(() -> {
            try{
                String str=api_util_get(finalPath,log_view);
                if(str==null){
                    log_view.close_task();
                    return;
                }
                log_view.info_add(str);
            } catch (InterruptedIOException e){
                log_view.info_add(ZLogView.status_hint,"任务被取消");
            }
            log_view.close_task();
        }));
    }

    public void get_post_str(String url,String data){
        if(log_view.start_task(true,null,false))
            return;
        url=require_https(url,true);
        log_view.info_add("正在连接");
        log_view.info_add(ZLogView.status_gray,"url:"+url+"\ndata:"+data);

        String finalUrl = url;
        log_view.start_thread(new Thread(() -> {
            try{
                String str=api_util_post(finalUrl,data,log_view);
                if(str==null){
                    log_view.close_task();
                    return;
                }
                log_view.info_add(str);
            } catch (InterruptedIOException e){
                log_view.info_add(ZLogView.status_hint,"任务被取消");
            }
            log_view.close_task();
        }));
    }
}

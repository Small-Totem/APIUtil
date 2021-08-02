package com.zjh.apiutil.tools;
import android.os.Environment;

import com.zjh.apiutil.view.ZLogView;

import java.io.FileOutputStream;
import java.io.IOException;

//这个类可以向sd卡输出文件

public class FileIO {
    private FileOutputStream file_out;
    public ZLogView log_view;

    public FileIO(ZLogView logView){
        log_view=logView;
    }

    public void create_file(String path_and_name){
        new Thread(() ->{
            try {
                file_out = new FileOutputStream(Environment.getExternalStorageDirectory().getPath()+path_and_name);
            } catch (Exception e) {
                log_view.info_add(ZLogView.status_error,e.toString());
            }
        }).start();
    }

    //写在/mnt/sdcard/目录下面的文件
    public void write_line(String info){
        info+="\n";
        byte [] bytes = info.getBytes();
        new Thread(() ->{
            try{
                file_out.write(bytes);
            } catch(Exception e){
                log_view.info_add(ZLogView.status_error,e.toString());
            }
        }).start();
    }

    public void close_file(){
        new Thread(() ->{
            try {
                file_out.close();
            } catch (IOException e) {
                log_view.info_add(ZLogView.status_error,e.toString());
            }
        }).start();
    }
}

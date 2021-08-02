package com.zjh.apiutil.tools;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class StaticTools {
    public static String stamp_to_time(long stamp){
        return stamp_to_time(Long.toString(stamp));
    }
    public static String stamp_to_time(String stamp) {
        // 时间戳转换日期
        String sd;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
        sd = sdf.format(new Date(Long.parseLong(stamp)));
        return sd;
    }
    public static int get_max(int[] a){
        if(a.length==1)
            return a[0];
        int temp=a[0];
        for(int i=1;i<a.length;i++){
            if(a[i]>temp)
                temp=a[i];
        }
        return temp;
    }
    public static int get_min(int[] a){
        if(a.length==1)
            return a[0];
        int temp=a[0];
        for(int i=1;i<a.length;i++){
            if(a[i]<temp)
                temp=a[i];
        }
        return temp;
    }
    /*private byte[] bitmap_to_byte_array(@NonNull Bitmap bitmap){
        ByteArrayOutputStream b=new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, b);
        return b.toByteArray();
    }*/
    public static int[] get_pid_and_p_from_json(String json_str) {
        // [0]是pid [1]是p
        int[] temp = new int[2];
        try {
            JSONObject json = JSON.parseObject(json_str);
            temp[0] = Integer.parseInt(Objects.requireNonNull(json.get("pid")).toString());
            temp[1] = Integer.parseInt(Objects.requireNonNull(json.get("p")).toString());
            return temp;
        } catch (Exception e) {
            return new int[] { -1, -1 };
        }
    }
}

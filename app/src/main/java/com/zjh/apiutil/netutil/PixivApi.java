/*  Small-Totem 2021.5.17
    由pid获取原图链接的方法: POST https://api.pixiv.cat/v1/generate , data="p=xxx",其中xxx为pid
    会得到一个json,里面有原图的链接
    这个api来自https://pixiv.cat/generator.html 通过view-source找到.
*/
package com.zjh.apiutil.netutil;
import android.os.Bundle;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zjh.apiutil.view.ZLogView;

import java.io.IOException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.zjh.apiutil.netutil.Api.api_util_post;
import static com.zjh.apiutil.netutil.Api.connect_test;

public class PixivApi {

    public static String get_pixiv_cat_path(int pid,int p){
        String path = "https://pixiv.cat/" + pid;
        if (p != 0)
            path += "-" + p;
        path += ".jpg";
        return path;
    }

    public static String require_pixiv_master_1200(String path){
        if(path.contains("_master1200"))
            return path;
        path=path.replace("img-original","img-master");
        path=path.replace(".jpg","_master1200.jpg");
        path=path.replace(".png","_master1200.jpg");
        path=path.replace(".gif","_master1200.jpg");
        return path;
    }
    public static String require_pixiv_square_1200(String path){
        if(path.contains("_square1200"))
            return path;
        path = require_pixiv_master_1200(path);
        path = path.replace("_master1200","_square1200");
        return path;
    }

    //注意,这里返回的path是用了代理的(i.pixiv.cat)
    public static String get_pixiv_origin_pic_path(int pid) throws IOException {
        String json_str = api_util_post("https://api.pixiv.cat/v1/generate","p="+pid,null);
        JSONObject json = JSONObject.parseObject(json_str);
        boolean success = Boolean.parseBoolean(Objects.requireNonNull(json.get("success")).toString());
        boolean multiple = Boolean.parseBoolean(Objects.requireNonNull(json.get("multiple")).toString());

        if(!success)
            throw new IOException("获取失败,pid可能错误");
        if(multiple)
            throw new IOException("多图,请使用get_pixiv_origin_pic_path(int pid,int p)");
        return Objects.requireNonNull(json.get("original_url_proxy")).toString();
    }
    public static String get_pixiv_origin_pic_path(int pid,int p) throws IOException{
        if(p==0)
            return get_pixiv_origin_pic_path(pid);
        String json_str = api_util_post("https://api.pixiv.cat/v1/generate","p="+pid,null);
        JSONObject json = JSONObject.parseObject(json_str);
        boolean success = Boolean.parseBoolean(Objects.requireNonNull(json.get("success")).toString());
        boolean multiple = Boolean.parseBoolean(Objects.requireNonNull(json.get("multiple")).toString());
        if(!success)
            throw new IOException("获取失败,pid可能错误");
        if(!multiple)
            return get_pixiv_origin_pic_path(pid);

        JSONArray json_str_original_urls_proxy=JSONObject.parseArray(Objects.requireNonNull(json.get("original_urls_proxy")).toString());
        return json_str_original_urls_proxy.get(p-1).toString();
    }

    //涉及网络请求，要新开thread执行这个函数
    //log_view可为null
    public static int get_pixiv_error_info(int pid, int p, ZLogView log_view){
        String path=get_pixiv_cat_path(pid,p);

        Bundle b=connect_test(path,log_view);
        if(b==null){
            return -114514;
        }
        String html=b.getString("error_info");

        if(html!=null){
            if(html.contains("可能已被刪除")){
                return -1;
            }/* 这个实际不会碰到
            else if(html.contains("頁數錯誤")){
                log_view.update_editable(editable_id,Color.CYAN,"页数错误,'p'的值从1开始");
            }*/
            else if(html.contains("只有一張圖片")){
                return -2;
            }
            else if(html.contains("削除済みもしくは非公開")){
                return -3;
            }
            else if(html.contains("這個作品ID中有多張圖片")){
                //大概2021.5.28以来,pixiv.cat的部分错误信息中不会再返回图片张数,需要自己想办法
                return -4;
            }
            else if(html.contains("張圖片")){
                //这里的实现可优化，但懒的搞
                //用两次正则表达式，第一次删去404和h1，第二次删去数字以外的别的字符
                //这样就不能匹配404张图片，但是将就吧
                Pattern pattern1=Pattern.compile("404|h1");
                Pattern pattern2=Pattern.compile("[^0-9]");

                Matcher m1=pattern1.matcher(html);
                html=m1.replaceAll("").trim();
                Matcher m2=pattern2.matcher(html);
                return Integer.parseInt(m2.replaceAll("").trim());
            }
            else {
                return -810;
            }
        }
        else {
            return -114514;
        }
    }

    public static String get_pixiv_error_info_str(int error_id){
        switch(error_id){
            case -1:
                return "此作品可能已被删除或因其他原因无法获取";
            case -2:
                return "此作品只有一张图片,请将p的值改为0";
            case -3:
                return "作品被删除或非公开";
            case -4:
                return "此作品有多张图片,请尝试获取完整图片或更改p的值";
            case -810:
                return "未知原因";
            case -114514:
                return "网络错误,请重试";
            default:
                return "此作品有"+error_id+"张图片,请尝试获取完整图片或更改p的值";
        }
    }
}

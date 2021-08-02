package com.zjh.apiutil.netutil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.zjh.apiutil.view.ZLogView;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;

import at.grabner.circleprogress.CircleProgressView;


public class Api {
    public static String require_https(@NonNull String path, boolean replace_http){
        //android9以上默认禁用http,需要设置开启
        //于manifest
        //<!--android:usesCleartextTraffic="true"-->
        //参考自https://blog.csdn.net/nidongde521/article/details/86496804
        if(path.contains("http://")){
            if(!replace_http)
                return path;

            path=path.replace("http://","https://");
        }
        else if(!path.contains("://")){
            path="https://"+path;
        }
        return path;
    }
    public static String add_get_request(String path, String param_name, String value, boolean enable_empty_value){
        if(!enable_empty_value&&value.equals(""))
            return path;
        if(path.contains("?")){
            path+="&";
        }
        else {
            path+="?";
        }
        path+=param_name+"="+value;

        return path;
    }

    //注意,要处理返回null的情况
    //此函数已摆脱log_view依赖(log_view可为null)
    public static String api_util_get(String path, ZLogView log_view) throws InterruptedIOException {
        //get方法
        try {
            URL url = new URL(path);
            HttpURLConnection conn;

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");
            conn.setRequestMethod("GET");// GET和POST必须全大写
            //conn.setDoOutput(true);
            conn.setDoInput(true);

            try_connect_with_fuck_TrustManager(log_view, conn);

            String content_type=conn.getContentType();
            if(log_view!=null)
                log_view.info_add(ZLogView.status_gray,
                        "ContentType:"+content_type+"   ContentLength:"+conn.getContentLengthLong());

            InputStream is = conn.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String str;
            StringBuilder a= new StringBuilder();
            while ((str = br.readLine()) != null) {
                str = new String(str.getBytes(), StandardCharsets.UTF_8);// 解决中文乱码问题
                a.append(str);
            }
            is.close();

            //conn.disconnect();
            if(content_type.contains("json")&&log_view!=null)
                log_view.info_add(ZLogView.status_gray,"json:"+a.toString());
            return a.toString();
        } catch (InterruptedIOException e) {
           throw e;
        } catch (Exception e){
            if(log_view!=null)
                log_view.info_add(ZLogView.status_error,"ERR@api_util_get: "+e.toString());
            return null;
        }
    }

    //注意,要处理返回null的情况
    //此函数已摆脱log_view依赖(log_view可为null)
    public static String api_util_post(String path, String data, ZLogView log_view)throws InterruptedIOException {
        // 轮子，来自https://blog.csdn.net/u013310119/article/details/82705317
        /*
         * 调用对方接口方法
         *  @param path 对方或第三方提供的路径
         * @param data 向对方或第三方发送的数据，大多数情况下给对方发送JSON数据让对方解析
         */
        //post方法

        try {
            URL url = new URL(path);
            // 打开和url之间的连接
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            PrintWriter out;

            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");
            // 设置是否向httpUrlConnection输出，设置是否从httpUrlConnection读入，此外发送post请求必须设置这两个
            // 最常用的Http请求无非是get和post，get请求可以获取静态页面，也可以把参数放在URL字串后面，传递给servlet，
            // post与get的 不同之处在于post的参数不是放在URL字串里面，而是放在http请求的正文内。

            //public void setDoInput(boolean doinput)将此 URLConnection 的 doInput 字段的值设置为指定的值。
            //URL 连接可用于输入和/或输出。如果打算使用 URL 连接进行输入，则将 DoInput 标志设置为 true；如果不打算使用，则设置为 false。默认值为 true。
            //public void setDoOutput(boolean dooutput)将此 URLConnection 的 doOutput 字段的值设置为指定的值。
            //URL 连接可用于输入和/或输出。如果打算使用 URL 连接进行输出，则将 DoOutput 标志设置为 true；如果不打算使用，则设置为 false。默认值为 false。

            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");// GET和POST必须全大写

            out = new PrintWriter(conn.getOutputStream());//获取URLConnection对象对应的输出流
            out.print(data);//发送请求参数即数据
            out.flush();//缓冲数据

            InputStream is = conn.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String str;
            StringBuilder a= new StringBuilder();
            while ((str = br.readLine()) != null) {
                str = new String(str.getBytes(), StandardCharsets.UTF_8);// 解决中文乱码问题
                a.append(str);
            }
            is.close();
            //conn.disconnect();
            return a.toString();
        } catch (InterruptedIOException e) {
           throw e;
        } catch (Exception e){
            if (log_view!=null)
                log_view.info_add(ZLogView.status_error,"ERR@api_util_post: "+e.toString());
            return null;
        }
    }

    //此函数已摆脱log_view依赖(log_view可为null,view可为null)
    public static Bundle get_byte_array_with_ZLog_progress(InputStream is, ZLogView log_view, Long contentLength, TextView view){
        //搞这个麻烦玩意是为了获取下载进度
        //不要下载进度的话,直接InputStream inputStream = conn.getInputStream();
        //然后BitmapFactory.decodeStream(inputStream);就完事了
        //bundle里面装了    boolean success,Byte[] byte_array,long total_read
        if(view==null && log_view!=null){
            view=log_view.info_add_editable(Color.WHITE,"[0%]0/"+contentLength);
        }
        long total_read=0;
        ByteArrayOutputStream out_stream = new ByteArrayOutputStream();

        Bundle bundle=new Bundle();

        try {
            byte[] buffer = new byte[1024]; // 用数据装
            int len;
            double progress;

            NumberFormat nf = NumberFormat.getNumberInstance();
            nf.setMaximumFractionDigits(2);
            nf.setRoundingMode(RoundingMode.UP);

            while ((len = is.read(buffer)) != -1) {
                total_read += len;
                if(log_view!=null){
                    if (contentLength <= 0)
                        log_view.update_editable(view, Color.WHITE, "[?%]" + total_read + "/?");
                    else{
                        progress = total_read * 100d / (double) contentLength;
                        log_view.update_editable(view, Color.WHITE, "[" + nf.format(progress) + "%]" + total_read + "/" + contentLength);
                    }
                }
                out_stream.write(buffer, 0, len);
            }
            out_stream.close();

            if(log_view!=null){
                if (contentLength <= 0)
                    log_view.update_editable(view, Color.WHITE, "[100%]" + total_read + "/" +total_read);
                log_view.close_editable(view);
            }

            bundle.putBoolean("success",true);
            bundle.putByteArray("byte_array",out_stream.toByteArray());
            bundle.putLong("total_read",total_read);
            return bundle;
        }
        catch (Exception e){
            if(log_view!=null){
                log_view.close_editable(view);
                if(e instanceof InterruptedIOException)
                    log_view.info_add(ZLogView.status_hint,"任务被取消");
                else
                    log_view.info_add_by_view(ZLogView.status_error,"ERR@get_byte_array_with_ZLog_progress: "+"数据流中断:"+e.toString(),view,true);
            }

            bundle.putBoolean("success",false);
            bundle.putByteArray("byte_array",out_stream.toByteArray());
            bundle.putLong("total_read",total_read);
            return bundle;
        }
    }

    //此函数已摆脱log_view依赖(log_view可为null,referer可为null)
    //只要传进来了log_view就会默认要显示进度
    public static Bitmap get_bitmap(String path, ZLogView log_view) throws InterruptedIOException{
        if(log_view==null)
            return get_bitmap(path,null,null, -1,null);
        else
            return get_bitmap(path,log_view,log_view.linearLayout, -1,null);
    }
    public static Bitmap get_bitmap(String path, ZLogView log_view, String referer) throws InterruptedIOException{
        if(log_view==null)
            return get_bitmap(path,null, referer,null, -1,null,true);
        else
            return get_bitmap(path,log_view,referer,log_view.linearLayout, -1,null,true);
    }
    public static Bitmap get_bitmap(String path, ZLogView log_view, LinearLayout layout, int info_index,TextView progress_view) throws InterruptedIOException{
        return get_bitmap(path,log_view,null,layout,info_index,progress_view,true);
    }
    public static Bitmap get_bitmap(String path, ZLogView log_view, String referer, LinearLayout layout, int info_index, TextView progress_view, boolean print_error) throws InterruptedIOException{
        try {
            URL url = new URL(path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");
            if (referer != null)
                conn.setRequestProperty("referer", referer);
            conn.setRequestMethod("GET");

            try_connect_with_fuck_TrustManager(log_view, conn);
            //conn.connect();


            int response_code = conn.getResponseCode();
            final long contentLength = conn.getContentLengthLong();
            String content_type = conn.getContentType();

            if (response_code == 200) {
                if (content_type.equals("image/jpeg") ||
                        content_type.equals("image/gif") ||
                        content_type.equals("image/png")) {

                    if (log_view != null) {
                        String text;
                        if (contentLength <= 0)
                            text = "目标大小:未知";
                        else
                            text = "目标大小:" + contentLength / 1024 + "KB";

                        if (info_index == -1)
                            log_view.info_add(ZLogView.status_gray, text);
                        else
                            log_view.info_add(ZLogView.status_gray, text, layout, info_index);
                    }

                    final InputStream input_stream = conn.getInputStream();

                    if (log_view == null) {
                        return BitmapFactory.decodeStream(input_stream);
                    }

                    Bundle bundle = get_byte_array_with_ZLog_progress(input_stream, log_view, contentLength, progress_view);
                    //if(!bundle.getBoolean("success")&&log_view!=null)
                    //    log_view.info_add_clickable();
                    byte[] b = bundle.getByteArray("byte_array");

                    //conn.disconnect();

                    //BitmapFactory.Options options = new BitmapFactory.Options();
                    //options.inPreferredConfig=Bitmap.Config.RGB_565;

                    return BitmapFactory.decodeByteArray(b, 0, b.length);
                }
                if (log_view != null&&print_error)
                    log_view.info_add(ZLogView.status_error, "ERR@get_bitmap: " + "格式错误,content_type:" + content_type);
                //conn.disconnect();
                return null;
            }
            if (log_view != null&&print_error)
                log_view.info_add(ZLogView.status_error, "ERR@get_bitmap: " + "获取数据失败,response_code:" + response_code);
            //conn.disconnect();
            return null;
        }catch (IOException e){
            if(e instanceof InterruptedIOException)
                throw (InterruptedIOException)e;
            if(log_view!=null&&print_error)
                log_view.info_add(ZLogView.status_error, "ERR@get_bitmap: " + e.toString());
            return null;
        }
    }

    public static Bundle get_bitmap_with_CircleProgressView(String path, CircleProgressView progress_view){
        try {
            URL url = new URL(path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");
            conn.setRequestMethod("GET");
            conn.connect();

            int response_code = conn.getResponseCode();
            final long contentLength = conn.getContentLengthLong();
            String content_type = conn.getContentType();

            if (response_code == 200) {
                if (content_type.equals("image/jpeg") ||
                        content_type.equals("image/gif") ||
                        content_type.equals("image/png")) {

                    final InputStream is = conn.getInputStream();


                    long total_read=0;
                    ByteArrayOutputStream out_stream = new ByteArrayOutputStream();
                    Bundle bundle=new Bundle();


                    try {
                        byte[] buffer = new byte[1024]; // 用数据装
                        int len;
                        float progress;

                        while ((len = is.read(buffer)) != -1) {
                            total_read += len;

                            if (contentLength > 0) {
                                progress = total_read * 100f / (float) contentLength;
                                progress_view.setValueAnimated(progress,200);
                            }

                            out_stream.write(buffer, 0, len);
                        }
                        out_stream.close();

                        bundle.putBoolean("success",true);
                        bundle.putByteArray("byte_array",out_stream.toByteArray());
                        bundle.putLong("total_read",total_read);
                        //conn.disconnect();
                        return bundle;

                    }
                    catch (Exception e){
                        bundle.putBoolean("success",false);
                        bundle.putByteArray("byte_array",out_stream.toByteArray());
                        bundle.putLong("total_read",total_read);
                        //conn.disconnect();
                        return bundle;
                    }
                }
                //conn.disconnect();
                return null;
            }
            //conn.disconnect();
            return null;
        }catch (IOException e){
            return null;
        }
    }


    //此函数已摆脱log_view依赖(log_view可为null,referer可为null)
    //没找到怎么才能判断bitmap是否完整,所以有了这个方法(为了加载不完整的时候判断并提示重新加载)
    //待改进
    public static Bundle get_bitmap_with_status(String path, ZLogView log_view, String referer)throws IOException{
        URL url = new URL(path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setRequestProperty("accept", "*/*");
        conn.setRequestProperty("connection", "Keep-Alive");
        conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");
        if(referer!=null)
            conn.setRequestProperty("referer", referer);
        conn.setRequestMethod("GET");

        try_connect_with_fuck_TrustManager(log_view,conn);

        int response_code=conn.getResponseCode();
        final long contentLength = conn.getContentLengthLong();
        String content_type=conn.getContentType();

        if (response_code == 200){
            if(content_type.equals("image/jpeg")||
                    content_type.equals("image/gif")||
                    content_type.equals("image/png")){

                if(log_view!=null){
                    String text;
                    if(contentLength<=0)
                        text="目标大小:未知";
                    else
                        text="目标大小:"+contentLength/1024+"KB";

                    log_view.info_add(ZLogView.status_gray,text);
                }

                final InputStream input_stream = conn.getInputStream();

                return get_byte_array_with_ZLog_progress(input_stream,log_view,contentLength,null);
            }
            if(log_view!=null)log_view.info_add(ZLogView.status_error,"ERR@get_bitmap_with_status: "+"格式错误,content_type:"+content_type);
            //conn.disconnect();
            return null;
        }
        if(log_view!=null)log_view.info_add(ZLogView.status_error,"ERR@get_bitmap_with_status: "+"获取数据失败,response_code:"+response_code);
        //conn.disconnect();
        return null;
    }

    //此函数已摆脱log_view依赖(log_view可为null)
    public static void try_connect_with_fuck_TrustManager(ZLogView log_view, HttpURLConnection conn) throws IOException {
        //信任所有证书 参考自https://blog.csdn.net/zdreamLife/article/details/100058887

        try{
            //尝试正常连接
            conn.connect();
        }
        catch (SSLHandshakeException e){
            //如果证书有问题,就尝试不用证书

            if(log_view==null)
                throw e;
            else {
                log_view.info_add(ZLogView.status_warning,"ERR@try_connect_with_fuck_TrustManager: "+e.toString());
                log_view.info_add(ZLogView.status_warning,"证书验证失败");
            }


            log_view.info_add_clickable("信任所有证书", new ClickableSpan() {
                @Override
                public void onClick(@NonNull View view) {
                    try {
                        ((LinearLayout)view.getParent()).removeView(view);
                        SSLContext  sslcontext = SSLContext.getInstance("SSL");

                        sslcontext.init(null, new TrustManager[]{new FuckX509TrustManager()}, new java.security.SecureRandom());

                        HostnameVerifier ignoreHostnameVerifier = (s, sslsession) -> {
                            log_view.info_add(ZLogView.status_warning,"Hostname is not matched for cert. HostnameVerifier Ignored.");
                            return true;
                        };

                        HttpsURLConnection.setDefaultHostnameVerifier(ignoreHostnameVerifier);
                        HttpsURLConnection.setDefaultSSLSocketFactory(sslcontext.getSocketFactory());
                        log_view.info_add("已设置信任所有证书");
                    } catch (Exception e){
                        log_view.info_add(ZLogView.status_error,"ERR@try_connect_with_fuck_TrustManager: "+e.toString());
                    }
                }
            });
            log_view.info_add(ZLogView.status_warning,"设置信任所有证书后,所有网络请求都会以此模式执行");
        }
    }

    //此函数已摆脱log_view依赖(log_view可为null)
    public static Bundle connect_test(String path, ZLogView log_view){
        //返回response_code和error_info

        int response_code;
        Bundle test_bundle;
        try {
            URL url = new URL(path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");
            conn.setRequestMethod("GET");
            conn.setDoInput(true);

            conn.connect();

            response_code=conn.getResponseCode();

            test_bundle=new Bundle();
            test_bundle.putInt("response_code",response_code);

            if(response_code!=200) {
                InputStream is = conn.getErrorStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String str;
                StringBuilder a = new StringBuilder();
                while ((str = br.readLine()) != null) {
                    str = new String(str.getBytes(), StandardCharsets.UTF_8);// 解决中文乱码问题
                    a.append(str);
                }
                test_bundle.putString("error_info", a.toString());
            }
            //conn.disconnect();
        } catch (Exception e) {
            if(log_view!=null)
                log_view.info_add(ZLogView.status_error,"ERR@connect_test: "+e.toString());
            return null;
        }
        return test_bundle;
    }
}

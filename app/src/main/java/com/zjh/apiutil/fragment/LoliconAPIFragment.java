package com.zjh.apiutil.fragment;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zjh.apiutil.MainActivity;
import com.zjh.apiutil.R;
import com.zjh.apiutil.databinding.FragmentLoliconApiBinding;
import com.zjh.apiutil.view.ZLogView;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Objects;

import static com.zjh.apiutil.netutil.Api.add_get_request;
import static com.zjh.apiutil.netutil.Api.api_util_get;
import static com.zjh.apiutil.netutil.Api.get_bitmap;
import static com.zjh.apiutil.netutil.Api.get_bitmap_with_status;
import static com.zjh.apiutil.netutil.PixivApi.require_pixiv_square_1200;
import static com.zjh.apiutil.tools.StaticTools.stamp_to_time;

public class LoliconAPIFragment extends Fragment {
    private ZLogView log_view;
    private Dialog settings_dialog;

    private String apikey        ="";
    private String keyword       ="";
    private String proxy         ="";
    private boolean r18          =false;
    private boolean like         =false;
    private boolean master1200   =true;
    private boolean square1200   =false;

    private String curr_json_data=null;
    private int curr_pid         =-1;
    private int curr_p           =-1;

    EditText dialog_loliconapi_EditText_apikey;
    EditText dialog_loliconapi_EditText_keyword;
    EditText dialog_loliconapi_EditText_proxy;
    SwitchCompat dialog_loliconapi_SwitchCompat_r18;
    SwitchCompat dialog_loliconapi_SwitchCompat_master1200;
    SwitchCompat dialog_loliconapi_SwitchCompat_square1200;
    SwitchCompat dialog_loliconapi_SwitchCompat_use_pid_to_load;

    com.zjh.apiutil.databinding.FragmentLoliconApiBinding lolicon_api_binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
        //return inflater.inflate(R.layout.fragment_lolicon_api, container,false);
        //这里不用指定R.layout.fragment_lolicon_api,是因为FragmentLoliconApiBinding本身就是根据R.layout.fragment_lolicon_api生成的类
        lolicon_api_binding= FragmentLoliconApiBinding.inflate(inflater, container, false);

        log_view=((MainActivity) requireActivity()).log_view;

        init_view();

        Drawable d= Objects.requireNonNull(ContextCompat.getDrawable(requireContext(), R.drawable.ui_logout)).mutate();
        d=DrawableCompat.wrap(d);
        ColorStateList c=ColorStateList.valueOf(Color.RED);
        DrawableCompat.setTintList(d,c);

        lolicon_api_binding.loliconApi.setOnClickListener(view -> lolicon_api());
        lolicon_api_binding.loliconApiTest.setOnClickListener(view -> lolicon_api_test());

        SQLiteDatabase db =((MainActivity)requireActivity()).database.getWritableDatabase();

        lolicon_api_binding.loliconApiLike.setOnClickListener(view -> {

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
                        update_pixiv_like_text("这幅作品之前收藏过了(#"+i+",于"+stamp_to_time(cursor.getString(cursor.getColumnIndex("add_date"))),log_view);
                        cursor.close();
                        return;
                    }
                }
                cursor.close();

                ContentValues cv = new ContentValues();//实例化一个ContentValues用来装载待插入的数据cv.put("username","Jack Johnson");
                cv.put("pid", curr_pid);
                cv.put("p", curr_p);
                cv.put("json", curr_json_data);
                cv.put("add_date",Long.toString(System.currentTimeMillis()));
                db.insert("pixiv_like_table", null, cv);

                update_pixiv_like_text("已收藏(pid="+curr_pid+",p="+curr_p+")",log_view);
            }
        });
        lolicon_api_binding.loliconApiSettings.setOnClickListener(view -> {
            dialog_loliconapi_EditText_apikey.setText(apikey);
            dialog_loliconapi_EditText_keyword.setText(keyword);
            dialog_loliconapi_SwitchCompat_r18.setChecked(r18);
            dialog_loliconapi_EditText_proxy.setText(proxy);
            dialog_loliconapi_SwitchCompat_master1200.setChecked(master1200);
            dialog_loliconapi_SwitchCompat_square1200.setChecked(square1200);
            settings_dialog.show();
        });

        return lolicon_api_binding.getRoot();
    }

    private void init_view(){
        LayoutInflater factory = LayoutInflater.from(getActivity());
        final View dialog_lolicon_api_settings = factory.inflate(R.layout.dialog_lolicon_api_settings, null);

        dialog_loliconapi_EditText_apikey=dialog_lolicon_api_settings.findViewById(R.id.dialog_loliconapi_EditText_apikey);
        dialog_loliconapi_EditText_keyword=dialog_lolicon_api_settings.findViewById(R.id.dialog_loliconapi_EditText_keyword);
        dialog_loliconapi_SwitchCompat_r18=dialog_lolicon_api_settings.findViewById(R.id.dialog_loliconapi_SwitchCompat_r18);
        dialog_loliconapi_EditText_proxy=dialog_lolicon_api_settings.findViewById(R.id.dialog_loliconapi_EditText_proxy);
        dialog_loliconapi_SwitchCompat_master1200 =dialog_lolicon_api_settings.findViewById(R.id.dialog_loliconapi_SwitchCompat_size1200);
        dialog_loliconapi_SwitchCompat_square1200=dialog_lolicon_api_settings.findViewById(R.id.dialog_loliconapi_SwitchCompat_square1200);
        dialog_loliconapi_SwitchCompat_use_pid_to_load=dialog_lolicon_api_settings.findViewById(R.id.dialog_loliconapi_SwitchCompat_use_pid_to_load);

        dialog_loliconapi_SwitchCompat_master1200.setOnCheckedChangeListener((compoundButton, b) -> {
            if(b) dialog_loliconapi_SwitchCompat_square1200.setChecked(false);
        });
        dialog_loliconapi_SwitchCompat_square1200.setOnCheckedChangeListener((compoundButton, b) -> {
            if(b) dialog_loliconapi_SwitchCompat_master1200.setChecked(false);
        });
        dialog_loliconapi_SwitchCompat_use_pid_to_load.setOnCheckedChangeListener((compoundButton, b) -> {
            if(b){
                dialog_loliconapi_SwitchCompat_master1200.setEnabled(false);
                dialog_loliconapi_SwitchCompat_square1200.setEnabled(false);
            }
            else {
                dialog_loliconapi_SwitchCompat_master1200.setEnabled(true);
                dialog_loliconapi_SwitchCompat_square1200.setEnabled(true);
            }
        });

        settings_dialog=new AlertDialog.Builder(requireActivity())
                .setTitle("参数设置")
                .setView(dialog_lolicon_api_settings)
                .setNegativeButton("取消",null)
                .setPositiveButton("确认",
                        (dialogInterface, i) -> {
                            //apikey=dialog_binding.dialogLoliconapiEditTextApikey.getText().toString();
                            //这里不能这样用binding,待研究
                            apikey=dialog_loliconapi_EditText_apikey.getText().toString();
                            keyword=dialog_loliconapi_EditText_keyword.getText().toString();
                            proxy=dialog_loliconapi_EditText_proxy.getText().toString();
                            r18=dialog_loliconapi_SwitchCompat_r18.isChecked();
                            master1200 = dialog_loliconapi_SwitchCompat_master1200.isChecked();
                            square1200=dialog_loliconapi_SwitchCompat_square1200.isChecked();
                        })
                .create();
    }

    private String master1200(){
        if(master1200)
            return "1200";
        return "";
    }

    private boolean change_like_status(boolean _like){
        //return 是否change成功
        if(!like&&_like){
            if (curr_json_data == null || curr_pid == -1 || curr_p == -1) {
                log_view.info_add(ZLogView.status_hint, "没有有效数据,收藏失败");
                log_view.scroll_down();
                return false;
            }

            lolicon_api_binding.loliconApiLike.getDrawable().setTint(ContextCompat.getColor(requireContext(), R.color.light_red));
            like = true;
            return true;
        }
        else if(like&&!_like){
            lolicon_api_binding.loliconApiLike.getDrawable().setTint(ContextCompat.getColor(requireContext(),R.color.white_for_text));
            like=false;
            return true;
        }
        return false;
    }

    static public void update_pixiv_like_text(String text, ZLogView log_view){
        //5步走:找到之前的textview,如果不null就删掉,加新的,存新的到hashmap,下滑
        TextView pixiv_like_editable;
        pixiv_like_editable=log_view.TextView_HashMap_for_editable.get("pixiv_like");
        if(pixiv_like_editable!=null)
            log_view.delete_text(pixiv_like_editable,false);
        pixiv_like_editable=log_view.info_add_editable(text);
        log_view.TextView_HashMap_for_editable.put("pixiv_like",pixiv_like_editable);
        log_view.scroll_down();
    }

    public void lolicon_api(){
        if(log_view.start_task(true,null,true))
            return;
        change_like_status(false);
        log_view.start_thread(new Thread(() -> {
            curr_pid=-1;
            log_view.info_add("正在请求");
            String path = "https://api.lolicon.app/setu/";
            path= add_get_request(path,"apikey",apikey,false);
            path= add_get_request(path,"keyword",keyword,false);
            path= add_get_request(path,"r18",r18?"1":"0",false);
            path= add_get_request(path,"proxy",proxy,false);
            path= add_get_request(path,"size1200", master1200(),false);

            long time = System.currentTimeMillis();

            String curr_json=null;
            try{
                curr_json =api_util_get(path,log_view);
            }
            catch (InterruptedIOException e){
                    log_view.info_add(ZLogView.status_hint,"任务被取消");
            }


            time = System.currentTimeMillis() - time;

            log_view.info_add(ZLogView.status_gray,"请求花费:"+(time / 1000.0)+"s");

            if(curr_json !=null) {
                log_view.info_add("请求完成");
            }
            else {
                log_view.info_add(ZLogView.status_error, "请求失败");
                log_view.info_add_clickable("重试", new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View view) {
                        lolicon_api();
                    }
                });
                log_view.close_task();
                return;
            }
            JSONObject json = JSONObject.parseObject(curr_json);

            JSONArray json_data_array = JSONObject.parseArray(Objects.requireNonNull(json.get("data")).toString());
            JSONObject json_data;

            /*
            //update:loliconapi更新了,取消了这个限制
            log_view.info_add("剩余请求次数:"+ Objects.requireNonNull(json.get("quota")).toString());
            if(Objects.requireNonNull(json.get("quota")).toString().equals("0")){
                log_view.info_add(ZLogView.status_warning,"请求次数用尽");
            }

            log_view.info_add("次数回复时间:"+ Objects.requireNonNull(json.get("quota_min_ttl")).toString()+"s");
            */

            if(json_data_array.isEmpty()){
                log_view.info_add(ZLogView.status_error,"获取数据失败");

                log_view.close_task();
                return;
            }
            else
                json_data= JSONObject.parseObject(json_data_array.get(0).toString());
            curr_json_data=json_data.toJSONString();
            curr_pid=(int)Objects.requireNonNull(json_data.get("pid"));
            curr_p=(int)Objects.requireNonNull(json_data.get("p"));

            String url=Objects.requireNonNull(json_data.get("url")).toString();
            if(square1200)url=require_pixiv_square_1200(url);
            log_view.info_add(ZLogView.status_gray,"url:"+url);

            final Bitmap[] b = {null};
            Bundle bundle;
            long time0 = System.currentTimeMillis();
            if(dialog_loliconapi_SwitchCompat_use_pid_to_load.isChecked()){
                log_view.notify_continue_another_task();
                if(curr_p!=0)curr_p++;
                GetPixivPicFragment.get_pixiv_pic_from_pid(log_view,curr_pid,curr_p
                        ,false,null,false);
            }
            else {
                try {
                    log_view.info_add("正在加载图片");
                    bundle=get_bitmap_with_status(url,log_view,null);
                    if(bundle==null){
                        throw new IOException();
                    }
                    byte[] b_array= bundle.getByteArray("byte_array");
                    b[0] = BitmapFactory.decodeByteArray(b_array,0, b_array.length);
                    time0 = System.currentTimeMillis() - time0;
                } catch (IOException e) {
                    if(e instanceof InterruptedIOException)
                        log_view.info_add(ZLogView.status_hint,"任务被取消");
                    log_view.info_add(ZLogView.status_error,"图片加载失败:"+e.toString());

                    log_view.info_add_clickable("使用pid尝试重新获取", new ClickableSpan() {
                        @Override
                        public void onClick(@NonNull View view) {
                            ((LinearLayout)view.getParent()).removeView(view);
                            //因为 pixiv.cat/xxx-x.jpg 的p值需要比正常的大1,所以这里curr_p+1
                            if(curr_p!=0)curr_p++;
                            GetPixivPicFragment.get_pixiv_pic_from_pid(log_view,curr_pid,curr_p
                                    ,false,null,false);
                        }
                    });
                    log_view.close_task();
                    return;
                }


                if(b[0] ==null){
                    log_view.info_add(ZLogView.status_error,"图片获取失败");
                    log_view.info_add_clickable("使用pid尝试重新获取", new ClickableSpan() {
                        @Override
                        public void onClick(@NonNull View view) {
                            ((LinearLayout)view.getParent()).removeView(view);
                            if(curr_p!=0)curr_p++;
                            GetPixivPicFragment.get_pixiv_pic_from_pid(log_view,curr_pid,
                                    curr_p,false,null,false);
                        }
                    });
                }
                else{
                    log_view.info_add(b[0], curr_pid +"_"+curr_p,"form_lolicon_api");
                    if (!bundle.getBoolean("success")){
                        log_view.info_add_clickable("使用pid尝试重新获取", new ClickableSpan() {
                            @Override
                            public void onClick(@NonNull View view) {
                                ((LinearLayout)view.getParent()).removeView(view);
                                if(curr_p!=0)curr_p++;
                                GetPixivPicFragment.get_pixiv_pic_from_pid(log_view,curr_pid,curr_p
                                        ,false,null,false);
                            }
                        });
                        log_view.close_task();
                        return;
                    }
                    log_view.info_add(ZLogView.status_gray,"加载花费:"+(time0 / 1000.0)+"s");
                    if((time0 / 1000.0)>=30.0){
                        log_view.info_add(ZLogView.status_hint,"如果加载速度太慢，可以尝试挂梯子/换个网/强制使用pid加载");
                    }
                }
                log_view.close_task();
            }
        }));
    }

    public void lolicon_api_test(){
        if(log_view.start_task(true,null,true))
            return;
        change_like_status(false);
        String json_str="{\"code\":0,\"msg\":\"\",\"quota\":8,\"quota_min_ttl\":7187,\"count\":1," +
                "\"data\":[{\"pid\":67993552,\"p\":0,\"uid\":9436809,\"title\":\"うちのこみずぎ\"," +
                "\"author\":\"みなとゆう\",\"url\":\"https:\\/\\/i.pixiv.cat\\/img-original\\/img\\" +
                "/2018\\/06\\/30\\/15\\/33\\/37\\/67993552_p0.jpg\",\"r18\":false,\"width\":1300," +
                "\"height\":1835,\"tags\":[\"オリジナル\",\"原创\",\"女の子\",\"女孩子\",\"ケモ耳\"," +
                "\"兽耳\",\"うちのこ\",\"OC\",\"水着\",\"泳装\"]}]}";
        log_view.info_add(ZLogView.status_gray,"json:"+json_str);

        //其中的data项("["开头的部分)被定义为JSONArray,需要用JSONArray.get(0)来获取内部的东西

        JSONObject json = JSONObject.parseObject(json_str);
        JSONArray json_data_array = JSONObject.parseArray(Objects.requireNonNull(json.get("data")).toString());
        JSONObject json_data = JSONObject.parseObject(json_data_array.get(0).toString());

        log_view.info_add(ZLogView.status_gray,"url:"+ Objects.requireNonNull(json_data.get("url")).toString());

        log_view.start_thread(new Thread(() -> {
            Bitmap b=null;
            try {
                b = get_bitmap(Objects.requireNonNull(json_data.get("url")).toString(),log_view,"https://www.pixiv.net");
            } catch (Exception e) {
                if(e instanceof InterruptedException ||e instanceof InterruptedIOException)
                    log_view.info_add(ZLogView.status_hint,"任务被取消");
                else
                    log_view.info_add(ZLogView.status_error,e.toString());
            }
            if(b ==null){
                log_view.info_add(ZLogView.status_error,"图片获取失败");
                log_view.close_task();
            }
            else{
                log_view.info_add(ZLogView.status_gray,"----raw_data:");

                log_view.info_add(ZLogView.status_gray,"RowBytes="+b.getRowBytes());
                log_view.info_add(ZLogView.status_gray,"AllocationByteCount="+b.getAllocationByteCount()+"("+(b.getAllocationByteCount()/1024)+"KB)");
                log_view.info_add(ZLogView.status_gray,"ByteCount="+b.getByteCount());

                long MaxMemory = Runtime.getRuntime().maxMemory()/1024/1024;
                long FreeMemory = Runtime.getRuntime().freeMemory()/1024/1024;
                long UsedMemory = MaxMemory - FreeMemory;

                log_view.info_add(ZLogView.status_gray,"MaxMemory="+MaxMemory+"MB");
                log_view.info_add(ZLogView.status_gray,"FreeMemory="+FreeMemory+"MB");
                log_view.info_add(ZLogView.status_gray,"UsedMemory="+UsedMemory+"MB");


                log_view.info_add(b,
                        Objects.requireNonNull(json_data.get("pid")).toString()+"_"
                                +Objects.requireNonNull(json_data.get("p")).toString()
                        ,"form_lolicon_api");
                log_view.close_task(true);
            }
        }));
    }

    public static void add_api_info(ZLogView log_view, String url, Context c){
        //url不要带https
        log_view.info_add("API地址:");
        LinearLayout l0=log_view.info_add_group(26);
        log_view.info_add_clickable(url, ZLogView.R_color_light_blue, new ClickableSpan() {
            @Override
            public void onClick(@NonNull View view) {
                Uri uri=Uri.parse("https://"+url);
                Intent intent=new Intent(Intent.ACTION_VIEW,uri);
                c.startActivity(Intent.createChooser(intent,"请选择浏览器"));
            }
        }
        ,l0,-1);
    }

    public static void add_tips(ZLogView log_view,Context c){
        log_view.info_add_clickable("tips:LoliconAPI", new ClickableSpan() {
            @Override
            public void onClick(@NonNull View view) {
                ((LinearLayout) view.getParent()).removeView(view);

                add_api_info(log_view,"api.lolicon.app/#/setu",c);

                log_view.info_add("关于参数:");
                LinearLayout l=log_view.info_add_group(26);
                log_view.info_add("apikey:需要自行申请(去api.lolicon.app),有key之后调用额度为300/天",l,-1);
                log_view.info_add("num:一次获取的图片数(未启用)",l,-1);
                log_view.info_add("proxy:代理地址",l,-1);
                log_view.info_add("size1200:获取到的图片长/宽最大为1200,以节约流量/提高速度",l,-1);
                log_view.info_add("square1200:同上,但图片为方形",l,-1);
            }
        });
    }
}

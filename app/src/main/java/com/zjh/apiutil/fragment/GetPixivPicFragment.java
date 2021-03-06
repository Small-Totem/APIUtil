package com.zjh.apiutil.fragment;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.zjh.apiutil.MainActivity;
import com.zjh.apiutil.R;
import com.zjh.apiutil.databinding.FragmentGetPixivPicBinding;
import com.zjh.apiutil.view.ZLogView;

import java.io.IOException;
import java.io.InterruptedIOException;

import static androidx.core.content.ContextCompat.getColor;
import static com.zjh.apiutil.fragment.LoliconAPIFragment.update_pixiv_like_text;
import static com.zjh.apiutil.netutil.Api.get_bitmap;
import static com.zjh.apiutil.netutil.Api.get_bitmap_with_status;
import static com.zjh.apiutil.netutil.Api.require_https;
import static com.zjh.apiutil.netutil.PixivApi.get_pixiv_cat_path;
import static com.zjh.apiutil.netutil.PixivApi.get_pixiv_error_info;
import static com.zjh.apiutil.tools.StaticTools.stamp_to_time;

public class GetPixivPicFragment extends Fragment {
    private ZLogView log_view;

    private Dialog settings_dialog;
    private SwitchCompat manga_mode_SwitchCompat;

    public FragmentGetPixivPicBinding get_pixiv_pic_binding;
    private boolean manga_mode=false;

    private boolean like=false;
    public int pid=-1;
    public int p=-1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //return inflater.inflate(R.layout.fragment_lolicon_api, container,false);
        //??????????????????R.layout.fragment_lolicon_api,?????????FragmentLoliconApiBinding??????????????????R.layout.fragment_lolicon_api????????????
        get_pixiv_pic_binding = FragmentGetPixivPicBinding.inflate(inflater, container, false);

        log_view=((MainActivity)requireActivity()).log_view;

        get_pixiv_pic_binding.getPixivPicButton1.setOnClickListener(view ->{
            if(update_info(manga_mode))
                return;
            get_pixiv_pic_from_pid(manga_mode);
        });
        get_pixiv_pic_binding.getPixivPicButton2.setOnClickListener(view -> get_pic_from_path(true));

        get_pixiv_pic_binding.getPixivPicSettings.setImageResource(R.drawable.ui_settings);
        get_pixiv_pic_binding.getPixivPicSettings.setOnClickListener(view -> {
            manga_mode=manga_mode_SwitchCompat.isChecked();
            settings_dialog.show();
        });

        SQLiteDatabase db =((MainActivity)requireActivity()).database.getWritableDatabase();

        get_pixiv_pic_binding.getPixivPicLike.setOnClickListener(view -> {
            if(like){
                change_like_status(false);
                update_pixiv_like_text("???????????????",log_view);

                db.delete("pixiv_like_table","pid=?",new String[]{Integer.toString(pid)});
            }
            else {
                if(!change_like_status(true))
                    return;

                Cursor cursor = db.query("pixiv_like_table", new String[]{"pid", "add_date"}, null, null, null, null, null);
                int count=cursor.getCount();
                for(int i=0;i<count;i++){
                    cursor.moveToNext();
                    if(cursor.getInt(cursor.getColumnIndex("pid"))==pid){
                        update_pixiv_like_text("??????????????????????????????(#"+i+",???"+stamp_to_time(cursor.getString(cursor.getColumnIndex("add_date")))+")",log_view);
                        cursor.close();
                        return;
                    }
                }
                cursor.close();

                ContentValues cv = new ContentValues();//???????????????ContentValues??????????????????????????????
                cv.put("pid", pid);
                cv.put("p", p);
                cv.put("json", "null");
                cv.put("add_date",Long.toString(System.currentTimeMillis()));
                db.insert("pixiv_like_table", null, cv);

                update_pixiv_like_text("?????????(pid="+pid+",p="+p+")",log_view);
            }
        });

        init_view();

        return get_pixiv_pic_binding.getRoot();
    }


    private void init_view(){
        LayoutInflater factory = LayoutInflater.from(getActivity());
        final View dialog_get_pixiv_pic_settings = factory.inflate(R.layout.dialog_get_pixiv_pic_settings, null);

        manga_mode_SwitchCompat=dialog_get_pixiv_pic_settings.findViewById(R.id.dialog_get_pixiv_pic_SwitchCompat_manga_mode);

        settings_dialog=new AlertDialog.Builder(requireActivity())
                .setTitle("????????????")
                .setView(dialog_get_pixiv_pic_settings)
                .setNegativeButton("??????",null)
                .setPositiveButton("??????",
                        (dialogInterface, i) -> {
                            try{
                                manga_mode=manga_mode_SwitchCompat.isChecked();
                                get_pixiv_pic_binding.getPixivPicEditText2.setEnabled(!manga_mode);
                            }
                            catch (Exception e){
                                log_view.info_add(ZLogView.status_error,e.toString());
                            }
                        })
                .create();
    }

    private boolean change_like_status(boolean _like){
        //@return ??????change??????
        if(!like&&_like){
            if (pid == -1 || p == -1) {
                log_view.info_add(ZLogView.status_hint, "??????????????????,????????????");
                log_view.scroll_down();
                return false;
            }

            get_pixiv_pic_binding.getPixivPicLike.getDrawable().setTint(ContextCompat.getColor(requireContext(), R.color.light_red));
            like = true;
            return true;
        }
        else if(like&&!_like){
            get_pixiv_pic_binding.getPixivPicLike.getDrawable().setTint(ContextCompat.getColor(requireContext(),R.color.white_for_text));
            like=false;
            return true;
        }
        return false;
    }

    private boolean update_info(boolean _manga_mode){
        //return?????????{????????????????????????(??????????????????return)}
        if(log_view.start_task(true,null,false))
            return true;

        if(_manga_mode){
            try {
                pid = Integer.parseInt(get_pixiv_pic_binding.getPixivPicEditText1.getText().toString());
            } catch (NumberFormatException e) {
                log_view.info_add(ZLogView.status_error, "????????????");
                log_view.close_task();
                return true;
            }
        }
        else {
            try {
                pid = Integer.parseInt(get_pixiv_pic_binding.getPixivPicEditText1.getText().toString());
                p = Integer.parseInt(get_pixiv_pic_binding.getPixivPicEditText2.getText().toString());
            } catch (NumberFormatException e) {
                log_view.info_add(ZLogView.status_error, "????????????");
                log_view.close_task();
                return true;
            }
        }
        log_view.close_task();
        return false;
    }

    public void get_pixiv_pic_from_pid(boolean _manga_mode){
        get_pixiv_pic_from_pid(log_view,pid,p,_manga_mode,this,true);
    }

    public void set_pixiv_error_info(int error_id, TextView view){
        switch(error_id){
            case -1:
                log_view.update_editable_with_close(view,ZLogView.status_hint,"?????????????????????????????????????????????????????????");
                break;
            case -2:
                log_view.update_editable_with_close(view,ZLogView.status_hint,"????????????????????????????????????p????????????0?????????????????????");
                log_view.info_add_clickable("???p=0,???????????????????????????", new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View view) {
                        get_pixiv_pic_binding.getPixivPicEditText2.setText("0");
                        p=0;
                        manga_mode_SwitchCompat.setChecked(false);
                        get_pixiv_pic_binding.getPixivPicEditText2.setEnabled(true);
                        manga_mode=false;
                        get_pixiv_pic_from_pid(false);
                    }
                });
                break;
            case -3:
                log_view.update_editable_with_close(view,ZLogView.status_hint,"???????????????????????????");
                break;
            case -4:
                log_view.update_editable_with_close(view,ZLogView.status_hint,"????????????????????????,???????????????????????????");
                log_view.info_add_clickable("??????????????????", new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View view) {
                        manga_mode_SwitchCompat.setChecked(true);
                        get_pixiv_pic_binding.getPixivPicEditText2.setEnabled(false);
                        manga_mode=true;
                        get_pixiv_pic_from_pid(true);
                    }
                });
                break;
            case -810:
                log_view.update_editable_with_close(view,ZLogView.status_hint,"????????????");
                break;
            case -114514:
                log_view.update_editable_with_close(view,ZLogView.status_hint,"????????????,?????????");
                log_view.info_add_clickable("??????", new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View view) {
                        get_pixiv_pic_from_pid(manga_mode);
                    }
                });
                break;
            default:
                log_view.update_editable_with_close(view,ZLogView.status_hint,"????????????"+error_id+"?????????,?????????'p'??????(???1??????)?????????????????????");
                log_view.info_add_clickable("??????????????????", new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View view) {
                        manga_mode_SwitchCompat.setChecked(true);
                        get_pixiv_pic_binding.getPixivPicEditText2.setEnabled(false);
                        manga_mode=true;
                        get_pixiv_pic_from_pid(true);
                    }
                });
        }
    }

    public void get_pic_from_path(boolean clear_log){
        if(log_view.start_task(clear_log,null,true))
            return;

        change_like_status(false);

        String path=get_pixiv_pic_binding.getPixivPicEditText3.getText().toString();
        path=require_https(path,true);

        final Bitmap[] b = {null};
        String finalPath = path;
        log_view.start_thread(new Thread(() -> {
            long time0 = System.currentTimeMillis();
            try {
                log_view.info_add("????????????????????????");
                b[0] = get_bitmap(finalPath,log_view);
                time0 = System.currentTimeMillis() - time0;
            } catch (Exception e) {
                if(e instanceof InterruptedIOException)
                    log_view.info_add(ZLogView.status_hint,"???????????????");
                log_view.info_add(ZLogView.status_error,"??????????????????\n"+e.toString());
                log_view.close_task();
                return;
            }

            if(b[0] ==null){
                log_view.info_add(ZLogView.status_error,"??????????????????");
            }
            else{
                log_view.info_add(b[0]);
                log_view.info_add(ZLogView.status_gray,"????????????:"+(time0 / 1000.0)+"s");
            }
            log_view.close_task();
        }));
    }

    public static void get_pixiv_pic_from_pid
            (ZLogView log_view, int pid, int p, boolean manga_mode, GetPixivPicFragment fragment, boolean clear_log){
        get_pixiv_pic_from_pid(log_view,pid,p,manga_mode,fragment,clear_log,true,true);
    }
    public static void get_pixiv_pic_from_pid
            (ZLogView log_view, int pid, int p, boolean manga_mode, GetPixivPicFragment fragment, boolean clear_log, boolean using_scroll_down, boolean show_status_info){
        //GetPixivPicFragment fragment ??????null(??????????????????)?????????????????????????????????????????????
        if(log_view.start_task(clear_log,null,using_scroll_down))
            return;

        if(fragment != null)
            fragment.change_like_status(false);

        //????????????
        if(manga_mode)
            log_view.scroll_down_enabled(false);

        log_view.start_thread(new Thread(() -> {
            get_pixiv_pic_from_pid_without_thread(log_view, pid, p, manga_mode, fragment, show_status_info);
            log_view.close_task();
        }));
    }
    public static void get_pixiv_pic_from_pid_without_thread
            (ZLogView log_view, int pid, int p, boolean manga_mode, GetPixivPicFragment fragment, boolean show_status_info){
        Context context=log_view.activity;
        if(manga_mode){
            TextView target_state = log_view.info_add_editable("????????????????????????");

            //?????????????????????????????????
            //??????2021.5.28??????,pixiv.cat???????????????????????????????????????????????????,?????????????????????(???????????????-4)
            int info = get_pixiv_error_info(pid, 0, log_view);


            //????????????????????????????????????????????????????????????
            if(info==-4){
                log_view.update_editable(target_state, getColor(context, R.color.gray), "pid=" + pid + ",p_sum=?");
                log_view.close_editable(target_state);

                int curr_p=1;
                boolean should_check=false;
                LinearLayout curr_group=null;

                //?????????????????????:????????????????????????,???????????????????????????,?????????????????????
                //(???????????????????????????,?????????????????????)??????????????????,??????????????????

                while (true) {
                    if(should_check){
                            /*
                            int error_id=get_pixiv_error_info(pid,curr_p,log_view);
                            if(error_id==-114514){
                                should_check=false;
                                continue;
                            }*/

                        LinearLayout finalCurr_group = curr_group;
                        log_view.activity.runOnUiThread(()->((LinearLayout) finalCurr_group.getParent()).removeView(finalCurr_group));
                        break;
                    }
                    curr_group = log_view.info_add_group(0);
                    log_view.info_add("???" + curr_p + "???", curr_group, -1);
                    TextView editable_progress = log_view.info_add_editable("[0%]0/?", curr_group, -1);
                    TextView editable_pic = log_view.info_add_editable("????????????????????????", curr_group, -1);

                    try {
                        Bitmap b = get_bitmap(get_pixiv_cat_path(pid, curr_p), log_view,"www.pixiv.net",
                                curr_group, curr_group.indexOfChild(editable_progress), editable_progress,false);
                        if (b == null) {
                            should_check = true;
                            throw new IOException();
                        } else
                            log_view.update_editable_with_close(editable_pic, b, pid + "_" + curr_p, null);
                    } catch (IOException e) {
                        if (e instanceof InterruptedIOException) {
                            log_view.info_add(ZLogView.status_hint, "???????????????");
                        }
                        log_view.update_editable_with_close(editable_pic,
                                ZLogView.status_error, "???" + curr_p + "?????????????????????:" + e.toString());
                        log_view.close_editable(editable_progress);
                    }
                    curr_p++;
                }
                //??????,???curr_p-1???
            }
            //??????
            else if (info <= 0) {
                if (fragment == null) {
                    static_set_pixiv_error_info(log_view, info, target_state, pid, p);
                } else {
                    fragment.set_pixiv_error_info(info, target_state);
                }
            }
            else {
                log_view.update_editable(target_state, getColor(context, R.color.gray), "pid=" + pid + ",p_sum=" + info);
                log_view.close_editable(target_state);

                Thread[] t = new Thread[info];

                //??????????????????
                for (int i = 0; i < info; i++) {
                    LinearLayout group = log_view.info_add_group(0);
                    log_view.info_add("???" + (i + 1) + "???,???" + info + "???", group, -1);
                    TextView editable_progress = log_view.info_add_editable("[0%]0/?", group, -1);
                    TextView editable_pic = log_view.info_add_editable("????????????????????????", group, -1);

                    int finalI = i;
                    t[i] = new Thread(() -> {
                        try {
                            Bitmap b = get_bitmap(get_pixiv_cat_path(pid, finalI + 1), log_view, "https://www.pixiv.net",
                                    group, group.indexOfChild(editable_progress), editable_progress,false);
                            if (b == null) {
                                throw new IOException();
                            }
                            else
                                log_view.update_editable_with_close(editable_pic, b, pid + "_" + finalI, null);
                        } catch (IOException e) {
                            if(e instanceof InterruptedIOException) {
                                log_view.info_add(ZLogView.status_hint, "???????????????");
                            }
                            log_view.update_editable_with_close(editable_pic,
                                    ZLogView.status_error, "???" + (finalI + 1) + "?????????????????????:" + e.toString());
                            log_view.close_editable(editable_progress);
                        }
                    });
                }
                for (int i = 0; i < info; i++) {
                    t[i].start();
                }
                for (int i = 0; i < info; i++) {
                    try {
                        t[i].join();
                    } catch (InterruptedException e) {
                        //??????????????????,??????????????????,?????????????????????????????????
                        for (int i0 = 0; i0 < info; i0++) {
                            if(t[i0].isAlive())
                                t[i0].interrupt();
                        }
                    }
                }
            }
        }
        else {
            Bitmap b=null;
            long time0 = System.currentTimeMillis();
            Bundle bundle = null;
            try {
                if(show_status_info)
                    log_view.info_add("????????????????????????");
                log_view.info_add(ZLogView.status_gray,"pid="+pid+",p="+p);

                String path=get_pixiv_cat_path(pid,p);

                bundle=get_bitmap_with_status(path,log_view,null);
                if(bundle==null){
                    throw new IOException();
                }
                byte[] b_array= bundle.getByteArray("byte_array");
                b = BitmapFactory.decodeByteArray(b_array,0, b_array.length);

                time0 = System.currentTimeMillis() - time0;
            } catch (IOException e) {
                if(e instanceof InterruptedIOException)
                    log_view.info_add(ZLogView.status_hint,"???????????????");
            }

            if (b == null) {
                log_view.info_add(ZLogView.status_error, "??????????????????");
                TextView editable_text=log_view.info_add_editable("????????????????????????");
                if(fragment==null){
                    static_set_pixiv_error_info(log_view,get_pixiv_error_info(pid,p,log_view),editable_text,pid,p);
                }
                else {
                    fragment.set_pixiv_error_info(get_pixiv_error_info(pid,p,log_view),editable_text);
                }
                log_view.close_task();
            } else {
                log_view.info_add(b, Integer.toString(pid), null);
                if (!bundle.getBoolean("success")){
                    log_view.info_add_clickable("??????????????????", new ClickableSpan() {
                        @Override
                        public void onClick(@NonNull View view) {
                            ((LinearLayout)view.getParent()).removeView(view);
                            int curr_p=p;
                            if(curr_p!=0)curr_p++;
                            GetPixivPicFragment.get_pixiv_pic_from_pid(log_view,pid,curr_p
                                    ,false,null,false);
                        }
                    });
                    log_view.close_task();
                    return;
                }
                if(show_status_info)
                    log_view.info_add(ZLogView.status_gray, "????????????:" + (time0 / 1000.0) + "s");
            }
        }
    }

    public static void static_set_pixiv_error_info(ZLogView log_view,int error_id,TextView view,int pid,int p){
        switch(error_id){
            case -1:
                log_view.update_editable_with_close(view,ZLogView.status_hint,"?????????????????????????????????????????????????????????");
                break;
            case -2:
                log_view.update_editable_with_close(view,ZLogView.status_hint,"???????????????????????????,??????p????????????0");
                log_view.info_add_clickable("???p=0?????????", new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View view) {
                        ((LinearLayout)view.getParent()).removeView(view);
                        get_pixiv_pic_from_pid(log_view,pid,0,false,null,false);
                    }
                });
                break;
            case -3:
                log_view.update_editable_with_close(view,ZLogView.status_hint,"???????????????????????????");
                break;
            case -4:
                log_view.update_editable_with_close(view,ZLogView.status_hint,"????????????????????????,???????????????????????????");
                log_view.info_add_clickable("??????????????????", new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View view) {
                        ((LinearLayout)view.getParent()).removeView(view);
                        get_pixiv_pic_from_pid(log_view,pid,p,true,null,false);
                    }
                });
                break;
            case -810:
                log_view.update_editable_with_close(view,ZLogView.status_hint,"????????????");
                break;
            case -114514:
                log_view.update_editable_with_close(view,ZLogView.status_hint,"????????????,?????????");
                log_view.info_add_clickable("??????", new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View view) {
                        ((LinearLayout)view.getParent()).removeView(view);
                        get_pixiv_pic_from_pid(log_view,pid,p,false,null,false);
                    }
                });
                break;
            default:
                log_view.update_editable_with_close(view,ZLogView.status_hint,"????????????"+error_id+"?????????,???????????????????????????");
                log_view.info_add_clickable("??????????????????", new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View view) {
                        ((LinearLayout)view.getParent()).removeView(view);
                        get_pixiv_pic_from_pid(log_view,pid,p,true,null,false);
                    }
                });
        }
    }
}

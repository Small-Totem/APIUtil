package com.zjh.apiutil;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.text.style.ClickableSpan;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.navigation.NavigationView;
import com.zjh.apiutil.databinding.ActivityMainBinding;
import com.zjh.apiutil.fragment.FragmentSwitcher;
import com.zjh.apiutil.fragment.GetPixivPicFragment;
import com.zjh.apiutil.fragment.LoliconAPIFragment;
import com.zjh.apiutil.fragment.PixivHotPicFragment;
import com.zjh.apiutil.fragment.SyxzAPIFragment;
import com.zjh.apiutil.fragment.TestAPIFragment;
import com.zjh.apiutil.tools.DataBaseBackup;
import com.zjh.apiutil.tools.DataBaseMaid;
import com.zjh.apiutil.tools.StaticTools;
import com.zjh.apiutil.view.AnimationForView;
import com.zjh.apiutil.view.GridRecyclerView;
import com.zjh.apiutil.view.PixivLikeRecyclerViewAdapter;
import com.zjh.apiutil.view.ZLogView;

import java.util.LinkedList;
import java.util.Objects;

import static com.zjh.apiutil.tools.StaticTools.get_max;
import static com.zjh.apiutil.tools.StaticTools.get_min;
import static com.zjh.apiutil.tools.StaticTools.stamp_to_time;
import static com.zjh.apiutil.view.AnimationForView.load_view;
import static com.zjh.apiutil.view.PixivLikeRecyclerViewAdapter.analyze_json_to_pixiv_like;
import static java.lang.System.exit;

public class MainActivity extends AppCompatActivity {
    private ActionBarDrawerToggle drawer_toggle;
    private DrawerLayout drawer;
    private ActivityMainBinding main_binding;
    private int a = 10;

    private Dialog settings_dialog;
    private SwitchCompat dialog_mainactivity_settings_SwitchCompat_pixiv_like_auto_load;
    private SwitchCompat dialog_mainactivity_settings_SwitchCompat_pixiv_like_show_r18;
    private SwitchCompat dialog_mainactivity_settings_SwitchCompat_display_sequence;

    private PixivLikeRecyclerViewAdapter adapter;
    private GridRecyclerView recycler_view;

    public DataBaseMaid database;
    public ZLogView log_view;
    public FragmentSwitcher fragment_switcher;

    public boolean screen_vertical = true;

    static {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = getLayoutInflater();
        main_binding = ActivityMainBinding.inflate(inflater);
        setContentView(main_binding.getRoot());

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            screen_vertical = false;

        // ?????????????????????????????????binding ???????????????include????????? main_content_binding.log_view_ScrollView
        log_view = new ZLogView(this, findViewById(R.id.log_view_ScrollView), main_binding.taskDoingProgressBar);

        database = new DataBaseMaid(getApplicationContext(), "apiutil.db", 1, log_view);
        init_NavigationView();

        fragment_switcher=new FragmentSwitcher(getSupportFragmentManager());


        // ??????tips
        {
            log_view.info_add_clickable("????????????tips", new ClickableSpan() {
                @Override
                public void onClick(@NonNull View view) {
                    ((LinearLayout) view.getParent()).removeView(view);
                    log_view.info_add("??????tips:");
                    LinearLayout l= log_view.info_add_group(26);
                    log_view.info_add("???????????????,????????????????????????????????????????????????",l,-1);
                    log_view.info_add("???????????????????????????????????????",l,-1);
                    log_view.info_add("??????api???????????????,?????????????????????api???pixiv",l,-1);

                    log_view.info_add_clickable("github:Small_Totem(https://github.com/Small-Totem/APIUtil)", ZLogView.R_color_light_blue, new ClickableSpan() {
                                @Override
                                public void onClick(@NonNull View view) {
                                    Uri uri=Uri.parse("https://github.com/Small-Totem/APIUtil");
                                    Intent intent=new Intent(Intent.ACTION_VIEW,uri);
                                    startActivity(Intent.createChooser(intent,"??????????????????"));
                                }
                            });
                    log_view.info_add("enjoy!");


                    // ??(>??<*) !!!???(???????????)??? (*/?????*) ???(????????????)???" ???(#`????)???
                    log_view.info_add_clickable("??(>??<*)", new ClickableSpan() {
                        @Override
                        public void onClick(@NonNull View view) {
                            log_view.info_add_clickable("!!!???(???????????)???", new ClickableSpan() {
                                @Override
                                public void onClick(@NonNull View view) {
                                    log_view.info_add_clickable("(*/?????*)", new ClickableSpan() {
                                        @Override
                                        public void onClick(@NonNull View view) {
                                            log_view.info_add_clickable("???(????????????)???", new ClickableSpan() {
                                                @Override
                                                public void onClick(@NonNull View view) {
                                                    log_view.info_add_clickable("???(#`????)??? ", new ClickableSpan() {
                                                        @Override
                                                        public void onClick(@NonNull View view) {
                                                            if (a < 0)
                                                                drawer.openDrawer(GravityCompat.START);
                                                            else {
                                                                log_view.info_add(Integer.toString(a));
                                                                log_view.scroll_down();
                                                                a--;
                                                            }
                                                        }
                                                    });
                                                }
                                            });
                                        }
                                    });
                                }
                            });
                        }
                    });
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (log_view.flag_if_working && !log_view.curr_thread.isInterrupted()) {// ???????????????????????????Interrupted
            log_view.cancel_task();
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawer_toggle.syncState();
    }

    // ????????????????????????????????????????????????????????????
    // ???PixivLike??????????????????????????????
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, 0, 0, "??????????????????");
        menu.add(0, 1, 0, "??????????????????");
        menu.add(0, 2, 0, "??????json");
        menu.add(0, 3, 0, "??????json");
        menu.add(0, 4, 0, "??????");

        // ?????????view?????????,????????????view???tag(curr_pid)
        menu.getItem(0).setActionView(v);
        menu.getItem(1).setActionView(v);
        menu.getItem(2).setActionView(v);
        menu.getItem(3).setActionView(v);
        menu.getItem(4).setActionView(v);

        menu.getItem(2).setEnabled(false);// ????????????????????????,???????????????
    }

    @SuppressLint("SetTextI18n")
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        final PixivLikeRecyclerViewAdapter.PixivLikeHolder holder = (PixivLikeRecyclerViewAdapter.PixivLikeHolder) item
                .getActionView().getTag();
        final int curr_pid = holder.curr_pid;
        switch (item.getItemId()) {
            case 0:
                // ??????fragment???GetPixivPicFragment?????????get_pixiv_pic_from_pid()
                log_view.clear();
                set_toolbar_title_with_transition(getString(R.string.get_pixiv_pic));

                FrameLayout f = findViewById(R.id.main_pixiv_like_content);
                AnimationForView.close_view(f, 300, 1, new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        f.setVisibility(View.GONE);
                        f.removeAllViews();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });

                fragment_switcher.show_fragment_by_tag(FragmentSwitcher.GetPixivPicFragment);

                GetPixivPicFragment fragment = (GetPixivPicFragment)fragment_switcher.find_fragment_by_tag(FragmentSwitcher.GetPixivPicFragment);
                fragment.get_pixiv_pic_binding.getPixivPicEditText1.setText(Integer.toString(curr_pid));
                fragment.get_pixiv_pic_binding.getPixivPicEditText2.setText("0");
                fragment.pid = curr_pid;
                fragment.p = 0;
                fragment.get_pixiv_pic_from_pid(false);
                break;
            case 1:
                holder.load(true);
                break;
            case 2:
                break;
            case 3:
                SQLiteDatabase db = database.getWritableDatabase();
                Cursor cursor = db.query("pixiv_like_table", new String[] { "pid", "json" }, null, null, null, null, null);
                int count = cursor.getCount();
                String[] json_str = { null };
                for (int i = 0; i < count; i++) {
                    cursor.moveToNext();
                    if (cursor.getInt(cursor.getColumnIndex("pid")) == curr_pid) {
                        json_str[0] = cursor.getString(cursor.getColumnIndex("json"));
                        break;
                    }
                }
                cursor.close();
                if (json_str[0] != null) {
                    final EditText edit_text = new EditText(this);
                    edit_text.setText(json_str[0]);
                    AlertDialog.Builder dialog1 = new AlertDialog.Builder(this);
                    dialog1.setTitle("??????json");
                    dialog1.setView(edit_text);
                    dialog1.setCancelable(true);

                    dialog1.setPositiveButton("??????", (dialogInterface, i1) -> {
                        json_str[0] = edit_text.getText().toString();
                        ContentValues cv = new ContentValues();
                        cv.put("json", json_str[0]);
                        int[] pid_and_p_int_array = StaticTools.get_pid_and_p_from_json(json_str[0]);
                        if (pid_and_p_int_array[0] != -1) {
                            cv.put("pid", pid_and_p_int_array[0]);
                            cv.put("p", pid_and_p_int_array[1]);
                            holder.curr_pid = pid_and_p_int_array[0];
                            holder.curr_p = pid_and_p_int_array[1];
                        }
                        database.getWritableDatabase().update("pixiv_like_table", cv, "pid=?",
                                new String[] { Integer.toString(curr_pid) });

                        adapter.json_LinkedList.remove(adapter.get_true_position(holder.getAdapterPosition()));
                        adapter.json_LinkedList.add(adapter.get_true_position(holder.getAdapterPosition()),json_str[0]);

                        holder.json.setText(analyze_json_to_pixiv_like(json_str[0]));
                        if (pid_and_p_int_array[0] != -1) {
                            String pid_and_p_str = "#" + adapter.get_true_position(holder.getAdapterPosition()) + " pid=" + pid_and_p_int_array[0]
                                    + " p=" + pid_and_p_int_array[1];
                            holder.pid_and_p.setText(pid_and_p_str);
                        }
                    });
                    dialog1.setNegativeButton("??????", null);
                    dialog1.show();
                }
                break;
            case 4:
                AlertDialog.Builder dialog1 = new AlertDialog.Builder(this);
                String title = "??????pid???" + curr_pid + "??????????";
                dialog1.setTitle(title);
                dialog1.setCancelable(true);
                dialog1.setPositiveButton("???????????????", (dialogInterface, i1) -> {
                    database.getWritableDatabase().delete("pixiv_like_table", "pid=?",
                            new String[] { Integer.toString(curr_pid) });
                    adapter.remove_item(holder.getAdapterPosition());
                });
                dialog1.setNegativeButton("???,????????????", null);
                dialog1.show();
                break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
            screen_vertical = false;
        else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT)
            screen_vertical = true;

        if (recycler_view != null && !screen_vertical
                && recycler_view.getLayoutManager() instanceof StaggeredGridLayoutManager)
            ((StaggeredGridLayoutManager) recycler_view.getLayoutManager()).setSpanCount(4);
        else if (recycler_view != null && screen_vertical
                && recycler_view.getLayoutManager() instanceof StaggeredGridLayoutManager)
            ((StaggeredGridLayoutManager) recycler_view.getLayoutManager()).setSpanCount(2);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        //???show/hide??????fragment??????????????????????????????????????????(?????????)
        //???????????????
        fragment_switcher.remove_all();
    }

    @SuppressLint("SetTextI18n")
    private void init_NavigationView() {
        NavigationView NavigationView = main_binding.navigationView;
        // NavigationView.setItemIconTintList(null);//??????navigationView?????????????????????????????????????????????tint????????????

        Toolbar toolbar = main_binding.mainToolbar;
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);// ??????????????????title,????????????textview
        drawer = main_binding.drawerLayout;


        //??????????????????recyclerview????????????,??????????????????????????????????????????
        //update fragment???????????????(??????)
        final String[] curr_fragment_flag=new String[1];
        final boolean[] should_load_after_NavigationItemSelected=new boolean[1];//??????false

        NavigationView.setNavigationItemSelectedListener(item -> {
            if (item.toString().equals("??????")) {
                settings_dialog.show();
                return false;
            }
            if (item.toString().equals("??????")) {
                exit_function();
                return false;
            }

            if (!get_toolbar_title().equals(item.toString())) {// ??????????????????????????????
                if (log_view.flag_if_working) {// ?????????????????????????????????
                    log_view.info_add(ZLogView.status_warning, "???????????????????????????");
                    drawer.closeDrawers();
                    return false;
                }

                recycler_view = null;
                adapter = null;
                log_view.clear();
                set_toolbar_title_with_transition(item.toString());
                curr_fragment_flag[0]=item.toString();
                should_load_after_NavigationItemSelected[0]=true;

                // ??????????????????????????????,??????????????????FrameLayout,??????fragment
                if (item.toString().equals(getString(R.string.pixiv_like))) {
                    log_view.start_task();

                    fragment_switcher.hide_curr();
                    } else {
                    FrameLayout f = findViewById(R.id.main_pixiv_like_content);
                    if (f.getVisibility() == View.VISIBLE) {
                        AnimationForView.close_view(f, 300, 1, new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                f.setVisibility(View.GONE);
                                runOnUiThread(f::removeAllViews);
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {
                            }
                        });
                    }
                }
                // ????????????????????????switch ????????????case ??????
                if (curr_fragment_flag[0].equals(getString(R.string.lolicon_api))) {
                    fragment_switcher.show_fragment_by_tag(FragmentSwitcher.LoliconAPIFragment, () -> {
                        main_binding.mainFragmentFrameLayout.setTranslationY(0);//??????,?????????????????????behavior????????????????????????
                });
                    LoliconAPIFragment.add_tips(log_view,this);
                } else if (curr_fragment_flag[0].equals(getString(R.string.get_pixiv_pic))) {
                    fragment_switcher.show_fragment_by_tag(FragmentSwitcher.GetPixivPicFragment, () -> main_binding.mainFragmentFrameLayout.setTranslationY(0));
                } else if (curr_fragment_flag[0].equals(getString(R.string.get_stream))) {
                    fragment_switcher.show_fragment_by_tag(FragmentSwitcher.DownloadFragment, () -> main_binding.mainFragmentFrameLayout.setTranslationY(0));
                } else if (curr_fragment_flag[0].equals(getString(R.string.test_api))) {
                    fragment_switcher.show_fragment_by_tag(FragmentSwitcher.TestAPIFragment, () -> main_binding.mainFragmentFrameLayout.setTranslationY(0));
                    TestAPIFragment.add_tips(log_view,this);
                } else if (curr_fragment_flag[0].equals(getString(R.string.tools))) {
                    fragment_switcher.hide_curr();
                    log_view_add_tools();
                } else if (curr_fragment_flag[0].equals(getString(R.string.pixiv_hot_pic))) {
                    fragment_switcher.show_fragment_by_tag(FragmentSwitcher.PixivHotPicFragment, () -> main_binding.mainFragmentFrameLayout.setTranslationY(0));
                    PixivHotPicFragment.add_tips(log_view,this);
                } else if (curr_fragment_flag[0].equals(getString(R.string.syxz_api))) {
                    fragment_switcher.show_fragment_by_tag(FragmentSwitcher.SyxzAPIFragment, () -> main_binding.mainFragmentFrameLayout.setTranslationY(0));
                    SyxzAPIFragment.add_tips(log_view,this);
                }
            }
            drawer.closeDrawers();
            return false;// ?????????????????????????????????????????????true
        });// ????????????????????????

        //??????????????????????????????????????????????????????????????????
        final AnimationForView.ObjAnim obj_anim=new AnimationForView.ObjAnim(main_binding.mainFragmentFrameLayout,200,200,View.GONE);
        drawer_toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
                /*
                if(main_binding.mainFragmentFrameLayout.getVisibility()==View.VISIBLE)
                    close_view(main_binding.mainFragmentFrameLayout,200,1,View.INVISIBLE);
                */
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                invalidateOptionsMenu();

                /*
                if(main_binding.mainFragmentFrameLayout.getVisibility()!=View.VISIBLE)
                    load_view(main_binding.mainFragmentFrameLayout,200,1);
*/
                if(!should_load_after_NavigationItemSelected[0])
                    return;

                if(curr_fragment_flag[0].equals(getString(R.string.pixiv_like))){
                    load_pixiv_like_RecyclerView();
                }

                should_load_after_NavigationItemSelected[0]=false;
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                super.onDrawerStateChanged(newState);

                if(newState==DrawerLayout.STATE_SETTLING||newState==DrawerLayout.STATE_DRAGGING){
                    if(main_binding.mainFragmentFrameLayout.getVisibility()==View.VISIBLE)
                        obj_anim.close();
                }
                else if(newState==DrawerLayout.STATE_IDLE){
                    if(!drawer.isDrawerOpen(GravityCompat.START)&&main_binding.mainFragmentFrameLayout.getVisibility()!=View.VISIBLE)
                        obj_anim.load();
                }
            }

        };// ???????????????????????????????????????
        drawer.addDrawerListener(drawer_toggle);


        final View dialog_mainactivity_settings = LayoutInflater.from(this)
                .inflate(R.layout.dialog_mainactivity_settings, main_binding.getRoot(), false);

        SwitchCompat dialog_mainactivity_settings_SwitchCompat_hint = dialog_mainactivity_settings.findViewById(R.id.dialog_mainactivity_settings_SwitchCompat_hint);
        SwitchCompat dialog_mainactivity_settings_SwitchCompat_warning = dialog_mainactivity_settings.findViewById(R.id.dialog_mainactivity_settings_SwitchCompat_warning);
        SwitchCompat dialog_mainactivity_settings_SwitchCompat_error = dialog_mainactivity_settings.findViewById(R.id.dialog_mainactivity_settings_SwitchCompat_error);
        SwitchCompat dialog_mainactivity_settings_SwitchCompat_gray = dialog_mainactivity_settings.findViewById(R.id.dialog_mainactivity_settings_SwitchCompat_gray);
        SwitchCompat dialog_mainactivity_settings_SwitchCompat_status_bar = dialog_mainactivity_settings.findViewById(R.id.dialog_mainactivity_settings_SwitchCompat_status_bar);
        dialog_mainactivity_settings_SwitchCompat_pixiv_like_auto_load = dialog_mainactivity_settings.findViewById(R.id.dialog_mainactivity_settings_SwitchCompat_pixiv_like_auto_load);
        dialog_mainactivity_settings_SwitchCompat_pixiv_like_show_r18 = dialog_mainactivity_settings.findViewById(R.id.dialog_mainactivity_settings_SwitchCompat_pixiv_like_show_r18);
        dialog_mainactivity_settings_SwitchCompat_display_sequence = dialog_mainactivity_settings.findViewById(R.id.dialog_mainactivity_settings_SwitchCompat_display_sequence);
        TextView dialog_mainactivity_settings_TextView_display_sequence= dialog_mainactivity_settings.findViewById(R.id.dialog_mainactivity_settings_TextView_display_sequence);




        //????????????????????????
        dialog_mainactivity_settings_SwitchCompat_hint.setChecked(log_view.hint_enabled);
        dialog_mainactivity_settings_SwitchCompat_warning.setChecked(log_view.warning_enabled);
        dialog_mainactivity_settings_SwitchCompat_error.setChecked(log_view.error_enabled);
        dialog_mainactivity_settings_SwitchCompat_gray.setChecked(log_view.gray_enabled);
        dialog_mainactivity_settings_SwitchCompat_status_bar.setChecked(
                getSharedPreferences("settings_Preferences", MODE_PRIVATE).getBoolean("status_bar", false));
        dialog_mainactivity_settings_SwitchCompat_pixiv_like_auto_load.setChecked(
                getSharedPreferences("settings_Preferences", MODE_PRIVATE).getBoolean("pixiv_like_auto_load", true));
        dialog_mainactivity_settings_SwitchCompat_pixiv_like_show_r18.setChecked(
                getSharedPreferences("settings_Preferences", MODE_PRIVATE).getBoolean("pixiv_like_show_r18", false));
        dialog_mainactivity_settings_SwitchCompat_display_sequence.setChecked(
                getSharedPreferences("settings_Preferences", MODE_PRIVATE).getBoolean("pixiv_like_display_sequence", true));
        show_hide_status_bar(dialog_mainactivity_settings_SwitchCompat_status_bar.isChecked());
        if(dialog_mainactivity_settings_SwitchCompat_display_sequence.isChecked())
            dialog_mainactivity_settings_TextView_display_sequence.setText("????????????");
        else
            dialog_mainactivity_settings_TextView_display_sequence.setText("????????????");


        //OnCheckedChangeListener
        dialog_mainactivity_settings_SwitchCompat_pixiv_like_show_r18.setOnCheckedChangeListener((compoundButton, checked)-> {
            if (adapter != null){
                adapter.show_r18=checked;
                drawer.closeDrawers();
                //???????????????????????????????????????,??????
                //adapter.notifyItemRangeChanged(0,adapter.getItemCount());
                //?????????
                for(int position=0;position<adapter.getItemCount();position++){
                    PixivLikeRecyclerViewAdapter.PixivLikeHolder holder = (PixivLikeRecyclerViewAdapter.PixivLikeHolder)
                            recycler_view.findViewHolderForAdapterPosition(position);
                    if(holder!=null&&holder.r18){
                        adapter.notifyItemChanged(position);
                    }
                }
            }
            getSharedPreferences("settings_Preferences", MODE_PRIVATE).edit()
                    .putBoolean("pixiv_like_show_r18", checked).apply();
        });
        dialog_mainactivity_settings_SwitchCompat_display_sequence.setOnCheckedChangeListener((compoundButton, checked)-> {
            if(checked)
                dialog_mainactivity_settings_TextView_display_sequence.setText("????????????");
            else
                dialog_mainactivity_settings_TextView_display_sequence.setText("????????????");
            if (adapter != null){
                drawer.closeDrawers();
                load_pixiv_like_RecyclerView();
            }

            getSharedPreferences("settings_Preferences", MODE_PRIVATE).edit()
                    .putBoolean("pixiv_like_display_sequence", checked).apply();
        });
        dialog_mainactivity_settings_SwitchCompat_hint.setOnCheckedChangeListener((compoundButton, checked)-> log_view.hint_enabled = checked);
        dialog_mainactivity_settings_SwitchCompat_warning.setOnCheckedChangeListener((compoundButton, checked)-> log_view.warning_enabled = checked);
        dialog_mainactivity_settings_SwitchCompat_error.setOnCheckedChangeListener((compoundButton, checked)-> log_view.error_enabled = checked);
        dialog_mainactivity_settings_SwitchCompat_gray.setOnCheckedChangeListener((compoundButton, checked)->log_view.gray_enabled = checked);
        dialog_mainactivity_settings_SwitchCompat_status_bar.setOnCheckedChangeListener((compoundButton, checked)->{
            show_hide_status_bar(checked);
            getSharedPreferences("settings_Preferences", MODE_PRIVATE).edit()
                    .putBoolean("status_bar",checked).apply();
        });
        dialog_mainactivity_settings_SwitchCompat_pixiv_like_auto_load.setOnCheckedChangeListener((compoundButton, checked)->{
            if (adapter != null){
                drawer.closeDrawers();
                adapter.auto_load = checked;
            }
            getSharedPreferences("settings_Preferences", MODE_PRIVATE).edit()
                    .putBoolean("pixiv_like_auto_load", checked).apply();
        });


        settings_dialog = new AlertDialog.Builder(this).setTitle("??????").setView(dialog_mainactivity_settings)
                .setPositiveButton("?????????",null).create();

    }

    /*
    //??????fragment????????????(replace),?????????show/hide
    private void replace_fragment(Fragment fragment) {
        replace_fragment(R.id.main_fragment, fragment, null);
    }
    private void replace_fragment(int layout, Fragment fragment, Runnable run_on_commit) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragment == null) {
            remove_fragment(layout, fragmentManager);
            return;
        }

        FragmentTransaction transaction = fragmentManager.beginTransaction(); // ??????????????????
        transaction.setCustomAnimations(R.anim.fragment_slide_in_bottom, R.anim.fragment_remove)
                .replace(layout, fragment);

        if (run_on_commit != null)
            transaction.runOnCommit(run_on_commit);
        transaction.commit();
    }
    private void remove_fragment(int layout, FragmentManager fragmentManager) {
        Fragment f = fragmentManager.findFragmentById(layout);
        if (f != null) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.remove(f);
            transaction.commit();
        }
    }*/

    private void request_write_permission() {
        log_view.info_add(ZLogView.status_gray, "Build.VERSION.SDK_INT=" + Build.VERSION.SDK_INT);
        // android 11???????????????????????????????????????
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // ????????????????????????
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + this.getPackageName()));
                startActivityForResult(intent, 1024);
            } else
                log_view.info_add(ZLogView.status_gray, "Environment.isExternalStorageManager()==true");
        }
        // ????????????????????????
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE }, 1024);
        else
            log_view.info_add(ZLogView.status_gray,
                    "ActivityCompat.checkSelfPermission()==PackageManager.PERMISSION_GRANTED");
    }

    private void set_toolbar_title_with_transition(String new_text) {
        if (main_binding.toolbarMainTitleA.getText().equals("")) {
            main_binding.toolbarMainTitleA.setText(new_text);
            AnimationForView.load_view(main_binding.toolbarMainTitleA, 500, 1);
            AnimationForView.close_text(main_binding.toolbarMainTitleB, 500, 1, View.INVISIBLE);
        } else if (main_binding.toolbarMainTitleB.getText().equals("")) {
            main_binding.toolbarMainTitleB.setText(new_text);
            AnimationForView.load_view(main_binding.toolbarMainTitleB, 500, 1);
            AnimationForView.close_text(main_binding.toolbarMainTitleA, 500, 1, View.INVISIBLE);
        }
    }

    private void get_pixiv_like_RecyclerView(boolean staggered) {
        recycler_view = new GridRecyclerView(this);
        recycler_view.setLayoutParams(
                new ListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (staggered) {
            StaggeredGridLayoutManager staggeredGridLayoutManager = new StaggeredGridLayoutManager(
                    screen_vertical ? 2 : 4, StaggeredGridLayoutManager.VERTICAL);
            recycler_view.setLayoutManager(staggeredGridLayoutManager);
        } else
            recycler_view.setLayoutManager(new LinearLayoutManager(this));

        SQLiteDatabase db = database.getReadableDatabase();

        LinkedList<Integer> pid = null;
        LinkedList<Integer> p = null;
        LinkedList<String> add_date = null;
        LinkedList<String> json = null;
        try {
            Cursor cursor = db.query("pixiv_like_table", new String[] { "pid", "p", "add_date", "json" }, null, null,
                    null, null, null);
            int count = cursor.getCount();
            pid = new LinkedList<>();
            p = new LinkedList<>();
            add_date = new LinkedList<>();
            json = new LinkedList<>();
            for (int i = 0; i < count; i++) {
                cursor.moveToNext();
                pid.add(cursor.getInt(cursor.getColumnIndex("pid")));
                p.add(cursor.getInt(cursor.getColumnIndex("p")));
                add_date.add(stamp_to_time(cursor.getString(cursor.getColumnIndex("add_date"))));
                json.add(cursor.getString(cursor.getColumnIndex("json")));
            }
            cursor.close();
        } catch (Exception e) {
            log_view.info_add(ZLogView.status_error, e.toString());
        }

        adapter = new PixivLikeRecyclerViewAdapter(this, pid, p, add_date, json,staggered,dialog_mainactivity_settings_SwitchCompat_display_sequence.isChecked());
        adapter.auto_load=dialog_mainactivity_settings_SwitchCompat_pixiv_like_auto_load.isChecked();
        adapter.show_r18=dialog_mainactivity_settings_SwitchCompat_pixiv_like_show_r18.isChecked();

        recycler_view.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if(recycler_view==null){
                    return;
                }

                // ??????????????????,????????????
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING || newState == RecyclerView.SCROLL_STATE_SETTLING) {
                    adapter.unmoved = false;
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (staggered && adapter.auto_load) {
                        int first;
                        int last;
                        first = get_min(
                                ((StaggeredGridLayoutManager) Objects.requireNonNull(recycler_view.getLayoutManager()))
                                        .findFirstCompletelyVisibleItemPositions(null));
                        last = get_max(
                                ((StaggeredGridLayoutManager) Objects.requireNonNull(recycler_view.getLayoutManager()))
                                        .findLastCompletelyVisibleItemPositions(null));
                        for (int i = first; i <= last; i++) {
                            PixivLikeRecyclerViewAdapter.PixivLikeHolder holder = (PixivLikeRecyclerViewAdapter.PixivLikeHolder) recycler_view
                                    .findViewHolderForAdapterPosition(i);
                            if (holder != null&&(!holder.r18||adapter.show_r18))
                                holder.load(false);
                        }
                    }
                }
            }

            int[] a;

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // ?????????,??????????????????????????????
                // ?????????,dy??????0
                // ?????????????????????holder?????????????????????(scroll_state_down??????????????????????????????)
                // System.out.println("dx="+dx+" dy="+dy);
                /*
                if (dy > 0 && !adapter.scroll_state_down)
                    adapter.scroll_state_down = true;
                else if (dy < 0 && adapter.scroll_state_down)
                    adapter.scroll_state_down = false;
                */

                //update:?????????????????? ???????????????????????????????????????
                if(recycler_view==null){
                    return;
                }
                if(staggered){
                    a=((StaggeredGridLayoutManager) Objects.requireNonNull(recycler_view.getLayoutManager()))
                            .findFirstVisibleItemPositions(null);
                    if(a!=null&&a.length>=1){
                        adapter.a_visible_view_position =get_max(a);
                    }
                }
                else {//???????????????staggered?????????,????????????
                    adapter.a_visible_view_position =
                            ((LinearLayoutManager)Objects.requireNonNull(recycler_view.getLayoutManager()))
                            .findFirstVisibleItemPosition();
                }
            }
        });

        recycler_view.setAdapter(adapter);
    }
    private void load_pixiv_like_RecyclerView(){
        FrameLayout f = findViewById(R.id.main_pixiv_like_content);
        f.removeAllViews();
        AnimationForView.load_view(f, 300, 1);

        new Thread(()->{
            get_pixiv_like_RecyclerView(true);//??????????????????,??????????????????

            // ???????????????????????????
            LayoutAnimationController animationController = AnimationUtils
                    .loadLayoutAnimation(getApplicationContext(), R.anim.grid_layout_animation);
            animationController.getAnimation().setDuration(600);
            recycler_view.setLayoutAnimation(animationController);
            //recycler_view.scheduleLayoutAnimation();
            runOnUiThread(()->{
                f.addView(recycler_view);
                log_view.close_task();
            });
        }).start();
    }

    private void log_view_add_tools() {
        // ????????????
        log_view.info_add_clickable("????????????", new ClickableSpan() {
            @Override
            public void onClick(@NonNull View view) {
                ((LinearLayout) view.getParent()).removeView(view);
                log_view.info_add_clickable("test@data_base_maid@apiutil.db", new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View view) {
                        log_view.clear();
                        SQLiteDatabase db = database.getWritableDatabase();
                        // print
                        log_view.info_add_clickable("???????????????", new ClickableSpan() {
                            @Override
                            public void onClick(@NonNull View view) {
                                try {
                                    Cursor cursor = db.query("pixiv_like_table",
                                            new String[] { "pid", "p", "add_date", "json" }, null, null, null, null,
                                            null);
                                    int count = cursor.getCount();
                                    log_view.info_add(Color.YELLOW, "--lines_count=" + count);
                                    for (int i = 0; i < count; i++) {
                                        cursor.moveToNext();
                                        log_view.info_add("line=" + i + ",pid="
                                                + cursor.getInt(cursor.getColumnIndex("pid")) + ",p="
                                                + cursor.getInt(cursor.getColumnIndex("p")) + ",add_date="
                                                + cursor.getString(cursor.getColumnIndex("add_date")) + ",json="
                                                + cursor.getString(cursor.getColumnIndex("json")));
                                    }
                                    cursor.close();
                                } catch (Exception e) {
                                    log_view.info_add(ZLogView.status_error, e.toString());
                                }
                                log_view.scroll_down();
                            }
                        });
                        log_view.info_add_clickable("????????????", new ClickableSpan() {
                            @Override
                            public void onClick(@NonNull View view) {
                                log_view.info_add_clickable("????????????", Color.RED, new ClickableSpan() {
                                    @Override
                                    public void onClick(@NonNull View view) {
                                        db.delete("pixiv_like_table", null, null);
                                        log_view.info_add("deleted");
                                        ((LinearLayout) view.getParent()).removeView(view);
                                    }
                                });
                            }
                        });

                        log_view.info_add_clickable("??????IO??????", new ClickableSpan() {
                            @Override
                            public void onClick(@NonNull View view) {
                                request_write_permission();
                            }
                        });
                        log_view.info_add_clickable("???????????????", new ClickableSpan() {
                            @Override
                            public void onClick(@NonNull View view) {
                                log_view.info_add_clickable("????????????(????????????????????????)", Color.RED, new ClickableSpan() {
                                    @Override
                                    public void onClick(@NonNull View view) {
                                        DataBaseBackup dbb = new DataBaseBackup(getApplicationContext(), log_view);
                                        dbb.exec(DataBaseBackup.COMMAND_BACKUP);
                                        ((LinearLayout) view.getParent()).removeView(view);
                                    }
                                });
                            }
                        });
                        log_view.info_add_clickable("???????????????", new ClickableSpan() {
                            @Override
                            public void onClick(@NonNull View view) {
                                log_view.info_add_clickable("????????????", Color.RED, new ClickableSpan() {
                                    @Override
                                    public void onClick(@NonNull View view) {
                                        DataBaseBackup dbb = new DataBaseBackup(getApplicationContext(), log_view);
                                        dbb.exec(DataBaseBackup.COMMAND_RESTORE);
                                        ((LinearLayout) view.getParent()).removeView(view);
                                    }
                                });
                            }
                        });
                    }
                });
                log_view.info_add_clickable("test@RecyclerView@unstaggered", new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View view) {
                        fragment_switcher.hide_curr();
                        FrameLayout f = findViewById(R.id.main_pixiv_like_content);
                        AnimationForView.load_view(f, 300, 1);
                        get_pixiv_like_RecyclerView(false);
                        f.addView(recycler_view);
                        load_view(recycler_view,400,1);
                        log_view.clear();
                    }
                });

                log_view.info_add_clickable("test@RecyclerView@staggered_animation_slow_down", new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View view) {
                        fragment_switcher.hide_curr();
                        FrameLayout f = findViewById(R.id.main_pixiv_like_content);
                        AnimationForView.load_view(f, 300, 1);
                        get_pixiv_like_RecyclerView(true);
                        f.addView(recycler_view);

                        log_view.clear();

                        // ???????????????????????????
                        LayoutAnimationController animationController = AnimationUtils
                                .loadLayoutAnimation(getApplicationContext(), R.anim.grid_layout_animation);
                        animationController.getAnimation().setDuration(4000);
                        recycler_view.setLayoutAnimation(animationController);
                        recycler_view.scheduleLayoutAnimation();

                        // ?????????????????????recycler_view(???????????????)
                        // ???????????????????????????????????????
                        FrameLayout f_new = new FrameLayout(getApplicationContext());
                        f_new.setClickable(true);
                        f_new.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT));
                        f.addView(f_new);
                    }
                });

            }
        });
    }

    private void show_hide_status_bar(boolean show) {
        if (show) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);// ???????????????
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);// ???????????????
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
    }

    public String get_toolbar_title() {
        if (main_binding.toolbarMainTitleA.getText().equals(""))
            return main_binding.toolbarMainTitleB.getText().toString();
        else
            return main_binding.toolbarMainTitleA.getText().toString();
    }

    public void exit_function() {
        finish();
        Handler handler = new Handler();
        handler.postDelayed(() -> exit(0), 500);// ??????0.5s?????????????????????
    }
}
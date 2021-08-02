package com.zjh.apiutil.tools;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.zjh.apiutil.view.ZLogView;

public class DataBaseMaid extends SQLiteOpenHelper {
    public ZLogView log_view;//可以是null
    public String db_name;

    public DataBaseMaid(Context context,String name,int version,ZLogView view) {
        super(context, name, null, version);
        db_name=name;
        log_view=view;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "create table pixiv_like_table(pid INTEGER, p INTEGER, add_date VARCHAR(20),json NTEXT);";
        db.execSQL(sql);
        if(log_view!=null)
            log_view.info_add(ZLogView.status_gray,db_name+" created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}

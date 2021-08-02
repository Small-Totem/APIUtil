//轮子,数据库备份
//https://blog.csdn.net/qq_43184922/article/details/105884824
package com.zjh.apiutil.tools;
import android.content.Context;
import android.os.Environment;

import com.zjh.apiutil.view.ZLogView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class DataBaseBackup {
    public final static String COMMAND_BACKUP = "backupDatabase";
    public final static String COMMAND_RESTORE = "restoreDatabase";
    public final static String EXTERNAL_STORAGE_FOLDER = "backups";
    public final static String EXTERNAL_STORAGE_BACKUP_DIR = "Apiutil_database_backup";
    public final static String DB_NAME = "apiutil.db";
    private final Context context;
    public ZLogView log_view;

    public DataBaseBackup(Context _context,ZLogView _log_view) {
        context = _context;
        log_view = _log_view;
    }

    private File getExternalStoragePublicDir() {
        String path = Environment.getExternalStorageDirectory() + File.separator + EXTERNAL_STORAGE_FOLDER + File.separator;
        File dir = new File(path);
        if (!dir.exists())
            if(!dir.mkdirs()) {
                log_view.info_add(ZLogView.status_error, "文件夹创建失败");
            }
        return dir;
    }

    public void exec(String command) {
        File dbFile = context.getDatabasePath(DB_NAME);// 默认路径是 /data/data/(包名)/databases/*
        File exportDir = new File(getExternalStoragePublicDir(), EXTERNAL_STORAGE_BACKUP_DIR);//    /sdcard/Never Forget/Backup
        if (!exportDir.exists()){
            if(!exportDir.mkdirs()){
                log_view.info_add(ZLogView.status_error,"文件夹创建失败");
                return;
            }
        }
        File backup = new File(exportDir, DB_NAME);//备份文件与原数据库文件名一致
        if (command.equals(COMMAND_BACKUP)) {
            try {
                if(!backup.exists())
                    if(!backup.createNewFile())
                        throw new IOException("文件创建失败");
                fileCopy(dbFile, backup);//数据库文件拷贝至备份文件

                log_view.info_add("备份成功");
                log_view.info_add(ZLogView.status_gray,exportDir+"/"+DB_NAME);
            } catch (Exception e) {
                e.printStackTrace();
                log_view.info_add(ZLogView.status_error,e.toString());
            }
        } else if (command.equals(COMMAND_RESTORE)) {
            try {
                log_view.info_add("源数据上次修改时间:"+StaticTools.stamp_to_time(dbFile.lastModified()));
                fileCopy(backup, dbFile);//备份文件拷贝至数据库文件
                log_view.info_add("恢复成功");
            } catch (Exception e) {
                e.printStackTrace();
                log_view.info_add(ZLogView.status_error,e.toString());
            }
        }
    }

    private void fileCopy(File dbFile, File backup) throws IOException{
        try (FileChannel inChannel = new FileInputStream(dbFile).getChannel(); FileChannel outChannel = new FileOutputStream(backup).getChannel()) {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        }
    }
}

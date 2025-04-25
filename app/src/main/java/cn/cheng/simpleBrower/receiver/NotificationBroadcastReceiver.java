package cn.cheng.simpleBrower.receiver;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.io.File;

import cn.cheng.simpleBrower.MyApplication;
import cn.cheng.simpleBrower.util.CommonUtils;

// 用于接受下载完成提示的广播接收者
public class NotificationBroadcastReceiver extends BroadcastReceiver {

    public static final String TYPE = "type"; //这个type是为了Notification更新信息的，这个不明白的朋友可以去搜搜，很多

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int type = intent.getIntExtra(TYPE, -1);

        if (type != -1) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(type);
        }

        if (action.equals("notification_clicked")) {
            //处理点击事件
        }

        if (action.equals("notification_cancelled")) {
            // 记录的删除项
            MyApplication.setNum(type);
            String fileName = intent.getStringExtra("fileName");
            if (fileName != null) {
                if (!fileName.contains(".")) {
                    fileName += ".m3u8";
                }
                String finalFileName = fileName;
                new Thread(() -> {
                    CommonUtils.deleteFile(new File(finalFileName));
                }).start();
            }
        }
    }
}

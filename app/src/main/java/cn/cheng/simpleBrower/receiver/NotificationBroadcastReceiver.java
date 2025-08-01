package cn.cheng.simpleBrower.receiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import java.io.File;
import java.util.concurrent.ExecutorService;

import cn.cheng.simpleBrower.MyApplication;
import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.bean.NotificationBean;
import cn.cheng.simpleBrower.service.DownloadService;
import cn.cheng.simpleBrower.util.CommonUtils;

// 用于接受下载完成提示的广播接收者
public class NotificationBroadcastReceiver extends BroadcastReceiver {

    public static final String TYPE = "type"; //这个type是为了Notification更新信息的，这个不明白的朋友可以去搜搜，很多

    @Override
    public void onReceive(Context context, Intent intent) {
        int notificationId = intent.getIntExtra(TYPE, -1);
        String action = intent.getAction();
        if (notificationId == -1) {
            return;
        }
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationBean downLoadInfo = MyApplication.getDownLoadInfo(notificationId);

        // 处理按钮点击事件
        if ("notification_clicked".equals(action)) {
            if (downLoadInfo != null) {
                Notification notificationX = downLoadInfo.getNotification();
                RemoteViews contentView = notificationX.contentView;
                String state = downLoadInfo.getState();
                if (state != null) {
                    if (state.equals("暂停")) {
                        state = "继续";
                    } else {
                        state = "暂停";
                    }
                    contentView.setTextViewText(R.id.btn_state, state);
                    downLoadInfo.setState(state);
                    notificationManager.notify(notificationId, notificationX);
                    if (state.equals("暂停")) {
                        // 继续下载
                        Intent serviceIntent = new Intent(context, DownloadService.class);
                        intent.putExtra("notificationId", notificationId);
                        context.startService(serviceIntent);
                    }
                }
            }
        }

        // 处理删除事件
        if ("notification_cancelled".equals(action)) {
            if (downLoadInfo != null) {
                downLoadInfo.setState("继续");
            }
            ExecutorService pool = MyApplication.getPool(notificationId);
            if (pool != null) pool.shutdownNow();
            notificationManager.cancel(notificationId);
            // 记录的删除项
            MyApplication.setNum(notificationId);
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

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

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int notificationId = intent.getIntExtra("notificationId", -1);
        if (notificationId == -1) return;
        NotificationBean downLoadInfo = MyApplication.getDownLoadInfo(notificationId);
        if (downLoadInfo == null) return;

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // 处理按钮点击事件
        if ("notification_clicked".equals(action)) {
            //Notification notificationX = downLoadInfo.getNotification();
            String channelId = intent.getStringExtra("channelId");
            Notification notificationX = CommonUtils.getRunNotification(notificationManager, channelId);
            if (notificationX != null) {
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
                        downLoadInfo.getM3u8Download().start();
                    }
                }
            }
        }

        // 处理删除事件
        if ("notification_cancelled".equals(action)) {
            downLoadInfo.setState("继续");
            ExecutorService pool = downLoadInfo.getFixedThreadPool();
            if (pool != null) pool.shutdownNow();
            notificationManager.cancel(notificationId);
            MyApplication.deleteDownloadList(downLoadInfo.getUrl());
            MyApplication.deleteDownLoadInfo(notificationId);
        }
    }
}

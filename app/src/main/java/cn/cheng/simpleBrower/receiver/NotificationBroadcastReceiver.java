package cn.cheng.simpleBrower.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import cn.cheng.simpleBrower.util.NotificationUtils;

// 用于接受下载完成提示的广播接收者
public class NotificationBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int notificationId = intent.getIntExtra("notificationId", -1);
        if (notificationId == -1) return;

        // 处理删除事件
        if ("notification_cancelled".equals(action)) {
            NotificationUtils.deleteDownloadNotification(context, notificationId, true);
        }
    }
}

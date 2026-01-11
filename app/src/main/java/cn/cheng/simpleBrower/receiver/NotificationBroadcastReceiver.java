package cn.cheng.simpleBrower.receiver;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import cn.cheng.simpleBrower.MyApplication;
import cn.cheng.simpleBrower.bean.NotificationBean;
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

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

        // 处理删除事件
        if ("notification_cancelled".equals(action)) {
            try {
                downLoadInfo.setState("继续");
                MyApplication.deleteDownLoadInfo(notificationId);
                notificationManager.cancel(notificationId);
                CommonUtils.writeObjectIntoLocal("downloadList", downLoadInfo.getDate() + CommonUtils.zeroPadding(downLoadInfo.getNotificationId()), downLoadInfo);
            } catch (Throwable e) {
                CommonUtils.saveLog("=======处理删除事件notification_cancelled=======" + e.getMessage());
            }
        }
    }
}

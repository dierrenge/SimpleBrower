package cn.cheng.biShu.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import cn.cheng.biShu.MyApplication;
import cn.cheng.biShu.activity.TxtActivity;
import cn.cheng.biShu.activity.TxtCatalogActivity;
import cn.cheng.biShu.util.NotificationUtils;

// 用于接受下载完成提示的广播接收者
public class NotificationBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        // 处理删除事件
        if ("notification_cancelled".equals(action)) {
            int notificationId = intent.getIntExtra("notificationId", -1);
            if (notificationId == -1) return;
            NotificationUtils.deleteDownloadNotification(context, notificationId, true);
        }
        // 处理点击事件
        if ("notification_click".equals(action) && MyApplication.currentActivity != null) {
            if (MyApplication.currentActivity instanceof TxtCatalogActivity) {
                // 关闭目录界面再跳转TxtActivity
                Intent closeCIntent = new Intent("CLOSE_TC_ACTIVITY");
                context.sendBroadcast(closeCIntent);
            } else {
                // 跳转TxtActivity
                Intent i = new Intent(MyApplication.currentActivity, TxtActivity.class);
                i.putExtra("txtUrl", MyApplication.getTxtUrl());
                MyApplication.currentActivity.startActivity(i);
            }

        }
    }
}

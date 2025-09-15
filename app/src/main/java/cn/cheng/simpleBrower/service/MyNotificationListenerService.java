package cn.cheng.simpleBrower.service;

import android.app.Notification;
import android.content.Intent;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import cn.cheng.simpleBrower.activity.TxtActivity;
import cn.cheng.simpleBrower.custom.MyToast;

/**
 * 通知监听服务 (测试发现没什么用。。)
 * 通过通知内容监听QQ或者微信正在语音或者视频通话
 */
public class MyNotificationListenerService extends NotificationListenerService {
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        if (packageName.equals("com.tencent.mobileqq") || packageName.equals("com.tencent.mm")) {
            Notification notification = sbn.getNotification();
            if ((notification.flags & Notification.FLAG_ONGOING_EVENT) != 0) {
                // 判断通知内容是否包含通话关键词（如“通话中”）
                String text = notification.extras.getString(Notification.EXTRA_TEXT);
                if (text != null && (text.contains("通话中") || text.contains("视频通话"))) {
                    // 停止TTS服务
                    if (TxtActivity.txtActivity != null) {
                        Intent intentS = new Intent(TxtActivity.txtActivity, ReadService.class);
                        TxtActivity.txtActivity.stopService(intentS);
                        TxtActivity.flagRead = false;
                        MyToast.getInstance("通话中").show();
                    }
                }
            }
        }
    }
}

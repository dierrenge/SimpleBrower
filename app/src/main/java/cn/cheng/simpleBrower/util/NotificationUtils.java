package cn.cheng.simpleBrower.util;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Date;

import cn.cheng.simpleBrower.MyApplication;
import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.activity.DownloadActivity;
import cn.cheng.simpleBrower.bean.NotificationBean;
import cn.cheng.simpleBrower.receiver.NotificationBroadcastReceiver;

/**
 * 消息通知管理工具
 */
public class NotificationUtils {

    // 自定义前台消息
    public static final String groupKey = "group";
    public static final String channelKey = "common_channel";

    // 创建通知渠道 (Android 8.0+)
    public static void initNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm.getNotificationChannel(channelKey) == null) {
                NotificationChannel c = new NotificationChannel(channelKey, "悬浮窗服务",
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                nm.createNotificationChannel(c);
            }
        }
    }

    // 更新消息通知视图
    public static void updateRemoteViews(int id, String progress, String btnText, NotificationManager nm) {
        try {
            Context context = MyApplication.getContext();
            NotificationBean downLoadInfo = MyApplication.getDownLoadInfo(id);
            if (context == null || downLoadInfo == null || (progress == null && btnText == null)) return;
            String url = downLoadInfo.getUrl();
            String supDir = downLoadInfo.getSupDir();
            String title = downLoadInfo.getTitle();
            RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notification_download);
            contentView.setTextViewText(R.id.task_name, title);
            if (progress != null) {
                if (CommonUtils.matchingNumber(progress)) { // 判断数字
                    float progressF = Float.parseFloat(progress);
                    contentView.setProgressBar(R.id.pbDownload, 100, (int) progressF, false);
                    progress = "已下载" + String.format("%.2f", progressF) + "%";
                }
                contentView.setTextViewText(R.id.tvProcess, progress);
            }
            if (btnText != null) {
                contentView.setTextViewText(R.id.btn_state, btnText);
            }

            // 创建一个跳转指定Activity的Intent
            Intent i = new Intent(context, DownloadActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                pendingIntent = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_IMMUTABLE);
            } else {
                pendingIntent = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_MUTABLE);
            }

            // 创建一个用于记录滑动删除的intent 调用广播
            Intent intentCancel = new Intent(context, NotificationBroadcastReceiver.class);
            intentCancel.setAction("notification_cancelled");
            intentCancel.putExtra("notificationId", id);
            intentCancel.putExtra("fileName", supDir + "/" + title);
            // 意图可变标志  （这里PendingIntent必须设置意图可变标志，否则广播删除用到的TYPE变量永远是旧的）
            int flag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S?PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT:PendingIntent.FLAG_UPDATE_CURRENT;
            PendingIntent pendingIntentCancel = PendingIntent.getBroadcast(context, id, intentCancel, flag);

            // 创建一个处理按钮点击事件的intent 调用广播
            Intent intentClick = new Intent(context, NotificationBroadcastReceiver.class);
            intentClick.setAction("notification_clicked");
            intentClick.putExtra("notificationId", id);
            // 意图可变标志  （这里PendingIntent必须设置意图可变标志，否则广播删除用到的TYPE变量永远是旧的）
            int flag2 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S?PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT:PendingIntent.FLAG_UPDATE_CURRENT;
            PendingIntent pendingIntentClick = PendingIntent.getBroadcast(context, id, intentClick, flag2);

            contentView.setOnClickPendingIntent(R.id.btn_state, pendingIntentClick);
            Notification notification = new NotificationCompat.Builder(context, NotificationUtils.channelKey)
                    .setAutoCancel(false)
                    .setSmallIcon(R.mipmap.app_logo)
                    .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                    .setWhen(System.currentTimeMillis())
                    .setCustomContentView(contentView)
                    .setContentIntent(pendingIntent)
                    .setDeleteIntent(pendingIntentCancel)
                    .setSortKey(url)
                    .setGroup(NotificationUtils.groupKey) // 设置分组
                    .build();
            if (nm == null) {
                nm = context.getSystemService(NotificationManager.class);
            }
            nm.notify(id, notification);
            // 更新摘要通知
            NotificationUtils.flushSummaryNotification(context);
        } catch (Throwable e) {
            CommonUtils.saveLog("=======更新消息通知视图=======" + e.getMessage());
        }
    }

    // 创建通知
    public static Notification createNotification(Context context, String title, String text) {
        Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), R.mipmap.app_logo);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelKey)
                .setSmallIcon(R.mipmap.app_logo_r)
                .setLargeIcon(largeIcon)
                .setContentTitle(title)
                .setContentText(text)
                .setGroup(groupKey) // 绑定组ID
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY) // 仅摘要通知触发提醒
                .setAutoCancel(false)
                .setWhen(System.currentTimeMillis());
        if (text.endsWith("%")) {
            String progressStr = text.substring(0, text.length() - 1);
            if (CommonUtils.matchingNumber(progressStr)) {
                int progress = Math.round(Float.valueOf(progressStr));
                builder.setProgress(100, progress, false);
            }
        }

        return builder.build();
    }

    // 更新摘要通知（组级通知）
    public static void flushSummaryNotification(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        Notification summaryNotification = new NotificationCompat.Builder(context, channelKey)
                .setSmallIcon(R.mipmap.app_logo_r)
                .setGroup(groupKey)
                .setGroupSummary(true) // 关键：声明为摘要通知
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                .build();
        nm.notify(0, summaryNotification);
    }
}

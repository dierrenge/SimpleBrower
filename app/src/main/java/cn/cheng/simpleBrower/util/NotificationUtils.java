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
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

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
    public static void initChannel(Context context) {
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

    // 创建基础通知构建器
    public static NotificationCompat.Builder initBuilder(Context context, String title, String text, Class clazz) {
        // 通知渠道
        initChannel(context);
        // logo图标
        Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), R.mipmap.app_logo);
        // 跳转指定Activity的Intent
        Intent i = new Intent(context, clazz);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_MUTABLE);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelKey)
                .setSmallIcon(R.mipmap.app_logo_r)
                .setLargeIcon(largeIcon)
                .setContentTitle(title)
                .setContentText(text)
                .setGroup(groupKey) // 绑定组ID
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY) // 仅摘要通知触发提醒
                .setAutoCancel(false)
                .setContentIntent(pendingIntent)
                .setWhen(System.currentTimeMillis());
        return builder;
    }

    // 发布下载通知
    public static void notifyDownloadNotification(Context context, int id, String text) {
        // 获取基础信息
        NotificationBean downLoadInfo = MyApplication.getDownLoadInfo(id);
        if (context == null || downLoadInfo == null || text == null) return;
        String url = downLoadInfo.getUrl();
        String supDir = downLoadInfo.getSupDir();
        String title = downLoadInfo.getTitle();
        int progress = 0;
        String progressStr = text;
        if (CommonUtils.matchingNumber(text)) {
            Float progressF = Float.valueOf(text);
            progressStr = "已下载" + String.format("%.2f", progressF) + "%";
            progress = Math.round(progressF);
        }
        // 创建一个基础通知构建器
        NotificationCompat.Builder nBuilder = initBuilder(context, title, progressStr, DownloadActivity.class);
        // 设置下载进度
        nBuilder.setProgress(100, progress, false);
        // 设置排序标识
        nBuilder.setSortKey(url);

        // 创建一个跳转指定Activity的Intent
        Intent i = new Intent(context, DownloadActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_MUTABLE);
        }
        nBuilder.setContentIntent(pendingIntent);

        // 创建一个用于记录滑动删除的intent 调用广播
        Intent intentCancel = new Intent(context, NotificationBroadcastReceiver.class);
        intentCancel.setAction("notification_cancelled");
        intentCancel.putExtra("notificationId", id);
        intentCancel.putExtra("fileName", supDir + "/" + title);
        // 意图可变标志  （这里PendingIntent必须设置意图可变标志，否则广播删除用到的TYPE变量永远是旧的）
        int flag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S?PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT:PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pendingIntentCancel = PendingIntent.getBroadcast(context, id, intentCancel, flag);
        nBuilder.setDeleteIntent(pendingIntentCancel);

        NotificationManager nm = context.getSystemService(NotificationManager.class);
        // 发布通知 （放入通知管理器）
        nm.notify(id, nBuilder.build());
        // 发布摘要通知
        notifySummaryNotification(context);
    }

    // 发布摘要通知（组级通知）
    public static void notifySummaryNotification(Context context) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        Notification summaryNotification = new NotificationCompat.Builder(context, channelKey)
                .setSmallIcon(R.mipmap.app_logo_r)
                .setGroup(groupKey)
                .setGroupSummary(true) // 关键：声明为摘要通知
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                .build();
        nm.notify(0, summaryNotification);
    }
}

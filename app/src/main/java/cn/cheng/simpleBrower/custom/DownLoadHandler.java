package cn.cheng.simpleBrower.custom;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import cn.cheng.simpleBrower.MyApplication;
import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.activity.BrowserActivity;
import cn.cheng.simpleBrower.bean.NotificationBean;
import cn.cheng.simpleBrower.service.DownloadService;
import cn.cheng.simpleBrower.util.CommonUtils;

public class DownLoadHandler extends Handler {

    private static volatile DownLoadHandler instance;
    private Context context;
    private NotificationManager nm;

    // 单例
    public static DownLoadHandler getInstance() {
        if (instance == null) {
            instance = new DownLoadHandler();
        }
        return instance;
    }

    private DownLoadHandler() {
        this.context = MyApplication.getContext();
        this.nm = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        super.handleMessage(msg);
        if (!(msg.obj instanceof String[])) return;
        int n = 0;
        NotificationBean downLoadInfo = new NotificationBean();
        String[] arr = (String[]) msg.obj;
        if (arr.length >= 2) {
            try {
                // 根据 notificationId 获取 notification
                n = Integer.parseInt(arr[1]);
                downLoadInfo = MyApplication.getDownLoadInfo(n);
            } catch (Exception e) {}
            switch (msg.what) {
                case 0:
                    MyToast.getInstance(arr[0]).show();
                    break;
                case 1:
                    break;
                case 2:
                    //下载完成后清除所有下载信息，执行安装提示
                    MyApplication.deleteDownloadList(downLoadInfo.getUrl());
                    MyApplication.deleteDownLoadInfo(n);
                    nm.cancel(n);
                    MyToast.getInstance(arr[0]).show();
                    //停止掉当前的服务
                    if (nm.getActiveNotifications().length == 0) {
                        Intent intent = new Intent(context, DownloadService.class);
                        context.stopService(intent);
                    }
                    break;
                case 3:
                    // 获取进度信息
                    String str = arr[0];
                    // 更新状态栏上的下载进度等信息
                    // Notification notificationX = downLoadInfo.getNotification();
                    Notification notificationX = CommonUtils.getRunNotification(nm, downLoadInfo.getUrl());
                    if (notificationX != null) {
                        RemoteViews contentView = notificationX.contentView;
                        if (CommonUtils.matchingNumber(str)) { // 判断数字
                            contentView.setProgressBar(R.id.pbDownload, 100, (int) Float.parseFloat(str), false);
                            str = "已下载" + str + "%";
                        }
                        contentView.setTextViewText(R.id.tvProcess, str);
                        nm.notify(n, notificationX);
                    }
                    break;
                case 4:
                    MyApplication.deleteDownloadList(downLoadInfo.getUrl());
                    MyApplication.deleteDownLoadInfo(n);
                    nm.cancel(n);
                    //停止掉当前的服务
                    if (nm.getActiveNotifications().length == 0) {
                        Intent intent = new Intent(context, DownloadService.class);
                        context.stopService(intent);
                    }
                    break;
                default:
                    MyToast.getInstance(arr[0]).show();
            }
        }
    }
}

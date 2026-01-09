package cn.cheng.simpleBrower.custom;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import cn.cheng.simpleBrower.MyApplication;
import cn.cheng.simpleBrower.bean.NotificationBean;
import cn.cheng.simpleBrower.service.DownloadService;
import cn.cheng.simpleBrower.util.CommonUtils;
import cn.cheng.simpleBrower.util.NotificationUtils;

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
        this.nm = context.getSystemService(NotificationManager.class);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        super.handleMessage(msg);
        try {
            if (!(msg.obj instanceof String[] arr) || arr.length < 2) return;
            int n = CommonUtils.str2int(arr[1], 0);
            // 根据 notificationId 获取 notification
            NotificationBean downLoadInfo = MyApplication.getDownLoadInfo(n);
            switch (msg.what) {
                case 0:
                    MyToast.getInstance(arr[0]).show();
                    if (arr.length >= 3 && downLoadInfo != null) {
                        if ("0".equals(arr[2])) {
                            // 在子线程修改界面UI得使用handler
                            NotificationUtils.updateRemoteViews(n, null, "继续", nm);
                            downLoadInfo.setState("继续");
                        } else if ("10".equals(arr[2])) {
                            // 在子线程修改界面UI得使用handler
                            NotificationUtils.updateRemoteViews(n, "0", "继续", nm);
                            downLoadInfo.setState("继续");
                            downLoadInfo.setRangeRequest("false");
                        }
                        CommonUtils.writeObjectIntoLocal("downloadList", downLoadInfo.getDate() + CommonUtils.zeroPadding(downLoadInfo.getNotificationId()), downLoadInfo);
                    }
                    break;
                case 1:
                    break;
                case 2:
                    if (downLoadInfo == null) return;
                    //下载完成后清除所有下载信息，执行安装提示
                    MyApplication.deleteDownloadList(downLoadInfo.getUrl());
                    new Handler().postDelayed(() -> MyApplication.deleteDownLoadInfo(n), 3000);
                    nm.cancel(n);
                    MyToast.getInstance(arr[0]).show();
                    //停止掉当前的服务
                    if (nm.getActiveNotifications().length == 0) {
                        Intent intent = new Intent(context, DownloadService.class);
                        context.stopService(intent);
                    }
                    CommonUtils.writeObjectIntoLocal("downloadList", downLoadInfo.getDate() + CommonUtils.zeroPadding(downLoadInfo.getNotificationId()), downLoadInfo);
                    break;
                case 3:
                    if (downLoadInfo == null) return;
                    // 获取进度信息
                    String str = arr[0];
                    // 更新状态栏上的下载进度等信息
                    NotificationUtils.updateRemoteViews(n, str, null, nm);
                    // 内存监控
                    if ("暂停".equals(downLoadInfo.getState())) {
                        double availableMemoryRatio = CommonUtils.getAvailableMemoryRatio(context.getApplicationContext());
                        if (availableMemoryRatio < MyApplication.MIN_AVL_MEM_PCT) {
                            String state = "继续";
                            NotificationUtils.updateRemoteViews(n, null, state, nm);
                            downLoadInfo.setState(state);
                            MyToast.getInstance("可用内存过低").show();
                        }
                    }
                    // CommonUtils.writeObjectIntoLocal("downloadList", downLoadInfo.getDate() + CommonUtils.zeroPadding(downLoadInfo.getNotificationId()), downLoadInfo);
                    /*// Notification notificationX = downLoadInfo.getNotification();
                    // 原方式：contentView可能为空
                    Notification notificationX = CommonUtils.getRunNotification(nm, downLoadInfo.getUrl());
                    if (notificationX != null) {
                        RemoteViews contentView = notificationX.contentView;
                        if (CommonUtils.matchingNumber(str)) { // 判断数字
                            contentView.setProgressBar(R.id.pbDownload, 100, (int) Float.parseFloat(str), false);
                            str = "已下载" + str + "%";
                        }
                        contentView.setTextViewText(R.id.tvProcess, str);
                        nm.notify(n, notificationX);
                    }*/
                    break;
                case 4:
                    if (downLoadInfo == null) return;
                    // MyApplication.deleteDownloadList(downLoadInfo.getUrl());
                    MyApplication.deleteDownLoadInfo(n);
                    nm.cancel(n);
                    MyToast.getInstance(arr[0]).show();
                    //停止掉当前的服务
                    if (nm.getActiveNotifications().length == 0) {
                        Intent intent = new Intent(context, DownloadService.class);
                        context.stopService(intent);
                    }
                    break;
                default:
                    MyToast.getInstance(arr[0]).show();
            }
        } catch (Throwable e) {
            CommonUtils.saveLog("======DownLoadHandler==handleMessage======" + e.getMessage());
        }
    }
}

package cn.cheng.simpleBrower.service;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.service.notification.StatusBarNotification;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.cheng.simpleBrower.MyApplication;
import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.activity.BrowserActivity;
import cn.cheng.simpleBrower.activity.MainActivity;
import cn.cheng.simpleBrower.bean.NotificationBean;
import cn.cheng.simpleBrower.custom.DownLoadHandler;
import cn.cheng.simpleBrower.custom.M3u8DownLoader;
import cn.cheng.simpleBrower.custom.MyToast;
import cn.cheng.simpleBrower.receiver.NotificationBroadcastReceiver;
import cn.cheng.simpleBrower.util.CommonUtils;

/**
 * 下载Service （下载包括m3u8格式的文件）
 */
public class DownloadService extends Service {

    // 文件总目录
    String supDir = "";
    // 通知管理器
    private NotificationManager nm;
    // 消息通知
    private Notification notification;
    // 线程处理工具
    private Handler myHandler;
    // 通知提示视图
    private RemoteViews views;
    // 通知id 每次下载通知要不一样
    private int notificationId = 0;
    // 频道id 每次下载通知要不一样
    private String CHANNEL_ID = "";

    public DownloadService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // 线程消息传递处理
        // myHandler = new MyHandler(Looper.myLooper(), MyApplication.getActivity());
        myHandler = new DownLoadHandler();

        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        int what = intent.getIntExtra("what", 0);
        String title = intent.getStringExtra("title");
        String url = intent.getStringExtra("url");
        // String urlName = url.substring(url.lastIndexOf("/") + 1);
        if (title == null || "".equals(title)) {
            title = System.currentTimeMillis() + "";
        }
        if (title.contains("/")) {
            title = title.substring(title.lastIndexOf("/") + 1);
        }
        supDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/SimpleBrower";
        CHANNEL_ID = url;

        // 高版本通知Notification 必须先定义NotificationChannel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            for (StatusBarNotification activeNotification : nm.getActiveNotifications()) {
                if (activeNotification.getNotification() != null) {
                    String channelId = activeNotification.getNotification().getChannelId();
                    if (CHANNEL_ID.equals(channelId)) {
                        Message message = myHandler.obtainMessage(0, new String[]{"已存在一个相同的下载任务", ""});
                        myHandler.sendMessage(message);
                        return super.onStartCommand(intent, flags, startId);
                    }
                }
            }
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID
                    , "name", NotificationManager.IMPORTANCE_DEFAULT);
            File file = new File(supDir + "/" + title + ".m3u8");
            if (file.exists()) {
                Message message = myHandler.obtainMessage(0, new String[]{"该视频已在影音列表中", ""});
                myHandler.sendMessage(message);
                return super.onStartCommand(intent, flags, startId);
            }
            nm.createNotificationChannel(channel);
        }

        notificationId = CommonUtils.randomNum();

        // 创建一个Notification对象
        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        // 设置打开该通知，该通知自动消失
        nBuilder.setAutoCancel(false);
        // 设置通知的图标
        nBuilder.setSmallIcon(R.mipmap.app_logo);
        // 设置通知内容的标题
        // nBuilder.setContentTitle("下载中");
        // 设置通知内容
        // nBuilder.setContentText("打开APP查看详情");
        // 设置使用系统默认的声音、默认震动
        nBuilder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
        // 设置发送时间
        nBuilder.setWhen(System.currentTimeMillis());

        // 创建一个跳转原来Activity的Intent
        Intent i = new Intent(MyApplication.getActivity(), MyApplication.getActivity().getClass());
        i.setAction(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.setComponent(new ComponentName(MyApplication.getActivity().getPackageName(), MyApplication.getActivity().getPackageName() + "." + MyApplication.getActivity().getLocalClassName()));
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);//关键的一步，设置启动模式
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_MUTABLE);
        }
        nBuilder.setContentIntent(pendingIntent);

        // 创建一个用于记录滑动删除的intent 调用广播
        Intent intentCancel = new Intent(this, NotificationBroadcastReceiver.class);
        intentCancel.setAction("notification_cancelled");
        intentCancel.putExtra("notificationId", notificationId);
        intentCancel.putExtra("fileName", supDir + "/" + title);
        // 意图可变标志  （这里PendingIntent必须设置意图可变标志，否则广播删除用到的TYPE变量永远是旧的）
        int flag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S?PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT:PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pendingIntentCancel = PendingIntent.getBroadcast(this, notificationId, intentCancel, flag);
        nBuilder.setDeleteIntent(pendingIntentCancel);

        // 创建一个处理按钮点击事件的intent 调用广播
        Intent intentClick = new Intent(this, NotificationBroadcastReceiver.class);
        intentClick.setAction("notification_clicked");
        intentClick.putExtra("notificationId", notificationId);
        intentClick.putExtra("channelId", CHANNEL_ID);
        // 意图可变标志  （这里PendingIntent必须设置意图可变标志，否则广播删除用到的TYPE变量永远是旧的）
        int flag2 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S?PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT:PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pendingIntentClick = PendingIntent.getBroadcast(this, notificationId, intentClick, flag2);

        //初始化下载任务内容views
        views = new RemoteViews(getPackageName(), R.layout.notification_download);
        views.setTextViewText(R.id.task_name, title);
        views.setTextViewText(R.id.tvProcess, "已下载0.00%");
        views.setProgressBar(R.id.pbDownload, 100, 0, false);
        views.setOnClickPendingIntent(R.id.btn_state, pendingIntentClick);
        // 创建通知
        notification = nBuilder.setCustomContentView(views).build();
        // 发布通知 （放入通知管理器）
        nm.notify(notificationId, notification);

        // 设置消息属性
        NotificationBean notificationBean = new NotificationBean();
        // notificationBean.setNotification(notification);
        // 设置消息id
        notificationBean.setNotificationId(notificationId);
        // 设置下载文件名称
        notificationBean.setTitle(title);
        // 设置下载文件地址
        notificationBean.setUrl(url);
        // 设置下载类型（网站自身提供的下载为4）
        notificationBean.setWhat(what);
        // 设置生成目录
        notificationBean.setSupDir(supDir);
        // 设置线程数
        notificationBean.setThreadCount(100);
        // 设置重试次数
        notificationBean.setRetryCount(4);
        // 设置连接超时时间（单位：毫秒）
        notificationBean.setTimeoutMillisecond(10000L);
        // 设置下载文件初始消息显示的状态
        notificationBean.setState("暂停");
        MyApplication.setDownLoadInfo(notificationId, notificationBean);

        //启动线程开始执行下载任务
        if (Build.VERSION.SDK_INT >= 29) { // android 12的sd卡读写
            // M3u8DownLoader.test(url, myHandler);
            M3u8DownLoader m3u8Download = new M3u8DownLoader(notificationId);
            // notificationBean.setM3u8Download(m3u8Download);
            //开始下载
            m3u8Download.start();
        }
        // Message message2 = myHandler.obtainMessage(0, new String[]{CHANNEL_ID + "-开始下载", ""});
        // myHandler.sendMessage(message2);
        return super.onStartCommand(intent, flags, startId);
    }

    // 线程消息传递处理
    /*private class MyHandler extends Handler {

        private Activity context;

        public MyHandler(Looper looper, Activity context) {
            super(looper);
            this.context = context;
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
                        MyToast.getInstance(context, arr[0]).show();
                        break;
                    case 1:
                        break;
                    case 2:
                        //下载完成后清除所有下载信息
                        MyApplication.deleteDownloadList(downLoadInfo.getUrl());
                        MyApplication.deleteDownLoadInfo(n);
                        nm.cancel(n);
                        MyToast.getInstance(context, arr[0]).show();
                        //停止掉当前的服务
                        stopSelf();
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
                        break;
                }
            }
        }
    }*/
}
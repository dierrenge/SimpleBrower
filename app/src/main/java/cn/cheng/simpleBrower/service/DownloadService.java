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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.cheng.simpleBrower.MyApplication;
import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.activity.BrowserActivity;
import cn.cheng.simpleBrower.activity.MainActivity;
import cn.cheng.simpleBrower.custom.M3u8DownLoader;
import cn.cheng.simpleBrower.custom.MyToast;
import cn.cheng.simpleBrower.receiver.NotificationBroadcastReceiver;

/**
 * 下载Service （目前仅用于下载m3u8）
 */
public class DownloadService extends Service {

    // 通知管理器
    private NotificationManager nm;
    // 消息通知
    private Notification notification;
    // 线程处理工具
    private MyHandler myHandler;
    // 下载进度
    private int download_precent = 0;
    // 通知提示视图
    private RemoteViews views;
    // 通知id 每次下载通知要不一样
    private int notificationId = 1234;
    // 频道id 每次下载通知要不一样
    private String CHANNEL_ID = "一口一个大胖子，喜喜";

    private Map<Integer, ExecutorService> pools = new HashMap<>();

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
        myHandler = new MyHandler(Looper.myLooper(), MyApplication.getActivity());

        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        String title = intent.getStringExtra("title");
        if (title != null && !"".equals(title)) {
            CHANNEL_ID = title;
        }
        // 高版本通知Notification 必须先定义NotificationChannel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID
                    , "name", NotificationManager.IMPORTANCE_DEFAULT);
            if (nm.getActiveNotifications().length > 0) {
                for (StatusBarNotification activeNotification : nm.getActiveNotifications()) {
                    if (activeNotification.getNotification() != null) {
                        String channelId = activeNotification.getNotification().getChannelId();
                        if (CHANNEL_ID.equals(channelId)) {
                            Message message = myHandler.obtainMessage(0, new String[]{"该网页已存在一个下载任务", ""});
                            myHandler.sendMessage(message);
                            return super.onStartCommand(intent, flags, startId);
                        }
                    }
                }
                notificationId ++;
            }
            nm.createNotificationChannel(channel);
        }

        // 创建一个Notification对象
        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        // 设置打开该通知，该通知自动消失
        nBuilder.setAutoCancel(false);
        // 设置通知的图标
        nBuilder.setSmallIcon(R.drawable.btn_like);
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
            pendingIntent = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_ONE_SHOT);
        }
        nBuilder.setContentIntent(pendingIntent);

        // 创建一个用于记录滑动删除的调用广播
        Intent intentCancel = new Intent(this, NotificationBroadcastReceiver.class);
        intentCancel.setAction("notification_cancelled");
        intentCancel.putExtra(NotificationBroadcastReceiver.TYPE, notificationId);
        PendingIntent pendingIntentCancel;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingIntentCancel = PendingIntent.getBroadcast(this, 0, intentCancel, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntentCancel = PendingIntent.getBroadcast(this, 0, intentCancel, PendingIntent.FLAG_ONE_SHOT);
        }
        nBuilder.setDeleteIntent(pendingIntentCancel);

        // 创建通知
        notification = nBuilder.build();
        //设置任务栏中下载进程显示的views
        views = new RemoteViews(getPackageName(), R.layout.notification_download);
        notification.contentView = views;
        //将下载任务添加到任务栏中
        nm.notify(notificationId, notification);

        //初始化下载任务内容views
        String[] arr = new String[]{"0", notificationId+""};
        Message message = myHandler.obtainMessage(3, arr);
        myHandler.sendMessage(message);

        //启动线程开始执行下载任务
        if (Build.VERSION.SDK_INT >= 29) { // android 12的sd卡读写
            //启动线程开始执行下载任务
            String url = intent.getStringExtra("url");
            String dirName = intent.getStringExtra("dirName");
            if (dirName == null || "".equals(dirName)) {
                dirName = System.currentTimeMillis() + "";
            }
            if (title == null || "".equals(title)) {
                title = "test";
            }
            if (title.contains("/")) {
                title = title.substring(title.lastIndexOf("/") + 1);
            }
            // M3u8DownLoader.test(url, myHandler);
            M3u8DownLoader m3u8Download =  new M3u8DownLoader(url, notificationId);
            //设置生成目录
            String dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
            m3u8Download.setDir(dir + "/SimpleBrower/" + dirName, dir + "/SimpleBrower");
            //设置视频名称
            m3u8Download.setFileName(title);
            //设置线程数
            // m3u8Download.setThreadCount(100);
            ExecutorService fixedThreadPool = Executors.newFixedThreadPool(100);
            pools.put(notificationId, fixedThreadPool);
            m3u8Download.setFixedThreadPool(fixedThreadPool);
            //设置重试次数
            m3u8Download.setRetryCount(8);
            //设置连接超时时间（单位：毫秒）
            m3u8Download.setTimeoutMillisecond(10000L);
            //m3u8是否转换成MP4
            m3u8Download.setM3u8ToMp4(false);
            //开始下载
            m3u8Download.start(myHandler);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    // 线程消息传递处理
    private class MyHandler extends Handler {

        private Activity context;

        public MyHandler(Looper looper, Activity context) {
            super(looper);
            this.context = context;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (!(msg.obj instanceof String[])) {
                return;
            }
            String[] arr = (String[]) msg.obj;
            if (arr.length == 2) {
                switch (msg.what) {
                    case 0:
                        MyToast.getInstance(context, arr[0]).show();
                    case 1:
                        break;
                    case 2:
                        //下载完成后清除所有下载信息，执行安装提示
                        download_precent = 0;
                        nm.cancel(Integer.parseInt(arr[1]));
                        MyToast.getInstance(context, arr[0]).show();
                        //停止掉当前的服务
                        stopSelf();
                        break;
                    case 3:
                        // 获取记录的删除项
                        List<Integer> nums = MyApplication.getNums();
                        if (nums.contains(Integer.parseInt(arr[1]))) {
                            pools.get(Integer.parseInt(arr[1])).shutdownNow();
                            return;
                        }
                        String str = arr[0];
                        if (str.matches("^(([1-9]\\d*)(\\.\\d+)?)$|^((0)(\\.\\d+)?)$")) { // 判断数字
                            download_precent = (int) Float.parseFloat(str);
                            str = "已下载" + str + "%";
                        }
                        //更新状态栏上的下载进度信息
                        views.setTextViewText(R.id.tvProcess, str);
                        views.setProgressBar(R.id.pbDownload, 100, download_precent, false);
                        notification.contentView = views;
                        nm.notify(Integer.parseInt(arr[1]), notification);
                        break;
                    case 4:
                        nm.cancel(Integer.parseInt(arr[1]));
                        break;
                }
            }
        }
    }
}
package cn.cheng.simpleBrower.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.service.notification.StatusBarNotification;

import org.apache.commons.lang3.StringUtils;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import cn.cheng.simpleBrower.MyApplication;
import cn.cheng.simpleBrower.bean.NotificationBean;
import cn.cheng.simpleBrower.custom.DownLoadHandler;
import cn.cheng.simpleBrower.custom.M3u8DownLoader;
import cn.cheng.simpleBrower.util.CommonUtils;
import cn.cheng.simpleBrower.util.NotificationUtils;

/**
 * 下载Service （下载包括m3u8格式的文件）
 */
public class DownloadService extends Service {

    // 文件总目录
    String supDir = "";
    // 线程处理工具
    private Handler myHandler;
    // 通知id 每次下载通知要不一样
    private int notificationId = 0;

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
        try {
            // 线程消息传递处理 初始化
            myHandler =  DownLoadHandler.getInstance();

            // 剩余内存判断
            double availableMemoryRatio = CommonUtils.getAvailableMemoryRatio(this.getApplicationContext());
            if (availableMemoryRatio < MyApplication.MIN_AVL_MEM_PCT) {
                Message message = myHandler.obtainMessage(0, new String[]{"可用内存过低", ""});
                myHandler.sendMessage(message);
                return super.onStartCommand(intent, flags, startId);
            }

            // 获取初始数据
            int what = intent.getIntExtra("what", 0);
            String title = intent.getStringExtra("title");
            String url = intent.getStringExtra("url");
            if (StringUtils.isEmpty(url)) {
                return super.onStartCommand(intent, flags, startId);
            }
            if (title == null || "".equals(title)) {
                title = System.currentTimeMillis() + "";
            }
            if (title.contains("/")) {
                title = title.substring(title.lastIndexOf("/") + 1);
            }
            supDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/SimpleBrower";
            notificationId = intent.getIntExtra("notificationId", CommonUtils.randomNum());

            // 下载任务校验
            NotificationManager nm = this.getSystemService(NotificationManager.class);
            if (nm.getActiveNotifications().length > 5) {
                Message message = myHandler.obtainMessage(0, new String[]{"下载任务数已到上限", ""});
                myHandler.sendMessage(message);
                return super.onStartCommand(intent, flags, startId);
            }
            for (StatusBarNotification activeNotification : nm.getActiveNotifications()) {
                if (activeNotification.getNotification() != null) {
                    String sortKey = activeNotification.getNotification().getSortKey();
                    if (url.equals(sortKey)) {
                        Message message = myHandler.obtainMessage(0, new String[]{"已存在一个相同的下载任务", ""});
                        myHandler.sendMessage(message);
                        return super.onStartCommand(intent, flags, startId);
                    }
                }
            }
            File file = new File(supDir + "/" + title + ".m3u8");
            if (file.exists()) {
                Message message = myHandler.obtainMessage(0, new String[]{"该视频已在影音列表中", ""});
                myHandler.sendMessage(message);
                return super.onStartCommand(intent, flags, startId);
            }

            // 设置消息属性
            NotificationBean notificationBean = MyApplication.getDownLoadInfo(notificationId);
            if (notificationBean == null) {
                notificationBean = new NotificationBean();
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
                notificationBean.setThreadCount(150);
                // 设置重试次数
                notificationBean.setRetryCount(4);
                // 设置连接超时时间（单位：毫秒）
                notificationBean.setTimeoutMillisecond(10000L);
                // 设置日期
                notificationBean.setDate(new SimpleDateFormat("yyyyMMdd").format(new Date()));
                // 设置下载文件初始消息显示的状态
                notificationBean.setState("暂停");
                MyApplication.setDownLoadInfo(notificationId, notificationBean);
            }

            // 发布下载通知
            NotificationUtils.notifyDownloadNotification(this, notificationId, "0");

            //启动线程开始执行下载任务
            if (Build.VERSION.SDK_INT >= 29) { // android 12的sd卡读写
                // M3u8DownLoader.test(url, myHandler);
                M3u8DownLoader m3u8Download = new M3u8DownLoader(notificationId);
                //开始下载
                m3u8Download.start();
            }
        } catch (Throwable e) {
            CommonUtils.saveLog("=====DownloadService=====" + e.getMessage());
        }
        return super.onStartCommand(intent, flags, startId);
    }

}
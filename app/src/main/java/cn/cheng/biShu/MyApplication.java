package cn.cheng.biShu;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.cheng.biShu.bean.DownloadBean;
import cn.cheng.biShu.bean.NotificationBean;
import cn.cheng.biShu.util.CommonUtils;

public class MyApplication extends Application {

    public static final double MIN_AVL_MEM_PCT = 0.16; // 最小可用内存占比

    private static Context context;

    // 存放 当前网页 路径
    private static List<String> urls = new ArrayList<>();

    // 存放 当前文本行
    private static Map<String, ArrayList<String>> novelLinesMap = new HashMap<>();

    // 记录文本的本地路径url
    private static String txtUrl;

    // 记录网页中的下载对象（主要是影音下载对象）
    private static List<DownloadBean> downloadList = new ArrayList<>();
    private static List<DownloadBean> downloadListBak = new ArrayList<>(); // 副本

    // 记录点击下载的链接url
    private static String clickDownloadUrl;

    // 记录播放器影音播放进度
    private static HashMap<String, Long> videoPosition = new HashMap<>();

    // 记录下载任务消息
    private static HashMap<Integer, NotificationBean> downLoadInfoMap = new HashMap<>();

    // 标记打开txtActivity
    private static boolean openFlag;

    public static String downloadUrl; // 记录下载链接以防止重复

    public static String jumpUrl; // 记录当前网页地址

    public static Activity currentActivity; // 当前运行的activity

    public static boolean turnThePage;

    @Override
    public void onCreate() {
        super.onCreate();
        context=getApplicationContext();

        // 跟踪 Activity 的生命周期
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(Activity activity) {
            }

            @Override
            public void onActivityResumed(Activity activity) {
                currentActivity = activity;
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        });

        // 全局异常捕获
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            // 打印崩溃日志（写入文件）
            CommonUtils.saveLog("全局异常捕获：" + ex.getMessage());
        });
    }

    public static Context getContext() {
        return context;
    }

    public static List<String> getUrls() {
        return urls;
    }

    public static void setUrl(String url) {
        if (urls.size() > 0 && urls.get(urls.size() - 1).equals(url)) return;
        urls.add(url);
    }

    public static void clearUrls() {
        urls.clear();
    }

    public static Map<String, ArrayList<String>> getNovelLinesMap() {
        return novelLinesMap;
    }

    public static void setNovelLines(String name, ArrayList<String> lines) {
        novelLinesMap.clear(); // 内存只保留一个文本，减少没必要的内存占用
        MyApplication.novelLinesMap.put(name, lines);
    }

    public static String getTxtUrl() {
        return txtUrl;
    }

    public static void setTxtUrl(String txtUrl) {
        MyApplication.txtUrl = txtUrl;
    }

    public static void setDownload(DownloadBean bean) {
        int ret = 99;
        String title = bean.getTitle();
        // title去重处理(算法还能优化，先这样吧)
        while ((ret = compareDownload(bean)) == 1) {
            title = CommonUtils.preventDuplication(title);
            bean.setTitle(title);
        }
        if (ret == 0) {
            downloadList.add(bean);
            downloadListBak.add(bean);
        }
    }

    private static int compareDownload(DownloadBean bean) {
        String title = bean.getTitle();
        String url = bean.getUrl();
        for (DownloadBean b : downloadListBak) {
            String previousTitle = b.getTitle();
            String previousUrl = b.getUrl();
            if (url.equals(previousUrl)) {
                return 99;
            }
            if (title.equals(previousTitle)) {
                return 1;
            }
        }
        return 0;
    }

    public static List<DownloadBean> getDownloadList() {
        return downloadList;
    }
    public static void deleteDownloadList(String url) {
        downloadList.removeIf(item -> item.getUrl() != null && item.getUrl().equals(url));
        downloadListBak.removeIf(item -> item.getUrl() != null && item.getUrl().equals(url));
    }

    public static void clearDownloadList() {
        downloadList = new ArrayList<>();
        downloadListBak = new ArrayList<>();
    }

    public static String getClickDownloadUrl() {
        return clickDownloadUrl;
    }

    public static void setClickDownloadUrl(String clickDownloadUrl) {
        MyApplication.clickDownloadUrl = clickDownloadUrl;
    }

    public static void setVideoPosition(String name, Long position) {
        videoPosition.put(name, position);
    }

    public static HashMap<String, Long> getVideoPosition() {
        return videoPosition;
    }

    public static NotificationBean getDownLoadInfo(int key) {
        return downLoadInfoMap.get(key);
    }

    public static void setDownLoadInfo(int key, NotificationBean value) {
        downLoadInfoMap.put(key, value);
    }

    public static void deleteDownLoadInfo(int key) {
        downLoadInfoMap.remove(key);
    }

    public static HashMap<Integer, NotificationBean> getDownLoadInfoMap() {
        return downLoadInfoMap;
    }

    public static boolean isOpenFlag() {
        return openFlag;
    }

    public static void setOpenFlag(boolean openFlag) {
        MyApplication.openFlag = openFlag;
    }

}
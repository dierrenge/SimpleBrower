package cn.cheng.simpleBrower;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cn.cheng.simpleBrower.bean.DownloadBean;
import cn.cheng.simpleBrower.bean.NotificationBean;
import cn.cheng.simpleBrower.bean.PositionBean;
import cn.cheng.simpleBrower.util.CommonUtils;

public class MyApplication extends Application {

    private static Context context;

    private static Activity activity;

    // 记录下载的删除项
    private static List<Integer> nums = new LinkedList<>();

    // 存放 当前网页 路径
    private static List<String> urls = new ArrayList<>();

    // 存放 当前小说进度
    private static Map<String, PositionBean> novelMap = new HashMap<>();

    // 存放 当前小说行
    private static Map<String, ArrayList<String>> novelLinesMap = new HashMap<>();

    // 记录小说的本地路径url
    private static String txtUrl;

    // 记录网页中的下载对象（主要是影音下载对象）
    private static List<DownloadBean> downloadList = new ArrayList<>();
    private static List<DownloadBean> downloadListBak = new ArrayList<>(); // 副本

    // 记录点击下载的链接url
    private static String clickDownloadUrl;

    // 记录播放器影音播放进度
    private static HashMap<String, Long> videoPosition = new HashMap<>();

    // 记录下载任务消息
    private static HashMap<Integer, NotificationBean> downLoadBeanMap = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        context=getApplicationContext();
    }

    public static Context getContext() {
        return context;
    }

    // 必须事先设置activity
    public static Activity getActivity() {
        return activity;
    }

    public static void setNum(int num) {
        if (!nums.contains(num)) {
            nums.add(num);
        }
    }

    public static void removeNum(int num) {
        nums.remove(num);
    }

    public static List<Integer> getNums() {
        return nums;
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

    public static void setActivity(Activity activity) {
        MyApplication.activity = activity;
    }

    public static Map<String, PositionBean> getNovelMap() {
        return novelMap;
    }

    public static void setNovel(String name, PositionBean bean) {
        MyApplication.novelMap.put(name, bean);
    }

    public static Map<String, ArrayList<String>> getNovelLinesMap() {
        return novelLinesMap;
    }

    public static void setNovelLines(String name, ArrayList<String> lines) {
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
        return downLoadBeanMap.get(key);
    }

    public static void setDownLoadInfo(int key, NotificationBean value) {
        downLoadBeanMap.put(key, value);
    }

    public static void deleteDownLoadState(int key) {
        downLoadBeanMap.remove(key);
    }

}
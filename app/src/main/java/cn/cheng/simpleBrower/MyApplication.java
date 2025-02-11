package cn.cheng.simpleBrower;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cn.cheng.simpleBrower.bean.PositionBean;

public class MyApplication extends Application {

    private static Context context;

    private static Activity activity;

    private static List<Integer> nums = new LinkedList<>();

    // 存放 当前网页 路径
    private static List<String> urls = new ArrayList<>();

    // 存放 当前小说进度
    private static Map<String, PositionBean> novelMap = new HashMap<>();

    // 存放 当前小说行
    private static Map<String, ArrayList<String>> novelLinesMap = new HashMap<>();

    private static String txtUrl;

    public static Context getContext() {
        return context;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context=getApplicationContext();

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
}
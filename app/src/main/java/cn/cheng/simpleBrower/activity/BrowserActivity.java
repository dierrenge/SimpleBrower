package cn.cheng.simpleBrower.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.ConsoleMessage;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.cheng.simpleBrower.MyApplication;
import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.bean.SysBean;
import cn.cheng.simpleBrower.custom.FeetDialog;
import cn.cheng.simpleBrower.custom.M3u8DownLoader;
import cn.cheng.simpleBrower.custom.MyToast;
import cn.cheng.simpleBrower.service.DownloadService;
import cn.cheng.simpleBrower.util.AdBlocker;
import cn.cheng.simpleBrower.util.AssetsReader;
import cn.cheng.simpleBrower.util.CommonUtils;
import cn.cheng.simpleBrower.util.SysWindowUi;

/**
 * 浏览器页面
 */
public class BrowserActivity extends AppCompatActivity {

    private LinearLayout editLayout2;
    private LinearLayout url_Layout;
    private WebView webView;
    private FloatingActionMenu btn_menu;
    private FloatingActionButton backBtn;
    private FloatingActionButton flagBtn;
    private FloatingActionButton likeBtn;
    private Button url_back;
    private Button url_jump;
    private EditText url_box;
    private EditText urlText;
    private FeetDialog feetDialog;
    private Handler handler;
    // 存放 当前网页 路径
    private List<String> urls = new ArrayList<>();

    private myWebChromeClient xwebchromeclient;
    private FrameLayout video_fullView;// 全屏时视频加载view
    private View xCustomView;
    private WebChromeClient.CustomViewCallback xCustomViewCallback;

    private boolean flag = true; // 是否展示检查下载的提示框
    private boolean flagVideo = true; // 是否展示检查影音下载的提示框
    private boolean flagGif = true; // 是否开启动图过滤

    private static String currentUrl; // 当前网页网址

    private Map<String, Boolean> loadedUrls = new HashMap<>(); // 广告链接集

    private boolean hasAudioVideo = true; // 页面中有视频或音频

    private Map<String, Map<String, String>> savaMaps = new HashMap(); // 记录浏览位置（主要针对动态加载的页面）
    private String flagUrl = ""; // 前进或后退的网址

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            // 忽略强制网络策略: 在主线程中可以访问网络
            StrictMode.ThreadPolicy policy=new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);

            // 设置默认导航栏、状态栏样式
            SysWindowUi.setStatusBarNavigationBarStyle(this, SysWindowUi.NO_STATE__NO_STATE);
            setContentView(R.layout.activity_brower);

            SysBean sysBean = CommonUtils.readObjectFromLocal("SysSetting", SysBean.class);
            if (sysBean != null) {
                flagVideo = sysBean.isFlagVideo();
                flagGif = sysBean.isFlagGif();
            }

            // 注册广告过滤器
            AdBlocker.init(this);

            // MyApplication.clearUrls();

            initWebView();

            initOther();

            Intent intent = this.getIntent();

            // 设置此activity可用于打开 网络链接
            if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                Uri uri = intent.getData();
                if (uri != null) {
                    currentUrl = uri.toString();
                }
            } else {
                // 获取上个页面传过来的网址
                currentUrl = intent.getStringExtra("webInfo");
            }
            CommonUtils.saveLog("打开方式-网络链接：" + currentUrl);
            url_box.setText(currentUrl);
            // 跳转到该网站
            loadUrl(currentUrl);
        } catch (Throwable e) {
            MyToast.getInstance(this, "打开异常咯").show();
            e.printStackTrace();
            CommonUtils.saveLog("BrowserActivity:" + e.getMessage());
            this.finish();
        }
    }

    @Override
    public void onBackPressed() {
        // 如果可以回退就回退到上一页，而不是退出当前 Activity
        if (webView.canGoBack()) {
            // webView.goBack();
            backUrl();
        } else {
            if (inCustomView()) {
                hideCustomView();
            } else {
                webView.loadUrl("about:blank");
                // BrowserActivity.this.finish();
            }
            // 默认回退处理方式
            super.onBackPressed();
        }
    }


    private void initWebView() {
        /******************webView设置**************************/
        webView = this.findViewById(R.id.myBrower);
        WebSettings webSettings = webView.getSettings();
        // 不显示滚动条
        webView.setVerticalScrollBarEnabled(false);
        //设置支持缩放变焦 是否显示缩放按钮，默认false
        webSettings.setBuiltInZoomControls(true);
        // 不显示webview缩放按钮
        webSettings.setDisplayZoomControls(false);
        //设置是否支持缩放
        webSettings.setSupportZoom(true);
        //最小缩放等级
        webView.setInitialScale(60);
        //设置此属性，可任意比例缩放。大视图模式
        webSettings.setUseWideViewPort(true);
        // 设置缓存
        webSettings.setSaveFormData(false);
        webSettings.setSavePassword(false);
        // 支持js
        webSettings.setJavaScriptEnabled(true);
        //设置是否允许JS打开新窗口
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        //和setUseWideViewPort(true)一起解决网页自适应问题
        webSettings.setLoadWithOverviewMode(true);
        //是否使用缓存
        //webSettings.setAppCacheEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK); // Android 13 平替 setAppCacheEnabled(true)
        //开启本地DOM存储
        webSettings.setDomStorageEnabled(true);
        // 加载图片
        webSettings.setLoadsImagesAutomatically(true);
        //播放音频，多媒体需要用户手动？设置为false为可自动播放
        webSettings.setMediaPlaybackRequiresUserGesture(true);
        // 允许访问文件
        webSettings.setAllowFileAccess(true);
        // 设置WebView是否支持多窗口。如果设置为true，主程序要实现onCreateWindow(WebView, boolean, boolean, Message)，默认false
        //webSettings.setSupportMultipleWindows(true);
        // 允许JavaScript跨域执行
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setAllowFileAccessFromFileURLs(true);

        //处理http和https混合的问题(https加载的网页中用http去获取图片了；原因就是安卓5.0以后做了限制，解决方法就一行代码)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        } else {
            webSettings.setMixedContentMode(WebSettings.LOAD_NORMAL);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // 允许javascript出错
            try {
                Method method = Class.forName("android.webkit.WebView").
                        getMethod("setWebContentsDebuggingEnabled", Boolean.TYPE);
                if (method != null) {
                    method.setAccessible(true);
                    method.invoke(null, true);
                }
            } catch (Exception e) {
                // do nothing
            }
        }

        // 修复一些机型webview无法点击  但这个会导致网页文本无法复制
        /*webView.requestFocus(View.FOCUS_DOWN);
        webView.setOnTouchListener((View v, MotionEvent event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_UP:
                    if (!v.hasFocus()) {
                        v.requestFocus();
                    }
                    break;
            }
            return false;
        });*/

        // 浏览处理
        webView.setWebViewClient(myClient);
        // 设置下载监听
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String disposition, String mimetype, long length) {
                // 会用到的权限
                if (!flagVideo && !CommonUtils.hasStoragePermissions(BrowserActivity.this)) {
                    CommonUtils.requestStoragePermissions(BrowserActivity.this);
                    return;
                }
                // 调用系统下载处理
                // downloadBySystem(url, disposition, mimetype);
                // 使用自定义下载
                Message msg = Message.obtain();
                String name = URLUtil.guessFileName(url, disposition, mimetype);;
                if (StringUtils.isEmpty(name)) {
                    name = CommonUtils.getUrlName(url);
                }
                String finalName = name;
                new Thread(() -> {
                    // 用到的权限
                    if (CommonUtils.hasStoragePermissions(BrowserActivity.this)) {
                        String title = finalName;
                        try {
                            title = URLDecoder.decode(title, "utf-8");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        // title = title.length() > 30 ? title.substring(0, 24) + "···" + title.substring(title.length() - 6) : title;
                        title = M3u8DownLoader.getUrlContentFileSize(url, title);

                        if (!title.contains(".html;")) {
                            String[] arr = new String[]{finalName, url, title};
                            msg.obj = arr;
                            msg.what = 4;
                            handler.sendMessage(msg);
                        }
                    } else {
                        CommonUtils.requestStoragePermissions(BrowserActivity.this);
                    }
                }).start();
            }
        });

        // js 向 java 传递数据
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void postMessage(String message) {
                System.out.println("postMessage       666666666666\n" + message);
                // 这里你可以处理传递过来的参数
                /*Message msg = Message.obtain();
                String[] arr = new String[]{"", message};
                msg.obj = arr;
                msg.what = 10;
                handler.sendMessage(msg);*/
            }
        }, "Android");

        /*** 视频播放相关的方法 **/
        video_fullView = (FrameLayout) findViewById(R.id.video_fullView);
        xwebchromeclient = new myWebChromeClient();
        webView.setWebChromeClient(xwebchromeclient);
    }

    private void initOther() {
        /******************按钮设置**************************/
        btn_menu = findViewById(R.id.btn_menu);
        // flagBtn按钮
        flagBtn = findViewById(R.id.flagBtn);
        flagBtn.setOnClickListener(view -> {
            // 会用到的权限
            if (!flagVideo && !CommonUtils.hasStoragePermissions(BrowserActivity.this)) {
                CommonUtils.requestStoragePermissions(BrowserActivity.this);
                return;
            }
            flagVideo = !flagVideo;
            String msg = "影音下载监测已开启";
            if (!flagVideo) {
                msg = "影音下载监测已关闭";
            }
            MyToast.getInstance(this, msg).show();
            SysBean sysBean = new SysBean();
            sysBean.setFlagGif(flagGif);
            sysBean.setFlagVideo(flagVideo);
            CommonUtils.writeObjectIntoLocal(sysBean, "SysSetting");
        });
        // 收藏按钮
        likeBtn = findViewById(R.id.likeBtn);
        likeBtn.setOnClickListener(view -> {
            // 页面跳转后会用到的权限
            if (CommonUtils.hasStoragePermissions(this)) {
                if (Build.VERSION.SDK_INT >= 29) { // android 12的sd卡读写
                    //启动线程开始执行 收藏网址存档
                    new Thread(() -> {
                        try {
                            File file = CommonUtils.getFile("SimpleBrower/0_like", "like.txt", "");
                            // 没有文件 hasFile为true 则追加
                            boolean hasFile = false;
                            if (!file.exists()) {
                                hasFile = file.createNewFile();
                            } else {
                                hasFile = true;
                            }
                            try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file, hasFile));
                                 BufferedReader reader = new BufferedReader(new FileReader(file))
                            ) {
                                Message message = Message.obtain();
                                message.what = 1;
                                String likeUrl = currentUrl + "\n";
                                if (hasFile) {
                                    List<String> likes = new ArrayList<>();
                                    String line = null;
                                    while ((line = reader.readLine()) != null) {
                                        likes.add(line);
                                    }
                                    if (likes.size() > 0 && likes.contains(currentUrl)) {
                                        message.obj = "该网页已收藏过了。";
                                        handler.sendMessage(message);
                                        return;
                                    }
                                }
                                bos.write(likeUrl.getBytes());
                                message.obj = "网页收藏成功";
                                handler.sendMessage(message);
                            } catch (IOException e) {
                                e.getMessage();
                            }
                        } catch (Exception e) {
                            e.getMessage();
                        }
                    }).start();
                }
            } else {
                CommonUtils.requestStoragePermissions(this);
            }
        });
        // 返回主页按钮
        backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(view -> {
            // System.out.println("=============================================");
            // System.out.println(MyApplication.getUrls());
            BrowserActivity.super.onBackPressed();
            //  BrowerActivity.this.finish();
        });
        // 返回上一个网页按钮
        url_back = findViewById(R.id.url_back);
        url_back.setOnClickListener(view -> {
            if (webView.canGoBack()) {
                // webView.goBack();
                backUrl();
            } else {
                BrowserActivity.super.onBackPressed();
            }
        });
        // 跳转下一个网址栏网址按钮(相对于返回上一个)
        url_jump = findViewById(R.id.url_jump);
        url_jump.setOnClickListener(view -> {
            if (webView.canGoForward()) {
                List urlList = getUrls();
                if (urlList.size() > 0) {
                    int index = urlList.lastIndexOf(webView.getUrl());
                    String nextUrl = urlList.get(index + 1).toString();
                    webView.goForward();

                    // 标记前进或后退的网址
                    flagUrl = nextUrl + "前进";
                }
            }
        });

        /******************网址栏**************************/
        url_Layout = this.findViewById(R.id.url_Layout);
        editLayout2 = this.findViewById(R.id.editLayout2);
        editLayout2.setOnTouchListener((View view, MotionEvent motionEvent) -> {
            if (null != this.getCurrentFocus()) {
                url_box.clearFocus();
                /**
                 * 点击空白位置 隐藏软键盘
                 */
                InputMethodManager mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                return mInputMethodManager.hideSoftInputFromWindow(url_box.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                // return mInputMethodManager.hideSoftInputFromWindow(SearchActivity.this.getCurrentFocus().getWindowToken(), 0);
            }
            return false;
        });
        url_box = findViewById(R.id.url_box);
        url_box.setOnTouchListener((View view, MotionEvent motionEvent) -> {
            //获得焦点
            url_box.setFocusable(true);
            url_box.setFocusableInTouchMode(true);
            url_box.requestFocus();
            return false;
        });
        // 软件盘回车响应
        url_box.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == KeyEvent.ACTION_DOWN || actionId == EditorInfo.IME_ACTION_DONE) {
                String webInfo = url_box.getText().toString();
                if (!CommonUtils.isUrl(webInfo)) {
                    webInfo = "https://www.baidu.com/s?wd=" + webInfo;
                } else {
                    if (!webInfo.toLowerCase().startsWith("http://") && !webInfo.toLowerCase().startsWith("https://")) {
                        webInfo = "http://" + webInfo;
                    }
                }
                loadUrl(webInfo);
            }
            return true;
        });

        /******************视频网络地址提示**************************/
        urlText = findViewById(R.id.url_txt);
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message message) {
                int what = message.what;
                if (what != 1) {
                    String[] arr = (String[]) message.obj;
                    String title = arr[0];
                    String url = arr[1];

                    // 更新日志框内容
                    String oldText = urlText.getText().toString();
                    urlText.setText(oldText + "\n下载链接：" + url);

                    // 无格式链接 判断格式
                    if (what == 9) {
                        String fileType = CommonUtils.getNetFileType(url, 1000);
                        if (fileType == null) {
                            return false;
                        }
                        if (fileType.contains("/")) {
                            fileType = "." + fileType.substring(fileType.lastIndexOf("/") + 1);
                        }
                        // System.out.println(url + "*******************1***********************" + fileType);
                        // 影音文件格式
                        List<String> formats = AssetsReader.getList("audioVideo.txt");
                        if (!formats.contains(fileType)) {
                            return false;
                        }
                    }

                    // 弹框选择
                    if (flag && (feetDialog == null || !feetDialog.isShowing())) {
                        if (what == 4 && !url.contains(".m3u8")) {
                            String title2 = arr[2];
                            feetDialog = new FeetDialog(BrowserActivity.this, "下载", title2, "下载", "取消");
                        } else {
                            feetDialog = new FeetDialog(BrowserActivity.this);
                        }
                        if (what == 4 || flagVideo) {
                            feetDialog.setOnTouchListener(new FeetDialog.TouchListener() {
                                @Override
                                public void close() {
                                    flag = true;
                                    feetDialog.dismiss();
                                }

                                @Override
                                public void ok() {
                                    flag = true;
                                    download(url, title, what);
                                    feetDialog.dismiss();
                                }
                            });
                            flag = false;
                            feetDialog.show();
                        }
                    }
                } else {
                    MyToast.getInstance(BrowserActivity.this, message.obj + "").show();
                }
                return false;
            }
        });


        /**********************动态注册广播****************************/
        DownloadBroadcastReceiver broadcastReceiver = new DownloadBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        this.registerReceiver(broadcastReceiver, intentFilter);
    }

    // 下载
    private void download(String url, String titleO, int what) {
        CommonUtils.requestNotificationPermissions(this); // 通知
        String title = titleO;
        try {
            title = URLDecoder.decode(titleO, "utf-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (what != 4 || true) {
            MyApplication.setActivity(BrowserActivity.this);
            Intent intent = new Intent(BrowserActivity.this, DownloadService.class);
            intent.putExtra("what", what);
            intent.putExtra("url", url);
            intent.putExtra("title", title.length() > 30 ? title.substring(0, 24) + "···" + title.substring(title.length() - 6) : title);
            BrowserActivity.this.startService(intent);
        } else {
            String[] split = url.split("\\.");
            // 调用系统下载处理
            downloadBySystem(url, title, split[split.length - 1]);
        }
    }

    // 强制返回上一个网页
    private void backUrl() {
        // 返回时 也保存浏览位置（主要针对动态加载的页面）
        saveY(webView.getUrl() == null ? currentUrl : webView.getUrl());

        List urlList = getUrls();
        if (urlList.size() > 0) {
            int index = urlList.lastIndexOf(currentUrl);
            String previousUrl = urlList.get(index - 1).toString();
            if (index - 2 >= 0) {
                // 解决重定向
                if (!MyApplication.getUrls().contains(previousUrl)) {
                    webView.goBackOrForward(-2);
                    return;
                }
            }
            webView.goBackOrForward(-1);

            // 标记前进或后退的网址
            flagUrl = previousUrl;
        } else {
            BrowserActivity.super.onBackPressed();
        }
    }

    //访问url
    private void loadUrl(String url) {
        saveY(webView.getUrl() == null ? currentUrl : webView.getUrl());
        if (webView != null) {
            webView.loadUrl(url);
        }
    }
    private void loadUrl(WebView view, String url) {
        saveY(webView.getUrl() == null ? currentUrl : webView.getUrl());
        view.loadUrl(url);
    }

    // 保存浏览位置（主要针对动态加载的页面）
    private void saveY(String url) {
        // 使用 JavaScript 获取滚动位置和动态内容状态
        webView.evaluateJavascript("(function() { return { scrollY: window.scrollY, content: document.body.innerHTML }; })();", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                try {
                    JSONObject state = new JSONObject(value);
                    Map<String, String> savaMap = new HashMap<>();
                    savaMap.put("scrollY", state.getInt("scrollY") + ""); // 保存滚动位置
                    savaMap.put("dynamicContentState", state.getString("content")); // 保存动态内容状态
                    savaMaps.put(url, savaMap);
                } catch (JSONException e) {
                    CommonUtils.saveLog("=======保存浏览位置异常：" + e.getMessage());
                }
            }
        });
    }

    // 恢复浏览位置（主要针对动态加载的页面）
    private void reloadY(WebView view, String url) {
        int time = 0;
        if (url.contains("前进")) {
            time = 1000;
            url = url.replace("前进","");
        }
        Map<String, String> savaMap = savaMaps.get(url);
        if (savaMap != null) {
            view.postDelayed(() -> {
                // 使用 JavaScript 恢复动态内容状态 、 恢复滚动位置
                String dynamicContentState = JSONObject.quote(savaMap.get("dynamicContentState"));
                int scrollY = Integer.parseInt(savaMap.get("scrollY"));
                view.evaluateJavascript("(function() { " +
                        "document.body.innerHTML = " + dynamicContentState + "; " +
                        "window.scrollTo(0, " + scrollY + "); " +
                        "})();", null);
            }, time);
        }
    }

    // 获取栈内存在的URL
    private List<String> getUrls() {
        if (webView != null) {
            urls.clear();
            // urls = new ArrayList<>();
            WebBackForwardList mWebBackForwardList = webView.copyBackForwardList();
            for (int i = 0; i < mWebBackForwardList.getSize(); i++) {
                String url = mWebBackForwardList.getItemAtIndex(i).getUrl();
                if (!urls.contains(url)) {
                    urls.add(url);
                }
            }
        }
        return urls;
    }

    private List<String> getRealUrls() {
        List<String> list = new ArrayList<>();
        if (webView != null) {
            WebBackForwardList mWebBackForwardList = webView.copyBackForwardList();
            for (int i = 0; i < mWebBackForwardList.getSize(); i++) {
                String url = mWebBackForwardList.getItemAtIndex(i).getUrl();
                list.add(url);
            }
        }
        return list;
    }

    // 调用系统下载处理
    private void downloadBySystem(String url, String disposition, String mimetype) {
        // 指定下载地址
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        // 允许媒体扫描，根据下载的类型加入相册、音乐等媒体库
        request.allowScanningByMediaScanner();
        // 设置通知的显示类型，下载进行时和下载完成后进行显示通知
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        // 设置下载的网络类型
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
        // 设置该下载记录在下载管理界面可见
        request.setVisibleInDownloadsUi(true);
        // 设置下载中通知栏标题
        String fileName = URLUtil.guessFileName(url, disposition, mimetype);
        request.setTitle(URLDecoder.decode(fileName));
        // 设置下载中通知栏描述 为 文件名
        request.setDescription("彼黍浏览器文件下载");
        // 获取SD卡的读取权限（Android6.0以上需获取相应权限）
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // 没有写入外部存储权限 时 申请该权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
        // 设置下载文件保存的路径
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        // 获取系统下载管理器
        final DownloadManager downloadManager = (DownloadManager) this.getSystemService(DOWNLOAD_SERVICE);
        // 添加一个下载任务
        long downLoadId = downloadManager.enqueue(request);
    }

    // 浏览器客户端
    WebViewClient myClient = new WebViewClient() {
        private String startUrl = "";

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            startUrl = url;
            url_box.setText(url);
            currentUrl = url; // 记录当前网址
            // System.out.println(MyApplication.getUrls());
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            MyApplication.setUrl(url); // 记录为历史网址 (onPageFinished方法不会有重定向的网页链接)
            super.onPageFinished(view, url);

            // 恢复浏览位置（主要针对动态加载的页面）
            if (!"".equals(flagUrl)) {
                reloadY(view, flagUrl);
                flagUrl = "";
            }

            // 去广告
            /*String fun="javascript:" +
                    "function hideStyle(){" +
                    //"try { document.getElementById('header').previousSibling.previousSibling.style.display=\"none\"; } catch (error) {}" +
                    "try { document.querySelector('a[href=\"https://WIP2000.com\"]').parentElement.parentElement.parentElement.style.display=\"none\"; } catch (error) {}" +
                    "try { document.querySelectorAll('.g').forEach(function(item) {item.style.display=\"none\"}); } catch (error) {}" +
                    "try { document.querySelectorAll('.ad').forEach(function(item) {item.style.display=\"none\"}); } catch (error) {}" +
                    "try { document.querySelectorAll('.ads').forEach(function(item) {item.style.display=\"none\"}); } catch (error) {}" +
                    "try { document.querySelectorAll('#divOyYSJd').forEach(function(item) {item.style.display=\"none\"}); } catch (error) {}" +
                    "try { for (var i = 0; i < 50; i++) { var dom = document.querySelector('.gotop').nextSibling; dom.remove(); } } catch (error) {}" +
                    "try { for (var i = 0; i < 50; i++) { var dom = document.querySelector('.gotop').parentNode.nextSibling; dom.remove(); } } catch (error) {}" +
                    "try { for (var i = 0; i < 50; i++) { var dom = document.querySelector('.gotop').parentNode.parentNode.nextSibling; dom.remove(); } } catch (error) {}" +
                    "}" +
                    "hideStyle();";
            view.loadUrl(fun);*/

            // 打印网页源码
            /*webView.evaluateJavascript("(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();", new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String html) {
                    String[] split = html.split("u003C");
                    for (String s : split) {
                        // 在这里处理获取到的HTML源码
                        System.out.println("<" + s.replace("\\t", "").replace("\\n", "").replace("\\", ""));
                    }
                }
            });*/
        }

        // 网址 过滤
        // @SuppressLint("NewApi") // 忽略强制网络策略: 在主线程中可以访问网络
        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString().toLowerCase();
            String urlOrg = request.getUrl().toString();

            // 广告过滤图片、js等资源文件
            if (isAd(url, urlOrg)) {
                return AdBlocker.createEmptyResource();
            }
            view.post(() -> {
                // 去广告
                String fun = "(function() {" +
                        "try { document.querySelector('a[href=\"https://WIP2000.com\"]').parentElement.parentElement.parentElement.style.display=\"none\"; } catch (error) {}" +
                        "try { document.querySelectorAll('.g').forEach(function(item) {item.style.display=\"none\"}); } catch (error) {}" +
                        "try { document.querySelectorAll('.ad').forEach(function(item) {item.style.display=\"none\"}); } catch (error) {}" +
                        "try { document.querySelectorAll('.ads').forEach(function(item) {item.style.display=\"none\"}); } catch (error) {}" +
                        "try { document.querySelectorAll('#divOyYSJd').forEach(function(item) {item.style.display=\"none\"}); } catch (error) {}" +
                        // "try { for (var i = 0; i < 50; i++) { var dom = document.querySelector('.gotop').nextSibling; dom.remove(); } } catch (error) {}" +
                        // "try { for (var i = 0; i < 50; i++) { var dom = document.querySelector('.gotop').parentNode.nextSibling; dom.remove(); } } catch (error) {}" +
                        // "try { for (var i = 0; i < 50; i++) { var dom = document.querySelector('.gotop').parentNode.parentNode.nextSibling; dom.remove(); } } catch (error) {}" +
                        // "try { document.querySelectorAll('[style=\"line-height:0px;position:relative!important;padding:0;\"]').forEach(function(item) {item.style.display=\"none\"}); } catch (error) {}" +
                        "})();";
                try {
                    webView.evaluateJavascript(fun, new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String ret) {}
                    });
                } catch (Throwable e) {}

                String name = url.substring(url.lastIndexOf("/") + 1);
                Message msg = Message.obtain();
                String[] arr = new String[]{view.getTitle(), urlOrg};
                msg.obj = arr;

                // 判断视频请求
                if (url.contains(".mp4") || url.contains(".avi") || url.contains(".mov") || url.contains(".mkv") ||
                        url.contains(".flv") || url.contains(".f4v") || url.contains(".rmvb") || url.endsWith(".m3u8")) {
                    // 非m3u8链接 或者 链接中只包含一个m3u8
                    if (!url.contains(".m3u8") || (!url.substring(url.indexOf(".m3u8")+5).contains(".m3u8") && !url.contains("?"))) {
                        msg.what = 2;
                        handler.sendMessage(msg);
                    }
                }
                // 判断音频请求
                else if (url.contains(".mp3") || url.contains(".wav") || url.contains(".ape") || url.contains(".flac")
                        || url.contains(".ogg") || url.contains(".aac") || url.contains(".wma")) {
                    msg.what = 3;
                    handler.sendMessage(msg);
                }
                // 判断无格式的情况
                else if (!name.contains(".")) {
                    msg.what = 9;
                    handler.sendMessage(msg);
                }
            });
            // super.shouldInterceptRequest(view, request).getData();
            return super.shouldInterceptRequest(view, request);
        }

        // 设置网页页面拦截
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            if (uri != null) {
                String cUrl = uri.toString();
                boolean rtn = true;
                if (cUrl.startsWith("https:") || cUrl.startsWith("http:")) {
                    // 防止统一网页重复加载
                    /*List<String> historyUrls = MyApplication.getUrls();
                    int size = historyUrls.size();
                    if (size >= 3) {
                        String url1 = historyUrls.get(size - 1).replace("https", "").replace("http", "");
                        String url2 = historyUrls.get(size - 2).replace("https", "").replace("http", "");
                        String url3 = historyUrls.get(size - 3).replace("https", "").replace("http", "");
                        if (url1.equals(url2) && url2.equals(url3)) {
                            return true;
                        }
                        if (url1.equals(url2) || url2.equals(url3)) {
                            String name1 = url1.substring(url1.lastIndexOf("/") + 1);
                            String name2 = url2.substring(url2.lastIndexOf("/") + 1);
                            String name3 = url3.substring(url3.lastIndexOf("/") + 1);
                            if (name1.equals(name2) && name2.equals(name3)) {
                                return true;
                            }
                        }
                    }*/

                    // 广告过滤
                    if (isAd(cUrl)) {
                        return true;
                    }

                    if (!startUrl.equals(cUrl)) { // 重定向时，防止系统记录重定向前的地址
                        /*if (cUrl.contains("https://m.baidu.com") && cUrl.contains("title=")) {
                            String[] split = cUrl.split("title=");
                            String txt = split[1];
                            if (txt.contains("&")) {
                                cUrl = "https://www.baidu.com/s?wd=" + txt.split("&")[0];
                            }
                        } else if (cUrl.contains("https://m.baidu.com") && cUrl.contains("word=")) {
                            String[] split = cUrl.split("word=");
                            String txt = split[1];
                            if (txt.contains("&")) {
                                cUrl = "https://www.baidu.com/s?wd=" + txt.split("&")[0];
                            }
                        }*/
                        loadUrl(view, cUrl);
                        rtn = false;
                    } else {
                        return super.shouldOverrideUrlLoading(view, request);
                    }
                }
                // 当返回false时，用 WebView 加载该 url （否则不加载，即不跳转该地址）
                return rtn;
            }
            return super.shouldOverrideUrlLoading(view, request);
        }

        // 页面跳转、前进、后退 会触发
        @Override
        public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
            super.doUpdateVisitedHistory(view, url, isReload);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            //super.onReceivedSslError(view, handler, error)
            switch (error.getPrimaryError()) {
                case SslError.SSL_INVALID: // 校验过程遇到了bug
                case SslError.SSL_UNTRUSTED: // 证书有问题
                    handler.proceed();
                default:
                    handler.cancel();
            }
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            String url = url_box.getText().toString();
            if (url.toLowerCase().startsWith("http://")) {
                loadUrl("https://" + url.substring(7));
            } else {
                super.onReceivedError(view, request, error);
            }
        }
    };

    // 判断广告链接
    private boolean isAd(String url) {
        boolean ad;
        if (!loadedUrls.containsKey(url)) {
            ad = AdBlocker.isAd(url);
            loadedUrls.put(url, ad);
        } else {
            ad = loadedUrls.get(url);
        }
        // gif图片多为广告，直接过滤了
        if (flagGif) {
            if (url.contains("?")) {
                url = url.split("\\?")[0];
            }
            ad = url.endsWith(".gif") || ad;
        }
        return ad;
    }

    // 判断广告链接
    private boolean isAd(String url, String oUrl) {
        boolean ad;
        if (!loadedUrls.containsKey(url)) {
            ad = AdBlocker.isAd(url);
            loadedUrls.put(url, ad);
        } else {
            ad = loadedUrls.get(url);
        }
        // gif图片多为广告，直接过滤了
        if (flagGif) {
            if (url.contains("?")) {
                url = url.split("\\?")[0];
            }
            url = url.replace(".js", "").trim();
            ad = url.endsWith(".gif") || ad;
            /*if (!ad && (url.endsWith("jpg") || url.endsWith("png") || url.endsWith(".webp"))) {
                String type = CommonUtils.getNetFileType(oUrl, 300);
                if (type != null && (type.contains("gif"))) {
                    return true;
                }
            }*/
        }
        return ad;
    }

    // 用于接受下载完成提示的广播接收者
    public class DownloadBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                DownloadManager downloadManager = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
                Uri uri = downloadManager.getUriForDownloadedFile(downloadId);
                if (uri != null) {
                    Message message = Message.obtain();
                    message.what = 1;
                    message.obj = "文件下载完成，保存于：  " + uri.toString();
                    handler.sendMessage(message);
                }
            }
        }
    }

    /*** 以下视频播放相关的方法 *---------------------------------------------------------*/
    public class myWebChromeClient extends WebChromeClient {

        private View xprogressvideo;

        // 播放网络视频时全屏会被调用的方法
        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            SysWindowUi.setStatusBarNavigationBarStyle(BrowserActivity.this, SysWindowUi.NO_STATE__NO_NAVIGATION);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            // 如果一个视图已经存在，那么立刻终止并新建一个
            if (xCustomView != null) {
                callback.onCustomViewHidden();
                return;
            }
            video_fullView.addView(view);
            xCustomView = view;
            xCustomViewCallback = callback;
            video_fullView.setVisibility(View.VISIBLE);
            webView.setVisibility(View.INVISIBLE);
            url_Layout.setVisibility(View.GONE);
            flag = false;
            btn_menu.setVisibility(View.GONE);
        }

        // 视频播放退出全屏会被调用的
        @Override
        public void onHideCustomView() {
            if (xCustomView == null)// 不是全屏播放状态
                return;
            SysWindowUi.setStatusBarNavigationBarStyle(BrowserActivity.this, SysWindowUi.NO_STATE__NO_STATE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            xCustomView.setVisibility(View.GONE);
            video_fullView.removeView(xCustomView);
            xCustomView = null;
            video_fullView.setVisibility(View.GONE);
            xCustomViewCallback.onCustomViewHidden();
            webView.setVisibility(View.VISIBLE);
            url_Layout.setVisibility(View.VISIBLE);
            flag = true;
            btn_menu.setVisibility(View.VISIBLE);
        }

        // 视频加载时进程loading
        @Override
        public View getVideoLoadingProgressView() {
            if (xprogressvideo == null) {
                LayoutInflater inflater = LayoutInflater.from(BrowserActivity.this);
                xprogressvideo = inflater.inflate(R.layout.video_loading_progress, null);
            }
            return xprogressvideo;
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            //System.out.println("=================" + newProgress);
            super.onProgressChanged(view, newProgress);
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            super.onConsoleMessage(consoleMessage);
            // 捕获JavaScript输出 console.log
            // System.out.println(consoleMessage.message() + "         ///////////////////////////");
            /*
            if ("iframe loaded".equals(consoleMessage.message())) {
                return true;
            }*/
            /*if (consoleMessage.message().contains("blob:")) {
                String url = consoleMessage.message().split("blob:")[1];
                // System.out.println("++++++++++++++++" + url);
                // 打印网页源码
                webView.evaluateJavascript("(function() { fetch('"+url+"').then(response => response.text()).then(text => { Android.postMessage(text); console.log(text); }).catch(error => console.error(error)); })();", null);
                return true;
            }*/
            return false;
        }
    }

    /**
     * 判断是否是全屏
     *
     * @return
     */
    public boolean inCustomView() {
        return (xCustomView != null);
    }


    /**
     * 全屏时按返加键执行退出全屏方法
     */
    public void hideCustomView() {
        xwebchromeclient.onHideCustomView();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        webView.resumeTimers();
        /**
         * 设置为横屏
         */
        if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
        webView.pauseTimers();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        video_fullView.removeAllViews();
        // 这个方法会 销毁所有的video和audio 包括js的所有正在运行的function
        webView.loadUrl("about:blank");
        webView.stopLoading();
        webView.setWebChromeClient(null);
        webView.setWebViewClient(null);
        webView.destroy();
        webView = null;
    }

    // 第一次授权提示拒绝后，再次授权
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case CommonUtils.STORAGE_PERMISSION_REQUEST_CODE:
                if (permissions.length > 0) {
                    for (int i = 0; i < permissions.length; i++) {
                        if (grantResults.length > 0 && grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            // 权限授权失败
                            if (ActivityCompat.shouldShowRequestPermissionRationale(BrowserActivity.this, permissions[i])) {
                                // 返回 true，Toast 提示
                                MyToast.getInstance(BrowserActivity.this, "无法访问该权限").show();
                            } else {
                                // 返回 false，需要显示对话框引导跳转到设置手动授权
                                FeetDialog feetDialog = new FeetDialog(BrowserActivity.this, "授权", "需前往授权后才能使用该功能", "授权", "取消");
                                feetDialog.setOnTouchListener(new FeetDialog.TouchListener() {
                                    @Override
                                    public void close() {
                                        feetDialog.dismiss();
                                    }
                                    @Override
                                    public void ok() {
                                        Intent intent = new Intent();
                                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        intent.setData(Uri.parse("package:" + BrowserActivity.this.getPackageName()));
                                        BrowserActivity.this.startActivityForResult(intent, 100);
                                        feetDialog.dismiss();
                                    }
                                });
                                feetDialog.show();
                            }
                            return;
                        }
                    }

                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

   /* @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) { // 和onBackPressed方法冲突了
            if (inCustomView()) {
                hideCustomView();
                return true;
            } else {
               webView.loadUrl("about:blank");
               // BrowserActivity.this.finish();
            }
        }
        return false;
    }*/

}
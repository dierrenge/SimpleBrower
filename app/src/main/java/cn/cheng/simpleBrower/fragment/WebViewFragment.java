package cn.cheng.simpleBrower.fragment;

import android.annotation.SuppressLint;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
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
import cn.cheng.simpleBrower.activity.BrowserActivity;
import cn.cheng.simpleBrower.activity.BrowserActivity2;
import cn.cheng.simpleBrower.bean.DownloadBean;
import cn.cheng.simpleBrower.bean.SysBean;
import cn.cheng.simpleBrower.custom.FeetDialog;
import cn.cheng.simpleBrower.custom.M3u8DownLoader;
import cn.cheng.simpleBrower.custom.MyToast;
import cn.cheng.simpleBrower.service.DownloadService;
import cn.cheng.simpleBrower.util.AdBlocker;
import cn.cheng.simpleBrower.util.AssetsReader;
import cn.cheng.simpleBrower.util.CommonUtils;
import cn.cheng.simpleBrower.util.SysWindowUi;

public class WebViewFragment extends Fragment {
    private LinearLayout viewViewLayout;
    private EditText url_box2;
    private ImageButton url_like2;
    private ImageButton url_flush2;
    private ImageButton url_stop;
    private ProgressBar viewViewProgressbar;
    private LinearLayout progressBg;
    private Handler progressHandler;
    private WebView webView;
    private CallListener callListener;
    private String jumpUrl;

    private CustomWebChromeClient xwebchromeclient;
    private FrameLayout video_fullView;// 全屏时视频加载view
    private View xCustomView;
    private WebChromeClient.CustomViewCallback xCustomViewCallback;

    private Handler handler; // 子线程与主线程通信
    private FeetDialog feetDialog;
    private boolean flag = true; // 是否展示检查下载的提示框
    private boolean flagGif = true; // 是否开启动图过滤

    private Map<String, Boolean> loadedUrls = new HashMap<>(); // 广告链接集

    // 无参构造函数
    public WebViewFragment() {
    }

    // 静态工厂方法
    public static WebViewFragment newInstance(String url) {
        WebViewFragment fragment = new WebViewFragment();
        Bundle args = new Bundle();
        args.putString("url", url);
        fragment.setArguments(args);
        return fragment;
    }

    // 回调接口
    public interface CallListener {
        // 视频全屏回调
        void onEnterFullScreen(View view, WebChromeClient.CustomViewCallback callback);
        // 视频退出全屏回调
        void onExitFullScreen();
        // 下载进度条回调
        LayoutInflater onProgressView();
        // 跳转网址回调
        void jump(String url);
        // 下载回调
        void downLoad();
        // 检测有下载的情况
        void sniffingDownload();
    }

    public void setFullScreenListener(CallListener listener) {
        this.callListener = listener;
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // 忽略强制网络策略: 在主线程中可以访问网络（这样会卡）
        // StrictMode.ThreadPolicy policy=new StrictMode.ThreadPolicy.Builder().permitAll().build();
        // StrictMode.setThreadPolicy(policy);

        View view = inflater.inflate(R.layout.fragment_webview, container, false);
        progressHandler = new Handler();
        viewViewLayout = view.findViewById(R.id.viewViewLayout);
        url_box2 = view.findViewById(R.id.url_box2);
        url_like2 = view.findViewById(R.id.url_like2);
        url_flush2 = view.findViewById(R.id.url_flush2);
        url_stop = view.findViewById(R.id.url_stop);
        viewViewProgressbar = view.findViewById(R.id.viewViewProgressbar);
        progressBg = view.findViewById(R.id.progressBg);
        webView = view.findViewById(R.id.webView);
        video_fullView = (FrameLayout) view.findViewById(R.id.video_fullView);

        // 获取 URL 参数
        Bundle args = getArguments();
        if (args != null) {
            jumpUrl = args.getString("url");
        }

        // 软件盘回车响应
        url_box2.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == KeyEvent.ACTION_DOWN || actionId == EditorInfo.IME_ACTION_DONE) {
                jumpLoading();
            }
            return true;
        });
        // 收藏
        url_like2.setOnClickListener(v -> {
            // 页面跳转后会用到的权限
            if (CommonUtils.hasStoragePermissions(requireContext())) {
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
                                String likeUrl = jumpUrl + "\n";
                                if (hasFile) {
                                    List<String> likes = new ArrayList<>();
                                    String line = null;
                                    while ((line = reader.readLine()) != null) {
                                        likes.add(line);
                                    }
                                    if (likes.size() > 0 && likes.contains(jumpUrl)) {
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
                CommonUtils.requestStoragePermissions(requireActivity(), null);
            }
        });
        // 刷新
        url_flush2.setOnClickListener(v -> {
            jumpLoading();
        });
        // 停止刷新
        url_stop.setOnClickListener(v -> {
            webView.stopLoading(); // 停止加载
            // webView.loadUrl("about:blank");  // 清空内容（可选）
            hideProgress();
        });

        SysBean sysBean = CommonUtils.readObjectFromLocal("SysSetting", SysBean.class);
        if (sysBean != null) {
            flagGif = sysBean.isFlagGif();
        }

        // 注册广告过滤器
        AdBlocker.init(requireContext());

        initWebView();

        // 多线程消息管理
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message message) {
                int what = message.what;
                if (what == 1) {
                    MyToast.getInstance(message.obj + "").show();
                } else if (what == 7) {
                    // 自动关闭空白页面
                    callListener.downLoad();
                } else {
                    String[] arr = (String[]) message.obj;
                    String title = arr[0];
                    String url = arr[1];

                    // 影音监测的情况
                    if (what != 4 || url.contains(".m3u8")) {
                        String title0 = title;
                        try {
                            title0 = URLDecoder.decode(title0, "utf-8");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        DownloadBean bean = new DownloadBean();
                        bean.setWhat(what);
                        bean.setTitle(title0);
                        bean.setFileType(arr[2]);
                        bean.setUrl(url);
                        MyApplication.setDownload(bean);
                        callListener.sniffingDownload();
                        return false;
                    }

                    // 弹框选择
                    if (flag && (feetDialog == null || !feetDialog.isShowing())) {
                        if (!url.contains(".m3u8")) {
                            String title2 = arr[2];
                            feetDialog = new FeetDialog(requireContext(), "下载", title2, "下载", "取消");
                            if (url.equals(jumpUrl)) {
                                callListener.downLoad(); // 下载的情况下自动关闭空白页面
                            }
                        } else {
                            feetDialog = new FeetDialog(requireContext());
                        }
                        feetDialog.setOnTouchListener(new FeetDialog.TouchListener() {
                            @Override
                            public void close() {
                                flag = true;
                                feetDialog.dismiss();
                            }

                            @Override
                            public void ok(String txt) {
                                flag = true;
                                download(url, arr.length >= 3 && arr[2].contains(" / ") ? txt : title, what);
                                feetDialog.dismiss();
                            }
                        });
                        flag = false;
                        feetDialog.show();
                    }

                }
                return false;
            }
        });

        if (jumpUrl != null) {
            url_box2.setText(jumpUrl);
            webView.loadUrl(jumpUrl);
        }

        return view;
    }

    // 下载
    private void download(String url, String titleO, int what) {
        if (!CommonUtils.requestNotificationPermissions(requireActivity())) return; // 通知
        String title = titleO;
        try {
            title = URLDecoder.decode(titleO, "utf-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        Intent intent = new Intent(requireContext(), DownloadService.class);
        intent.putExtra("what", what);
        intent.putExtra("url", url);
        intent.putExtra("title", title.length() > 30 ? title.substring(0, 24) + "···" + title.substring(title.length() - 6) : title);
        requireContext().startService(intent);
    }

    // 刷新网址栏
    private void jumpLoading() {
        String webInfo = url_box2.getText().toString();
        if (!CommonUtils.isUrl(webInfo)) {
            webInfo = "https://www.baidu.com/s?wd=" + webInfo;
        } else {
            if (!webInfo.toLowerCase().startsWith("http://") && !webInfo.toLowerCase().startsWith("https://")) {
                webInfo = "http://" + webInfo;
            }
        }
        webView.clearHistory();
        webView.loadUrl(webInfo);
    }

    private void initWebView() {
        // 初始化 WebView 配置
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
        // webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK); // Android 13 平替 setAppCacheEnabled(true)
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE); // 不缓存，主要有的网站登录用到cookie，设置LOAD_CACHE_ELSE_NETWORK导致无法登录
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

        // 设置下载监听
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String disposition, String mimetype, long length) {
                getDownloadName(map -> {
                    // 获取下载文件名
                    String name = map.get(url);
                    if (StringUtils.isEmpty(name) || !name.contains(".")) {
                        try {
                            name = URLUtil.guessFileName(url, disposition, mimetype);
                        } catch (Exception ignored) {}
                        if (StringUtils.isEmpty(name) || name.endsWith(".bin")) {
                            name = CommonUtils.getUrlName(url);
                        }
                    }
                    try {
                        name = URLDecoder.decode(name, "utf-8");
                    } catch (Exception ignored) {}
                    // 会用到的权限
                    if (!CommonUtils.hasStoragePermissions(requireContext())) {
                        CommonUtils.requestStoragePermissions(requireActivity(), null);
                        if (!name.contains(".html;") && !url.contains(".m3u8")) {
                            // 记录点击下载的链接url
                            MyApplication.setClickDownloadUrl(url);
                            Message msg = Message.obtain();
                            msg.what = 7;
                            handler.sendMessage(msg);
                        }
                        return;
                    }
                    // 调用系统下载处理
                    // downloadBySystem(url, disposition, mimetype);
                    // 使用自定义下载
                    String finalName = name;
                    new Thread(() -> {
                        String title = finalName;
                        // title = title.length() > 30 ? title.substring(0, 24) + "···" + title.substring(title.length() - 6) : title;
                        title = M3u8DownLoader.getUrlContentFileSize(url, title);

                        if (!title.contains(".html;")) {
                            // 记录点击下载的链接url
                            MyApplication.setClickDownloadUrl(url);
                            String[] arr = new String[]{finalName, url, title};
                            Message msg = Message.obtain();
                            msg.obj = arr;
                            msg.what = 4;
                            handler.sendMessage(msg);
                        }
                    }).start();
                });
            }
        });

        // 设置 WebViewClient 和 WebChromeClient
        webView.setWebViewClient(myClient);
        xwebchromeclient = new CustomWebChromeClient();
        webView.setWebChromeClient(new CustomWebChromeClient());
    }

    // 检测页面是否空白
    public void checkIfPageIsEmpty(ValueCallback<Boolean> callback) {
        webView.evaluateJavascript("(function() { " +
                "return document.body ? document.body.innerHTML : null; " +
                "})();", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                boolean isEmpty = value == null || "null".equals(value) || !value.contains(">");
                // if (!isEmpty) System.out.println(jumpUrl + "**************************\n" + value);
                callback.onReceiveValue(isEmpty);
            }
        });
    }

    // 获取页面a标签下载文件名
    public void getDownloadName(ValueCallback<HashMap<String, String>> callback) {
        webView.evaluateJavascript("(function() { " +
                "var aList = document.querySelectorAll('a[download]');" +
                "var list = [];" +
                "for (var i = 0; i < aList.length; i++) {" +
                "  var href = aList[i].href;" +
                "  var download = aList[i].download;" +
                "  if (href && download) {" +
                "    list.push({'href':href, 'download':download});" +
                "  }" +
                "}" +
                "return list; " +
                "})();", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                HashMap<String, String> map = new HashMap<>();
                if (value != null && value.startsWith("[") && value.endsWith("]")) {
                    try {
                        JSONArray jsonArray = new JSONArray(value);
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            map.put(jsonObject.getString("href"), jsonObject.getString("download"));
                        }
                    } catch (JSONException e) {
                        CommonUtils.saveLog(value + "\ngetDownloadName=======" + e.getMessage());
                    }
                }
                // if (!isEmpty) System.out.println(jumpUrl + "**************************\n" + value);
                callback.onReceiveValue(map);
            }
        });
    }

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
            url = url.replace(".js", "").trim();
            ad = url.endsWith(".gif") || ad;
        }
        return ad;
    }

    // 自定义 WebViewClient（处理页面加载）
    WebViewClient myClient = new WebViewClient() {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            jumpUrl = url;
            url_box2.setText(url);
            url_box2.setEnabled(false);
            progressBg.setVisibility(View.GONE);
            viewViewProgressbar.setVisibility(View.VISIBLE);
            url_flush2.setVisibility(View.GONE);
            url_stop.setVisibility(View.VISIBLE);
            // 添加超时检测（例如 60 秒）
            progressHandler.postDelayed(myRunnable, 60000);
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            MyApplication.setUrl(url); // 记录为历史网址
            hideProgress();
            super.onPageFinished(view, url);

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
        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString().toLowerCase();
            String urlOrg = request.getUrl().toString();

            // 广告过滤图片、js等资源文件
            if (isAd(url)) {
                return AdBlocker.createEmptyResource();
            }

            // 检查 Activity 状态
            if (getActivity() != null && !isDetached()) {
                view.post(() -> {
                    // 去广告
                    String fun = "(function() {" +
                            "try { document.querySelector('a[href=\"https://WIP2000.com\"]').parentElement.parentElement.parentElement.style.display=\"none\"; } catch (error) {}" +
                            "try { document.querySelectorAll('.g').forEach(function(item) {item.style.display=\"none\"}); } catch (error) {}" +
                            "try { document.querySelectorAll('.ad').forEach(function(item) {item.style.display=\"none\"}); } catch (error) {}" +
                            "try { document.querySelectorAll('.ads').forEach(function(item) {item.style.display=\"none\"}); } catch (error) {}" +
                            "try { document.querySelectorAll('#divOyYSJd').forEach(function(item) {item.style.display=\"none\"}); } catch (error) {}" +
                            "})();";
                    try {
                        webView.evaluateJavascript(fun, new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String ret) {}
                        });
                    } catch (Throwable e) {}

                    // 判断格式
                    String format = CommonUtils.getUrlFormat(url);
                    Message msg = Message.obtain();
                    if (StringUtils.isNotEmpty(format)) {// 有格式的情况
                        // 判断视频请求
                        if (format.equals(".mp4") || format.equals(".avi") || format.equals(".mov") || format.equals(".mkv") ||
                                format.equals(".flv") || format.equals(".f4v") || format.equals(".rmvb") || format.equals(".m3u8")) {
                            msg.what = 2;
                        }
                        // 判断音频请求
                        else if (format.equals(".mp3") || format.equals(".wav") || format.equals(".ape") || format.equals(".flac")
                                || format.equals(".ogg") || format.equals(".aac") || format.equals(".wma")) {
                            msg.what = 3;
                        }
                        if (msg.what != 0) {
                            msg.obj = new String[]{view.getTitle(), urlOrg, format};
                            handler.sendMessage(msg);
                        }
                    } else { // 无格式的情况
                        new Thread(() -> {
                            String fileType = CommonUtils.getNetFileType(url, 1000);
                            if (fileType != null) {
                                if (fileType.contains("/")) {
                                    fileType = fileType.substring(fileType.lastIndexOf("/") + 1);
                                }
                                if (fileType.contains("?")) {
                                    fileType = fileType.substring(0, fileType.indexOf("?"));
                                }
                                if (fileType.contains(";")) {
                                    fileType = fileType.substring(0, fileType.indexOf(";"));
                                }
                                // System.out.println(url + "*******************1***********************" + fileType);
                                // 影音文件格式
                                List<String> formats = AssetsReader.getList("audioVideo.txt");
                                fileType = "." + fileType;
                                if (formats.contains(fileType)) {
                                    String[] arr = new String[]{view.getTitle(), urlOrg, fileType};
                                    msg.obj = arr;
                                    msg.what = 9;
                                    handler.sendMessage(msg);
                                }
                            }
                        }).start();
                    }
                });
            }
            return super.shouldInterceptRequest(view, request);
        }

        // 设置网页页面拦截
        // @SuppressLint("NewApi") // 忽略强制网络策略: 在主线程中可以访问网络（这样会卡）
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            if (!url.startsWith("https:") && !url.startsWith("http:")) {
                return true;
            }
            // 广告过滤
            if (isAd(url)) {
                return true;
            }
            // 仅处理用户触发的 非重定向 主框架请求
            if ((request.getRequestHeaders() == null || request.getRequestHeaders().get("Referer") == null)
                    && !request.isRedirect() && request.isForMainFrame()) {
                if (callListener != null) {
                    // 暂停webView
                     new Handler().postDelayed(() -> {
                         view.stopLoading();
                     }, 200);
                     view.onPause();
                     view.pauseTimers();
                    // 跳转
                    callListener.jump(url);
                    // return true; // 拦截跳转，由 Activity 处理
                    return false; // 不拦截跳转，但手动干预跳转方式，其余如cookie等底层逻辑还是WebView自行处理
                }
            }
            return false; // 其他情况由 WebView 自行处理
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
            hideProgress();
            super.onReceivedError(view, request, error);
        }
    };

    // 自定义 WebChromeClient（处理全屏事件）
    private class CustomWebChromeClient extends WebChromeClient {

        private View xprogressvideo;

        // 播放网络视频时全屏会被调用的方法
        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            callListener.onEnterFullScreen(view, callback);
            // 如果一个视图已经存在，那么立刻终止并新建一个
            if (xCustomView != null) {
                callback.onCustomViewHidden();
                return;
            }
            video_fullView.addView(view);
            xCustomView = view;
            xCustomViewCallback = callback;
            video_fullView.setVisibility(View.VISIBLE);
            viewViewLayout.setVisibility(View.INVISIBLE);
            flag = false;
        }

        // 视频播放退出全屏会被调用的
        @Override
        public void onHideCustomView() {
            if (xCustomView == null)// 不是全屏播放状态
                return;
            callListener.onExitFullScreen();
            xCustomView.setVisibility(View.GONE);
            video_fullView.removeView(xCustomView);
            xCustomView = null;
            video_fullView.setVisibility(View.GONE);
            xCustomViewCallback.onCustomViewHidden();
            viewViewLayout.setVisibility(View.VISIBLE);
            flag = true;
        }

        // 视频加载时进程loading
        @Override
        public View getVideoLoadingProgressView() {
            if (xprogressvideo == null) {
                LayoutInflater inflater = callListener.onProgressView();
                xprogressvideo = inflater.inflate(R.layout.video_loading_progress, null);
            }
            return xprogressvideo;
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (newProgress >= 100) {
                hideProgress();
            } else {
                viewViewProgressbar.setProgress(newProgress);
            }
            super.onProgressChanged(view, newProgress);
        }

        // 更新 Activity 标题
        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            // setTitle(title);
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            super.onConsoleMessage(consoleMessage);
            return false;
        }
    }

    // 超时处理进度条
    Runnable myRunnable = new Runnable() {
        @Override
        public void run() {
            if (viewViewProgressbar.getVisibility() == View.VISIBLE) {
                if (webView != null) {
                    webView.stopLoading();
                }
                hideProgress();
                MyToast.getInstance("加载超时").show();
            }
        }
    };

    // 隐藏进度条等
    private void hideProgress() {
        viewViewProgressbar.setVisibility(View.GONE);
        progressBg.setVisibility(View.VISIBLE);
        url_flush2.setVisibility(View.VISIBLE);
        url_stop.setVisibility(View.GONE);
        url_box2.setEnabled(true);
        progressHandler.removeCallbacks(myRunnable);
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
    }

    @Override
    public void onResume() {
        super.onResume();
        // 暂停webView
        webView.onResume();
        webView.resumeTimers();
    }

    @Override
    public void onPause() {
        super.onPause();
        // 激活webView
        webView.onPause();
        webView.pauseTimers();
    }

    // 释放 WebView 资源
    @Override
    public void onDestroy() {
        super.onDestroy();
        video_fullView.removeAllViews();
        if (webView != null) {
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }
        progressHandler.removeCallbacks(myRunnable);
    }

    public WebView getWebView() {
        return webView;
    }
}

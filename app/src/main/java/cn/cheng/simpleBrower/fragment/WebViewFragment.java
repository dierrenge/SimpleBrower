package cn.cheng.simpleBrower.fragment;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONObject;

import java.lang.reflect.Method;

import cn.cheng.simpleBrower.MyApplication;
import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.activity.BrowserActivity;
import cn.cheng.simpleBrower.activity.BrowserActivity2;
import cn.cheng.simpleBrower.util.CommonUtils;
import cn.cheng.simpleBrower.util.SysWindowUi;

public class WebViewFragment extends Fragment {
    private WebView webView;
    private CallListener callListener;
    private String jumpUrl;

    private CustomWebChromeClient xwebchromeclient;
    private FrameLayout video_fullView;// 全屏时视频加载view
    private View xCustomView;
    private WebChromeClient.CustomViewCallback xCustomViewCallback;

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
        void onEnterFullScreen(View view, WebChromeClient.CustomViewCallback callback);
        void onExitFullScreen();
        LayoutInflater onProgressView();
        // 跳转网址
        void jump(String url);
    }

    public void setFullScreenListener(CallListener listener) {
        this.callListener = listener;
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_webview, container, false);
        webView = view.findViewById(R.id.webView);
        video_fullView = (FrameLayout) view.findViewById(R.id.video_fullView);

        // 获取 URL 参数
        Bundle args = getArguments();
        if (args != null) {
            jumpUrl = args.getString("url");
            if (jumpUrl != null) {
                webView.loadUrl(jumpUrl);
            }
        }

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

        // 设置 WebViewClient 和 WebChromeClient
        webView.setWebViewClient(myClient);
        xwebchromeclient = new CustomWebChromeClient();
        webView.setWebChromeClient(new CustomWebChromeClient());

        return view;
    }

    // 检测页面是否空白
    public void checkIfPageIsEmpty(ValueCallback<Boolean> callback) {
        webView.evaluateJavascript("(function() { " +
                "return document.body ? document.body.innerHTML : null; " +
                "})();", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                boolean isEmpty = value == null || "null".equals(value) || !value.contains(">");
                // if (!isEmpty) System.out.println(webView.getUrl() + "**************************\n" + value);
                callback.onReceiveValue(isEmpty);
            }
        });
    }

    // 自定义 WebViewClient（处理页面加载）
    WebViewClient myClient = new WebViewClient() {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            MyApplication.setUrl(url); // 记录为历史网址
            super.onPageFinished(view, url);
        }

        // 网址 过滤
        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
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

            });
            return super.shouldInterceptRequest(view, request);
        }

        // 设置网页页面拦截
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            if (!url.startsWith("https:") && !url.startsWith("http:")) {
                return true;
            }
            // 仅处理用户触发的 非重定向 主框架请求
            if ((request.getRequestHeaders() == null || request.getRequestHeaders().get("Referer") == null)
                    && !request.isRedirect() && request.isForMainFrame()) {
                if (callListener != null) {
                    callListener.jump(url);
                    return true; // 拦截跳转，由 Activity 处理
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
            webView.setVisibility(View.INVISIBLE);
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
            webView.setVisibility(View.VISIBLE);
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
            super.onProgressChanged(view, newProgress);
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            super.onConsoleMessage(consoleMessage);
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
    }

    // 释放 WebView 资源
    @Override
    public void onDestroy() {
        super.onDestroy();
        video_fullView.removeAllViews();
        // webView.stopLoading();
        webView.setWebChromeClient(null);
        webView.setWebViewClient(null);
        webView.destroy();
        webView = null;
    }

    public WebView getWebView() {
        return webView;
    }
}

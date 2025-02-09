package cn.cheng.simpleBrower.fragment;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.lang.reflect.Method;

import cn.cheng.simpleBrower.MyApplication;
import cn.cheng.simpleBrower.R;

public class WebViewFragment extends Fragment {
    private WebView webView;
    private CallListener callListener;

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

        // 获取 URL 参数
        Bundle args = getArguments();
        if (args != null) {
            String url = args.getString("url");
            if (url != null) {
                webView.loadUrl(url);
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
        webView.setWebChromeClient(new CustomWebChromeClient());

        return view;
    }

    // 自定义 WebViewClient（处理页面加载错误）
    WebViewClient myClient = new WebViewClient() {
        private String startUrl = "";

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            startUrl = url;
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            MyApplication.setUrl(url); // 记录为历史网址 (onPageFinished方法不会有重定向的网页链接)
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
            Uri uri = request.getUrl();
            if (uri != null) {
                String cUrl = uri.toString();
                boolean rtn = true;
                if (cUrl.startsWith("https:") || cUrl.startsWith("http:")) {

                    if (!startUrl.equals(cUrl)) { // 重定向时，防止系统记录重定向前的地址
                        if (callListener != null) {
                            callListener.jump(cUrl);
                        }
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
        private View customView;
        private WebChromeClient.CustomViewCallback customViewCallback;

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            if (callListener != null) {
                callListener.onEnterFullScreen(view, callback);
            }
        }

        @Override
        public void onHideCustomView() {
            if (callListener != null) {
                callListener.onExitFullScreen();
            }
        }
    }

    // 释放 WebView 资源
    @Override
    public void onDestroyView() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroyView();
    }

    public WebView getWebView() {
        return webView;
    }
}

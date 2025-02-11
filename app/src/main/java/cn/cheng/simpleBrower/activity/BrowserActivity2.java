package cn.cheng.simpleBrower.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import java.util.Stack;

import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.fragment.WebViewFragment;
import cn.cheng.simpleBrower.util.CommonUtils;
import cn.cheng.simpleBrower.util.SysWindowUi;

public class BrowserActivity2 extends AppCompatActivity implements WebViewFragment.CallListener {
    private static final int MAX_HISTORY_SIZE = 20; // 最大历史记录数量
    private Stack<WebViewFragment> backStack = new Stack<>();
    private Stack<WebViewFragment> forwardStack = new Stack<>();

    private LinearLayout btn_menu2;
    private Button btnForward;
    private Button btnBack;

    private static String currentUrl; // 当前网页网址
    private WebViewFragment preFragment; // 上一个fragment

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置默认导航栏、状态栏样式
        SysWindowUi.setStatusBarNavigationBarStyle(this, SysWindowUi.NO_STATE__NO_STATE);

        setContentView(R.layout.activity_brower2);

        btn_menu2 = findViewById(R.id.btn_menu2);
        btnBack = findViewById(R.id.btnBack);
        btnForward = findViewById(R.id.btnForward);
        btnBack.setOnClickListener(v -> {
            goBackOrForward("back", b -> {
                onBack();
            });
        });
        btnForward.setOnClickListener(v -> {
            goBackOrForward("forward", b -> {
                onForward();
            });
        });

        // 设置此activity可用于打开 网络链接
        Intent intent = this.getIntent();
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

        // 加载初始页面
        navigateTo(WebViewFragment.newInstance(currentUrl));
    }

    // 跳转到新页面
    public void navigateTo(WebViewFragment fragment) {
        if (backStack.size() > 0) {
            preFragment = backStack.peek();
        }

        // 内存优化：限制历史栈大小
        if (backStack.size() >= MAX_HISTORY_SIZE) {
            WebViewFragment oldest = backStack.remove(0);
            if (oldest.getWebView() != null) {
                oldest.getWebView().destroy();
            }
            getSupportFragmentManager().beginTransaction().remove(oldest).commit();
        }

        // 添加新 Fragment
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragmentContainer, fragment)
                .hide(fragment) // 先隐藏新 Fragment
                .commit();

        forwardStack.clear(); // 清空前进栈
        backStack.push(fragment);
        fragment.setFullScreenListener(this);
        showFragment(fragment);
    }

    // 返回操作
    public void onBack() {
        if (backStack.size() > 1) {
            WebViewFragment fragment = backStack.pop();
            // System.out.println("*************************" + fragment.getWebView().getUrl());
            WebViewFragment backFragment = backStack.peek();
            // System.out.println("*************************" + backFragment.getWebView().getUrl());
            backFragment.checkIfPageIsEmpty(isEmpty -> {
                // 递归处理空白页
                if (isEmpty) {
                    // 隐藏上一个fragment
                    if (fragment != null) {
                        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                        fragmentTransaction.hide(fragment);
                        fragmentTransaction.commit();
                    }
                    onBack();
                } else {
                    forwardStack.push(preFragment);
                    // System.out.println("*************************" + backFragment.getWebView().getUrl());
                    showFragment(backFragment);
                }
            });
        } else {
            BrowserActivity2.super.onBackPressed();
        }
    }

    // 前进操作
    public void onForward() {
        if (!forwardStack.isEmpty()) {
            WebViewFragment next = forwardStack.pop();
            // System.out.println("+++++++++++++++++++++++++" + next.getWebView().getUrl());
            backStack.push(next);
            showFragment(next);
        }
    }

    // 兼顾webView的前进返回
    private void goBackOrForward(String type, ValueCallback<Boolean> callback) {
        preFragment = backStack.peek();
        WebView webView = preFragment.getWebView();
        // System.out.println("=================================" + webView.getUrl());
        if ("back".equals(type)) {
            if (webView.canGoBack()) {
                forwardStack.clear(); // webView能返回说明会使用其自身的前进栈，故清空我们的fragment前进栈
                webView.goBack();
            } else {
                callback.onReceiveValue(true);
            }
        } else if ("forward".equals(type)) {
            if (webView.canGoForward()) {
                webView.goForward();
            } else {
                callback.onReceiveValue(true);
            }
        }
    }

    // 显示 Fragment
    private void showFragment(WebViewFragment fragment) {
        try {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            if (preFragment != null) {
                fragmentTransaction.hide(preFragment);
            }
            fragmentTransaction.show(fragment).commit();
        } catch (Exception e) {
            CommonUtils.saveLog("===========showFragment===========" + e.getMessage());
        }
    }

    // 全屏播放处理
    @Override
    public void onEnterFullScreen(View view, WebChromeClient.CustomViewCallback callback) {
        SysWindowUi.setStatusBarNavigationBarStyle(BrowserActivity2.this, SysWindowUi.NO_STATE__NO_NAVIGATION);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        btn_menu2.setVisibility(View.GONE);
    }

    @Override
    public void onExitFullScreen() {
        SysWindowUi.setStatusBarNavigationBarStyle(BrowserActivity2.this, SysWindowUi.NO_STATE__NO_STATE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        btn_menu2.setVisibility(View.VISIBLE);
    }

    @Override
    public LayoutInflater onProgressView() {
        LayoutInflater inflater = LayoutInflater.from(BrowserActivity2.this);
        return inflater;
    }

    @Override
    public void jump(String url) {
        currentUrl = url;
        navigateTo(WebViewFragment.newInstance(currentUrl));
    }

    // 处理物理返回键
    @Override
    public void onBackPressed() {
        if (backStack.size() > 1) {
            preFragment = backStack.peek();
            if (preFragment.inCustomView()) {
                preFragment.hideCustomView();
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
            goBackOrForward("back", b -> {
                onBack();
            });
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        /**
         * 设置为横屏
         */
        if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    // 内存优化：销毁不可见 Fragment
    @Override
    protected void onDestroy() {
        for (WebViewFragment fragment : backStack) {
            if (fragment.getWebView() != null) {
                fragment.getWebView().destroy();
            }
        }
        for (WebViewFragment fragment : forwardStack) {
            if (fragment.getWebView() != null) {
                fragment.getWebView().destroy();
            }
        }
        super.onDestroy();
    }
}
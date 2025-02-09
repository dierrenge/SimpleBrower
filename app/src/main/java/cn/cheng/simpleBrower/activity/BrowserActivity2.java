package cn.cheng.simpleBrower.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Stack;

import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.fragment.WebViewFragment;
import cn.cheng.simpleBrower.util.CommonUtils;
import cn.cheng.simpleBrower.util.SysWindowUi;

public class BrowserActivity2 extends AppCompatActivity implements WebViewFragment.CallListener {
    private static final int MAX_HISTORY_SIZE = 10; // 最大历史记录数量
    private Stack<WebViewFragment> backStack = new Stack<>();
    private Stack<WebViewFragment> forwardStack = new Stack<>();
    private FrameLayout fullScreenContainer;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    private Button btnForward;
    private Button btnBack;

    private static String currentUrl; // 当前网页网址

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置默认导航栏、状态栏样式
        SysWindowUi.setStatusBarNavigationBarStyle(this, SysWindowUi.NO_STATE__NO_STATE);

        setContentView(R.layout.activity_brower2);

        btnBack = findViewById(R.id.btnBack);
        btnForward = findViewById(R.id.btnForward);
        btnBack.setOnClickListener(v -> {
            onBack();
        });
        btnForward.setOnClickListener(v -> {
            onForward();
        });

        // 初始化全屏容器
        fullScreenContainer = new FrameLayout(this);
        fullScreenContainer.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

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
        // 内存优化：限制历史栈大小
        if (backStack.size() >= MAX_HISTORY_SIZE) {
            WebViewFragment oldest = backStack.remove(0);
            if (oldest.getWebView() != null) {
                oldest.getWebView().destroy();
            }
        }

        forwardStack.clear(); // 清空前进栈
        backStack.push(fragment);
        fragment.setFullScreenListener(this);
        showFragment(fragment);
    }

    // 返回操作
    public void onBack() {
        System.out.println("=============onBack============" + backStack.size() );
        if (backStack.size() > 1) {
            WebViewFragment current = backStack.pop();
            forwardStack.push(current);
            showFragment(backStack.peek());
        }
    }

    // 前进操作
    public void onForward() {
        if (!forwardStack.isEmpty()) {
            WebViewFragment next = forwardStack.pop();
            backStack.push(next);
            showFragment(next);
        }
    }

    // 显示 Fragment
    private void showFragment(WebViewFragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    // 全屏播放处理
    @Override
    public void onEnterFullScreen(View view, WebChromeClient.CustomViewCallback callback) {
        if (customView != null) {
            callback.onCustomViewHidden();
            return;
        }

        // 隐藏系统 UI
        getSupportActionBar().hide();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 添加全屏视图
        customView = view;
        customViewCallback = callback;
        fullScreenContainer.addView(customView);
        ((ViewGroup) getWindow().getDecorView()).addView(fullScreenContainer);

        // 锁定横屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Override
    public void onExitFullScreen() {
        if (customView == null) return;

        // 恢复系统 UI
        getSupportActionBar().show();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 移除全屏视图
        fullScreenContainer.removeView(customView);
        ((ViewGroup) getWindow().getDecorView()).removeView(fullScreenContainer);
        customViewCallback.onCustomViewHidden();
        customView = null;
        customViewCallback = null;

        // 恢复竖屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    public void jump(String url) {
        currentUrl = url;
        navigateTo(WebViewFragment.newInstance(currentUrl));
    }

    // 处理物理返回键
    @Override
    public void onBackPressed() {
        if (customView != null) {
            onExitFullScreen();
        } else if (backStack.size() > 1) {
            onBack();
        } else {
            super.onBackPressed();
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
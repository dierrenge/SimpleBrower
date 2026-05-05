package cn.cheng.biShu.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
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
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import java.util.Stack;

import cn.cheng.biShu.MyApplication;
import cn.cheng.biShu.R;
import cn.cheng.biShu.custom.DownloadListDialog;
import cn.cheng.biShu.custom.MoreFunctionDialog;
import cn.cheng.biShu.fragment.WebViewFragment;
import cn.cheng.biShu.util.CommonUtils;
import cn.cheng.biShu.util.SysWindowUi;

public class BrowserActivity2 extends AppCompatActivity {
    private Stack<WebViewFragment> backStack = new Stack<>();
    private Stack<WebViewFragment> forwardStack = new Stack<>();

    private LinearLayout layout_bg;
    private LinearLayout btn_menu2;
    private ImageButton btnForward;
    private ImageButton btnBack;
    private ImageButton btnHome;
    private LinearLayout btnMonitorL;
    private ImageButton btnMonitor;
    private ImageButton btnMore;
    private LinearLayout btnMoreL;

    private static String currentUrl; // 当前网页网址
    private WebViewFragment preFragment; // 上一个fragment

    private ActivityResultLauncher<Intent> resultLauncher; // 授权回调
    private ValueCallback<Boolean> callback; // 授权回调函数

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // 保存当前 URL 和栈状态
        outState.putString("CURRENT_URL", currentUrl);
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 恢复状态
        if (savedInstanceState != null) {
            currentUrl = savedInstanceState.getString("CURRENT_URL");
        }
        // 设置默认导航栏、状态栏样式
        SysWindowUi.setStatusBarNavigationBarStyle(this, SysWindowUi.NO_STATE__NO_STATE);

        // 注册权限请求的返回监听
        initResultLauncherLauncher();

        setContentView(R.layout.activity_brower2);

        layout_bg = findViewById(R.id.layout_bg);
        btn_menu2 = findViewById(R.id.btn_menu2);
        btnBack = findViewById(R.id.btnBack);
        btnForward = findViewById(R.id.btnForward);
        btnHome = findViewById(R.id.btnHome);
        btnMonitorL = findViewById(R.id.btnMonitorL);
        btnMonitor = findViewById(R.id.btnMonitor);
        btnMore = findViewById(R.id.btnMore);
        btnMoreL = findViewById(R.id.btnMoreL);
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
        btnHome.setOnClickListener(v -> {
            BrowserActivity2.super.onBackPressed();
        });
        btnMonitor.setOnClickListener(v -> {
            DownloadListDialog dialog = new DownloadListDialog(BrowserActivity2.this, resultLauncher);
            dialog.setOnCallListener(new DownloadListDialog.CallListener() {
                @Override
                public void deleteAll() {
                    btnMonitor.setBackgroundResource(R.drawable.btn_monitor);
                }
                @Override
                public void setCallback(ValueCallback<Boolean> callback) {
                    BrowserActivity2.this.callback = callback;
                }
                @Override
                public void setBackground(boolean flag) {
                    if (flag) {
                        layout_bg.setVisibility(View.VISIBLE);
                    } else {
                        layout_bg.setVisibility(View.GONE);
                    }
                }
            });
            dialog.show();
        });
        btnMonitorL.setOnClickListener(v -> btnMonitor.callOnClick());
        btnMore.setOnClickListener(v -> {
            MoreFunctionDialog dialog = new MoreFunctionDialog(BrowserActivity2.this);
            dialog.setCallListener(flag -> {
                if (flag) {
                    layout_bg.setVisibility(View.VISIBLE);
                } else {
                    layout_bg.setVisibility(View.GONE);
                }
            });
            dialog.show();
        });
        btnMoreL.setOnClickListener(v -> {
            MoreFunctionDialog dialog = new MoreFunctionDialog(BrowserActivity2.this);
            dialog.show();
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
        // CommonUtils.saveLog("打开方式-网络链接：" + currentUrl);

        // 加载初始页面
        navigateTo(WebViewFragment.newInstance(currentUrl));
    }

    // 注册权限请求的返回监听
    private void initResultLauncherLauncher() {
        // 注册权限请求的返回监听
        resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> { // 授权返回后的处理
                    if (callback != null) {
                        callback.onReceiveValue(true);
                    }
                }
        );
    }

    // 跳转到新页面
    public void navigateTo(WebViewFragment fragment) {
        // 检查 Activity 状态
        if (isFinishing() || isDestroyed()) return;

        if (backStack.size() > 0) {
            preFragment = backStack.peek();
        }

        // 内存优化：限制历史栈大小
        double availableMemoryRatio = CommonUtils.getAvailableMemoryRatio(this.getApplicationContext());
        if (backStack.size() > 3 && availableMemoryRatio < MyApplication.MIN_AVL_MEM_PCT) {
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
        fragment.setFullScreenListener(callListener); // 设置回调监听
        fragment.setResultLauncher(resultLauncher); // 授权回调
        showFragment(fragment);
    }

    // 返回操作
    public void onBack() {
        /*String str = "";
        for (int i = backStack.size() - 1; i >= 0; i--) {
            str += backStack.get(i).getWebView().getUrl() + "\n";
        }
        System.out.println(backStack.size() + "返回栈：\n" + str);*/
        int size = backStack.size();
        if (size > 1) {
            WebViewFragment fragment = backStack.pop();
            fragment.getWebView().stopLoading(); // 当前网页停止加载
            WebViewFragment backFragment = backStack.peek();
            // 判断当前网页是否是下载链接的
            String url = fragment.getWebView().getUrl();
            if (url != null && url.equals(MyApplication.getClickDownloadUrl())) {
                showFragment(backFragment);
            } else {
                boolean backFlag = false;
                // try {
                //     String url1 = fragment.getArguments().getString("url");
                //     String url2 = backFragment.getArguments().getString("url");
                //     if (url2.equals(url1)) backFlag = true;
                // } catch (Exception ignored) {}
                if (backFlag) { // 相邻两个页面网址相同的情况
                    // 隐藏上一个fragment
                    FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                    fragmentTransaction.hide(fragment);
                    fragmentTransaction.commit();
                    onBack();
                } else {
                    forwardStack.push(preFragment);
                    showFragment(backFragment);
                }
                // 判断上一个网页是否空白的
                /*backFragment.checkIfPageIsEmpty(previousIsEmpty -> {
                    // 递归处理空白页
                    if (previousIsEmpty) {
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
                });*/
            }
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
                // 暂停跳转前的webView
                webView.onPause();
                webView.pauseTimers();
                callback.onReceiveValue(true);
            }
        } else if ("forward".equals(type)) {
            if (webView.canGoForward()) {
                webView.goForward();
            } else {
                // 暂停跳转前的webView
                webView.onPause();
                webView.pauseTimers();
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
            WebView webView = fragment.getWebView();
            if (webView != null) {
                // 激活webView
                webView.onResume();
                webView.resumeTimers();
                String url = webView.getUrl();
                if (url != null) {
                    currentUrl = url;
                    MyApplication.jumpUrl = url;
                }
            }
        } catch (Exception e) {
            CommonUtils.saveLog("===========showFragment===========" + e.getMessage());
        }
    }

    // 回调监听
    private WebViewFragment.CallListener callListener = new WebViewFragment.CallListener() {
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

        @Override
        public void downLoad() {
            if (backStack.size() > 1) {
                goBackOrForward("back", b -> {
                    onBack();
                });
            }
        }

        // 检测有下载的情况
        @Override
        public void sniffingDownload() {
            btnMonitor.setBackgroundResource(R.drawable.btn_monitor2);
        }
        // 授权回调函数
        @Override
        public void setCallback(ValueCallback<Boolean> callback) {
            BrowserActivity2.this.callback = callback;
        }
    };

    // 处理物理返回键
    @Override
    public void onBackPressed() {
        if (backStack.size() > 0) {
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
        // 检查是否需要重建 WebView
        if (backStack.isEmpty() && currentUrl != null) {
            navigateTo(WebViewFragment.newInstance(currentUrl));
        }
        // 恢复可见的 Fragment
        if (!backStack.isEmpty()) {
            showFragment(backStack.peek());
        }
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
        backStack.clear();
        for (WebViewFragment fragment : forwardStack) {
            if (fragment.getWebView() != null) {
                fragment.getWebView().destroy();
            }
        }
        forwardStack.clear();
        // 退出浏览器页面后 清空可下载列表
        MyApplication.clearDownloadList();
        super.onDestroy();
    }
}
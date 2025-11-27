package cn.cheng.simpleBrower.activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.DownloadManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;


import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import cn.cheng.simpleBrower.MyApplication;
import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.bean.SysBean;
import cn.cheng.simpleBrower.custom.FeetDialog;
import cn.cheng.simpleBrower.custom.MyToast;
import cn.cheng.simpleBrower.custom.SettingDialog;
import cn.cheng.simpleBrower.receiver.MyDeviceAdminReceiver;
import cn.cheng.simpleBrower.service.DownloadService;
import cn.cheng.simpleBrower.service.ReadService;
import cn.cheng.simpleBrower.util.AssetsReader;
import cn.cheng.simpleBrower.util.CommonUtils;
import cn.cheng.simpleBrower.util.SysWindowUi;

public class MainActivity extends AppCompatActivity {

    private ImageView settingBtn;
    private Button jumpBtn;
    private Button txtBtn;
    private Button downloadBtn;
    private Button videoBtn;
    private Button likesBtn;
    private EditText edit;
    private ConstraintLayout editLayout;
    private boolean isExit = false; // 退出 标记
    private final static String BAIDU = "https://www.baidu.com";
    // private final static String BAIDU = "https://www.czys.top/v_play/bXZfMTg0MzQtbm1fMTQ=.html";
    // 测试m3u8视频（伪png这种ts文件的）
    // private final static String BAIDU = "https://yundunm3.czys.art:88/hls/qingyunian/2/01.m3u8";
    // 测试m3u8视频（正常）
    //private final static String BAIDU = "https://baikevideo.cdn.bcebos.com/media/mda-Og7wRzKHv5Z824nu/5e24506044a8dca815e9b106eab60de9.m3u8";

    private DevicePolicyManager mDevicePolicyManager;
    private ComponentName mComponentName;
    private ImageView lockBtn;

    // 授权回调
    private String type = "";
    ActivityResultLauncher<Intent> allFilesAccessLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化设备策略管理器和组件
        mDevicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        mComponentName = new ComponentName(this, MyDeviceAdminReceiver.class);

        // 状态栏设置透明
        SysWindowUi.hideStatusNavigationBar(this, false);

        setContentView(R.layout.activity_main);

        new Handler().post(() -> {
            AssetsReader.init(this, "audioVideo.txt");
            AssetsReader.init(this, "like.txt");
        });

        // 安装包后只运行一次
        if (!CommonUtils.onlySet(this, "only")) {
            initSetting();
        } else {
            init();
        }
    }

    private void initSetting() {
        // android 12的sd卡读写
        if (Build.VERSION.SDK_INT >= 29) {
            //启动线程开始执行
            new Handler().post(() -> {
                // 设置默认配置
                SysBean sysBean = new SysBean();
                sysBean.setFlagGif(true);
                sysBean.setFlagVideo(false);
                CommonUtils.writeObjectIntoLocal(sysBean, "SysSetting");

                init();
            });
        }
    }

    private void init() {
        // 注册权限请求的返回监听
        allFilesAccessLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> { // 授权返回后的处理
                    if (!CommonUtils.hasStoragePermissions(this)) return;
                    Intent intent = null;
                    switch (type) {
                        case "like" :
                            intent = new Intent(MainActivity.this, LikeActivity.class);
                            break;
                        case "video" :
                            intent = new Intent(MainActivity.this, VideoListActivity.class);
                            break;
                        case "txt" :
                            intent = new Intent(MainActivity.this, TxtListActivity.class);
                            break;
                        case "download" :
                            intent = new Intent(this, DownloadActivity.class);
                            break;
                    }
                    type = "";
                    if (intent != null) this.startActivity(intent);
                }
        );
        // 初始化控件 并绑定事件
        lockBtn = findViewById(R.id.lockBtn);
        lockBtn.setOnClickListener(view -> {
            if (mDevicePolicyManager.isAdminActive(mComponentName)) {
                // 如果设备管理员权限已激活
                // 返回桌面
                CommonUtils.backHome(this);
                // 立即锁屏
                mDevicePolicyManager.lockNow();
            } else {
                // 请求激活设备管理员权限
                requestDeviceAdminPermission();
            }
        });
        settingBtn = findViewById(R.id.settingBtn);
        settingBtn.setOnClickListener(view -> {
            SettingDialog dialog = new SettingDialog(MainActivity.this);
            dialog.show();
        });

        editLayout = this.findViewById(R.id.editLayout);
        editLayout.setOnTouchListener((View view, MotionEvent motionEvent) -> {
            if(null != this.getCurrentFocus()){
                edit.clearFocus();
                /**
                 * 点击空白位置 隐藏软键盘
                 */
                InputMethodManager mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                return mInputMethodManager.hideSoftInputFromWindow(edit.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                // return mInputMethodManager.hideSoftInputFromWindow(SearchActivity.this.getCurrentFocus().getWindowToken(), 0);
            }
            return false;
        });

        edit = this.findViewById(R.id.edit);
        // edit.setText(BAIDU);
        edit.setOnTouchListener((View view, MotionEvent motionEvent) -> {
            //获得焦点
            edit.setFocusable(true);
            edit.setFocusableInTouchMode(true);
            edit.requestFocus();
            return false;
        });
        edit.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == KeyEvent.ACTION_DOWN || actionId == EditorInfo.IME_ACTION_DONE) {
                jump();
            }
            return true;
        });

        jumpBtn = this.findViewById(R.id.jumpBtn);
        jumpBtn.setOnClickListener(view -> {
            jump();
        });

        likesBtn = this.findViewById(R.id.likesBtn);
        likesBtn.setOnClickListener(view -> {
            // 页面跳转后会用到的权限
            if (CommonUtils.hasStoragePermissions(this)) {
                Intent intent = new Intent(MainActivity.this, LikeActivity.class);
                this.startActivity(intent);
            } else {
                type = "like";
                CommonUtils.requestStoragePermissions(this, allFilesAccessLauncher);
            }
        });

        videoBtn = this.findViewById(R.id.videoBtn);
        videoBtn.setOnClickListener(view -> {
            // 页面跳转后会用到的权限
            if (CommonUtils.hasStoragePermissions(this)) {
                Intent intent = new Intent(MainActivity.this, VideoListActivity.class);
                this.startActivity(intent);
            } else {
                type = "video";
                CommonUtils.requestStoragePermissions(this, allFilesAccessLauncher);
            }
        });

        txtBtn = this.findViewById(R.id.txtBtn);
        txtBtn.setOnClickListener(view -> {
            // 页面跳转后会用到的权限
            if (CommonUtils.hasStoragePermissions(this)) {
                Intent intent = new Intent(MainActivity.this, TxtListActivity.class);
                this.startActivity(intent);
            } else {
                type = "txt";
                CommonUtils.requestStoragePermissions(this, allFilesAccessLauncher);
            }
        });

        downloadBtn = this.findViewById(R.id.downloadBtn);
        downloadBtn.setOnClickListener(view -> {
            // 页面跳转后会用到的权限
            if (CommonUtils.hasStoragePermissions(this)) {
                Intent i = new Intent(this, DownloadActivity.class);
                this.startActivity(i);
            } else {
                type = "download";
                CommonUtils.requestStoragePermissions(this, allFilesAccessLauncher);
            }
        });
    }

    // 跳转
    private void jump() {
        // 获取edit输入的网址信息
        Editable text = edit.getText();
        String webInfo = BAIDU;
        if (text != null && !"".equals(text.toString().trim())) {
            webInfo = text.toString();
            if (!CommonUtils.isUrl(webInfo)) {
                webInfo = "https://www.baidu.com/s?wd=" + webInfo;
            } else {
                if (!webInfo.toLowerCase().startsWith("http://") && !webInfo.toLowerCase().startsWith("https://")) {
                    webInfo = "http://" + webInfo;
                }
            }
        }
        // Intent intent = new Intent(MainActivity.this, BrowserActivity.class);
        Intent intent = new Intent(MainActivity.this, BrowserActivity2.class);
        intent.putExtra("webInfo", webInfo);
        this.startActivity(intent);
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
        // 设置下载文件保存的路径
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        // 获取系统下载管理器
        final DownloadManager downloadManager = (DownloadManager) this.getSystemService(DOWNLOAD_SERVICE);
        // 添加一个下载任务
        long downLoadId = downloadManager.enqueue(request);
    }

    // 锁屏权限请求
    private void requestDeviceAdminPermission() {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mComponentName);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.device_admin_activation_message));
        startActivityForResult(intent, 1);
    }

    // 此activity失去焦点后再次获取焦点时调用(调用其他activity再回来时)
    @Override
    protected void onResume() {
        super.onResume();
        // 有读写权限后只执行一次
        if (CommonUtils.hasStoragePermissions(this) && !CommonUtils.onlySet(this, "onlyLike")) {
            new Handler().post(() -> {
                // 存档默认收藏网址
                CommonUtils.setDefaultLikes();
            });
        }
    }

    /**
     * 双击放回键 退出
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isExit) {
                // 停止服务
                if (TxtActivity.txtActivity != null) {
                    Intent intentS = new Intent(TxtActivity.txtActivity, ReadService.class);
                    TxtActivity.txtActivity.stopService(intentS);
                }
                this.finish();
            } else {
                isExit = true;
                MyToast.getInstance("再按一下就退出了哦").show();
                new Handler().postDelayed(() -> {
                    isExit = false;
                }, 2000);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
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
                            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permissions[i])) {
                                // 返回 true，Toast 提示
                                MyToast.getInstance("无法访问该权限").show();
                            } else {
                                // 返回 false，需要显示对话框引导跳转到设置手动授权
                                FeetDialog feetDialog = new FeetDialog(MainActivity.this, "授权", "需前往授权后才能使用该功能", "授权", "取消");
                                feetDialog.setOnTouchListener(new FeetDialog.TouchListener() {
                                    @Override
                                    public void close() {
                                        feetDialog.dismiss();
                                    }
                                    @Override
                                    public void ok(String txt) {
                                        Intent intent = new Intent();
                                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        intent.setData(Uri.parse("package:" + MainActivity.this.getPackageName()));
                                        MainActivity.this.startActivityForResult(intent, 100);
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


}
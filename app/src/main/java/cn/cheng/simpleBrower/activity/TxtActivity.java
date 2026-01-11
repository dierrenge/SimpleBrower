package cn.cheng.simpleBrower.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

import cn.cheng.simpleBrower.MyApplication;
import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.bean.PositionBean;
import cn.cheng.simpleBrower.custom.MyToast;
import cn.cheng.simpleBrower.custom.ReadView;
import cn.cheng.simpleBrower.custom.TopDialog;
import cn.cheng.simpleBrower.service.ReadService;
import cn.cheng.simpleBrower.util.CommonUtils;
import cn.cheng.simpleBrower.util.SysWindowUi;

public class TxtActivity extends AppCompatActivity {

    public static Activity txtActivity;
    public static boolean flagRead;

    private TextView n_title;
    private Button back;
    private Button n_change;
    private ReadView n_content;
    private Handler msgHandler;

    String txtUrl = "";
    ArrayList<String> lines;
    // 当前小说进度
    PositionBean positionBean;

    private ReadService readService;

    private Intent intentS;
    private MsgReceiver msgReceiver;

    private Handler handler = new Handler();

    // 双击标记
    private boolean doubleClick = false;

    // 滑动距离边界值
    private static final int DISTANCE = 10;

    // 打开方式 标记
    boolean otherFlag = false;

    // 电话状态监听
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;
    // 微信等通话状态监听
    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener audioListener;

    @SuppressLint({"MissingInflatedId", "UnspecifiedRegisterReceiverFlag", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 状态栏设置透明
        SysWindowUi.hideStatusNavigationBar(this, false);

        setContentView(R.layout.activity_txt);
        txtActivity = this;
        try {
            //Service
            intentS = new Intent(this, ReadService.class);
            // 绑定式service
            // bindService(intentS, conn, Context.BIND_AUTO_CREATE);

            //动态注册广播接收器
            msgReceiver = new MsgReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("com.example.communication.RECEIVER");
            registerReceiver(msgReceiver, intentFilter);

            msgHandler = new Handler(new Handler.Callback() {
                @Override
                public boolean handleMessage(@NonNull Message message) {
                    if (message.what == 0) {
                        MyToast.getInstance(message.obj + "").show();
                    }
                    return false;
                }
            });

            Intent intent = getIntent();

            String action = intent.getAction();
            Uri uri = intent.getData();
            if (Intent.ACTION_VIEW.equals(action) && uri != null) {
                // 设置此activity可用于打开 txt文件
                txtUrl = CommonUtils.correctUrl(uri, this);
                otherFlag = true;
            } else {
                // 获取上一界面传过来的数据
                txtUrl = intent.getStringExtra("txtUrl");
            }
            if (txtUrl == null || !txtUrl.contains("/")) {
                MyToast.getInstance("还未访问授权喔").show();
                this.finish();
                return;
            }
            // CommonUtils.saveLog("打开方式-txt文件：" + txtUrl);

            String catalog = intent.getStringExtra("catalog");
            String title = txtUrl.substring(txtUrl.lastIndexOf("/") + 1);
            Bundle extras = intent.getExtras();
            if (extras != null) {
                positionBean = (PositionBean) extras.get("positionBean");
            }

            // 复用朗读服务的情况
            if (otherFlag || (txtUrl != null && !txtUrl.equals(MyApplication.getTxtUrl()))) {
                flagRead = false;
                // 停止服务
                stopService(intentS);
                // 注销广播
                // unregisterReceiver(msgReceiver);
            } else {
                flagRead = true;
            }

            // 读取内存中的小说行
            Map<String, ArrayList<String>> novelLinesMap = MyApplication.getNovelLinesMap();
            ArrayList<String> list = novelLinesMap.get(txtUrl);
            if (list != null) {
                lines = list;
            } else {
                lines = new ArrayList<>();
                CommonUtils.readLines(txtUrl, lines);
                MyApplication.setNovelLines(txtUrl, lines);
            }

            // 设置书名
            n_title = findViewById(R.id.n_title);
            n_title.setText(title);

            // 返回
            back = findViewById(R.id.n_back);
            back.setOnClickListener(view -> {
                this.finish();
            });

            // 更多
            n_change = findViewById(R.id.n_change);
            n_change.setOnClickListener(view -> {
                TopDialog topDialog = new TopDialog(TxtActivity.this, flagRead);
                topDialog.setOnTouchListener(new TopDialog.TouchListener() {
                    @Override
                    public void close() {
                        topDialog.dismiss();
                    }

                    @Override
                    public void catalog() {
                        topDialog.dismiss();
                        Intent intent1 = new Intent(TxtActivity.this, TxtCatalogActivity.class);
                        intent1.putExtra("txtUrl", txtUrl);
                        Bundle bundle = new Bundle();
                        bundle.putSerializable("positionBean", (Serializable) positionBean);
                        intent1.putExtras(bundle);
                        TxtActivity.this.startActivity(intent1);
                    }

                    @Override
                    public void read() {
                        // 跳转到文字转语音设置界面
                        /*Intent intent = new Intent();
                        intent.setAction("com.android.settings.TTS_SETTINGS");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        TxtActivity.this.startActivity(intent);*/

                        if (!otherFlag) {
                            flagRead = !flagRead;
                            TxtActivity.this.read();
                        }

                        topDialog.dismiss();
                    }

                    @Override
                    public void readSet() {
                        // 停止朗读服务
                        flagRead = false;
                        TxtActivity.this.stopService(intentS);
                        try {
                            // 跳转到文字转语音设置界面
                            Intent intent = new Intent();
                            intent.setAction("com.android.settings.TTS_SETTINGS");
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            TxtActivity.this.startActivity(intent);
                        } catch (Throwable e) {
                            MyToast.getInstance("设置失败！").show();
                            CommonUtils.saveLog("跳转到文字转语音设置界面:" + e.getMessage());
                        }
                        //topDialog.dismiss();
                    }

                    @Override
                    public void powerSet() {
                        try {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                // 停止朗读服务
                                flagRead = false;
                                TxtActivity.this.stopService(intentS);

                                // PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                                // boolean hasIgnored = false;
                                // 判断当前APP是否有加入电池优化的白名单，如果没有，弹出加入电池优化的白名单的设置对话框。
                                // hasIgnored = powerManager.isIgnoringBatteryOptimizations(TxtActivity.this.getPackageName());
                                // 直接跳转设置省电策略
                                MyToast.getInstance("设置后需稍等策略生效！").show();
                                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                                intent.setData(Uri.parse("package:" + TxtActivity.this.getPackageName()));
                                startActivity(intent);
                            } else {
                                MyToast.getInstance("该手机系统不支持此功能！").show();
                            }
                        } catch (Throwable e) {
                            MyToast.getInstance("设置失败！").show();
                            CommonUtils.saveLog("设置忽略电池优化:" + e.getMessage());
                        }
                    }
                });
                topDialog.show();
            });

            // 内容
            n_content = findViewById(R.id.n_content);
            n_content.setOnTouchListener(new View.OnTouchListener() {
                float mPosX, mPosY;

                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            mPosX = event.getRawX();
                            mPosY = event.getRawY();
                            break;
                        case MotionEvent.ACTION_UP:
                            float X = event.getRawX() - mPosX;
                            float Y = event.getRawY() - mPosY;
                            // 活动判断
                            if (Math.abs(Y) > Math.abs(X)) {
                                if (Y > DISTANCE) {
                                    // 下滑
                                } else if (Y < -DISTANCE) {
                                    // 上滑
                                }
                            } else {
                                if (X > DISTANCE) {
                                    // 右滑
                                    setPreviousPosition();
                                } else if (X < -DISTANCE){
                                    // 左滑
                                    setNextPosition();
                                }
                            }
                            // 双击判断
                            if (Math.abs(Y) <= DISTANCE && Math.abs(X) <= DISTANCE) {
                                if (doubleClick) { // 双击朗读
                                    if (!otherFlag) {
                                        flagRead = !flagRead;
                                        TxtActivity.this.read();
                                    }
                                } else {
                                    doubleClick = true;
                                    new Handler().postDelayed(() -> {
                                        doubleClick = false;
                                    }, 1600);
                                }
                            }
                            break;
                    }
                    return true;
                }
            });

            // 设置初始页面文本
            new Handler().postDelayed(() -> {
                if (catalog == null || "".equals(catalog)) {
                    positionBean = CommonUtils.readObjectFromLocal(txtUrl, PositionBean.class);
                }
                if (positionBean == null) {
                    positionBean = new PositionBean();
                    positionBean.setEndLine(-1);
                    positionBean.setEndNum(0);
                    setNextPosition();
                } else {
                    if ("catalog".equals(catalog)) {
                        setNextPosition();
                    } else {
                        n_content.setText(positionBean.getTxt());
                    }
                }
            }, 400);

            // 朗读翻页
            handler = new Handler(new Handler.Callback() {
                @Override
                public boolean handleMessage(@NonNull Message message) {
                    if (message.what == 0 && txtUrl.equals((String) message.obj)) {
                        setNextPosition();
                    }
                    return false;
                }
            });

            // 设置电话状态监听
            try {
                telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
                phoneStateListener = new PhoneStateListener() {
                    @Override
                    public void onCallStateChanged(int state, String phoneNumber) {
                        switch (state) {
                            case TelephonyManager.CALL_STATE_RINGING:
                                // 来电响铃
                                break;
                            case TelephonyManager.CALL_STATE_OFFHOOK:
                                // 通话开始（接听或拨出）
                                stopDuringCall();
                                break;
                            case TelephonyManager.CALL_STATE_IDLE:
                                // 通话结束
                                break;
                        }
                    }
                };
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
            } catch (Throwable e) {}

            // 设置微信等通话状态监听（请求音频焦点开始监听）
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioListener = new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            // 焦点临时丢失：可能微信通话开始
                            // 发现朗读时再次进入该页面也会触发该项监听，故需在onResume中设置标记
                            if (MyApplication.isOpenFlag()) stopDuringCall();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS:
                            // 焦点永久丢失：微信通话接管
                            break;
                        case AudioManager.AUDIOFOCUS_GAIN:
                            // 焦点恢复：微信通话结束
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            // 焦点临时丢失，可降低音量（较少用于通话）
                            break;
                    }
                }
            };
            int result = audioManager.requestAudioFocus(
                    audioListener,
                    AudioManager.STREAM_VOICE_CALL, // 使用语音通话流，与微信通话一致
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT // 临时请求焦点
            );
            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                CommonUtils.saveLog("TxtActivity:" + "音频焦点请求失败");
            }
        } catch (Throwable e) {
            MyToast.getInstance("打开异常咯").show();
            e.printStackTrace();
            CommonUtils.saveLog("TxtActivity:" + e.getMessage());
            this.finish();
        }
    }

    // 通话时停止朗读
    private void stopDuringCall() {
        if (TxtActivity.txtActivity != null && TxtActivity.flagRead) {
            Intent intentS = new Intent(TxtActivity.txtActivity, ReadService.class);
            TxtActivity.txtActivity.stopService(intentS);
            TxtActivity.flagRead = false;
            MyToast.getInstance(TxtActivity.this, "通话开始").show();
        }
    }

    // 设置下一页面文本，记录行数字数等
    private void setNextPosition() {
        int endLine = positionBean.getEndLine();
        int endNum = positionBean.getEndNum();
        if (endLine != 0 && (endLine < lines.size() - 1 || (endLine == lines.size() - 1 && endNum < lines.get(endLine).length()))) {
            positionBean.setSize(1320); // 字母i  24行、每行55个
            CommonUtils.readNextPageDef(lines, positionBean);
            n_content.setText(span(positionBean.getTxt()));
            new Handler().post(() -> {
                /*if (init == 0) {
                    init = 1;
                    n_content.setMaxLines(n_content.getLineNum() + 1);
                }*/
                positionBean.setSize(n_content.getCharNum());
                CommonUtils.readNextPage(lines, positionBean);
                n_content.setText(positionBean.getTxt());

                if (!otherFlag) {
                    read();
                }
            });
        }
    }

    // 设置上一页面文本，记录行数字数等
    private void setPreviousPosition() {
        int startLine = positionBean.getStartLine();
        int startNum = positionBean.getStartNum();
        if (startLine != -1 && startLine <= lines.size() - 1 && !(startNum == 0 && startLine == 0)) {
            positionBean.setSize(1320); // 字母i  24行、每行55个
            CommonUtils.readPreviousPageDef(lines, positionBean);
            n_content.setText(span(positionBean.getTxt()));
            new Handler().post(() -> {
                positionBean.setSize(n_content.getCharNum());
                CommonUtils.readPreviousPage(lines, positionBean, msgHandler);
                n_content.setText(span(positionBean.getTxt()));
                // 重置第一页
                if (positionBean.getStartLine() == 0 && positionBean.getStartNum() == 0) {
                    positionBean.setEndLine(-1);
                    positionBean.setEndNum(0);
                    setNextPosition();
                } else {
                    new Handler().post(() -> {
                        // 获取当前显示最后一行内容
                        Layout layout = n_content.getLayout();
                        int i = n_content.getLineNum();
                        int lineStart = layout.getLineStart(i);
                        int lineEnd = layout.getLineEnd(i);
                        String lastTxt = n_content.getText().subSequence(lineStart, lineEnd).toString();
                        // 但不是实际最后一行时
                        if (!positionBean.getTxt().endsWith(lastTxt)) {
                            lineStart = layout.getLineStart(0);
                            lineEnd = layout.getLineEnd(0);
                            String firstTxt = n_content.getText().subSequence(lineStart, lineEnd).toString();
                            positionBean.setTxt(positionBean.getTxt().substring(firstTxt.length()));
                            int num = positionBean.getStartNum();
                            int line = positionBean.getStartLine();
                            if (lines.get(line).length() > firstTxt.length() && lines.get(line).contains(firstTxt)) {
                                positionBean.setStartNum(num + firstTxt.length());
                            } else {
                                positionBean.setStartLine(line + 1);
                            }
                        }
                        n_content.setText(positionBean.getTxt());

                        if (!otherFlag) {
                            read();
                        }
                    });
                }
            });
        }
    }

    // 预设置文字时，设置文字不可见
    private SpannableStringBuilder span(String txt) {
        SpannableStringBuilder span = new SpannableStringBuilder(txt);
        span.setSpan(new ForegroundColorSpan(Color.TRANSPARENT), 0, txt.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        return span;
    }

    /**
     * 广播接收器
     *
     * @author len
     */
    public class MsgReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String url = intent.getStringExtra("txtUrl");
            Message message = handler.obtainMessage();
            message.what = 0;
            message.obj = url;
            handler.sendMessage(message);
        }

    }

    // 此activity失去焦点后再次获取焦点时调用(调用其他activity再回来时)
    @Override
    protected void onResume() {
        MyApplication.setOpenFlag(false);
        new Handler().postDelayed(() -> {
            MyApplication.setOpenFlag(true);
        }, 1500);
        super.onResume();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 绑定式service
        // unbindService(conn);

        // 注销广播
        unregisterReceiver(msgReceiver);

        if (flagRead) {
            MyApplication.setTxtUrl(txtUrl);
        } else {
            MyApplication.setTxtUrl(null);
        }

        if (positionBean != null) {
            new Handler().post(() -> {
                CommonUtils.writeObjectIntoLocal(positionBean, txtUrl);
            });
        }
    }

    private void read() {
        if (txtUrl != null) {
            // 记录进度
            if (positionBean != null) {
                new Handler().post(() -> {
                    CommonUtils.writeObjectIntoLocal(positionBean, txtUrl);
                });
            }
            // 朗读文本
            String txt = positionBean != null && flagRead ? positionBean.getTxt() : "";

            // 绑定式service
            // readService.startRead(txt, flagRead);

            intentS.putExtra("txtUrl", txtUrl);
            intentS.putExtra("txt", txt);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intentS);
            } else {
                startService(intentS);
            }

            if (!flagRead) {
                stopService(intentS);
            }
        }
    }

    // 绑定式service
    /*ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //返回一个MsgService对象
            readService = ((ReadService.MsgBinder)service).getService();

            readService.setReader(new ReadService.Reader() {
                @Override
                public void read() {
                    Message message = handler.obtainMessage();
                    message.what = 0;
                    handler.sendMessage(message);
                }
            });
        }
    };*/
}
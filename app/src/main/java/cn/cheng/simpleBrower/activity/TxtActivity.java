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
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
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
    // 历史小说进度
    PositionBean historyBean = new PositionBean();

    private ReadService readService;

    private Intent intentS;
    private MsgReceiver msgReceiver;

    private Handler handler = new Handler();

    // 双击标记
    private boolean doubleClick = false;

    // 滑动距离边界值
    private static final int DISTANCE = 10;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_txt);
        txtActivity = this;

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
                    MyToast.getInstance(TxtActivity.this, message.obj + "").show();
                }
                return false;
            }
        });

        Intent intent = getIntent();

        String action = intent.getAction();
        Uri uri = intent.getData();
        if (Intent.ACTION_VIEW.equals(action) && uri != null) {
            // 设置此activity可用于打开 txt文件
            txtUrl = CommonUtils.correctUrl(uri.getPath());
        } else {
            // 获取上一界面传过来的数据
            txtUrl = intent.getStringExtra("txtUrl");
        }
        CommonUtils.saveLog("打开方式-txt文件：" + txtUrl);

        String catalog = intent.getStringExtra("catalog");
        String title = txtUrl.substring(txtUrl.lastIndexOf("/") + 1);
        Bundle extras = intent.getExtras();
        if (extras != null) {
            positionBean = (PositionBean) extras.get("positionBean");
        }

        // 复用朗读服务的情况
        if (txtUrl != null && !txtUrl.equals(MyApplication.getTxtUrl())) {
            // 停止服务
            stopService(intentS);
            // 注销广播
            // unregisterReceiver(msgReceiver);
        } else {
            flagRead = true;
        }

        // 历史进度
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
            TopDialog topDialog = new TopDialog(TxtActivity.this);
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

                    flagRead = !flagRead;
                    TxtActivity.this.read(positionBean.getTxt());

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
                        MyToast.getInstance(TxtActivity.this, "设置失败！").show();
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
                            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                            intent.setData(Uri.parse("package:" + TxtActivity.this.getPackageName()));
                            startActivity(intent);
                        } else {
                            MyToast.getInstance(TxtActivity.this, "该手机系统不支持此功能！").show();
                        }
                    } catch (Throwable e) {
                        MyToast.getInstance(TxtActivity.this, "设置失败！").show();
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
                        float X = Math.abs(mPosX - event.getRawX());
                        float Y = Math.abs(mPosY - event.getRawY());
                        // 活动判断
                        if (Y > X) {
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
                        if (Y <= DISTANCE && X <= DISTANCE) {
                            if (doubleClick) { // 双击朗读
                                flagRead = !flagRead;
                                TxtActivity.this.read(positionBean.getTxt());
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
                if (message.what == 0) {
                    setNextPosition();
                }
                return false;
            }
        });
    }

    // 设置下一页面文本，记录行数字数等
    private void setNextPosition() {
        int endLine = positionBean.getEndLine();
        int endNum = positionBean.getEndNum();
        // 记录历史进度
        historyBean.setEndLine(endLine);
        historyBean.setEndNum(endNum);
        MyApplication.setNovel(txtUrl, historyBean);
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

                read(positionBean.getTxt());
            });
        }
    }

    // 设置上一页面文本，记录行数字数等
    private void setPreviousPosition() {
        int startLine = positionBean.getStartLine();
        int startNum = positionBean.getStartNum();
        // 记录历史进度
        historyBean.setStartLine(startLine);
        historyBean.setStartNum(startNum);
        MyApplication.setNovel(txtUrl, historyBean);
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

                        read(positionBean.getTxt());
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


    /**
     * 广播接收器
     *
     * @author len
     */
    public class MsgReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int progress = intent.getIntExtra("progress", 0);
            Message message = handler.obtainMessage();
            message.what = 0;
            handler.sendMessage(message);
        }

    }

    // 此activity失去焦点后再次获取焦点时调用(调用其他activity再回来时)
    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 绑定式service
        // unbindService(conn);

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

    private void read(String txt) {
        if (txtUrl != null) {
            // 记录进度
            if (positionBean != null) {
                new Handler().post(() -> {
                    CommonUtils.writeObjectIntoLocal(positionBean, txtUrl);
                });
            }

            // 绑定式service
            // readService.startRead(txt, flagRead);

            if (flagRead) {
                intentS.putExtra("flagRead", false);
                startService(intentS);
                stopService(intentS);
            }

            intentS.putExtra("txtUrl", txtUrl);
            Bundle bundle = new Bundle();
            bundle.putSerializable("positionBean", (Serializable) positionBean);
            intentS.putExtras(bundle);
            intentS.putExtra("flagRead", flagRead);
            startService(intentS);

            if (!flagRead) {
                stopService(intentS);
            }
        }
    }

}
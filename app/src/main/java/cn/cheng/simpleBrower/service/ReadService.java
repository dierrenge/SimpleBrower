package cn.cheng.simpleBrower.service;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import java.util.ArrayList;
import java.util.Locale;

import cn.cheng.simpleBrower.activity.TxtActivity;
import cn.cheng.simpleBrower.bean.PositionBean;
import cn.cheng.simpleBrower.custom.MyToast;
import cn.cheng.simpleBrower.receiver.HeadphoneReceiver;
import cn.cheng.simpleBrower.util.CommonUtils;

public class ReadService extends Service implements TextToSpeech.OnInitListener {

    public TextToSpeech textToSpeech;
    private final HeadphoneReceiver receiver = new HeadphoneReceiver();
    private String txtUrl = "";
    private ArrayList<String> lines;
    private PositionBean positionBean;
    boolean flag;
    String txt = "";

    private Intent intent = new Intent("com.example.communication.RECEIVER");
    private Reader reader;

    // 电话状态监听
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;
    private boolean phoneFlag = false;

    @Override
    public void onCreate() {
        textToSpeech = new TextToSpeech(this, this);
        // 注册广播接收器 接收器监听噪音ACTION_AUDIO_BECOMING_NOISY（可判断耳机断开链接）
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(receiver, intentFilter);
        super.onCreate();

        // 设置电话状态监听
        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String phoneNumber) {
                switch (state) {
                    case TelephonyManager.CALL_STATE_RINGING:
                        // 来电响铃
                        phoneFlag = true;
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        // 通话开始（接听或拨出）
                        // 停止TTS服务
                        if (phoneFlag && TxtActivity.txtActivity != null && TxtActivity.flagRead && textToSpeech != null && textToSpeech.isSpeaking()) {
                            startRead(txt, false);
                            MyToast.getInstance(TxtActivity.txtActivity, "通话开始").show();
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // 通话结束
                        // 开启TTS服务
                        if (phoneFlag && TxtActivity.txtActivity != null && TxtActivity.flagRead && textToSpeech != null &&  !textToSpeech.isSpeaking()) {
                            startRead(txt, true);
                            MyToast.getInstance(TxtActivity.txtActivity, "通话结束").show();
                            phoneFlag = false;
                        }
                        break;
                }
            }
        };
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // 获取上一界面传过来的数据
        txtUrl = intent.getStringExtra("txtUrl");
        Bundle extras = intent.getExtras();
        if (extras != null && extras.containsKey("positionBean")) {
            positionBean = (PositionBean) extras.get("positionBean");
            txt = positionBean.getTxt();
        }
        flag = intent.getBooleanExtra("flagRead", false);
        // 朗读开始
        startRead(txt, flag);

        return START_STICKY;
    }

    @Override
    public void onInit(int status) {
        //判断是否转化成功
        if (status == TextToSpeech.SUCCESS) {
            //设置语言为中文
            int languageCode = textToSpeech.setLanguage(Locale.CHINA);
            //判断是否支持这种语言，Android原生不支持中文，使用科大讯飞的tts引擎就可以了
            if (languageCode == TextToSpeech.LANG_NOT_SUPPORTED) {
                System.out.println("TAGonInit: 不支持这种语言");
            } else {
                //不支持就改成英文
                textToSpeech.setLanguage(Locale.US);
            }
            // 设置音调,值越大声音越尖（女生），值越小则变成男声,1.0是常规
            textToSpeech.setPitch(1.0f);
            // 设置语速
            textToSpeech.setSpeechRate(3.6f);
            // 在onInIt方法里直接调用tts的播报功能
            startRead(positionBean == null ? "" : positionBean.getTxt(), flag);

            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String s) {

                }

                @Override
                public void onDone(String s) {
                    // reader.read();
                    //发送Action为com.example.communication.RECEIVER的广播
                    intent.putExtra("txtUrl", txtUrl);
                    sendBroadcast(intent);
                }

                @Override
                public void onError(String s) {
                    CommonUtils.saveLog("-------onError:" + s);
                }
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    public void setReader(Reader reader) {
        this.reader = reader;
    }

    public interface Reader {
        void read();
    }

    /**
     * 返回一个Binder对象
     */
    @Override
    public IBinder onBind(Intent intent) {
        return new MsgBinder();
    }

    public class MsgBinder extends Binder{
        /**
         * 获取当前Service的实例
         * @return
         */
        public ReadService getService(){
            return ReadService.this;
        }
    }

    public void startRead(String txt, boolean flag) {
        try {
            if (flag) {
                textToSpeech.speak(txt == null ? "" : txt, TextToSpeech.QUEUE_FLUSH, null, "UniqueID");
                // textToSpeech.speak(txt, TextToSpeech.QUEUE_ADD, null, "UniqueID");
            } else {
                textToSpeech.stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
package cn.cheng.simpleBrower.service;

import android.app.Notification;
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

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Locale;

import cn.cheng.simpleBrower.activity.TxtActivity;
import cn.cheng.simpleBrower.bean.PositionBean;
import cn.cheng.simpleBrower.custom.MyToast;
import cn.cheng.simpleBrower.receiver.HeadphoneReceiver;
import cn.cheng.simpleBrower.util.CommonUtils;
import cn.cheng.simpleBrower.util.NotificationUtils;

public class ReadService extends Service implements TextToSpeech.OnInitListener {

    private Reader reader;

    public void setReader(Reader reader) {
        this.reader = reader;
    }

    public interface Reader {
        void read();
    }

    public TextToSpeech textToSpeech;
    private final HeadphoneReceiver receiver = new HeadphoneReceiver();
    private Intent intent = new Intent("com.example.communication.RECEIVER");
    private String txtUrl = "";
    String txt = "";

    @Override
    public void onCreate() {
        super.onCreate();
        // 注册广播接收器 接收器监听噪音ACTION_AUDIO_BECOMING_NOISY（可判断耳机断开链接）
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(receiver, intentFilter);

        // 必须设置为前台服务
        Notification n = NotificationUtils.initBuilder(this,
                "朗读服务", "彼黍朗读服务运行中", TxtActivity.class).build();
        startForeground(2, n);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 清除旧语音资源
        speechDestroy();
        // 获取上一界面传过来的数据
        txtUrl = intent.getStringExtra("txtUrl");
        txt = intent.getStringExtra("txt");
        // 朗读开始
        if (StringUtils.isNotEmpty(txt)) textToSpeech = new TextToSpeech(this, this);

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
            textToSpeech.speak(txt == null ? "" : txt, TextToSpeech.QUEUE_FLUSH, null, "UniqueID");

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

    private void speechDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        speechDestroy();
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

}
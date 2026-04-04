package cn.cheng.biShu.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import androidx.core.app.NotificationCompat;


import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

import cn.cheng.biShu.MyApplication;
import cn.cheng.biShu.activity.TxtActivity;
import cn.cheng.biShu.receiver.HeadphoneReceiver;
import cn.cheng.biShu.util.CommonUtils;
import cn.cheng.biShu.util.NotificationUtils;

public class ReadService extends Service {

    public static TextToSpeech textToSpeech;
    private final HeadphoneReceiver receiver = new HeadphoneReceiver();
    private long time;

    @Override
    public void onCreate() {
        super.onCreate();
        // 注册广播接收器 接收器监听噪音ACTION_AUDIO_BECOMING_NOISY（可判断耳机断开链接）
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, intentFilter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(receiver, intentFilter);
        }

        // 设置为前台服务
        NotificationCompat.Builder builder = NotificationUtils.initBuilder(this, "朗读服务", "彼黍朗读服务运行中", TxtActivity.class);
        startForeground(1, builder.build());
        NotificationUtils.notifySummaryNotification(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 清除旧语音资源
        speechDestroy();
        // 获取上一界面传过来的数据
        String txtUrl = intent.getStringExtra("txtUrl");
        String txt = intent.getStringExtra("txt");
        if (StringUtils.isEmpty(txt)) return START_STICKY;
        // 朗读开始
        textToSpeech = new TextToSpeech(this, status -> {
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
                // 监听朗读结束
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String s) {
                        time = System.currentTimeMillis();
                    }
                    @Override
                    public void onDone(String s) {
                        // 清除旧语音资源
                        speechDestroy();
                        // 防止多翻页
                        long endTime = System.currentTimeMillis() - time;
                        if (txt.length() > 20 && endTime < 4000) return;
                        // 发送Action为com.example.communication.RECEIVER的广播
                        Intent intentReceiver = new Intent("com.example.communication.RECEIVER");
                        intentReceiver.putExtra("txtUrl", txtUrl);
                        sendBroadcast(intentReceiver);
                    }
                    @Override
                    public void onError(String s) {
                        CommonUtils.saveLog("-------onError:" + s);
                    }
                });
                // 在onInIt方法里直接调用tts的播报功能
                textToSpeech.speak(txt, TextToSpeech.QUEUE_FLUSH, null, "UniqueID");
            }
        });

        return START_STICKY;
    }

    private void speechDestroy() {
        if (textToSpeech != null) {
            textToSpeech.setOnUtteranceProgressListener(null);
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        speechDestroy();
    }

    /**
     * 无需绑定
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
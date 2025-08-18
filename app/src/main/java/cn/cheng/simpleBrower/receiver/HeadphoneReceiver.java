package cn.cheng.simpleBrower.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import cn.cheng.simpleBrower.activity.TxtActivity;
import cn.cheng.simpleBrower.custom.MyToast;
import cn.cheng.simpleBrower.service.ReadService;

/**
 * 创建接收器，处理状态变化
 * 耳机线被拔掉或是支持A2DP（蓝牙协议）的音频连接断开后，系统会认为该应用因为声音输出方式改变，导致产生噪音（NOISY），
 * 系统会广播该Intent Action：（ACTION_AUDIO_BECOMING_NOISY），让我们的广播接收器可以做出相应的处理，比如：暂停播放、降低音量等
 *
 */
public class HeadphoneReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)) {
            // 停止TTS服务
            if (TxtActivity.txtActivity != null) {
                Intent intentS = new Intent(TxtActivity.txtActivity, ReadService.class);
                TxtActivity.txtActivity.stopService(intentS);
                TxtActivity.flagRead = false;
                MyToast.getInstance("耳机已断开连接").show();
            }
        }
    }
}

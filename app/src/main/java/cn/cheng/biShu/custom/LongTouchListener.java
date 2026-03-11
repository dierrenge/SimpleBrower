package cn.cheng.biShu.custom;

import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.ValueCallback;

import cn.cheng.biShu.util.CommonUtils;

/**
 * 自定义长按触摸监听器
 * Created by YanGeCheng on 2026/2/12.
 */

public abstract class LongTouchListener implements View.OnTouchListener {
    float xDown, yDown;
    long timeDown;
    static final int LONG_TIME = 300;
    Handler handler = new Handler();
    Runnable runnable;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            xDown = event.getRawX();
            yDown = event.getRawY();
            timeDown = event.getEventTime(); // 记录初始触摸时间
            runnable = () -> longEvent(xDown, yDown);
            handler.postDelayed(runnable, LONG_TIME);
            downEvent(b -> {
                handler.removeCallbacks(runnable);
            });
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float xMove = Math.abs(xDown - event.getRawX());
            float yMove = Math.abs(yDown - event.getRawY());
            realUpEvent();
            float timeMove = Math.abs(timeDown - event.getEventTime());
            if (xMove >= 20 || yMove >= 20 || timeMove < LONG_TIME) {
                handler.removeCallbacks(runnable);
                if (xMove < 20 && yMove < 20) clickEvent();
            } else {
                if (xMove < 20 && yMove < 20) lClickEvent();
            }
        }
        return false;
    }

    public void downEvent(ValueCallback<Boolean> callback) {}
    public void realUpEvent() {}

    public void lClickEvent() {}

    public abstract void clickEvent();

    public abstract void longEvent(float x, float y);

}

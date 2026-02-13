package cn.cheng.simpleBrower.custom;

import android.view.MotionEvent;
import android.view.View;

/**
 * 自定义长按触摸监听器
 * Created by YanGeCheng on 2026/2/12.
 */

public abstract class LongTouchListener implements View.OnTouchListener {
    long timeDown;
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            downEvent();
            // 记录初始触摸时间
            timeDown = event.getEventTime();
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            realUpEvent();
            float timeMove = Math.abs(timeDown - event.getEventTime());
            if (timeMove > 300) {
                upEvent(event.getRawX(), event.getRawY());
            }
        }
        return false;
    }

    public void downEvent() {}
    public void realUpEvent() {}

    public abstract void upEvent(float x, float y);

}

package cn.cheng.simpleBrower.custom;

import android.view.MotionEvent;
import android.view.View;

/**
 * 自定义过滤长按和长距离滑动的触摸监听器
 * Created by YanGeCheng on 2021/10/16.
 */

public abstract class FilterLongDownSlideTouchListener implements View.OnTouchListener {
    float xDown, yDown;
    long timeDown;
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // 记录初始触摸位置
            xDown = event.getRawX();
            yDown = event.getRawY();
            timeDown = event.getEventTime();
            downEvent();
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            // 记录移动距离 只有短距离滑动才有效
            float xMove = Math.abs(xDown - event.getRawX());
            float yMove = Math.abs(yDown - event.getRawY());
            float timeMove = Math.abs(timeDown - event.getEventTime());
            upRealEvent();
            if (xMove < 20 && yMove < 20 && timeMove < 400) {
                upEvent();
            }
        }
        return false;
    }

    public void downEvent(){};

    public void upRealEvent(){};

    public abstract void upEvent();

}

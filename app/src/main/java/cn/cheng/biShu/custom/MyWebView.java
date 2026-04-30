package cn.cheng.biShu.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MyWebView extends WebView {

    private boolean isUserClick = false;

    public MyWebView(@NonNull Context context) {
        super(context);
    }

    public MyWebView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            HitTestResult hitTestResult = getHitTestResult();
            // 检测触摸点是否为超链接类型
            if (hitTestResult.getType() == HitTestResult.SRC_ANCHOR_TYPE ||
                    hitTestResult.getType() == HitTestResult.ANCHOR_TYPE) {
                isUserClick = true;
            }
        }
        return super.onTouchEvent(event);
    }

    public boolean isUserClickTriggered() {
        return isUserClick;
    }

    public void resetClickState() {
        isUserClick = false;
    }
}

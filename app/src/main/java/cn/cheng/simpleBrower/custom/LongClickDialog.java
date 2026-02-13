package cn.cheng.simpleBrower.custom;

import static android.content.Context.INPUT_METHOD_SERVICE;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;

import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.util.CommonUtils;

/**
 * Created by YanGeCheng on 2026/2/10.
 * 长按弹框
 */
public class LongClickDialog extends Dialog {

    private LinearLayout click_dialog_l;
    private Button open;
    private Button copy;
    private Button modify;
    private Button delete;
    private Button selectMore;

    private TouchListener touchListener;

    private int x, y; // 点击屏幕的位置
    int screenWidth, screenHeight; // 屏幕的大小
    int popupWidth, popupHeight; // 弹框的大小

    public LongClickDialog(@NonNull Context context, float x, float y, int popupWidth, int popupHeight) {
        super(context, R.style.dialog);
        // 点击屏幕的位置
        this.x = Math.round(x);
        this.y = Math.round(y);
        // 弹框的大小
        this.popupWidth = popupWidth;
        this.popupHeight = popupHeight;
        // 屏幕的大小
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
    }

    @SuppressLint({"MissingInflatedId", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 绑定view
        setContentView(R.layout.click_dialog);
        // 设置返回键可以关闭弹框
        setCancelable(true);
        // 设置触摸弹框以外区域可以关闭弹框
        setCanceledOnTouchOutside(true);

        // 初始化控件
        click_dialog_l = findViewById(R.id.click_dialog_l);
        open = findViewById(R.id.dialog_open);
        copy = findViewById(R.id.dialog_copy);
        modify = findViewById(R.id.dialog_modify);
        delete = findViewById(R.id.dialog_delete);
        selectMore = findViewById(R.id.dialog_selectMore);
        // 间距
        int padding = 40;
        // view窗口显示设置
        Window window = this.getWindow();
        window.setGravity(Gravity.TOP | Gravity.START);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.dimAmount = 0.3F;
        params.x = (x + popupWidth > screenWidth) ? screenWidth - popupWidth - padding : (x + padding) < popupWidth ? padding : x - popupWidth/2;
        params.y = (y + popupHeight > screenHeight) ? screenHeight - popupHeight - padding : (y + padding) < popupHeight ? padding : y - popupHeight/2;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            params.setCanPlayMoveAnimation(false);
        }
        window.setAttributes(params);
        CommonUtils.saveLog( "x=======" + x + ",   y=======" + y);
        CommonUtils.saveLog( "popupWidth=======" + popupWidth + ",   popupHeight=======" + popupHeight);
        CommonUtils.saveLog( "screenWidth=======" + screenWidth + ",   screenHeight=======" + screenHeight);
        CommonUtils.saveLog( "params.x=======" + params.x + ",   params.y=======" + params.y);
        click_dialog_l.setVisibility(View.VISIBLE);

        // 按钮触摸事件
        open.setOnTouchListener(new FilterLongDownSlideTouchListener() {
            @Override
            public void downEvent() {
                open.setBackgroundResource(R.drawable.btn_bg5_top_down);
            }
            @Override
            public void upRealEvent() {
                open.setBackgroundResource(R.drawable.btn_bg5_top);
            }
            @Override
            public void upEvent() {
                if (touchListener != null) {
                    touchListener.open();
                }
            }
        });
        copy.setOnTouchListener(new FilterLongDownSlideTouchListener() {
            @Override
            public void downEvent() {
                copy.setBackgroundResource(R.drawable.btn_bg5_down);
            }
            @Override
            public void upRealEvent() {
                copy.setBackgroundResource(R.drawable.btn_bg5);
            }
            @Override
            public void upEvent() {
                if (touchListener != null) {
                    touchListener.copy();
                }
            }
        });
        modify.setOnTouchListener(new FilterLongDownSlideTouchListener() {
            @Override
            public void downEvent() {
                modify.setBackgroundResource(R.drawable.btn_bg5_down);
            }
            @Override
            public void upRealEvent() {
                modify.setBackgroundResource(R.drawable.btn_bg5);
            }
            @Override
            public void upEvent() {
                if (touchListener != null) {
                    touchListener.modify();
                }
            }
        });
        delete.setOnTouchListener(new FilterLongDownSlideTouchListener() {
            @Override
            public void downEvent() {
                delete.setBackgroundResource(R.drawable.btn_bg5_down);
            }
            @Override
            public void upRealEvent() {
                delete.setBackgroundResource(R.drawable.btn_bg5);
            }
            @Override
            public void upEvent() {
                if (touchListener != null) {
                    touchListener.delete();
                }
            }
        });
        selectMore.setOnTouchListener(new FilterLongDownSlideTouchListener() {
            @Override
            public void downEvent() {
                selectMore.setBackgroundResource(R.drawable.btn_bg5_bottom_down);
            }
            @Override
            public void upRealEvent() {
                selectMore.setBackgroundResource(R.drawable.btn_bg5_bottom);
            }
            @Override
            public void upEvent() {
                if (touchListener != null) {
                    touchListener.selectMore();
                }
            }
        });

    }

    @Override
    public void show() {
        super.show();
    }

    @Override
    public void dismiss() {
        LongClickDialog.this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        super.dismiss();
    }

    // Dialog设置外部点击事件
    @Override
    public void setCanceledOnTouchOutside(boolean cancel) {
        if (touchListener != null) {
            touchListener.close();
        }
        super.setCanceledOnTouchOutside(cancel);
    }

    public void setOnTouchListener(TouchListener touchListener) {
        this.touchListener = touchListener;
    }

    public interface TouchListener {
        // 关闭弹框
        void close();
        // 打开
        void open();
        // 复制
        void copy();
        // 修改
        void modify();
        // 删除
        void delete();
        // 多选
        void selectMore();
    }

}

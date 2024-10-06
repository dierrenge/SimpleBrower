package cn.cheng.simpleBrower.custom;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.util.SysWindowUi;

/**
 * Created by YanGeCheng on 2023/4/2.
 * 底部对话框（弹框）
 */
public class FeetDialog extends Dialog {

    private Button closeBth;
    private Button okBtn;
    private TouchListener touchListener;
    private TextView dialog_title;
    private TextView dialog_text;
    private String title, text, okName, closeName;

    public FeetDialog(@NonNull Context context) {
        super(context, R.style.dialog);
    }

    public FeetDialog(@NonNull Context context, String title, String text, String okName, String closeName) {
        this(context);
        this.title = title;
        this.text = text;
        this.okName = okName;
        this.closeName = closeName;
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 绑定view
        setContentView(R.layout.feet_dialog);
        // 设置返回键可以关闭弹框
        setCancelable(true);
        // 设置触摸弹框以外区域可以关闭弹框
        setCanceledOnTouchOutside(true);

        // 初始化控件
        closeBth = findViewById(R.id.dialog_close);
        okBtn = findViewById(R.id.dialog_set_ok);
        dialog_title = findViewById(R.id.dialog_title);
        dialog_text = findViewById(R.id.dialog_text);
        if (title != null) {
            dialog_title.setText(title);
        }
        if (text != null) {
            dialog_text.setText(text);
        }
        if (okName != null) {
            okBtn.setText(okName);
        }
        if (closeName != null) {
            closeBth.setText(closeName);
        }

        // view窗口显示设置
        Window window = this.getWindow();
        window.setGravity(Gravity.BOTTOM);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.dimAmount = 0.3F;
        window.setAttributes(params);

        // 按钮触摸事件
        closeBth.setOnTouchListener(new FilterLongDownSlideTouchListener() {
            @Override
            public void upEvent() {
                if (touchListener != null) {
                    touchListener.close();
                }
            }
        });
        okBtn.setOnTouchListener(new FilterLongDownSlideTouchListener() {
            @Override
            public void upEvent() {
                if (touchListener != null) {
                    touchListener.ok();
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
        FeetDialog.this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
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
        // 应用锁屏
        void ok();
    }

}

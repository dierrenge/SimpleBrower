package cn.cheng.biShu.custom;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.NonNull;

import cn.cheng.biShu.R;
import cn.cheng.biShu.activity.DownloadActivity;
import cn.cheng.biShu.activity.LikeActivity;
import cn.cheng.biShu.activity.TxtListActivity;

/**
 * Created by YanGeCheng on 2025/4/25.
 * 更多功能弹框（弹框）
 */
public class MoreFunctionDialog extends Dialog {

    private Context context;
    private Button historyBtn;
    private Button novelBtn;
    private Button downloadMoreBtn;
    private CallListener callListener;

    public MoreFunctionDialog(@NonNull Context context) {
        super(context, R.style.dialog);
        this.context = context;
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 绑定view
        setContentView(R.layout.dialog_more_function);
        // 设置返回键可以关闭弹框
        setCancelable(true);
        // 设置触摸弹框以外区域可以关闭弹框
        setCanceledOnTouchOutside(true);

        // view窗口显示设置
        Window window = this.getWindow();
        window.setGravity(Gravity.BOTTOM);
        window.setWindowAnimations(R.style.DialogAnimation); // 应用动画样式
        // window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        // params.dimAmount = 0.2F;
        window.setAttributes(params);

        historyBtn = this.findViewById(R.id.historyBtn);
        historyBtn.setOnClickListener(view -> {
            Intent intent = new Intent(context, LikeActivity.class);
            intent.putExtra("flag", "历史");
            context.startActivity(intent);
        });
        novelBtn = this.findViewById(R.id.novelBtn);
        novelBtn.setOnClickListener(view -> {
            Intent intent = new Intent(context, TxtListActivity.class);
            context.startActivity(intent);
        });

        downloadMoreBtn = this.findViewById(R.id.downloadMoreBtn);
        downloadMoreBtn.setOnClickListener(view -> {
            Intent i = new Intent(context, DownloadActivity.class);
            context.startActivity(i);
        });
    }

    public void setCallListener(CallListener callListener) {
        this.callListener = callListener;
    }

    public interface CallListener {
        // 设置阴影
        void setBackground(boolean flag);
    }

    @Override
    public void show() {
        if (callListener != null) callListener.setBackground(true);
        super.show();
    }

    @Override
    public void dismiss() {
        // MoreFunctionDialog.this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        if (callListener != null) callListener.setBackground(false);
        super.dismiss();
    }

    // Dialog设置外部点击事件
    @Override
    public void setCanceledOnTouchOutside(boolean cancel) {
        super.setCanceledOnTouchOutside(cancel);
    }
}

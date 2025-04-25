package cn.cheng.simpleBrower.custom;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

import cn.cheng.simpleBrower.MyApplication;
import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.activity.LikeActivity;
import cn.cheng.simpleBrower.activity.MainActivity;
import cn.cheng.simpleBrower.activity.TxtListActivity;
import cn.cheng.simpleBrower.bean.DownloadBean;
import cn.cheng.simpleBrower.service.DownloadService;
import cn.cheng.simpleBrower.util.CommonUtils;

/**
 * Created by YanGeCheng on 2025/4/25.
 * 更多功能弹框（弹框）
 */
public class MoreFunctionDialog extends Dialog {

    private Context context;
    private Button historyBtn;
    private Button novelBtn;
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
        setContentView(R.layout.more_function_dialog);
        // 设置返回键可以关闭弹框
        setCancelable(true);
        // 设置触摸弹框以外区域可以关闭弹框
        setCanceledOnTouchOutside(true);

        // view窗口显示设置
        Window window = this.getWindow();
        window.setGravity(Gravity.BOTTOM);
        window.setWindowAnimations(R.style.DialogAnimation); // 应用动画样式
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.dimAmount = 0.3F;
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
    }

    @Override
    public void show() {
        super.show();
    }

    @Override
    public void dismiss() {
        MoreFunctionDialog.this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        super.dismiss();
    }

    // Dialog设置外部点击事件
    @Override
    public void setCanceledOnTouchOutside(boolean cancel) {
        super.setCanceledOnTouchOutside(cancel);
    }

    public void setOnCallListener(CallListener touchListener) {
        this.callListener = touchListener;
    }

    public interface CallListener {
        void doing();
    }

}

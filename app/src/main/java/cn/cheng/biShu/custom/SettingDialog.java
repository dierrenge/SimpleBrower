package cn.cheng.biShu.custom;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;

import androidx.annotation.NonNull;

import cn.cheng.biShu.R;
import cn.cheng.biShu.bean.SysBean;
import cn.cheng.biShu.util.CommonUtils;

/**
 * Created by YanGeCheng on 2023/4/2.
 * （弹框）
 */
public class SettingDialog extends Dialog {

    private Activity activity;
    private CheckBox gifTip;

    public SettingDialog(@NonNull Activity activity) {
        super(activity, R.style.dialog);
        this.activity = activity;
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 绑定view
        setContentView(R.layout.dialog_setting);
        // 设置返回键可以关闭弹框
        setCancelable(true);
        // 设置触摸弹框以外区域可以关闭弹框
        setCanceledOnTouchOutside(true);

        // 初始化控件
        gifTip = findViewById(R.id.gifTip);
        gifTip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SysBean sysBean = new SysBean();
                sysBean.setFlagGif(gifTip.isChecked());
                CommonUtils.writeObjectIntoLocal(sysBean, "SysSetting");
            }
        });
        setSys();

        // view窗口显示设置
        Window window = this.getWindow();
        window.setGravity(Gravity.TOP | Gravity.RIGHT);
        // window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.x = 70;
        params.y = 90;
        params.dimAmount = 0.3F;
        window.setAttributes(params);
    }

    private void setSys() {
        SysBean sysBean = CommonUtils.readObjectFromLocal("SysSetting", SysBean.class);
        if (sysBean != null) {
            boolean flagGif = sysBean.isFlagGif();
            if (gifTip != null) {
                gifTip.setChecked(flagGif);
            }
        }
    }

    @Override
    public void show() {
        super.show();
    }

    @Override
    public void dismiss() {
        SettingDialog.this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        super.dismiss();
    }

}

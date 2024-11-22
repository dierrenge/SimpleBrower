package cn.cheng.simpleBrower.custom;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;

import cn.cheng.simpleBrower.MyApplication;
import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.activity.MainActivity;
import cn.cheng.simpleBrower.bean.SysBean;
import cn.cheng.simpleBrower.util.CommonUtils;

/**
 * Created by YanGeCheng on 2023/4/2.
 * （弹框）
 */
public class SettingDialog extends Dialog {

    private RadioButton downLoadTip;
    private RadioButton gifTip;
    private RadioGroup downLoadGroup;
    private RadioGroup gifGroup;

    public SettingDialog(@NonNull Context context) {
        super(context, R.style.dialog);
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 绑定view
        setContentView(R.layout.setting_dialog);
        // 设置返回键可以关闭弹框
        setCancelable(true);
        // 设置触摸弹框以外区域可以关闭弹框
        setCanceledOnTouchOutside(true);

        // 初始化控件
        downLoadGroup = findViewById(R.id.downLoadGroup);
        downLoadTip = findViewById(R.id.downLoadTip);
        downLoadTip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean checked = downLoadTip.isChecked();
                // 会用到的权限
                if (!checked && !CommonUtils.hasStoragePermissions(MyApplication.getActivity())) {
                    CommonUtils.requestStoragePermissions(MyApplication.getActivity());
                    return;
                }
                SysBean sysBean = new SysBean();
                if (checked) {
                    downLoadTip.setChecked(false);
                    sysBean.setFlagVideo(false);
                } else {
                    downLoadGroup.clearCheck();
                    downLoadTip.setChecked(true);
                    sysBean.setFlagVideo(true);
                }
                sysBean.setFlagGif(gifTip.isChecked());
                CommonUtils.writeObjectIntoLocal(sysBean, "SysSetting");
            }
        });
        gifGroup = findViewById(R.id.gifGroup);
        gifTip = findViewById(R.id.gifTip);
        gifTip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean checked = gifTip.isChecked();
                SysBean sysBean = new SysBean();
                if (checked) {
                    gifTip.setChecked(false);
                    sysBean.setFlagGif(false);
                } else {
                    gifGroup.clearCheck();
                    gifTip.setChecked(true);
                    sysBean.setFlagGif(true);
                }
                sysBean.setFlagVideo(downLoadTip.isChecked());
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
            boolean flagVideo = sysBean.isFlagVideo();
            boolean flagGif = sysBean.isFlagGif();
            if (downLoadTip != null && downLoadGroup != null) {
                if (!flagVideo) {
                    downLoadTip.setChecked(flagVideo);
                } else {
                    downLoadGroup.clearCheck();
                    downLoadTip.setChecked(true);
                }
            }
            if (gifTip != null && gifGroup != null) {
                if (!flagGif) {
                    gifTip.setChecked(flagGif);
                } else {
                    gifGroup.clearCheck();
                    gifTip.setChecked(true);
                }
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

package cn.cheng.simpleBrower.custom;

import static android.content.Context.INPUT_METHOD_SERVICE;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;

import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.util.CommonUtils;
import cn.cheng.simpleBrower.util.SysWindowUi;

/**
 * Created by YanGeCheng on 2023/4/2.
 * 底部对话框（弹框）
 */
public class FeetDialog extends Dialog {

    private Button closeBth;
    private Button okBtn;
    private TouchListener touchListener;
    private LinearLayout feet_dialog_l;
    private TextView dialog_title;
    private TextView dialog_text;
    private LinearLayout dialog_text_layout;
    private View dialog_for_focus;
    private EditText dialog_text_filename;
    private TextView dialog_text_fileType;
    private TextView dialog_text_fileSize;
    private String title, text, okName, closeName;
    private Context context;
    private String url, fName;
    private Handler handler;

    public FeetDialog(@NonNull Context context) {
        super(context, R.style.dialog);
        this.context = context;
    }

    public FeetDialog(@NonNull Context context, String title, String text, String okName, String closeName) {
        this(context);
        this.context = context;
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
        feet_dialog_l = findViewById(R.id.feet_dialog_l);
        closeBth = findViewById(R.id.dialog_close);
        okBtn = findViewById(R.id.dialog_set_ok);
        dialog_title = findViewById(R.id.dialog_title);
        dialog_text = findViewById(R.id.dialog_text);
        dialog_text_layout = findViewById(R.id.dialog_text_layout);
        dialog_for_focus = findViewById(R.id.dialog_for_focus);
        dialog_text_filename = findViewById(R.id.dialog_text_filename);
        dialog_text_fileType = findViewById(R.id.dialog_text_fileType);
        dialog_text_fileSize = findViewById(R.id.dialog_text_fileSize);
        if (title != null) {
            dialog_title.setText(title);
        }
        if (text != null) {
            if (text.contains(" / ")) {
                dialog_text.setVisibility(View.GONE);
                dialog_text_layout.setVisibility(View.VISIBLE);
                loadFileSize(text);
            } else {
                dialog_text.setText(text);
            }
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
                    String txt = "";
                    String type = "";
                    if (text.contains(" / ")) {
                        txt = dialog_text_filename.getText().toString();
                        if (StringUtils.isEmpty(txt)) txt = CommonUtils.randomStr();
                        type = dialog_text_fileType.getText().toString();
                        type = type.substring(0, type.lastIndexOf(" / "));
                    }
                    touchListener.ok(txt + type);
                }
            }
        });
        // 隐藏输入键盘
        feet_dialog_l.setOnClickListener(v -> {
            InputMethodManager im = (InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE);
            if (im != null) { // 隐藏键盘
                im.hideSoftInputFromWindow(dialog_text_filename.getWindowToken(), 0);
            }
            dialog_for_focus.requestFocus();
        });

        // 异步获取文件大小
        handler = new Handler(message -> {
            String finalText = (String) message.obj;
            loadFileSize(finalText);
            return false;
        });
        if (url != null && fName != null) {
            new Thread(() -> {
                String finalText = M3u8DownLoader.getUrlContentFileSize(url, fName);
                Message msg = Message.obtain();
                msg.obj = finalText;
                handler.sendMessage(msg);
            }).start();
        }
    }

    private void loadFileSize(String text) {
        String name = text.substring(0, text.lastIndexOf(" / "));
        String type = " / ";
        String size = text.substring(text.lastIndexOf(" / ") + 3);
        if (size.startsWith("http")) {
            url = size;
            size = "  ∞";
            fName = name;
        }
        if (name.contains(".")) {
            type = name.substring(name.lastIndexOf(".")) + type;
            name = name.substring(0, name.lastIndexOf("."));
        }
        dialog_text_filename.setText(name);
        dialog_text_fileType.setText(type);
        dialog_text_fileSize.setText(size);
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
        // 应用
        void ok(String text);
    }

}

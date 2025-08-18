package cn.cheng.simpleBrower.custom;

import android.app.Activity;
import android.content.Context;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import cn.cheng.simpleBrower.MyApplication;
import cn.cheng.simpleBrower.R;

/**
 * Created by YanGeCheng on 2022/2/17.
 * 自定义toast
 */
public class MyToast {

    private MyToast() {}

    public static Toast getInstance(String message) {
        Context context = MyApplication.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View toastView = inflater.inflate(R.layout.toast_view, null);
        TextView textView = toastView.findViewById(R.id.toast_msg);
        textView.setText(message);
        Toast toast = new Toast(context);
        toast.setView(toastView);
        toast.setDuration(Toast.LENGTH_SHORT);
        return toast;
    }

    public static Message getMessage(String msg) {
        Message message = new Message();
        message.obj = msg;
        return message;
    }
}

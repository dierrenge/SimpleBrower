package cn.cheng.simpleBrower.util;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

/**
 * Created by YanGeCheng on 2022/3/11.
 * 手机文件目录路径
 */
public class PhoneSysPath {


    /**
     * 手机根目录
     * （此方法不适用于Android10
     * 除非使用sdk29版本，并在清单文件application标签中添加 android:requestLegacyExternalStorage="true"）
     * @return
     */
    public static String getSDPath() {
        String  sdPath = null;
        // 判断sd卡是否存在
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            // 获取sd卡根目录
            sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        } else {
            // 获取系统根目录
            sdPath = Environment.getRootDirectory().getAbsolutePath();
        }
        return sdPath;
    }

    /**
     * 手机分区目录
     * @param context
     * @return
     */
    public static String getSandboxPath(Context context) {
        String sdPath = null;
        // 判断sd卡是否存在
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            // Android10及之后版本
            if (Build.VERSION.SDK_INT >= 29) {
                // 获取沙盒目录
                sdPath = context.getExternalFilesDir(null).getAbsolutePath();
            } else {
                 sdPath = getSDPath();
            }
        } else {
            // 获取系统根目录
            sdPath = Environment.getRootDirectory().getAbsolutePath();
        }
        return sdPath;
    }

}

package cn.cheng.simpleBrower.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.lang.reflect.Method;

/**
 * Created by YanGeCheng on 2021/9/6.
 *
 * 系统UI工具类
 */
public class SysWindowUi {

    /**
     * 系统状态栏和导航栏风格
     */
    public static final int NO_STATE__NO_NAVIGATION = 001; // 隐藏状态栏-隐藏导航栏
    public static final int IMMERSIVE_STATE__IMMERSIVE_NAVIGATION = 002; // 沉浸式状态栏-沉浸式导航栏
    public static final int NO_STATE__IMMERSIVE_NAVIGATION = 003; // 隐藏状态栏-沉浸式导航栏
    public static final int NO_STATE__NO_STATE = 004; // 原型


    /**
     * noStateBarAndNavigationBar 为true时，隐藏系统状态栏和导航栏用于加载页，
     * noStateBarAndNavigationBar 为false时，透明沉浸式系统状态栏和导航栏用于其他页面
     * @param activity
     * @param noStateBarAndNavigationBar
     */
    public static void hideStatusNavigationBar(Activity activity, Boolean noStateBarAndNavigationBar) {
        int uiFlags;
        if (noStateBarAndNavigationBar) {
            uiFlags =
                    // 稳定布局(当StatusBar和NavigationBar动态显示和隐藏时，系统为fitSystemWindow=true的view设置的padding大小都不会变化，所以view的内容的位置也不会发生移动。)
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            // 沉浸式(避免某些用户交互造成系统自动清除全屏状态。)
                            | View.SYSTEM_UI_FLAG_IMMERSIVE
                            // 主体内容占用系统导航栏的空间
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            // 在不隐藏StatusBar状态栏的情况下，将view所在window的显示范围扩展到StatusBar下面
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            // 状态栏字体颜色设置为黑色这个是Android 6.0才出现的属性   默认是白色
                            | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                            // 隐藏导航栏
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            // 沾粘标签（用户会向内滑动以展示系统栏，半透明的系统栏会临时的进行显示，一段时间后自动隐藏）
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            if (Build.VERSION.SDK_INT >= 28) {
                // 隐藏状态栏
                uiFlags |= View.SYSTEM_UI_FLAG_FULLSCREEN;
            } else {
                // 版本小于28不能设置占用刘海区，加载页使用白色背景，达到同样的效果
                uiFlags = uiFlags
                        // 状态栏显示处于低能显示状态(low profile模式)，状态栏上一些图标显示会被隐藏
                        | View.SYSTEM_UI_FLAG_LOW_PROFILE
                        // 在不隐藏StatusBar状态栏的情况下，将view所在window的显示范围扩展到StatusBar下面
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            }
        } else {
            // 状态栏字体颜色设置为黑色这个是Android 6.0才出现的属性   默认是白色
            uiFlags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        activity.getWindow().getDecorView().setSystemUiVisibility(uiFlags);

        // 需要设置这个 flag 才能调用 setStatusBarColor 来设置状态栏颜色
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        // 将导航栏设置成透明色
        if (noStateBarAndNavigationBar)
            activity.getWindow().setNavigationBarColor(Color.TRANSPARENT);

        // 将状态栏设置成透明色
        activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
    }


    /**
     * 设置系统状态栏和导航栏风格
     * @param activity
     * @param style
     */
    public static void setStatusBarNavigationBarStyle(Activity activity, int style) {
        if (activity == null || style == 0) return;
        Window window = activity.getWindow();
        if (style == NO_STATE__NO_STATE) {
            int uiFlags = View.SYSTEM_UI_FLAG_VISIBLE // 状态栏和Activity共存，Activity不全屏显示。也就是应用平常的显示画面
                    | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR; // 状态栏字体颜色设置为黑色这个是Android 6.0才出现的属性   默认是白色
            window.getDecorView().setSystemUiVisibility(uiFlags);
            // 导航栏半透明 关闭
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            // 状态栏半透明 关闭
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            // 需要设置这个 flag 才能调用 setStatusBarColor 来设置状态栏颜色
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            // 将状态栏设置成透明色
            window.setStatusBarColor(Color.TRANSPARENT);
        } else {
            if (Build.VERSION.SDK_INT < 16) {
                window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } else {
                if (style == SysWindowUi.NO_STATE__NO_NAVIGATION) {
                    int uiFlags =
                            // 稳定布局(当StatusBar和NavigationBar动态显示和隐藏时，系统为fitSystemWindow=true的view设置的padding大小都不会变化，所以view的内容的位置也不会发生移动。)
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    // 沉浸式(避免某些用户交互造成系统自动清除全屏状态。)
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE
                                    // 主体内容占用系统导航栏的空间
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    // 隐藏导航栏
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    // 设置IMMERSIVE和IMMERSIVE_STICKY来进入沉浸模式（隐藏了系统栏和其他UI控件）
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                    if (Build.VERSION.SDK_INT >= 28) {
                        // 隐藏状态栏
                        uiFlags |= View.SYSTEM_UI_FLAG_FULLSCREEN;
                    } else {
                        // 版本小于28不能设置占用刘海区，加载页使用白色背景，达到同样的效果
                        uiFlags = uiFlags
                                // 状态栏显示处于低能显示状态(low profile模式)，状态栏上一些图标显示会被隐藏
                                | View.SYSTEM_UI_FLAG_LOW_PROFILE
                                // 在不隐藏StatusBar状态栏的情况下，将view所在window的显示范围扩展到StatusBar下面
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                    }
                    window.getDecorView().setSystemUiVisibility(uiFlags);
                    // 导航栏半透明
                    window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                    // 状态栏半透明
                    window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                }
                if (style == SysWindowUi.IMMERSIVE_STATE__IMMERSIVE_NAVIGATION) {
                    if (Build.VERSION.SDK_INT >= 28) {
                        // 设置窗口占用刘海区
                        WindowManager.LayoutParams lp = window.getAttributes();
                        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                        window.setAttributes(lp);
                    }
                    int uiFlags =
                            // 稳定布局(当StatusBar和NavigationBar动态显示和隐藏时，系统为fitSystemWindow=true的view设置的padding大小都不会变化，所以view的内容的位置也不会发生移动。)
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    // 沉浸式(避免某些用户交互造成系统自动清除全屏状态。)
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE
                                    // 主体内容占用系统导航栏的空间
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    // 在不隐藏StatusBar状态栏的情况下，将view所在window的显示范围扩展到StatusBar下面
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    // 状态栏字体颜色设置为黑色这个是Android 6.0才出现的属性   默认是白色
                                    | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                    window.getDecorView().setSystemUiVisibility(uiFlags);
                    // 导航栏半透明
                    window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                    // 状态栏半透明
                    window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                }
                if (style == SysWindowUi.NO_STATE__IMMERSIVE_NAVIGATION) {
                    int uiFlags =
                            // 稳定布局(当StatusBar和NavigationBar动态显示和隐藏时，系统为fitSystemWindow=true的view设置的padding大小都不会变化，所以view的内容的位置也不会发生移动。)
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    // 沉浸式(避免某些用户交互造成系统自动清除全屏状态。)
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE
                                    // 主体内容占用系统导航栏的空间
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    // 沾粘标签（用户会向内滑动以展示系统栏，半透明的系统栏会临时的进行显示，一段时间后自动隐藏）
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                    if (Build.VERSION.SDK_INT >= 28) {
                        // 隐藏状态栏
                        uiFlags |= View.SYSTEM_UI_FLAG_FULLSCREEN;
                    } else {
                        // 版本小于28不能设置占用刘海区，加载页使用白色背景，达到同样的效果
                        uiFlags = uiFlags
                                // 状态栏显示处于低能显示状态(low profile模式)，状态栏上一些图标显示会被隐藏
                                | View.SYSTEM_UI_FLAG_LOW_PROFILE
                                // 在不隐藏StatusBar状态栏的情况下，将view所在window的显示范围扩展到StatusBar下面
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                    }
                    window.getDecorView().setSystemUiVisibility(uiFlags);
                    // 导航栏半透明
                    window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                    // 状态栏半透明
                    window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                }
            }
        }
    }


    /**
     * 获取状态栏高度
     * @param context
     * @return
     */
    public static int getStatusBarHeight(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen","android");
        int height = resources.getDimensionPixelSize(resourceId);
        return height;
    }


    /**
     * 获取虚拟键栏的高度
     * @param context
     * @return
     */
    public static int getNavigationBarHeight(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height","dimen", "android");
        int height = resources.getDimensionPixelSize(resourceId);
        return height;
    }


    /**
     * 获取是否存在NavigationBar
     * @param context
     * @return
     */
    public static boolean checkDeviceHasNavigationBar(Context context) {
        boolean hasNavigationBar = false;
        Resources rs = context.getResources();
        int id = rs.getIdentifier("config_showNavigationBar", "bool", "android");
        if (id > 0) {
            hasNavigationBar = rs.getBoolean(id);
        }
        try {
            Class systemPropertiesClass = Class.forName("android.os.SystemProperties");
            Method m = systemPropertiesClass.getMethod("get", String.class);
            String navBarOverride = (String) m.invoke(systemPropertiesClass, "qemu.hw.mainkeys");
            if ("1".equals(navBarOverride)) {
                hasNavigationBar = false;
            } else if ("0".equals(navBarOverride)) {
                hasNavigationBar = true;
            }
        } catch (Exception e) {

        }
        return hasNavigationBar;
    }

    // 获取屏幕高度
    public static int getScreenHeight(Activity activity) {
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        return dm.heightPixels;
    }

    // 获取屏幕宽度
    public static int getScreenWidth(Activity activity) {
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
    }

    // 获取视图的长宽
    public static int[] getLayoutPixelSize(Context context, int resource) {
        View view = LayoutInflater.from(context).inflate(resource, null);
        // 设置测量规格
        int widthSpec = View.MeasureSpec.makeMeasureSpec(
                Resources.getSystem().getDisplayMetrics().widthPixels, // 屏幕宽度为约束
                View.MeasureSpec.AT_MOST
        );

        int heightSpec = View.MeasureSpec.makeMeasureSpec(
                Resources.getSystem().getDisplayMetrics().heightPixels, // 屏幕高度
                View.MeasureSpec.AT_MOST
        );
        view.measure(widthSpec, heightSpec);
        int popupWidth = view.getMeasuredWidth();
        int popupHeight = view.getMeasuredHeight();
        return new int[]{popupWidth, popupHeight};
    }

}

package cn.cheng.simpleBrower.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.location.provider.ProviderProperties;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.util.HashMap;
import java.util.List;

import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.util.CommonUtils;
import cn.cheng.simpleBrower.util.CoordinateTransform;

public class MockLocationService extends Service {
    private WindowManager windowManager;
    private View floatingView;
    private Handler timer = new Handler();
    private LocationManager locationManager;
    private String[] mockProviders = new String[] {LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER};
    private volatile boolean stop;
    private double lat;
    private double lng;
    private double altitude; // 海拔

    @Override
    public void onCreate() {
        super.onCreate();
        // 创建悬浮窗视图
        floatingView = LayoutInflater.from(this).inflate(R.layout.location_dialog, null);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : // API 26+
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSPARENT
        );
        params.gravity = Gravity.TOP | Gravity.CENTER;
        // 添加悬浮窗
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(floatingView, params);

        // 必须设置为前台服务
        startForeground(1, createNotification());
    }

    private Notification createNotification() {
        // 创建通知渠道 (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("定位消息404", "悬浮窗服务",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
        // 创建一个Notification对象
        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(this, "定位消息404");
        // 设置打开该通知，该通知自动消失
        nBuilder.setAutoCancel(false);
        // 设置通知的图标
        nBuilder.setSmallIcon(R.mipmap.app_logo);
        // 设置使用系统默认的声音、默认震动
        nBuilder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
        // 设置发送时间
        nBuilder.setWhen(System.currentTimeMillis());

        return nBuilder.setCustomContentView(
                new RemoteViews(getPackageName(), R.layout.notification_location)).build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 获取经纬度
        lat = intent.getDoubleExtra("latitude", 36.667662);
        lng = intent.getDoubleExtra("longitude", 117.027707);
        altitude = intent.getDoubleExtra("altitude", 55.0D);
        // 坐标系转换 GCJ-02（高德使用的火星坐标系）转WGS-84（手机使用的地球坐标系）
        double[] ll = CoordinateTransform.gcj02ToWgs84(lng, lat);
        lng = ll[0];
        lat = ll[1];
        // 初始化虚拟位置设置
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        for (String providerStr : mockProviders) {
            init(providerStr, lat, lng, altitude);
        }
        // 循环设置最新位置
        timer.postDelayed(runnable, 100);
        return START_STICKY;
    }

    private void init(String providerStr, double lat, double lng, double altitude) {
        try {
            // 1. 创建模拟位置提供器
            LocationProvider provider = locationManager.getProvider(providerStr);
            if (provider != null) {
                locationManager.addTestProvider(
                        provider.getName() // 提供者名称
                        , provider.requiresNetwork() // 提供者是否需要网络
                        , provider.requiresSatellite() // 提供者是否需要卫星
                        , provider.requiresCell() // 提供者是否需要任何定位辅助数据
                        , provider.hasMonetaryCost() // 提供者是否收费
                        , provider.supportsAltitude() // 是否支持海拔
                        , provider.supportsSpeed() // 是否支持速度
                        , provider.supportsBearing() // 是否支持方向
                        , provider.getPowerRequirement() // 设备电源的消耗级别
                        , provider.getAccuracy()); // 精度
            } else {
                locationManager.addTestProvider(
                        providerStr // 提供者名称
                        , true // 提供者是否需要网络
                        , true // 提供者是否需要卫星
                        , true // 提供者是否需要任何定位辅助数据
                        , false // 提供者是否收费
                        , true // 是否支持海拔
                        , true // 是否支持速度
                        , true // 是否支持方向
                        , ProviderProperties.POWER_USAGE_HIGH // 设备电源的消耗级别
                        , ProviderProperties.ACCURACY_FINE); // 精度
            }

            // 2. 创建虚拟位置
            Location mockLocation = createLocation(providerStr, lat, lng, altitude);

            // 3、激活模拟位置提供器
            locationManager.setTestProviderEnabled(providerStr, true);
            locationManager.setTestProviderStatus(providerStr, LocationProvider.AVAILABLE, null, System.currentTimeMillis());
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                CommonUtils.saveLog("requestLocationUpdates=== 缺少权限");
                return;
            }
            locationManager.requestLocationUpdates(providerStr, 100, 1f, locationListener);
            // 4、设置最新位置，一定要在requestLocationUpdate完成后进行，才能收到监听
            locationManager.setTestProviderLocation(providerStr, mockLocation);

        } catch (Exception e) {
            CommonUtils.saveLog(providerStr + "=====init======" + e.getMessage());
        }
    }

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            // 当位置更新时触发（仅限自身应用）
        }
    };

    // 创建虚拟位置
    private Location createLocation(String providerStr, double lat, double lng, double altitude) {
        Location mockLocation = new Location(providerStr);
        mockLocation.setLatitude(lat); // 纬度
        mockLocation.setLongitude(lng); // 经度
        mockLocation.setAltitude(altitude); // 海拔
        mockLocation.setSpeed(1.2f); // 速度 m/s
        mockLocation.setBearing(0.0f); // 方向
        mockLocation.setTime(System.currentTimeMillis());
        mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        if (providerStr.equals(LocationManager.NETWORK_PROVIDER)) {
            mockLocation.setAccuracy(2.0f); // 精度
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
                mockLocation.setVerticalAccuracyMeters(1f); // 需设置垂直精度
                mockLocation.setSpeedAccuracyMetersPerSecond(0.2f);// 需设置速度精度
            }
        } else {
            mockLocation.setAccuracy(1.0f); // 精度
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
                mockLocation.setVerticalAccuracyMeters(0.5f); // 需设置垂直精度
                mockLocation.setSpeedAccuracyMetersPerSecond(0.1f);// 需设置速度精度
            }
            Bundle extras = new Bundle();
            extras.putInt("satellites", 7); // 卫星数量
            extras.putBoolean("mockLocation", false); // 禁用位置来源检查
            mockLocation.setExtras(extras);
        }
        return mockLocation;
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            try {
                for (String providerStr : mockProviders) {
                    Location mockLocation = createLocation(providerStr, lat, lng, altitude);
                    locationManager.setTestProviderLocation(providerStr, mockLocation);
                }
            } catch (Exception e) {
                CommonUtils.saveLog("useMockPosition========" + e.getMessage());
            }
            if (!stop) timer.postDelayed(this, 100);
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (floatingView != null) windowManager.removeView(floatingView);
        stop = true;
        timer.removeCallbacks(runnable);
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
            try {
                for (String providerStr : mockProviders) {
                    locationManager.removeTestProvider(providerStr);
                }
            } catch (Exception e) {
                CommonUtils.saveLog("removeTestProvider========" + e.getMessage());
            }
        }
        super.onDestroy();
    }
}


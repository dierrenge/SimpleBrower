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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.util.HashMap;
import java.util.List;

import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.util.CommonUtils;

public class MockLocationService extends Service {
    private LocationManager locationManager;
    private Handler timer = new Handler();
    private HashMap<String, Location> mockLocationMap = new HashMap<>();

    private WindowManager windowManager;
    private View floatingView;

    private volatile boolean stop;

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
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.END;

        // 添加悬浮窗
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(floatingView, params);

        // 必须设置为前台服务
        startForeground(1, createNotification());
    }

    private Notification createNotification() {
        // 创建通知渠道 (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "FLOAT_SERVICE_CHANNEL",
                    "悬浮窗服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        return new NotificationCompat.Builder(this, "FLOAT_SERVICE_CHANNEL")
                .setContentTitle("悬浮窗服务运行中")
                .setSmallIcon(R.mipmap.app_logo)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        double lat = intent.getDoubleExtra("latitude", 0);
        double lng = intent.getDoubleExtra("longitude", 0);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        CommonUtils.saveLog("lat========" + lat);
        CommonUtils.saveLog("lng========" + lng);
        List<String> mockProviders = locationManager.getAllProviders();
        // CommonUtils.saveLog("mockProviders========" + mockProviders);
        for (String providerStr : mockProviders) {
            init(providerStr, lat, lng);
        }
        // 循环设置最新位置
        timer.postDelayed(runnable, 500);
        new Handler().postDelayed(() -> {
            stop = true;
            timer.removeCallbacks(runnable);
        }, 1000*10);
        return START_STICKY;
    }

    private void init(String providerStr, double lat, double lng) {
        try {
            LocationProvider provider = locationManager.getProvider(providerStr);
            // 1. 创建模拟位置提供器
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
            Location mockLocation = new Location(providerStr);
            mockLocation.setLatitude(lat); // 纬度
            mockLocation.setLongitude(lng); // 经度
            mockLocation.setAltitude(50.0); // 海拔
            mockLocation.setAccuracy(1.0f); // 精度
            mockLocation.setSpeed(0.5f); // 速度 m/s
            mockLocation.setBearing(90.0f); // 方向
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
                mockLocation.setVerticalAccuracyMeters(0.5f); // 需设置垂直精度
                mockLocation.setSpeedAccuracyMetersPerSecond(0.1f);// 需设置速度精度
            }
            mockLocation.setTime(System.currentTimeMillis());
            mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mockLocation.setBearingAccuracyDegrees(90.0f);
                mockLocation.setElapsedRealtimeUncertaintyNanos(SystemClock.elapsedRealtimeNanos());
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                mockLocation.setMslAltitudeAccuracyMeters(50f);
                mockLocation.setMslAltitudeMeters(50D);
            }
            Bundle extras = new Bundle();
            // extras.putInt("satellites", 3); // 卫星数量
            // extras.putString("city", "成都");
            // extras.putString("address", "四川省成都市武侯区锦城大道辅路1057号靠近泰达时代中心2");
            extras.putBoolean("mockLocation", false); // 禁用位置来源检查
            mockLocation.setExtras(extras);

            // 3、激活模拟位置提供器
            locationManager.setTestProviderEnabled(providerStr, true);
            locationManager.setTestProviderStatus(providerStr, LocationProvider.AVAILABLE, null, System.currentTimeMillis());
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                CommonUtils.saveLog("requestLocationUpdates=== 缺少权限");
                return;
            }
            //if (LocationManager.GPS_PROVIDER.equals(providerStr)) {
                locationManager.requestLocationUpdates(providerStr, 1000, 10f, new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        // 当位置更新时触发（仅限自身应用）
                        // CommonUtils.saveLog(location.getLatitude() + "===onLocationChanged===" + location.getLongitude());
                    }

                    @Override
                    public void onProviderEnabled(String provider) {
                        // 当位置提供者（如GPS）被启用时触发
                        // 但不能识别是哪个应用触发的
                        CommonUtils.saveLog("===onProviderEnabled=====" + provider);
                    }
                });
            //}
            mockLocationMap.put(providerStr, mockLocation);
            // 4、设置最新位置，一定要在requestLocationUpdate完成后进行，才能收到监听
            locationManager.setTestProviderLocation(providerStr, mockLocation);

        } catch (Exception e) {
            CommonUtils.saveLog(providerStr + "=====init======" + e.getMessage());
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (floatingView != null) windowManager.removeView(floatingView);
        timer.removeCallbacks(runnable);
        try {
            List<String> mockProviders = locationManager.getAllProviders();
            for (String providerStr : mockProviders) {
                Location mockLocation = mockLocationMap.get(providerStr);
                if (mockLocation != null) {
                    locationManager.removeTestProvider(providerStr);
                }
            }
        } catch (Exception e) {
            CommonUtils.saveLog("removeTestProvider========" + e.getMessage());
        }
        super.onDestroy();
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            try {
                List<String> mockProviders = locationManager.getAllProviders();
                for (String providerStr : mockProviders) {
                    Location mockLocation = mockLocationMap.get(providerStr);
                    if (mockLocation != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            mockLocation.setMock(true);
                        }
                        mockLocation.setLatitude(mockLocation.getLatitude() + 0.0001);
                        locationManager.setTestProviderLocation(providerStr, mockLocation);
                    }
                }
            } catch (Exception e) {
                CommonUtils.saveLog("useMockPosition========" + e.getMessage());
            }
            if (!stop) timer.postDelayed(this, 1000);
        }
    };
}


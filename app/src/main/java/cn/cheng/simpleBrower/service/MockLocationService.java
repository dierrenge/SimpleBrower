package cn.cheng.simpleBrower.service;

import android.app.Service;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.location.provider.ProviderProperties;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.annotation.Nullable;

import java.util.List;

import cn.cheng.simpleBrower.util.CommonUtils;

public class MockLocationService extends Service {
    private LocationManager locationManager;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        double lat = intent.getDoubleExtra("latitude", 0);
        double lng = intent.getDoubleExtra("longitude", 0);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        List<String> mockProviders = locationManager.getAllProviders();
        try {
            if (mockProviders.isEmpty()) {
                String providerStr = "bishu_provider";
                init(providerStr, lat, lng);
            } else {
                for (String providerStr : mockProviders) {
                    init(providerStr, lat, lng);
                }
            }
        } catch (SecurityException e) {
            CommonUtils.saveLog("useMockPosition========" + e.getMessage());
        }

        return START_STICKY;
    }

    private void init(String providerStr, double lat, double lng) {
        CommonUtils.saveLog("\nproviderStr==" + providerStr);
        LocationProvider provider = locationManager.getProvider(providerStr);
        // 1. 获取位置提供者
        if (provider != null) {
            locationManager.addTestProvider(
                    provider.getName()
                    , provider.requiresNetwork()
                    , provider.requiresSatellite()
                    , provider.requiresCell()
                    , provider.hasMonetaryCost()
                    , provider.supportsAltitude()
                    , provider.supportsSpeed()
                    , provider.supportsBearing()
                    , provider.getPowerRequirement()
                    , provider.getAccuracy());
        } else {
            if (providerStr.equals(LocationManager.GPS_PROVIDER)) {
                locationManager.addTestProvider(
                        providerStr
                        , true, true, false, false, true, true, true
                        , ProviderProperties.POWER_USAGE_HIGH, ProviderProperties.ACCURACY_FINE);
            } else if (providerStr.equals(LocationManager.NETWORK_PROVIDER)) {
                locationManager.addTestProvider(
                        providerStr
                        , true, false, true, false, false, false, false
                        , ProviderProperties.POWER_USAGE_LOW, ProviderProperties.ACCURACY_FINE);
            } else {
                locationManager.addTestProvider(
                        providerStr
                        , false, false, false, false, true, true, true
                        , ProviderProperties.POWER_USAGE_LOW, ProviderProperties.ACCURACY_FINE);
            }
        }
        locationManager.setTestProviderEnabled(providerStr, true);
        locationManager.setTestProviderStatus(providerStr, LocationProvider.AVAILABLE, null, System.currentTimeMillis());

        // 2. 设置虚拟位置
        Location mockLocation = new Location(providerStr);
        mockLocation.setLatitude(lat);
        mockLocation.setLongitude(lng);
        mockLocation.setAccuracy(5.0f);
        mockLocation.setTime(System.currentTimeMillis());
        if (Build.VERSION.SDK_INT >= 17) {
            mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        }
        locationManager.setTestProviderLocation(providerStr, mockLocation);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}


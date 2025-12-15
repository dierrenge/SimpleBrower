package cn.cheng.simpleBrower.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;

import java.text.SimpleDateFormat;
import java.util.Date;

import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.service.MockLocationService;
import cn.cheng.simpleBrower.util.CommonUtils;
import cn.cheng.simpleBrower.util.SysWindowUi;

public class MapActivity extends AppCompatActivity implements AMap.OnMapClickListener, LocationSource, AMapLocationListener {
    private Button back;
    private MapView mapView;
    private AMap aMap;
    private LatLng selectedLocation; // 用户选择的坐标
    private Intent intentS;
    private OnLocationChangedListener mListener;//声明位置监听
    private AMapLocationClient mlocationClient;//声明定位客户端
    private AMapLocationClientOption mLocationOption;//声明定位参数配置选项
    private boolean isFirstLoc = true;//判断是否第一次定位
    private double currentAltitude;//当前海拔
    private LatLng currentLatLng;//当前定位
    private MyLocationStyle myLocationStyle;// 定位样式
    private Marker marker; // 标记

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 状态栏设置透明
        SysWindowUi.hideStatusNavigationBar(this, false);
        setContentView(R.layout.activity_map);
        // 返回按钮
        back = findViewById(R.id.map_back);
        back.setOnClickListener(view -> {
            this.finish();
        });

        // 地图设置隐私合规接口
        AMapLocationClient.updatePrivacyAgree(this, true);
        AMapLocationClient.updatePrivacyShow(this, true, true);
        // 创建地图
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        if (aMap == null) aMap = mapView.getMap();
        // 设置地图点击监听
        aMap.setOnMapClickListener(this);
        // 设置地图定位监听
        aMap.setLocationSource(this);
        // 初始化地图控制器对象
        // aMap.setTrafficEnabled(true);// 显示实时交通状况
        aMap.getUiSettings().setZoomControlsEnabled(false);// 设置地图缩放按钮不显示
        //初始化定位蓝点样式类
        myLocationStyle = new MyLocationStyle();
        myLocationStyle.interval(2000); // 2秒定位一次 只在连续定位模式下生效
        myLocationStyle.showMyLocation(true);// 设置是否显示定位指针
        myLocationStyle.anchor(0.5f, 0.5f); // 将定位指针移动到屏幕中心
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER); // 设置定位模式(定位、但不会移动到地图中心点，定位指针依照设备方向旋转，并且会跟随设备移动)
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.setMyLocationEnabled(true);// 是否启动显示定位蓝点,默认是false。
        // 设置虚拟位置按钮
        findViewById(R.id.btnSetMockLocation).setOnClickListener(v -> {
            if (selectedLocation != null) {
                startMockLocationService();
            }
        });
    }

    /**
     * 激活定位
     */
    @Override
    public void activate(LocationSource.OnLocationChangedListener listener) {
        mListener = listener;
        if (mlocationClient == null) {
            //初始化定位
            try {
                mLocationOption = new AMapLocationClientOption();//初始化定位参数
                mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);//设置为高精度定位模式
                mLocationOption.setInterval(2000); // 2秒定位一次
                mLocationOption.setNeedAddress(true); // 返回地址信息
                mLocationOption.setWifiScan(true); // 允许WIFI扫描
                mLocationOption.setSensorEnable(true); // 启用传感器获取方向
                mLocationOption.setOffset(true); // 允许坐标偏移纠正
                mLocationOption.setLocationCacheEnable(false); // 关闭缓存
                mLocationOption.setHttpTimeOut(20000); // 超时时间
                mlocationClient = new AMapLocationClient(this);//声明定位客户端
                mlocationClient.setLocationListener(this);//设置定位回调监听
                mlocationClient.setLocationOption(mLocationOption);//设置定位参数
                mlocationClient.startLocation();//启动定位
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }
    }

    /**
     * 停止定位
     */
    @Override
    public void deactivate() {
        mListener = null;
        if (mlocationClient != null) {
            mlocationClient.stopLocation();
            mlocationClient.onDestroy();
        }
        mlocationClient = null;
    }

    /**
     * 监听定位回调
     */
    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (mListener != null && aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0) {
                //定位成功回调信息，设置相关消息
                // aMapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见官方定位类型表
                // aMapLocation.getLatitude();//获取纬度
                // aMapLocation.getLongitude();//获取经度
                // aMapLocation.getAccuracy();//获取精度信息
                // aMapLocation.getAddress();//地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。
                // aMapLocation.getCountry();//国家信息
                // aMapLocation.getProvince();//省信息
                // aMapLocation.getCity();//城市信息
                // aMapLocation.getDistrict();//城区信息
                // aMapLocation.getStreet();//街道信息
                // aMapLocation.getStreetNum();//街道门牌号信息
                // aMapLocation.getCityCode();//城市编码
                // aMapLocation.getAdCode();//地区编码
                currentAltitude = aMapLocation.getAltitude();// 海拔
                currentLatLng = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude()); //获取当前定位
                // 首次定位移动到当前位置
                if (isFirstLoc) {
                    aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16));
                }
                // 移动地图中心点
                // aMap.moveCamera(CameraUpdateFactory.changeLatLng(currentLatLng));
                // 更新地图指针位置
                mListener.onLocationChanged(aMapLocation);
                isFirstLoc = false;
            } else {
                // 错误信息
                CommonUtils.saveLog("AmapError location Error, ErrCode:"
                        + aMapLocation.getErrorCode() + ", errInfo:"
                        + aMapLocation.getErrorInfo());
            }
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        selectedLocation = latLng;
        if (marker != null) {
            marker.remove();// 清除旧标记
        }
        marker = aMap.addMarker(new MarkerOptions().position(latLng));
        marker.showInfoWindow();// 显示信息窗口
    }

    // 处理地图生命周期方法
    @Override
    protected void onResume() {
        mapView.onResume();
         if (mlocationClient != null && !isFirstLoc) {
             mlocationClient.startLocation();
         }
        super.onResume();
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        if (mlocationClient != null && !isFirstLoc) {
            mlocationClient.stopLocation();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mapView.onDestroy();
        if (intentS != null) {
            stopService(intentS);
        }
        deactivate();
        super.onDestroy();
    }
    /**
     * 生命周期-onSaveInstanceState
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mapView.onSaveInstanceState(outState);//保存地图当前的状态
        super.onSaveInstanceState(outState);
    }

    private void startMockLocationService() {
        if (intentS == null) {
            intentS = new Intent(this, MockLocationService.class);
        }
        intentS.putExtra("latitude", selectedLocation.latitude);
        intentS.putExtra("longitude", selectedLocation.longitude);
        intentS.putExtra("altitude", currentAltitude);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intentS);
        } else {
            startService(intentS);
        }
    }
}
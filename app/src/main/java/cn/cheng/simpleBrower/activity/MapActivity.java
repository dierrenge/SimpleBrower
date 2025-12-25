package cn.cheng.simpleBrower.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeQuery;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import cn.cheng.simpleBrower.R;
import cn.cheng.simpleBrower.bean.LocationBean;
import cn.cheng.simpleBrower.custom.LocationListDialog;
import cn.cheng.simpleBrower.custom.MyToast;
import cn.cheng.simpleBrower.service.MockLocationService;
import cn.cheng.simpleBrower.util.CommonUtils;
import cn.cheng.simpleBrower.util.SysWindowUi;

public class MapActivity extends AppCompatActivity {
    private MapView mapView;
    private AMap aMap;
    private LatLng selectedLocation; // 用户选择的坐标
    private double altitude; // 实时定位的海拔
    private Intent intentS;
    private LocationSource locationSource; // 定位源
    private LocationSource.OnLocationChangedListener mListener; // 位置监听
    private AMapLocationClient mlocationClient; // 定位客户端
    private AMapLocationClientOption mLocationOption; // 定位参数配置选项
    private MyLocationStyle myLocationStyle; // 定位样式
    private GeocodeSearch geocodeSearch; // 逆地理编码服务
    private ActivityResultLauncher<Intent> allFilesAccessLauncher; // 授权回调
    private boolean isFirstLoc = true; //判断是否第一次定位
    private Marker marker; // 标记
    private List<LocationBean> locationList; // 历史定位记录

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 状态栏设置透明
        SysWindowUi.hideStatusNavigationBar(this, false);
        setContentView(R.layout.activity_map);
        // 初始化地图设置
        initMap(savedInstanceState);
        // 初始化逆地理编码服务
        initSearch(this);
        // 返回
        findViewById(R.id.map_back).setOnClickListener(v -> this.finish());
        // 虚拟定位
        findViewById(R.id.btnSetMockLocation).setOnClickListener(v -> {
            if (selectedLocation != null) {
                checkMockLocation();
            } else {
                MyToast.getInstance("请先选择定位地点").show();
            }
        });
        // 历史定位记录
        locationList = CommonUtils.locationListFileWalk();
        findViewById(R.id.btnRecord).setOnClickListener(v -> {
            LocationListDialog dialog = new LocationListDialog(this, locationList);
            dialog.setOnCallListener(bean -> {
                LatLng latLng = new LatLng(bean.getLatitude(), bean.getLongitude(), false);
                // 标记
                onMapClick(latLng);
                // 定位
                checkMockLocation();
            });
            dialog.show();
        });
        // 注册权限请求的返回监听
        initFilesAccessLauncher();
    }

    private void initMap(Bundle savedInstanceState) {
        // 地图设置隐私合规接口
        AMapLocationClient.updatePrivacyAgree(this, true);
        AMapLocationClient.updatePrivacyShow(this, true, true);
        // 创建地图
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        if (aMap == null) aMap = mapView.getMap();
        // 设置地图点击监听
        aMap.setOnMapClickListener(this::onMapClick);
        // 定位源
        locationSource = new LocationSource() {
            // 激活定位
            @Override
            public void activate(LocationSource.OnLocationChangedListener listener) {
                mListener = listener;
                if (mlocationClient != null) return;
                try {
                    mLocationOption = new AMapLocationClientOption(); //初始化定位参数
                    mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy); //设置为高精度定位模式
                    mLocationOption.setInterval(500); // 半秒定位一次
                    mLocationOption.setNeedAddress(true); // 返回地址信息
                    mLocationOption.setWifiScan(true); // 允许WIFI扫描
                    mLocationOption.setSensorEnable(true); // 启用传感器获取方向
                    mLocationOption.setOffset(true); // 允许坐标偏移纠正
                    mLocationOption.setLocationCacheEnable(false); // 关闭缓存
                    mLocationOption.setHttpTimeOut(3000); // 超时时间
                    // mLocationOption.setLocationPurpose(AMapLocationClientOption.AMapLocationPurpose.Sport); // 运动场景优化
                    mlocationClient = new AMapLocationClient(MapActivity.this); // 定位客户端
                    mlocationClient.setLocationListener(aMapLocation -> { // 设置定位回调监听
                        if (mListener == null || aMapLocation == null) return;
                        if (aMapLocation.getErrorCode() == 0) {
                            //定位成功回调信息，设置相关消息
                            // aMapLocation.getLocationType(); //获取当前定位结果来源，如网络定位结果，详见官方定位类型表
                            // aMapLocation.getLatitude(); //获取纬度
                            // aMapLocation.getLongitude(); //获取经度
                            // aMapLocation.getAccuracy(); //获取精度信息
                            // aMapLocation.getAddress(); //地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。
                            // aMapLocation.getCountry(); //国家信息
                            // aMapLocation.getProvince(); //省信息
                            // aMapLocation.getCity(); //城市信息
                            // aMapLocation.getDistrict(); //城区信息
                            // aMapLocation.getStreet(); //街道信息
                            // aMapLocation.getStreetNum(); //街道门牌号信息
                            // aMapLocation.getCityCode(); //城市编码
                            // aMapLocation.getAdCode(); //地区编码
                            altitude = aMapLocation.getAltitude(); // 海拔
                            LatLng cLatLng = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude()); //获取当前定位
                            // 首次定位移动到当前位置
                            if (isFirstLoc) {
                                aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cLatLng, 16));
                            }
                            // 移动地图中心点
                            // aMap.moveCamera(CameraUpdateFactory.changeLatLng(currentLatLng));
                            // 更新地图指针位置
                            mListener.onLocationChanged(aMapLocation);
                            isFirstLoc = false;
                        } else {
                            // 错误信息
                            CommonUtils.saveLog(aMapLocation.getErrorCode() + "-l-" + aMapLocation.getErrorInfo());
                        }
                    });
                    mlocationClient.setLocationOption(mLocationOption); //设置定位参数
                    mlocationClient.startLocation(); //启动定位
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            // 停止定位
            @Override
            public void deactivate() {
                mListener = null;
                if (mlocationClient != null) {
                    mlocationClient.stopLocation();
                    mlocationClient.onDestroy();
                }
                mlocationClient = null;
            }
        };
        // 设置地图定位监听
        aMap.setLocationSource(locationSource);
        // 初始化地图控制器对象
        // aMap.setTrafficEnabled(true); // 显示实时交通状况
        aMap.getUiSettings().setZoomControlsEnabled(false); // 设置地图缩放按钮不显示
        //初始化定位蓝点样式类
        myLocationStyle = new MyLocationStyle();
        myLocationStyle.interval(500); // 半秒定位一次 只在连续定位模式下生效
        myLocationStyle.showMyLocation(true); // 设置是否显示定位指针
        myLocationStyle.anchor(0.5f, 0.5f); // 将定位指针移动到屏幕中心
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER); // 设置定位模式(定位、但不会移动到地图中心点，定位指针依照设备方向旋转，并且会跟随设备移动)
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.setMyLocationEnabled(true); // 是否启动显示定位蓝点,默认是false
    }

    private void initSearch(Context context) {
        try {
            geocodeSearch = new GeocodeSearch(context);
            geocodeSearch.setOnGeocodeSearchListener(new GeocodeSearch.OnGeocodeSearchListener() {
                @Override
                public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
                    if (i == 1000) { // 请求成功
                        // 坐标
                        LatLonPoint point = regeocodeResult.getRegeocodeQuery().getPoint();
                        double latitude = point.getLatitude();
                        double longitude = point.getLongitude();
                        LocationBean cBean = new LocationBean(latitude, longitude);
                        if (locationList.contains(cBean)) return;
                        // 完整地址
                        RegeocodeAddress address = regeocodeResult.getRegeocodeAddress();
                        String fullAddress = address.getFormatAddress();
                        // 保存定位记录（不重复）
                        String name = "" + longitude + latitude;
                        String dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
                        String url = dir + "/SimpleBrower/0_like/locationList/" + name + ".json";
                        LocationBean oldBean = CommonUtils.readObjectFromLocal(LocationBean.class, url);
                        if (oldBean == null) {
                            LocationBean bean = new LocationBean();
                            bean.setLatitude(latitude);
                            bean.setLongitude(longitude);
                            bean.setAddress(fullAddress);
                            bean.setTime(new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()));
                            CommonUtils.writeObjectIntoLocal("locationList", name, bean);
                        }
                        // 更新记录
                        locationList = CommonUtils.locationListFileWalk();
                    } else {
                        MyToast.getInstance("地址获取失败"+i).show();
                    }
                }
                @Override
                public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

                }
            });
        } catch (Exception e) {
            CommonUtils.saveLog("initSearch:" + e.getMessage());
        }
    }

    // 注册权限请求的返回监听
    private void initFilesAccessLauncher() {
        // 注册权限请求的返回监听
        allFilesAccessLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> { // 授权返回后的处理
                    if (CommonUtils.isMockLocationApp(this) && CommonUtils.hasOverlayPermission(this)) {
                        startMockLocationService();
                    }
                }
        );
    }

    // 标记地图位置
    public void onMapClick(LatLng latLng) {
        selectedLocation = latLng;
        if (marker != null) {
            marker.remove(); // 清除旧标记
        }
        marker = aMap.addMarker(new MarkerOptions().position(latLng));
        marker.showInfoWindow(); // 显示信息窗口
    }

    private void checkMockLocation() {
        if (CommonUtils.isMockLocationApp(this)) {
            checkOverlayPermission();
        } else {
            CommonUtils.openDeveloperOptions(this, allFilesAccessLauncher);
        }
    }

    private void checkOverlayPermission() {
        if (CommonUtils.hasOverlayPermission(this)) {
            startMockLocationService();
        } else {
            CommonUtils.requestOverlayPermission(this, allFilesAccessLauncher);
        }
    }

    private void startMockLocationService() {
        // 开启服务
        if (intentS == null) {
            intentS = new Intent(this, MockLocationService.class);
        }
        intentS.putExtra("latitude", selectedLocation.latitude);
        intentS.putExtra("longitude", selectedLocation.longitude);
        intentS.putExtra("altitude", altitude);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intentS);
        } else {
            startService(intentS);
        }
        // 移动到标记点
        aMap.animateCamera(CameraUpdateFactory.changeLatLng(selectedLocation));
        onResume();
        // 发起请求（传入经纬度查询地址）
        LatLonPoint point = new LatLonPoint(selectedLocation.latitude, selectedLocation.longitude);
        RegeocodeQuery query = new RegeocodeQuery(point, 200, GeocodeSearch.AMAP);
        geocodeSearch.getFromLocationAsyn(query);
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
        locationSource.deactivate();
        super.onDestroy();
    }
    /**
     * 生命周期-onSaveInstanceState
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mapView.onSaveInstanceState(outState); //保存地图当前的状态
        super.onSaveInstanceState(outState);
    }

}
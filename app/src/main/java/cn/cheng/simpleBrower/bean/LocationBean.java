package cn.cheng.simpleBrower.bean;

import androidx.annotation.Nullable;

public class LocationBean {
    private String address; // 地址
    private double latitude; // 纬度
    private double longitude; // 经度
    private String time; // 时间 yyyyMMddHHmmssSSS

    public LocationBean() {

    }

    public LocationBean(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null){
            return false;
        }
        if (getClass() != obj.getClass()){
            return false;
        }
        LocationBean bean = (LocationBean) obj;
        if (bean.getLongitude() == longitude && bean.getLatitude() == latitude) {
            return true;
        }
        return false;
    }
}

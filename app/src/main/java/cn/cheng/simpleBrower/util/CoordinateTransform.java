package cn.cheng.simpleBrower.util;

/**
 * 定位坐标系转换工具csdn
 * Created by DarthP
 */
public class CoordinateTransform {

    private static final double PI = 3.1415926535897932384626;
    private static final double A = 6378245.0;
    private static final double EE = 0.00669342162296594323;

    /**
     * WGS84转GCJ02(火星坐标系)
     * @param lng 经度
     * @param lat 纬度
     * @return [经度, 纬度]
     */
    public static double[] wgs84ToGcj02(double lng, double lat) {
        if (outOfChina(lng, lat)) {
            return new double[]{lng, lat};
        }
        double dLat = transformLat(lng - 105.0, lat - 35.0);
        double dLng = transformLng(lng - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * PI;
        double magic = Math.sin(radLat);
        magic = 1 - EE * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI);
        dLng = (dLng * 180.0) / (A / sqrtMagic * Math.cos(radLat) * PI);
        double mgLat = lat + dLat;
        double mgLng = lng + dLng;
        return new double[]{mgLng, mgLat};
    }

    /**
     * GCJ02转WGS84
     * @param lng 经度
     * @param lat 纬度
     * @return [经度, 纬度]
     */
    public static double[] gcj02ToWgs84(double lng, double lat) {
        double[] gcj = wgs84ToGcj02(lng, lat);
        return new double[]{lng * 2 - gcj[0], lat * 2 - gcj[1]};
    }

    /**
     * GCJ02转BD09
     * @param lng 经度
     * @param lat 纬度
     * @return [经度, 纬度]
     */
    public static double[] gcj02ToBd09(double lng, double lat) {
        double z = Math.sqrt(lng * lng + lat * lat) + 0.00002 * Math.sin(lat * PI);
        double theta = Math.atan2(lat, lng) + 0.000003 * Math.cos(lng * PI);
        double bdLng = z * Math.cos(theta) + 0.0065;
        double bdLat = z * Math.sin(theta) + 0.006;
        return new double[]{bdLng, bdLat};
    }

    /**
     * BD09转GCJ02
     * @param lng 经度
     * @param lat 纬度
     * @return [经度, 纬度]
     */
    public static double[] bd09ToGcj02(double lng, double lat) {
        double x = lng - 0.0065;
        double y = lat - 0.006;
        double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * PI);
        double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * PI);
        double ggLng = z * Math.cos(theta);
        double ggLat = z * Math.sin(theta);
        return new double[]{ggLng, ggLat};
    }

    /**
     * WGS84转BD09
     * @param lng 经度
     * @param lat 纬度
     * @return [经度, 纬度]
     */
    public static double[] wgs84ToBd09(double lng, double lat) {
        double[] gcj = wgs84ToGcj02(lng, lat);
        return gcj02ToBd09(gcj[0], gcj[1]);
    }

    /**
     * BD09转WGS84
     * @param lng 经度
     * @param lat 纬度
     * @return [经度, 纬度]
     */
    public static double[] bd09ToWgs84(double lng, double lat) {
        double[] gcj = bd09ToGcj02(lng, lat);
        return gcj02ToWgs84(gcj[0], gcj[1]);
    }

    private static boolean outOfChina(double lng, double lat) {
        if (lng < 72.004 || lng > 137.8347) {
            return true;
        }
        return lat < 0.8293 || lat > 55.8271;
    }

    private static double transformLat(double lng, double lat) {
        double ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + 0.1 * lng * lat + 0.2 * Math.sqrt(Math.abs(lng));
        ret += (20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(lat * PI) + 40.0 * Math.sin(lat / 3.0 * PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(lat / 12.0 * PI) + 320 * Math.sin(lat * PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    private static double transformLng(double lng, double lat) {
        double ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng));
        ret += (20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(lng * PI) + 40.0 * Math.sin(lng / 3.0 * PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(lng / 12.0 * PI) + 300.0 * Math.sin(lng / 30.0 * PI)) * 2.0 / 3.0;
        return ret;
    }

    public static void main(String[] args) {
        // 测试代码
        double lng = 116.404;
        double lat = 39.915;

        System.out.println("原始WGS84坐标: " + lng + ", " + lat);

        double[] gcj = wgs84ToGcj02(lng, lat);
        System.out.println("转换为GCJ02: " + gcj[0] + ", " + gcj[1]);

        double[] bd = gcj02ToBd09(gcj[0], gcj[1]);
        System.out.println("转换为BD09: " + bd[0] + ", " + bd[1]);

        double[] wgs = bd09ToWgs84(bd[0], bd[1]);
        System.out.println("转回WGS84: " + wgs[0] + ", " + wgs[1]);
    }
}

package cn.cheng.simpleBrower.util;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.util.Base64;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import cn.cheng.simpleBrower.MyApplication;
import cn.cheng.simpleBrower.bean.LocationBean;
import cn.cheng.simpleBrower.bean.NotificationBean;
import cn.cheng.simpleBrower.bean.PositionBean;
import cn.cheng.simpleBrower.custom.FeetDialog;

public class CommonUtils {

    public static final int STORAGE_PERMISSION_REQUEST_CODE = 1;
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 2;

    /**
     * 删除List集合 指定角标元素
     *
     * @param list
     * @param index
     */
    public static void listRemove(List list, int index) {
        Iterator iterator = list.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            // System.out.println(i + "===========================================");
            if (i == index) {
                iterator.remove();
            }
            i++;
        }
    }

    /**
     * 克隆List集合 指定角标元素
     *
     * @param list
     * @param index
     */
    public static List<String> listCopy(List<String> list, int index) {
        List<String> newList = new ArrayList();
        for (int i = 0; i < index; i++) {
            newList.add(list.get(i));
        }
        return newList;
    }

    /**
     * 判断字符串是否为URL
     *
     * @param urls 需要判断的String类型url
     * @return true:是URL；false:不是URL
     */
    public static boolean isUrl(String urls) {
        boolean isurl = false;
        if (urls != null && !"".equals(urls.trim())) {
            isurl = urls.toLowerCase().matches("^(http:\\/\\/|https:\\/\\/)?[a-z0-9]+([\\-\\.]{1}[a-z0-9]+)*\\.[a-z]{2,5}(:[0-9]{1,5})?(\\/.*)?|^((http:\\/\\/|https:\\/\\/)?([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(:\\d{0,5})?(\\/.*)?$");
        }
        return isurl;
    }

    /**
     * 文件夹过滤获取文件
     *
     * @param dir      总目录
     * @param formats  目标文件格式集
     * @param fileList 结果文件地址集
     */
    public static void fileListFilter(String dir, List<String> formats, List<String> fileList) {
        if (dir != null && formats != null) {
            File fileDir = new File(dir);
            String[] list = fileDir.list();
            if (list != null) { // 文件夹
                for (int i = 0; i < list.length && i < 50; i++) {
                    fileListFilter(dir + File.separator + list[i], formats, fileList);
                }
            } else { // 文件
                String[] s = dir.split("\\.");
                if (s.length > 0) {
                    // 匹配符合的格式
                    String f = "." + s[s.length - 1].toLowerCase();
                    if (formats.contains(f)) {
                        fileList.add(dir);
                    }
                }
            }
        }
    }

    public static void txtListFilter(String dir, List<String> fileList) {
        if (dir != null) {
            File fileDir = new File(dir);
            if (fileDir.isFile()) {
                if (dir.toLowerCase().contains(".txt")) {
                    fileList.add(dir);
                }
            } else {
                String[] list = fileDir.list();
                if (list != null) { // 文件夹
                    for (int i = 0; i < list.length && i < 50; i++) {
                        txtListFilter(dir + File.separator + list[i], fileList);
                    }
                }
            }

        }
    }

    /**
     * 文件夹过滤获取文件，使用Files.walk方法
     *
     * @param dir      总目录
     * @param formats  目标文件格式集
     * @param fileList 结果文件地址集
     */
    public static void fileWalk(String dir, List<String> formats, List<String> fileList, int maxDepth) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try (Stream<Path> paths = Files.walk(Paths.get(dir), maxDepth)) { // 递归指定层数目录
                paths.map(path -> path.toString()).filter(path -> {
                    if (path.contains("/") && !path.endsWith("/")) {
                        String name = path.substring(path.lastIndexOf("/") + 1);
                        if (name.startsWith(".")) { // 排除隐藏文件
                            return false;
                        }
                    }
                    String[] s = path.split("\\.");
                    if (s.length > 0) {
                        // 匹配符合的格式
                        String f = "." + s[s.length - 1].toLowerCase();
                        return (formats.contains(f));
                    }
                    return false;
                }).forEach(fileList::add);
            } catch (Exception e) {
                e.getMessage();
            }
        } else {
            fileListFilter(dir, formats, fileList);
        }
        // 排序 (先按字符串长度排序，其次按字符串中的数字大小排序)
        Collections.sort(fileList, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                // 获取字符串包含的数字 并 获取去除数字后的字符串
                List<String> nums1 = getNums(o1);
                String o1X = o1;
                for (String s : nums1) {
                    o1X = o1X.replace(s, "");
                }
                List<String> nums2 = getNums(o2);
                String o2X = o2;
                for (String s : nums2) {
                    o2X = o2X.replace(s, "");
                }
                // 比较去除数字后的字符串长度
                if (o1X.length() == o2X.length()) {
                    // 去除数字后的字符串长度 相同时
                    if (nums1.size() == nums2.size() && nums1.isEmpty()) {
                        return 0;
                    } else {
                        if (nums1.isEmpty() || nums2.isEmpty()) {
                            return nums1.size() - nums2.size() > 0 ? 1 : -1;
                        } else {
                            for (int i = 0; i < nums1.size(); i++) {
                                if (nums2.size() > i) {
                                    String numStr1 =  nums1.get(i);
                                    String numStr2 =  nums2.get(i);
                                    if (!numStr1.equals(numStr2)) {
                                        double num1 = 0;
                                        double num2 = 0;
                                        if (!numStr1.matches("(([1-9]\\d*)(\\.\\d+)?)|((0)(\\.\\d+)?)")) {
                                            numStr1 = getIntegerByNumberStr(numStr1) + "";
                                        }
                                        if (!numStr2.matches("(([1-9]\\d*)(\\.\\d+)?)|((0)(\\.\\d+)?)")) {
                                            numStr2 = getIntegerByNumberStr(numStr2) + "";
                                        }
                                        num1 = Double.parseDouble(numStr1);
                                        num2 = Double.parseDouble(numStr2);
                                        return num1 - num2 > 0 ? 1 : -1;
                                    }
                                }
                            }
                            return 0;
                        }
                    }
                } else {
                    // 去除数字后的字符串长度 不同时
                    return o1X.length() - o2X.length() > 0 ? 1 : -1;
                }
            }
        });
    }

    /**
     * 综合分析缓存和磁盘中的下载列表 进行过滤
     * @param dir
     * @param fileList
     */
    public static void downloadListFileWalk(String dir, List<String> fileList) {
        try {
            // 遍历获取缓存列表
            HashMap<Integer, NotificationBean> downLoadInfoMap = MyApplication.getDownLoadInfoMap();
            Set<Map.Entry<Integer, NotificationBean>> entries = downLoadInfoMap.entrySet();
            for (Map.Entry<Integer, NotificationBean> entry : entries) {
                Integer notificationId = entry.getKey();
                NotificationBean bean = entry.getValue();
                if (bean != null) {
                    fileList.add(bean.getDate() + CommonUtils.zeroPadding(notificationId));
                }
            }
            // 过滤获取磁盘列表
            List<String> fileList2 = new ArrayList<>();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                try (Stream<Path> paths = Files.walk(Paths.get(dir), 1)) { // 递归指定层数目录
                    paths.map(path -> path.toString()).filter(path -> {
                        if (path.contains("/") && !path.endsWith("/") && path.contains(".")) {
                            String name = CommonUtils.getUrlName(path);
                            // 排除指定文件
                            if (!fileList.contains(name)) {
                                return true;
                            }
                        }
                        return false;
                    }).forEach(fileList2::add);
                } catch (Exception e) {
                    e.getMessage();
                }
            }
            // 缓存列表排序
            Collections.sort(fileList, new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    try {
                        long l1 = Long.parseLong(o1);
                        long l2 = Long.parseLong(o2);
                        return l2-l1 > 0 ? 1 : -1;
                    } catch (Exception e) {
                        return 0;
                    }
                }
            });
            // 磁盘列表排序
            Collections.sort(fileList2, new Comparator<String>() {
                @Override
                public int compare(String s1, String s2) {
                    String o1 = CommonUtils.getUrlName(s1);
                    String o2 = CommonUtils.getUrlName(s2);
                    try {
                        long l1 = Long.parseLong(o1);
                        long l2 = Long.parseLong(o2);
                        return l2-l1 > 0 ? 1 : -1;
                    } catch (Exception e) {
                        return 0;
                    }
                }
            });
            fileList.addAll(fileList2);
        } catch (Exception e) {
            CommonUtils.saveLog("downloadListFileWalk==========" + e.getMessage());
        }
    }

    /**
     * 磁盘中读取定位记录
     */
    public static List<LocationBean> locationListFileWalk() {
        String dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        dir += "/SimpleBrower/0_like/locationList";
        List<LocationBean> fileList = new ArrayList<>();
        try {
            // 过滤获取磁盘列表
            List<String> fileList2 = new ArrayList<>();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                try (Stream<Path> paths = Files.walk(Paths.get(dir), 1)) { // 递归指定层数目录
                    paths.map(path -> path.toString()).filter(path -> {
                        if (path.contains("/") && !path.endsWith("/") && path.contains(".")) {
                            return true;
                        }
                        return false;
                    }).forEach(fileList2::add);
                } catch (Exception e) {
                    e.getMessage();
                }
            }
            // 读取列表文件
            for (String path : fileList2) {
                LocationBean oldBean = CommonUtils.readObjectFromLocal(LocationBean.class, path);
                if (oldBean != null) {
                    fileList.add(oldBean);
                }
            }
            // 排序
            Collections.sort(fileList, new Comparator<LocationBean>() {
                @Override
                public int compare(LocationBean s1, LocationBean s2) {
                    String o1 = s1.getTime();
                    String o2 = s2.getTime();
                    try {
                        long l1 = Long.parseLong(o1);
                        long l2 = Long.parseLong(o2);
                        return l2-l1 > 0 ? 1 : -1;
                    } catch (Exception e) {
                        return 0;
                    }
                }
            });
        } catch (Exception e) {
            CommonUtils.saveLog("locationListFileWalk==========" + e.getMessage());
        }
        return fileList;
    }

    /**
     * 获取字符串中首次出现的阿拉伯数字
     *
     * @param str
     * @return
     */
    public static String getNumStr(String str) {
        String ret = "";
        if (str != null) {
            if (str.contains("/")) {
                str = str.substring(str.lastIndexOf("/"));
            }
            Pattern pattern = Pattern.compile("(([1-9]\\d*)(\\.\\d+)?)|((0)(\\.\\d+)?)");
            Matcher matcher = pattern.matcher(str);
            if (matcher.find()) {
                ret = matcher.group();
            }
        }
        return ret;
    }

    /**
     * 获取字符串中首次出现的中文数字
     *
     * @param str
     * @return
     */
    public static String getZWNumStr(String str) {
        String ret = "";
        if (str != null) {
            if (str.contains("/")) {
                str = str.substring(str.lastIndexOf("/"));
            }
            Pattern pattern = Pattern.compile("[一二三四五六七八九十百千万亿]+");
            Matcher matcher = pattern.matcher(str);
            if (matcher.find()) {
                ret = matcher.group();
            }
        }
        return ret;
    }

    /**
     * 获取字符串中的数字集合
     *
     * @param str
     * @return
     */
    public static List<String> getNums(String str) {
        List<String> nums = new ArrayList<>();
        String num = "no";
        String strX = str;
        while (!"".equals(num)) {
            String numN = getNumStr(strX);
            String numZ = getZWNumStr(strX);
            if (!"".equals(numN) && "".equals(numZ)) {
                nums.add(numN);
            }
            if ("".equals(numN) && !"".equals(numZ)) {
                nums.add(numZ);
            }
            if (!"".equals(numN) && !"".equals(numZ)) {
                if (strX.indexOf(numN) < strX.indexOf(numZ)) {
                    nums.add(numN);
                    nums.add(numZ);
                } else {
                    nums.add(numZ);
                    nums.add(numN);
                }
            }
            num = "".equals(numN) ? numZ : numN;
            if (!"".equals(num)) {
                strX = strX.replaceFirst(numN, "").replaceFirst(numZ, "");
            }
        }
        return nums;
    }

    /**
     * 中文数字转阿拉伯数字 支持到12位
     *
     * @param numberStr 中文数字
     * @return int 数字
     */
    public static int getIntegerByNumberStr(String numberStr) {

        // 返回结果
        int sum = 0;

        // null或空串直接返回
        if (numberStr == null || ("").equals(numberStr)) {
            return sum;
        }

        // 过亿的数字处理
        if (numberStr.indexOf("亿") > 0) {
            String currentNumberStr = numberStr.substring(0, numberStr.indexOf("亿"));
            int currentNumber = testA(currentNumberStr);
            sum += currentNumber * Math.pow(10, 8);
            numberStr = numberStr.substring(numberStr.indexOf("亿") + 1);
        }

        // 过万的数字处理
        if (numberStr.indexOf("万") > 0) {
            String currentNumberStr = numberStr.substring(0, numberStr.indexOf("万"));
            int currentNumber = testA(currentNumberStr);
            sum += currentNumber * Math.pow(10, 4);
            numberStr = numberStr.substring(numberStr.indexOf("万") + 1);
        }

        // 小于万的数字处理
        if (!("").equals(numberStr)) {
            int currentNumber = testA(numberStr);
            sum += currentNumber;
        }

        return sum;
    }

    /**
     * 把亿、万分开每4位一个单元，解析并获取到数据
     * @param testNumber
     * @return
     */
    public static int testA(String testNumber) {
        // 返回结果
        int sum = 0;

        // null或空串直接返回
        if(testNumber == null || ("").equals(testNumber)){
            return sum;
        }

        // 获取到千位数
        if (testNumber.indexOf("千") > 0) {
            String currentNumberStr = testNumber.substring(0, testNumber.indexOf("千"));
            sum += testB(currentNumberStr) * Math.pow(10, 3);
            testNumber = testNumber.substring(testNumber.indexOf("千") + 1);
        }

        // 获取到百位数
        if (testNumber.indexOf("百") > 0) {
            String currentNumberStr = testNumber.substring(0, testNumber.indexOf("百"));
            sum += testB(currentNumberStr) * Math.pow(10, 2);
            testNumber = testNumber.substring(testNumber.indexOf("百") + 1);
        }

        // 对于特殊情况处理 比如10-19是个数字，十五转化为一十五，然后再进行处理
        if (testNumber.indexOf("十") == 0) {
            testNumber = "一" + testNumber;
        }

        // 获取到十位数
        if (testNumber.indexOf("十") > 0) {
            String currentNumberStr = testNumber.substring(0, testNumber.indexOf("十"));
            sum += testB(currentNumberStr) * Math.pow(10, 1);
            testNumber = testNumber.substring(testNumber.indexOf("十") + 1);
        }

        // 获取到个位数
        if(!("").equals(testNumber)){
            sum += testB(testNumber.replaceAll("零",""));
        }

        return sum;
    }
    public static int testB(String replaceNumber) {
        switch (replaceNumber) {
            case "一":
                return 1;
            case "二":
                return 2;
            case "三":
                return 3;
            case "四":
                return 4;
            case "五":
                return 5;
            case "六":
                return 6;
            case "七":
                return 7;
            case "八":
                return 8;
            case "九":
                return 9;
            case "零":
                return 0;
            default:
                return 0;
        }
    }

    /**
     * 是否是横屏
     *
     * @param activity 活动
     * @return true - 横屏, false - 不是横屏
     */
    public static boolean isLandscape(Activity activity) {
        int requestedOrientation = activity.getRequestedOrientation();
        return requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    }

    /**
     * 获取网络地址中的文件名称
     * https://ch1-ctc-dd.tv002.com/down/37955b56ed24b568e5858f1f6de3fb93/fpnddtsnc%2Cats.zip?cts=U43127963&ctp=222A210A144A194&ctt=1718314242&limit=1&spd=260000&ctk=37955b56ed24b568e5858f1f6de3fb93&chk=1600bd7b73ae1afd715ae12bea782dd7-1368728
     *
     * @param url
     * @return
     */
    public static String getUrlName(String url) {
        String name = "";
        try {
            if (url.contains("?")) {
                String urlX = url.substring(0, url.lastIndexOf("?"));
                name = urlX.substring(urlX.lastIndexOf("/") + 1);
            } else {
                name = url.substring(url.lastIndexOf("/") + 1);
            }
            if (name.contains(".")) {
                name = name.substring(0, name.lastIndexOf("."));
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return "".equals(name) ? System.currentTimeMillis() + "" : name;
    }

    // 获取地址中的文件名称（保留格式）
    public static String getUrlName2(String url) {
        String name = "";
        try {
            if (url.contains("?")) {
                String urlX = url.substring(0, url.lastIndexOf("?"));
                name = urlX.substring(urlX.lastIndexOf("/") + 1);
            } else {
                name = url.substring(url.lastIndexOf("/") + 1);
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return name;
    }

    /**
     * 获取网络地址中的文件格式
     * https://ch1-ctc-dd.tv002.com/down/37955b56ed24b568e5858f1f6de3fb93/fpnddtsnc%2Cats.zip?cts=U43127963&ctp=222A210A144A194&ctt=1718314242&limit=1&spd=260000&ctk=37955b56ed24b568e5858f1f6de3fb93&chk=1600bd7b73ae1afd715ae12bea782dd7-1368728
     *
     * @param url
     * @return
     */
    public static String getUrlFormat(String url) {
        String format = "";
        try {
            if (url.contains("?")) {
                String urlX = url.substring(0, url.lastIndexOf("?"));
                format = urlX.substring(urlX.lastIndexOf("/") + 1);
            } else {
                format = url.substring(url.lastIndexOf("/") + 1);
            }
            if (format.contains(".")) {
                format = format.substring(format.lastIndexOf("."));
            } else {
                format = "";
            }
        } catch (Exception e) {
            e.getMessage();
        }
        return format;
    }

    public static String getUrlFormat(String url, String contentType) {
        // 网址中获取
        String format = "";
        try {
            if (url.contains("?")) {
                String urlX = url.substring(0, url.lastIndexOf("?"));
                format = urlX.substring(urlX.lastIndexOf("/") + 1);
            } else {
                format = url.substring(url.lastIndexOf("/") + 1);
            }
            if (format.contains(".")) {
                format = format.substring(format.lastIndexOf("."));
                if (format.toLowerCase().endsWith("php")) {
                    format = "";
                }
            } else {
                format = "";
            }
        } catch (Exception e) {
            e.getMessage();
        }
        // 根据请求文件格式判断
        if ("".equals(format)) {
            format =  MIMEUtils.geType(contentType);
        }
        return format;
    }

    // 获取url中的一级域名
    public static String getUrlDomain(String url) {
        String[] split = url.split("//");
        String domain = split[1].split("/")[0];
        if (domain.indexOf(".") != domain.lastIndexOf(".")) {
            domain = domain.substring(domain.indexOf("."));
        }
        return domain;
    }

    // 获取url中的http协议及域名
    public static String getUrlHead(String url) {
        String[] split = url.split("//");
        String domain = split[1].split("/")[0];
        return split[0] + "//" + domain;
    }

    /**
     * 文件删除
     *
     * @param file
     * @return
     */
    public static boolean deleteFile(File file) {
        boolean ret = false;
        if (file != null && file.exists()) {
            if (file.isFile() || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                ret = file.delete();
                System.gc();
            } else {
                try {
                    Files.walkFileTree(Paths.get(file.getPath()), new SimpleFileVisitor<Path>(){
                        //遍历删除文件
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }
                        //遍历删除目录
                        public FileVisitResult postVisitDirectory(Path dir,IOException exc) throws IOException{
                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                    ret = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return ret;
    }

    /**
     * 批量删除指定目录下的所以文件
     * @param file
     */
    public static boolean batchDeleteFile(File file) {
        boolean ret = true;
        if (file != null && file.exists()) {
            if (file.isFile() || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                ret = file.delete();
                System.gc();
            } else {
                try (Stream<Path> pathStream = Files.walk(file.toPath())) {
                    pathStream
                            .parallel()  // 启用并行处理
                            .filter(p -> !Files.isDirectory(p))
                            .forEach(p -> {
                                try {
                                    Files.delete(p);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                    ret = deleteFile(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return ret;
    }

    /**
     * 文件管理 权限检查
     *
     * @param context
     * @return
     */
    public static boolean hasStoragePermissions(Context context) {
        boolean ret;
        // android 11 所有文件管理权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ret = Environment.isExternalStorageManager();
        } else {
            //版本判断，如果比android 13 就走正常的权限获取
            if (android.os.Build.VERSION.SDK_INT < 33) {
                int readPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
                int writePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                ret = readPermission == PackageManager.PERMISSION_GRANTED && writePermission == PackageManager.PERMISSION_GRANTED;
            } else {
                int audioPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO);
                int imagePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES);
                int videoPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO);
                ret = audioPermission == PackageManager.PERMISSION_GRANTED && imagePermission == PackageManager.PERMISSION_GRANTED && videoPermission == PackageManager.PERMISSION_GRANTED;
            }
        }
        return ret;
    }

    /**
     * 文件管理 权限申请
     *
     * @param context
     */
    public static void requestStoragePermissions(Activity context, ActivityResultLauncher<Intent> allFilesAccessLauncher) {
        // android 11 所有文件管理权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            FeetDialog feetDialog = new FeetDialog(context, "授权", "需授权后才能使用该功能", "授权", "取消");
            feetDialog.setOnTouchListener(new FeetDialog.TouchListener() {
                @Override
                public void close() {
                    feetDialog.dismiss();
                }
                @Override
                public void ok(String txt) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + context.getPackageName()));
                    if (allFilesAccessLauncher != null) {
                        allFilesAccessLauncher.launch(intent);
                    } else {
                        context.startActivityForResult(intent, 100);
                    }
                    feetDialog.dismiss();
                }
            });
            feetDialog.show();
        } else {
            String[] permissions;
            if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
            } else {
                permissions = new String[]{Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO};
            }
            ActivityCompat.requestPermissions(context, permissions, STORAGE_PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * 通知 权限检查
     *
     * @param context
     * @return
     */
    public static boolean hasNotificationPermissions(Context context) {
        return  ((NotificationManager) context.getSystemService(NOTIFICATION_SERVICE)).areNotificationsEnabled();
    }

    /**
     * 通知 权限申请
     *
     * @param context
     */
    public static boolean requestNotificationPermissions(Activity context) {
        if (!hasNotificationPermissions(context)) {
            FeetDialog feetDialog = new FeetDialog(context, "授权", "该功能将会使用通知", "授权", "取消");
            feetDialog.setOnTouchListener(new FeetDialog.TouchListener() {
                @Override
                public void close() {
                    feetDialog.dismiss();
                }
                @Override
                public void ok(String txt) {
                    Intent settingsIntent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                    settingsIntent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
                    context.startActivityForResult(settingsIntent, 200);
                    feetDialog.dismiss();
                }
            });
            feetDialog.show();
            return false;
        }
        return true;
    }

    /**
     * 定位 权限检查
     *
     * @param context
     * @return
     */
    public static boolean hasLocationPermissions(Context context) {
        return ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 定位 权限申请
     *
     * @param context
     */
    public static void requestLocationPermissions(Activity context, ActivityResultLauncher<Intent> allFilesAccessLauncher) {
        String[] permissions;
        // Android 13需单独请求前台定位权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION};
        } else {
            permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        }
        ActivityCompat.requestPermissions(context, permissions, LOCATION_PERMISSION_REQUEST_CODE);
    }

    /**
     * 悬浮窗 权限检查
     *
     * @param context
     * @return
     */
    public static boolean hasOverlayPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true; // Android 6.0以下默认有权限
    }

    /**
     * 悬浮窗 权限申请
     *
     * @param context
     */
    public static void requestOverlayPermission(Activity context, ActivityResultLauncher<Intent> allFilesAccessLauncher) {
        FeetDialog feetDialog = new FeetDialog(context, "授权", "显示悬浮窗提高模拟定位稳定性", "授权", "取消");
        feetDialog.setOnTouchListener(new FeetDialog.TouchListener() {
            @Override
            public void close() {
                feetDialog.dismiss();
            }
            @Override
            public void ok(String txt) {
                feetDialog.dismiss();
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context.getPackageName()));
                allFilesAccessLauncher.launch(intent);
            }
        });
        feetDialog.show();
    }

    /**
     * 是否为虚拟定位应用
     *
     * @param context
     */
    public static boolean isMockLocationApp(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 使用 AppOpsManager 检测模拟位置权限
            AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            try {
                int mode = appOps.checkOpNoThrow(
                        AppOpsManager.OPSTR_MOCK_LOCATION,
                        android.os.Process.myUid(),
                        context.getPackageName()
                );
                return mode == AppOpsManager.MODE_ALLOWED;
            } catch (Exception e) {
                return false;
            }
        } else {
            // 旧版 Android 的检测方式
            return Settings.Secure.getInt(context.getContentResolver(),
                    Settings.Secure.ALLOW_MOCK_LOCATION, 0) != 0;
        }
    }

    /**
     * 跳转开发者选项界面
     *
     * @param context
     */
    public static void openDeveloperOptions(Activity context, ActivityResultLauncher<Intent> allFilesAccessLauncher) {
        FeetDialog feetDialog = new FeetDialog(context, "授权", "需在开发者选项设置虚拟位置应用", "设置", "取消");
        feetDialog.setOnTouchListener(new FeetDialog.TouchListener() {
            @Override
            public void close() {
                feetDialog.dismiss();
            }
            @Override
            public void ok(String txt) {
                try {
                    // 标准开发者选项 Intent
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                    // 添加 FLAG_ACTIVITY_NEW_TASK 确保从非 Activity 上下文启动
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    // 部分厂商定制 ROM 的特殊处理
                    if (intent.resolveActivity(context.getPackageManager()) == null) {
                        intent = new Intent(Settings.ACTION_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    }
                    allFilesAccessLauncher.launch(intent);
                } catch (Exception e) {
                    // 备用方案：打开常规设置
                    context.startActivity(new Intent(Settings.ACTION_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                }
                feetDialog.dismiss();
            }
        });
        feetDialog.show();
    }

    /**
     * 按行读取小说
     * @param filePath
     * @param lines
     */
    public static void readLines(String filePath, ArrayList<String> lines) {
        String code = getCharset(new File(filePath));
        if (code != null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), Charset.forName(code)))) {
                String line;
                while ((line = br.readLine()) != null) {
                    // 首行缩进两个字符
                    if (!line.startsWith(line.trim()) || "".equals(line.trim())) {
                        line = "\u3000\u3000" + line.trim();
                    }
                    lines.add(line + "\n");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static String getCharset(File file) {
        String charset = "GBK";
        byte[] first3Bytes = new byte[3];
        try {
            boolean checked = false;
            BufferedInputStream bis = new BufferedInputStream(
                    new FileInputStream(file));
            bis.mark(0);
            int read = bis.read(first3Bytes, 0, 3);
            if (read == -1)
                return charset;
            if (first3Bytes[0] == (byte) 0xFF && first3Bytes[1] == (byte) 0xFE) {
                charset = "UTF-16LE";
                checked = true;
            } else if (first3Bytes[0] == (byte) 0xFE && first3Bytes[1]
                    == (byte) 0xFF) {
                charset = "UTF-16BE";
                checked = true;
            } else if (first3Bytes[0] == (byte) 0xEF && first3Bytes[1]
                    == (byte) 0xBB
                    && first3Bytes[2] == (byte) 0xBF) {
                charset = "UTF-8";
                checked = true;
            }
            bis.reset();
            if (!checked) {
                int loc = 0;
                while ((read = bis.read()) != -1) {
                    loc++;
                    if (read >= 0xF0)
                        break;
                    //单独出现BF以下的，也算是GBK
                    if (0x80 <= read && read <= 0xBF)
                        break;
                    if (0xC0 <= read && read <= 0xDF) {
                        read = bis.read();
                        if (0x80 <= read && read <= 0xBF)// 双字节 (0xC0 - 0xDF)
                            // (0x80 -
                            // 0xBF),也可能在GB编码内
                            continue;
                        else
                            break;
                        // 也有可能出错，但是几率较小
                    } else if (0xE0 <= read && read <= 0xEF) {
                        read = bis.read();
                        if (0x80 <= read && read <= 0xBF) {
                            read = bis.read();
                            if (0x80 <= read && read <= 0xBF) {
                                charset = "UTF-8";
                                break;
                            } else
                                break;
                        } else
                            break;
                    }
                }
                System.out.println(loc + " " + Integer.toHexString(read));
            }
            bis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return charset;
    }

    /**
     * 读取下一页
     * @param lines
     * @param positionBean
     */
    public static void readNextPage(ArrayList<String> lines, PositionBean positionBean) {
        try {
            int size = positionBean.getSize();
            int endLine = positionBean.getEndLine();
            int endNum = positionBean.getEndNum();
            positionBean.setStartLine(endLine);
            positionBean.setStartNum(endNum);
            String txt = "";
            if (endLine >= 0 && lines.get(endLine).length() >= endNum) {
                txt = lines.get(endLine).substring(endNum);
            }
            if (txt.length() >= size) {
                // 当前行剩余字数还比整页能展示的数据多的情况
                positionBean.setEndLine(endLine);
                txt = txt.substring(0, size);
                positionBean.setEndNum(endNum + size);
            } else {
                for (int i = endLine + 1; i < lines.size(); i++) {
                    txt = txt + lines.get(i);
                    positionBean.setEndLine(i);
                    if (txt.length() >= size) {
                        int num = lines.get(i).length() - (txt.length() - size);
                        txt = txt.substring(0, size);
                        positionBean.setEndNum(num);
                        break;
                    } else if (lines.size() - 1 == endLine + 1) { // 到最后一行 txt长度不及size时
                        positionBean.setEndNum(lines.get(i).length());
                    }
                }
            }
            // 只有最后一行时
            if (lines.size() - 1 == endLine) {
                positionBean.setEndLine(lines.size() - 1);
                if (txt.length() <= size) {
                    positionBean.setEndNum(lines.get(lines.size() - 1).length());
                } else {
                    positionBean.setEndNum(size);
                }
            }
            positionBean.setTxt(txt);
        } catch (Throwable e) {
            CommonUtils.saveLog("readNextPage：" + e.getMessage());
        }
    }
    public static void readNextPageDef(ArrayList<String> lines, PositionBean positionBean) {
        try {
            int size = positionBean.getSize();
            int endLine = positionBean.getEndLine();
            int endNum = positionBean.getEndNum();
            String txt = "";
            if (endLine >= 0 && lines.get(endLine).length() >= endNum) {
                txt = lines.get(endLine).substring(endNum);
            }
            for (int i = endLine + 1; i < lines.size(); i++) {
                txt = txt + lines.get(i);
                if (txt.length() >= size) {
                    break;
                }
            }
            positionBean.setTxt(txt);
        } catch (Throwable e) {
            CommonUtils.saveLog("readNextPageDef：" + e.getMessage());
        }
    }


    /**
     * 读取上一页
     * @param lines
     * @param positionBean
     */
    public static void readPreviousPage(ArrayList<String> lines, PositionBean positionBean, Handler msgHandler) {
        try {
            int size = positionBean.getSize();
            int startLine = positionBean.getStartLine();
            int startNum = positionBean.getStartNum();
            positionBean.setEndLine(startLine);
            positionBean.setEndNum(startNum);
            String txt = "";
            if (startLine >= 0 && startNum > 0 && lines.get(startLine).length() >= startNum) {
                txt = lines.get(startLine).substring(0, startNum);
            }
            if (txt.length() >= size) {
                // 当前行剩余字数还比整页能展示的数据多的情况
                positionBean.setStartLine(startLine);
                txt = txt.substring(txt.length() - size);
                positionBean.setStartNum(startNum - size);
            } else {
                for (int i = startLine - 1; i >= 0; i--) {
                    txt = lines.get(i) + txt;
                    // txt = txt + lines.get(i); // 先从下往上排版 好统计字数
                    positionBean.setStartLine(i);
                    if (txt.length() >= size) {
                        int num = txt.length() - size;
                        txt = txt.substring(txt.length() - size);
                        positionBean.setStartNum(num);
                        break;
                    } else if (startLine - 1 == 0) { // 到第一行，txt的长度不及size时
                        positionBean.setStartNum(0);
                    }
                }
            }
            // 只有第一行时
            if (startLine == 0) {
                positionBean.setStartLine(0);
                positionBean.setStartNum(0);
                if (txt.length() > size) {
                    positionBean.setStartNum(txt.length() - size);
                }
            }
            positionBean.setTxt(txt);
        } catch (Throwable e) {
            CommonUtils.saveLog("readPreviousPage：" + e.getMessage());
        }

    }
    public static void readPreviousPageDef(ArrayList<String> lines, PositionBean positionBean) {
        try {
            int size = positionBean.getSize();
            int startLine = positionBean.getStartLine();
            int startNum = positionBean.getStartNum();
            String txt = "";
            if (startLine >= 0 && startNum > 0 && lines.get(startLine).length() >= startNum) {
                txt = lines.get(startLine).substring(0, startNum);
            }
            for (int i = startLine - 1; i >= 0; i--) {
                txt = txt + lines.get(i); // 先从下往上排版 好统计字数
                if (txt.length() >= size) {
                    break;
                }
            }
            positionBean.setTxt(txt);
        } catch (Throwable e) {
            CommonUtils.saveLog("readPreviousPageDef：" + e.getMessage());
        }
    }

    // 获取章节名
    public static ArrayList<HashMap<String, String>> getTitles(ArrayList<String> lines, int startLine) {
        ArrayList<HashMap<String, String>> titles = new ArrayList<>();
        int index = 0;
        if (lines != null) {
            for (int i = 0; i < lines.size(); i++) {
                String title = getTitleFrom(lines.get(i));
                // 获取章节名
                if (!"".equals(title)) {
                    HashMap<String, String> map = new HashMap<>();
                    map.put("title", title);
                    map.put("line", i + "");
                    titles.add(map);
                }
                // 记录当前页面 对应的章节行数
                if (startLine == i - 1 && titles.size() > 0) {
                    index = titles.size() - 1;
                }
            }
            if (titles.size() > 0) {
                titles.get(0).put("index", index + "");
            }
        }
        return titles;
    }
    public static String getTitleFrom(String lineTxt) {
        if (lineTxt != null) {
            if (lineTxt.contains("\u3000\u3000")) {
                lineTxt = lineTxt.replace("\u3000\u3000", "");
            }
            if (lineTxt.contains("  ")) {
                lineTxt = lineTxt.replaceAll(" +", " ");
            }
            if (lineTxt.contains("：")) {
                lineTxt = lineTxt.replace("：", " ");
            }
            if (lineTxt.contains(":")) {
                lineTxt = lineTxt.replace(":", " ");
            }
            if (lineTxt.contains("-----")) {
                lineTxt = lineTxt.replace("-----", "");
            }
            String[] s = lineTxt.trim().split(" ");
            if (s.length > 1 && s[0].trim().startsWith("第") && (s[0].trim().endsWith("卷") || s[0].trim().endsWith("章") || s[0].trim().endsWith("节") || s[0].trim().endsWith("集"))) {
                return lineTxt;
            } else if (s.length > 2 && s[1].trim().startsWith("第") && (s[1].trim().endsWith("卷") || s[1].trim().endsWith("章") || s[1].trim().endsWith("节") || s[1].trim().endsWith("集"))) {
                return lineTxt;
            } else if (s.length > 3 && s[2].trim().startsWith("第") && (s[2].trim().endsWith("卷") || s[2].trim().endsWith("章") || s[2].trim().endsWith("节") || s[2].trim().endsWith("集"))) {
                return lineTxt;
            } else if (s.length > 0 && s[0].trim().startsWith("番外")) {
                String otherTxt = lineTxt.substring(s[0].length());
                if (otherTxt.length() < 12) {
                    return lineTxt;
                }
            } else if (s.length == 2 && s[0].replace("章", "").replace("节", "").replace("集", "").matches("\\d+")) {
                String otherTxt = lineTxt.substring(s[0].length());
                if (otherTxt.length() < 12) {
                    return lineTxt;
                }
            } else {
                String[] ss = lineTxt.trim().replace(" ", "").replace("、", " ").split(" ");
                if (ss.length == 2 && ss[0].matches("\\d+")) {
                    String otherTxt = lineTxt.substring(ss[0].length());
                    if (otherTxt.length() < 12) {
                        return lineTxt;
                    }
                }
                if (lineTxt.contains("【") && lineTxt.contains("】")) {
                    if (lineTxt.split("【")[1].split("】")[0].matches("\\d+")) {
                        return lineTxt;
                    }
                }
                if (lineTxt.contains(".") && lineTxt.split("\\.")[0].matches("\\d+")) {
                    return lineTxt;
                }
                if (lineTxt.startsWith("第") && lineTxt.contains("章") && hasNum(lineTxt.substring(0, lineTxt.indexOf("章")).replace("第", ""))) {
                    return lineTxt;
                }
            }
        }
        return "";
    }

    // 匹配纯数字
    public static boolean hasNum(String str) {
        boolean ret = false;
        if (str != null) {
            if (str.matches("\\d+")) {
                return true;
            }
            String s = str;
            String[] nums = new String[]{"十", "百", "千", "一", "二", "三", "四", "五", "六", "七", "八", "九", "万", "亿"};
            for (String num : nums) {
                s = s.replace(num, "");
                if ("".equals(s.trim())) {
                    return true;
                }
            }
            ret = "".equals(s.trim());
        }
        return ret;
    }

    /**
     * 将对象保存到本地
     *
     * @param bean     对象
     * @return true 保存成功
     */
    public static boolean writeObjectIntoLocal(Object bean, String key) {
        try {
            File file = CommonUtils.getFile("SimpleBrower/0_like", "set2.txt", key);
            JSONObject jsonObject = new JSONObject();
            if (file.exists()) {
                String json = "";
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line = "";
                    while ((line = br.readLine()) != null) {
                        json = json + line;
                    }
                }
                jsonObject = new JSONObject(json);
            }
            jsonObject.put(key, new Gson().toJson(bean));
            String jsonStr = jsonObject.toString();
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                bw.write(jsonStr);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public static boolean writeObjectIntoLocal(String fileDir, String fileName, Object bean) {
        try {
            String jsonStr = new Gson().toJson(bean);
            File file = CommonUtils.getFile("SimpleBrower/0_like/" + fileDir, fileName + ".json", "");
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                bw.write(jsonStr);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除对象保存到本地
     *
     * @return true 保存成功
     */
    public static boolean deleteObjectIntoLocal(String key) {
        try {
            File file = CommonUtils.getFile("SimpleBrower/0_like", "set2.txt", key);
            if (file.exists()) {
                String json = "";
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line = "";
                    while ((line = br.readLine()) != null) {
                        json = json + line;
                    }
                }
                JSONObject jsonObject = new JSONObject(json);
                jsonObject.remove(key);
                String jsonStr = jsonObject.toString();
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                    bw.write(jsonStr);
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public static boolean deleteLocalObject(String fileDir, String fileName) {
        try {
            File file = CommonUtils.getFile("SimpleBrower/0_like/" + fileDir, fileName + ".json", "");
            return file.delete();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 读取本地对象
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T readObjectFromLocal(String key, Class<T> classOfT) {
        T bean = null;
        try {
            File file = CommonUtils.getFile("SimpleBrower/0_like", "set2.txt", key);
            if (!file.exists()) {
                return null;
            }
            String json = "";
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line = "";
                while ((line = br.readLine()) != null) {
                    json = json + line;
                }
            }
            if (StringUtils.isNotEmpty(json)) {
                JSONObject jsonObject = new JSONObject(json);
                if (jsonObject.has(key)) {
                    Gson gson = new Gson();
                    bean = gson.fromJson(jsonObject.get(key).toString(), classOfT);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bean;
    }
    public static <T> T readObjectFromLocal(Class<T> classOfT, String fileName) {
        T bean = null;
        try {
            File file = new File(fileName);
            if (!file.exists()) {
                return null;
            }
            String json = "";
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line = "";
                while ((line = br.readLine()) != null) {
                    json = json + line;
                }
            }
            if (StringUtils.isNotEmpty(json)) {
                Gson gson = new Gson();
                bean = gson.fromJson(json, classOfT);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bean;
    }

    /**
     * 网络文件格式
     * @param url
     * @timeoutMillisecond 链接连接超时时间（单位：毫秒）
     * @return
     */
    public static String getNetFileType(String url, int timeoutMillisecond) {
        String mimeType = "未知格式";
        // Long timeStart = System.currentTimeMillis();
        HttpURLConnection httpURLConnection = null;
        try {
            URL url2 = new URL(url);
            httpURLConnection = (HttpURLConnection) url2.openConnection();
            httpURLConnection.setConnectTimeout(timeoutMillisecond);
            httpURLConnection.setReadTimeout(timeoutMillisecond);
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setDoInput(true);
            // 模拟电脑请求
            httpURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Safari/537.36");
            // 跨域设置相关
            httpURLConnection.setRequestProperty("Access-Control-Allow-Origin", "*");
            //* 代办允许所有方法
            httpURLConnection.setRequestProperty("Access-Control-Allow-Methods", "*");
            // Access-Control-Max-Age 用于 CORS 相关配置的缓存
            httpURLConnection.setRequestProperty("Access-Control-Max-Age", "3600");
            // 提示OPTIONS预检时，后端需要设置的两个常用自定义头
            httpURLConnection.setRequestProperty("Access-Control-Allow-Headers", "*");
            // 允许前端带认证cookie：启用此项后，上面的域名不能为'*'，必须指定具体的域名，否则浏览器会提示
            httpURLConnection.setRequestProperty("Access-Control-Allow-Credentials", "true");
            // 以识别各种格式
            httpURLConnection.setRequestProperty("Accept-Encoding", "identity");
            // 获取格式
            mimeType = httpURLConnection.getContentType();
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
        // Long timeEnd = System.currentTimeMillis();
        // System.out.println(mimeType + "===耗时：" + (timeEnd - timeStart)/1000F + "秒");
        return mimeType;
    }

    /**
     * 获取手机Download目录的文件
     * @param dirPath
     * @param fName
     * @return
     */
    public static File getFile(String dirPath, String fName, String key) {
        String dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        if ("SysSetting".equals(key)) {
            dir = PhoneSysPath.getSandboxPath(MyApplication.getContext());
        }
        dir += "/" + dirPath;
        File dirFile = new File(dir);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        return new File(dir + "/" + fName);
    }

    // Uri获取文件名称
    public static String getFileName(Context context, Uri uri) {
        String fileName = "unknown";
        String scheme = uri.getScheme();
        if ("content".equals(scheme)) {
            try (Cursor cursor = context.getContentResolver().query(
                    uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) fileName = cursor.getString(index);
                }
            } catch (Exception e) {}
        }
        else if ("file".equals(scheme)) {
            fileName = new File(uri.getPath()).getName();
        }
        return fileName;
    }

    // Uri拷贝文件
    public static void getCopyFile(Context context, Uri uri, File toFile) {
        byte[] buff = new byte[1024 * 4];
        int len = 0;
        try (BufferedInputStream bis = new BufferedInputStream(context.getContentResolver().openInputStream(uri));
             BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(toFile))
        ) {
            while ((len = bis.read(buff)) != -1) {
                bos.write(buff, 0, len);
            }
        } catch (Exception e) {
            CommonUtils.saveLog("文件拷贝失败：" + e.getMessage());
        }
    }

    // 根据m3u8文件获取hls文件目录
    public static String getHlsDirBy(File m3u8) {
        String hlsDir = "";
        try (BufferedReader br = new BufferedReader(new FileReader(m3u8))) {
            String line = "";
            while ((line = br.readLine()) != null) {
                if (line.contains(PhoneSysPath.getDownloadDir())) {
                    hlsDir = line.substring(0, line.lastIndexOf('/'));
                    break;
                }
            }
        } catch (Exception e) {
            CommonUtils.saveLog("获取hls文件目录失败：" + e.getMessage());
        }
        return hlsDir;
    }

    /**
     * 安装包后是否已设置 （用于判断只设置一次的配置）
     */
    public static boolean onlySet(Context context, String key) {
        String path = PhoneSysPath.getSandboxPath(context);
        File file = new File(path, key);
        if (file.exists()) {
            try(BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line = br.readLine();
                if (key.equals(line)) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try(FileWriter writer = new FileWriter(file)) {
                writer.write(key);
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("***************执行一次***********************" + key);
        }
        return false;
    }

    /**
     * 存档默认收藏网址
     */
    public static void setDefaultLikes() {
        try {
            // 存档默认收藏网址
            File file = CommonUtils.getFile("SimpleBrower/0_like", "like.txt", "");
            // 没有文件 hasFile为true 则追加
            boolean hasFile = false;
            if (!file.exists()) {
                hasFile = file.createNewFile();
            } else {
                hasFile = true;
            }
            try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file, hasFile));
                 BufferedReader reader = new BufferedReader(new FileReader(file))
            ) {
                List<String> likes = new ArrayList<>();
                if (hasFile) {
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        likes.add(line);
                    }
                }
                List<String> list = AssetsReader.getList("like.txt");
                if (list == null) {
                    list = AssetsReader.getList(MyApplication.getContext(), "like.txt");
                }
                for (String currentUrl : list) {
                    String likeUrl = currentUrl + "\n";
                    if (!likes.contains(currentUrl)) {
                        bos.write(likeUrl.getBytes());
                    }
                }
                bos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            CommonUtils.saveLog("有读写权限后只执行一次" + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * 保存日志到本地
     * 异步
     */
    public static void saveLog(String txt) {
        new Handler().post(() -> {
            try {
                File file = CommonUtils.getFile("SimpleBrower/log", "log.txt", "");
                String oldTxt = "";
                if (file.exists()) {
                    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                        String line = "";
                        while ((line = br.readLine()) != null) {
                            oldTxt += line + "\n";
                        }
                    }
                }
                oldTxt += txt + "\n";
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                    bw.write(oldTxt);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // 手机目录文件url修正
    public static String correctUrl(Uri uri, Activity activity) {
        String txtUrl = uri.getPath();
        CommonUtils.saveLog("打开方式-原文件地址uri.getPath()：" + txtUrl);
        if (txtUrl == null) return null;
        String urlHead = Environment.getExternalStorageDirectory().getAbsolutePath();
        /*红米手机*/
        if (txtUrl.startsWith("/document/primary:")) {
            // 安卓存储访问框架查看文件时
            txtUrl = txtUrl.split("document/primary:")[1];
            if (!txtUrl.startsWith("/")) {
                txtUrl = "/" + txtUrl;
            }
        } else if (txtUrl.startsWith("/external_files/")) {
            // 访问正常SD卡目录时
            txtUrl = txtUrl.split("external_files")[1];
        }
        if (!txtUrl.startsWith(urlHead)) {
            txtUrl = urlHead + txtUrl;
        }
        if (txtUrl.startsWith(urlHead + "/Android/data") && !txtUrl.contains(activity.getPackageName())) {
            // 访问非本应用沙盒目录的情况
            // 复制到本应用沙盒目录
            String dir = PhoneSysPath.getSandboxPath(activity) + "/other";
            File dirFile = new File(dir);
            if (!dirFile.exists()) {
                dirFile.mkdirs();
            }
            txtUrl = dir + "/" + getUrlName2(txtUrl);
            try (ParcelFileDescriptor pfd = activity.getContentResolver().openFileDescriptor(uri, "r");
                 FileInputStream fis = new FileInputStream(pfd.getFileDescriptor());
                 BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(txtUrl))) {
                byte[] buff = new byte[1024 * 8];
                int len;
                while((len = fis.read(buff)) != -1) {
                    bos.write(buff, 0, len);
                }
            } catch (Exception e) {
                CommonUtils.saveLog("打开方式-访问非本应用沙盒目录异常：" + e.getMessage());
                return null;
            }
        }
        /*荣耀手机*/
        if (txtUrl.startsWith("/root")) {
            txtUrl = txtUrl.substring(5);
        }
        if (!CommonUtils.hasStoragePermissions(activity) && !txtUrl.startsWith(urlHead + "/Android/data/" + activity.getPackageName())) {
            // 访问非沙盒目录 需要读写权限
            return "授权";
        }
        CommonUtils.saveLog("打开方式-解析后文件地址：" + txtUrl);
        return txtUrl;
    }

    // 获取系统时间
    public static String SysTime() {
       return new SimpleDateFormat("HH:mm").format(new Date());
    }

    public static int setTsNumLog(File file) {
        int num = 0;
        String TS_FLAG = "47 40 ";
        int value = 0;
        List<String> sbHexList = new ArrayList<>();
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            while ((value = is.read()) != -1) {
                // 转换16进制字符
                // System.out.println(String.format("%02X ", value) + "============" + num);
                saveLog(String.format("%02X ", value) + "============" + num);
                sbHexList.add(String.format("%02X ", value));
                if (num >= 1) {
                    String flag = sbHexList.get(num - 1) + sbHexList.get(num);
                    flag = flag.toUpperCase();
                    if (flag.equals(TS_FLAG)) { // ts文件16进制关键标志
                        // System.out.println((num - 1) + "----------获取伪png这种ts文件实际字节开始下标--------" + flag);
                        saveLog((num - 1) + "----------获取伪png这种ts文件实际字节开始下标--------" + flag);
                        return num - 1;
                    }
                }
                num++;
                // 兜底 防止一直循环
                if (num >= 1024) {
                    break;
                }
            }
            is.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    public static int randomNum() {
        int num = 0;
        try {
            String timeStr = new SimpleDateFormat("HHmmssSSS").format(new Date());
            num = Integer.parseInt(timeStr);
        } catch (Exception e) {
            saveLog("randomNum:" + e.getMessage());
        }
        return num;
    }

    public static String randomStr() {
        String str = "xxx";
        try {
            str = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        } catch (Exception e) {
            saveLog("randomStr:" + e.getMessage());
        }
        return str;
    }

    // HHmmssSSS 数字字符转换缺位补零
    public static String zeroPadding(Integer id) {
        return ("" + id).length() < 9 ? ("0" + id) : ("" + id);
    }

    // 字符串放重复处理（末尾累加数字）
    public static String preventDuplication(String str) {
        int num = 1;
        try {
            String patternStr = "（(\\d+)）\\s*$"; // 这个正则表达式匹配末尾的整数序列
            Pattern pattern = Pattern.compile(patternStr);
            Matcher matcher = pattern.matcher(str);
            if (matcher.find()) {
                num = Integer.parseInt(matcher.group(1));
                str = str.substring(0, str.lastIndexOf("（" + num + "）"));
                num++;
            }
        } catch (Exception e) {
            num = new Random().nextInt();
            saveLog("preventDuplication==============" + e.getMessage());
        }
        return str + "（" + num + "）";
    }

    // 判断数字（包括小数）
    public static boolean matchingNumber(String str) {
        return str != null && str.matches("^(([1-9]\\d*)(\\.\\d+)?)$|^((0)(\\.\\d+)?)$");
    }

    // 获取剩余可用内存占比
    public static double getAvailableMemoryRatio(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        double r = (double) memoryInfo.availMem / memoryInfo.totalMem;
        // saveLog("剩余内存====" + memoryInfo.availMem + "***总内存====" + memoryInfo.totalMem + "***占比====" + r);
        // 保留两位小数
        return r;
    }

    // 字符数字转换
    public static int str2int(String str, int def) {
        int n = def;
        try {
            n = Integer.parseInt(str);
        } catch (Exception ignored) {}
        return n;
    }

    // 计算两个数百分比值
    public static Float getPercentage(long a, long b) {
        if (b == 0) {
            return 0F;
        } else {
            return new BigDecimal(a)
                    .divide(new BigDecimal(b), 4, BigDecimal.ROUND_HALF_UP) // 除以b (四舍五入保留4位小数)
                    .multiply(new BigDecimal(100)) // 乘以100
                    .floatValue();
        }
    }

    // 返回桌面
    public static void backHome(Activity activity) {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(homeIntent);
    }

    // 判断是否重定向
    public static String checkRedirect(String url, int timeoutMillisecond) {
        // Long timeStart = System.currentTimeMillis();
        HttpURLConnection httpURLConnection = null;
        try {
            URL url2 = new URL(url);
            httpURLConnection = (HttpURLConnection) url2.openConnection();
            httpURLConnection.setInstanceFollowRedirects(false); // 不自动重定向
            httpURLConnection.setConnectTimeout(timeoutMillisecond);
            httpURLConnection.setReadTimeout(timeoutMillisecond);
            httpURLConnection.connect();
            // 获取格式
            int code = httpURLConnection.getResponseCode();
            if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP) {
                return httpURLConnection.getHeaderField("Location");
                // Long timeEnd = System.currentTimeMillis();
                // System.out.println("===耗时：" + (timeEnd - timeStart)/1000F + "秒");
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
        return "";
    }

    // 文件大小换算
    public static String getSize(int length)  {
        String size = "未知大小";
        if (length > 0) {
            if (length < 1024) {
                size = String.format("%.2f", length/1F) + "B";
            } else if (length < 1024F*1024F) {
                size = String.format("%.2f", length/1024F) + "KB";
            } else if (length < 1024F*1024F*1024F) {
                size = String.format("%.2f", length/1024F/1024F) + "MB";
            } else {
                size = String.format("%.2f", length/1024F/1024F/1024F) + "GB";
            }
        }
        return size;
    }

    // Base64文件大小
    public static String getBase64FileSize(String downLoadUrl, String fileName) {
        String title = fileName == null ? "未命名" : fileName;
        String base64Str = downLoadUrl.substring(downLoadUrl.indexOf(",")+1);
        byte[] decode = Base64.decode(base64Str, Base64.DEFAULT);
        String fileSize = CommonUtils.getSize(decode.length);
        return title + " / " + fileSize;
    }

    // 获取手机文件管理包名
    public static String[] getPackageName(Context context) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("*/*"); // 设置文件类型
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo info : activities) {
            String packageName = info.activityInfo.packageName;
            String name = info.activityInfo.name;
            if (packageName != null && packageName.startsWith("com.android.")) {
                return new String[] {packageName, name};
            }
        }
        return null;
    }
}

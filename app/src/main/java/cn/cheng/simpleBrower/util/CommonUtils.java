package cn.cheng.simpleBrower.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;

import org.json.JSONException;
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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import cn.cheng.simpleBrower.MyApplication;
import cn.cheng.simpleBrower.bean.PositionBean;
import cn.cheng.simpleBrower.custom.FeetDialog;

public class CommonUtils {

    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1;

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
    public static void fileWalk(String dir, List<String> formats, List<String> fileList) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try (Stream<Path> paths = Files.walk(Paths.get(dir), 2)) { // 递归两层目录
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
                return o1.length() != o2.length() ? o1.length() - o2.length() : (int) (getNum(o1) - getNum(o2));
            }
        });
    }

    /**
     * 获取字符串中的数字
     *
     * @param str
     * @return
     */
    public static double getNum(String str) {
        double ret = 999999;
        if (str != null) {
            if (str.contains("/")) {
                str = str.substring(str.lastIndexOf("/"));
            }
            Pattern pattern = Pattern.compile("(([1-9]\\d*)(\\.\\d+)?)|((0)(\\.\\d+)?)");
            Matcher matcher = pattern.matcher(str);
            if (matcher.find()) {
                ret = Double.parseDouble(matcher.group());
            }
        }
        return ret;
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
                name = url.substring(0, url.lastIndexOf("?")).substring(url.lastIndexOf("/") + 1);
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
                format = url.substring(0, url.lastIndexOf("?")).substring(url.lastIndexOf("/") + 1);
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

    /**
     * 文件删除
     *
     * @param file
     * @return
     */
    public static boolean deleteFile(File file) {
        boolean ret = false;
        if (file != null && file.exists()) {
            ret = file.delete();
            System.gc();
        }
        return ret;
    }

    /**
     * 权限检查
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
     * 权限申请
     *
     * @param context
     */
    public static void requestStoragePermissions(Activity context) {
        // android 11 所有文件管理权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            FeetDialog feetDialog = new FeetDialog(context, "授权", "需授权后才能使用该功能", "授权", "取消");
            feetDialog.setOnTouchListener(new FeetDialog.TouchListener() {
                @Override
                public void close() {
                    feetDialog.dismiss();
                }
                @Override
                public void ok() {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + context.getPackageName()));
                    context.startActivityForResult(intent, 100);
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
        int size = positionBean.getSize();
        int endLine = positionBean.getEndLine();
        int endNum = positionBean.getEndNum();
        positionBean.setStartLine(endLine);
        positionBean.setStartNum(endNum);
        String txt = "";
        if (endLine >= 0 && lines.get(endLine).length() >= endNum) {
            txt = lines.get(endLine).substring(endNum);
        }
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
    }
    public static void readNextPageDef(ArrayList<String> lines, PositionBean positionBean) {
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
    }


    /**
     * 读取上一页
     * @param lines
     * @param positionBean
     */
    public static void readPreviousPage(ArrayList<String> lines, PositionBean positionBean, Handler msgHandler) {
        int size = positionBean.getSize();
        int startLine = positionBean.getStartLine();
        int startNum = positionBean.getStartNum();
        positionBean.setEndLine(startLine);
        positionBean.setEndNum(startNum);
        String txt = "";
        if (startLine >= 0 && startNum > 0 && lines.get(startLine).length() > startNum) {
            txt = lines.get(startLine).substring(0, startNum);
        }
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
        // 只有第一行时
        if (startLine == 0) {
            positionBean.setStartLine(0);
            positionBean.setStartNum(0);
            if (txt.length() > size) {
                positionBean.setStartNum(txt.length() - size);
            }
        }
        positionBean.setTxt(txt);
    }
    public static void readPreviousPageDef(ArrayList<String> lines, PositionBean positionBean) {
        int size = positionBean.getSize();
        int startLine = positionBean.getStartLine();
        int startNum = positionBean.getStartNum();
        String txt = "";
        if (startLine >= 0 && startNum > 0 && lines.get(startLine).length() > startNum) {
            txt = lines.get(startLine).substring(0, startNum);
        }
        for (int i = startLine - 1; i >= 0; i--) {
            txt = txt + lines.get(i); // 先从下往上排版 好统计字数
            if (txt.length() >= size) {
                break;
            }
        }
        positionBean.setTxt(txt);
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
            } else if (s.length == 2 && s[0].matches("\\d+")) {
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
            }
        }
        return "";
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
            JSONObject jsonObject = new JSONObject(json);
            if (jsonObject.has(key)) {
                Gson gson = new Gson();
                bean = gson.fromJson(jsonObject.get(key).toString(), classOfT);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bean;
    }

    /**
     * 网络文件格式
     * @param url
     * @return
     */
    public static String getNetFileType(String url) {
        String mimeType = "未知格式";
        // Long timeStart = System.currentTimeMillis();
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(1200);
            conn.setReadTimeout(1200);
            conn.connect();
            mimeType = conn.getContentType();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        // Long timeEnd = System.currentTimeMillis();
        // System.out.println("耗时：" + (timeEnd - timeStart)/1000F + "秒");
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
            dir = PhoneSysPath.getSandboxPath(MyApplication.getActivity());
        }
        dir += "/" + dirPath;
        File dirFile = new File(dir);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        return new File(dir + "/" + fName);
    }

    /**
     * 安装包后是否已设置 （用于判断只设置一次的配置）
     */
    public static boolean onlySet(Context context) {
        String path = PhoneSysPath.getSandboxPath(context);
        File file = new File(path, "set.txt");
        if (file.exists()) {
            try(BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line = br.readLine();
                if ("only".equals(line)) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try(FileWriter writer = new FileWriter(file)) {
                writer.write("only");
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
                List<String> list = AssetsReader.getLikeList();
                for (String currentUrl : list) {
                    String likeUrl = currentUrl + "\n";
                    if (!likes.contains(currentUrl)) {
                        bos.write(likeUrl.getBytes());
                    }
                }
            } catch (IOException e) {
                e.getMessage();
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }


    /**
     * 保存日志到本地
     *
     */
    public static boolean saveLog(String txt) {
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
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 手机目录文件url修正
    public static String correctUrl(String txtUrl) {
        if (txtUrl.contains("Android/data")) {
            // 访问沙盒目录时
            txtUrl = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + txtUrl.substring(txtUrl.indexOf("Android/data"));
        } else if (txtUrl.contains("/external_files")) {
            // 访问正常SD卡目录时
            txtUrl = txtUrl.replace("/external_files", Environment.getExternalStorageDirectory().getAbsolutePath());
        }
        return txtUrl;
    }
}
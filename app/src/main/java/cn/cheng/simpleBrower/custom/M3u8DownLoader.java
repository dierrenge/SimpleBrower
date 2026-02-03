package cn.cheng.simpleBrower.custom;

import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.webkit.URLUtil;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import cn.cheng.simpleBrower.MyApplication;
import cn.cheng.simpleBrower.bean.NotificationBean;
import cn.cheng.simpleBrower.util.CommonUtils;

/**
 * m3u8格式视频下载器
 */
public class M3u8DownLoader {

    /**
     *
     * 解决java不支持AES/CBC/PKCS7Padding模式解密
     *
     */
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private int what; // 区分点击下载和监测下载 （what为4的时候是点击下载）

    // ts文件16进制关键标志
    // private static final String TS_FLAG = "FF 47 40 ";
    // private static final String TS_FLAG = "82 47 40 ";
    private static final String TS_FLAG = "47 40 ";

    //要下载的m3u8链接
    private final String DOWNLOADURL;

    // 通知id
    private final int id;

    // 获取伪png这种ts文件实际字节开始下标
    // static int num = 0;

    // 线程池
    private ExecutorService fixedThreadPool;

    // 线程数
    private int threadCount;

    //重试次数
    private int retryCount = 30;

    //链接连接超时时间（单位：毫秒）
    private long timeoutMillisecond = 1000L;

    // dir的上级目录
    private String supDir;

    //视频文件名称
    private String fileName;

    //已完成ts片段个数
    private int finishedCount = 0;

    //解密算法名称
    private String method;

    //密钥
    private String key = "";

    //所有ts片段下载链接
    private List<String> tsList = new ArrayList<>();

    //m3u8文本行
    private ArrayList<String> m3u8Lines = new ArrayList<>();

    // 下载任务消息记录
    private NotificationBean notificationBean;

    private Handler handler;

    public M3u8DownLoader(int notificationId) {
        this.handler = DownLoadHandler.getInstance();
        this.id = notificationId;
        this.notificationBean = MyApplication.getDownLoadInfo(notificationId);
        this.DOWNLOADURL = notificationBean.getUrl();
        // 设置下载类型（网站自身提供的下载为4）
        this.what = notificationBean.getWhat();
        //设置生成目录
        this.supDir = notificationBean.getSupDir();
        //设置视频名称
        this.fileName = notificationBean.getTitle();
        //设置线程数
        this.threadCount = notificationBean.getThreadCount();
        //设置重试次数
        this.retryCount = notificationBean.getRetryCount();
        //设置连接超时时间（单位：毫秒）
        this.timeoutMillisecond = notificationBean.getTimeoutMillisecond();
    }

    /**
     * 模拟http请求获取内容
     *
     * @param urls http链接
     * @return 内容
     */
    private StringBuilder getUrlContent(String urls) throws M3u8Exception {
        int count = 1;
        HttpURLConnection httpURLConnection = null;
        StringBuilder content = new StringBuilder();
        InputStream inputStream = null;
        BufferedReader bufferedReader = null;
        while (count <= retryCount) {
            try {
                URL url = new URL(urls);
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setConnectTimeout((int) timeoutMillisecond);
                httpURLConnection.setReadTimeout((int) timeoutMillisecond);
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

                // 模拟 防盗链设置
                httpURLConnection.addRequestProperty("Referer", urls);

                String line;
                inputStream = httpURLConnection.getInputStream();
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                while ((line = bufferedReader.readLine()) != null)
                    content.append(line).append("\n");
                bufferedReader.close();
                inputStream.close();
                // System.out.println(content);
                break;
            } catch (Exception e) {
                System.out.println("第" + count + "获取链接重试！\t" + urls);
                count++;
                e.printStackTrace();
            } finally {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (count > retryCount)
            throw new M3u8Exception("连接超时");
        return content;
    }

    /**
     * 获取所有的ts片段下载链接
     * @return 链接是否被加密，null为非加密
     */
    private void getTsUrl() throws M3u8Exception, URISyntaxException {
        StringBuilder content = getUrlContent(DOWNLOADURL);
        //判断是否是m3u8链接
        if (!content.toString().contains("#EXTM3U"))
            throw new M3u8Exception(DOWNLOADURL + "不是m3u8链接");
        String[] split = content.toString().split("\\n");
        String keyUrl = "";
        boolean isKey = false;
        for (String s : split) {
            //如果含有此字段，则说明只有一层m3u8链接
            if (s.contains("#EXT-X-KEY") || s.contains("#EXTINF")) {
                isKey = true;
                keyUrl = DOWNLOADURL;
                break;
            }
            //如果含有此字段，则说明ts片段链接需要从第二个m3u8链接获取
            if (s.contains(".m3u8")) {
                if (s.startsWith("http://") || s.startsWith("https://")) {
                    keyUrl = s;
                } else {
                    String relativeUrl = DOWNLOADURL.substring(0, DOWNLOADURL.lastIndexOf("/") + 1);
                    String relativeUrl2 = DOWNLOADURL.split("//")[0] + "//" +new URI(DOWNLOADURL).getHost();
                    if (s.startsWith("/")) {
                        keyUrl = relativeUrl2 + s;
                    } else {
                        keyUrl = relativeUrl + s;
                    }
                }
                break;
            }
        }
        if (StringUtils.isEmpty(keyUrl))
            throw new M3u8Exception("未发现有效链接");
        //获取密钥
        String key1;
        key1 = isKey ? getKey(keyUrl, content) : getKey(keyUrl, null);
        if (StringUtils.isNotEmpty(key1)) key = key1; // 需要解密
        else key = null; // 不需要解密
    }

    /**
     * 获取ts解密的密钥，并把ts片段加入set集合
     *
     * @param url     密钥链接，如果无密钥的m3u8，则此字段可为空
     * @param content 内容，如果有密钥，则此字段可以为空
     * @return ts是否需要解密，null为不解密
     */
    private String getKey(String url, StringBuilder content) throws M3u8Exception, URISyntaxException {
        StringBuilder urlContent;
        if (content == null || StringUtils.isEmpty(content.toString()))
            urlContent = getUrlContent(url);
        else urlContent = content;
        if (!urlContent.toString().contains("#EXTM3U"))
            throw new M3u8Exception(DOWNLOADURL + "不是m3u8链接");
        String[] split = urlContent.toString().split("\\n");
        boolean isTsUrl = false;
        int n = 0;
        String relativeUrl = url.substring(0, url.lastIndexOf("/") + 1);
        String relativeUrl2 = DOWNLOADURL.split("//")[0] + "//" + new URI(DOWNLOADURL).getHost();
        for (int i = 0; i < split.length; i++) {
            String s = split[i];
            //如果含有此字段，则获取加密算法以及获取密钥的链接
            if (s.contains("EXT-X-KEY")) {
                String[] split1 = s.split(",");
                // System.out.println("EXT-X-KEY--88888888888888888888888888888888888888888888888-length" + split1.length);
                // System.out.println(s);
                if (split1.length >= 2) {
                    if (split1[0].contains("METHOD"))
                        method = split1[0].split("=", 2)[1];
                    if (split1[1].contains("URI"))
                        key = split1[1].split("=", 2)[1];
                }
            } else {
                // 保存m3u8文本行
                if (!isTsUrl) {
                    m3u8Lines.add(s);
                    if (s.contains("#EXTINF")) {
                        isTsUrl = true;
                    }
                } else {
                    //将ts片段链接加入set集合
                    if (s.startsWith("http://") || s.startsWith("https://")) {
                        tsList.add(s);
                    } else {
                        if (s.startsWith("/")) {
                            tsList.add(relativeUrl2 + s);
                        } else {
                            tsList.add(relativeUrl + s);
                        }
                    }
                    //记录本地m3u8索引
                    n++;
                    if (s.endsWith(".ts")) {
                        m3u8Lines.add(supDir + "/m3u8/" + fileName + "/" + n + ".xyz");
                    } else {
                        m3u8Lines.add(supDir + "/m3u8/" + fileName + "/" + n + ".xyz2");
                    }
                    isTsUrl = false;
                }
            }
        }
        // 开始整理m3u8文本
        new Thread(() -> {
            File dir = new File(supDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String absolutePath = supDir + "/" + fileName + ".m3u8";
            notificationBean.setAbsolutePath(absolutePath);
            File file = new File(absolutePath);
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                StringBuffer buffer = new StringBuffer();
                for (String line : m3u8Lines) {
                    buffer.append(line + "\n");
                }
                bw.write(buffer.toString());
                bw.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        if (!StringUtils.isEmpty(key)) {
            key = key.replace("\"", "");
            if (key.startsWith("/")) {
                return getUrlContent(relativeUrl2 + key).toString().replaceAll("\\s+", "");
            } else {
                return getUrlContent(relativeUrl + key).toString().replaceAll("\\s+", "");
            }
        }
        return null;
    }

    /**
     * 解密ts
     *
     * @param sSrc ts文件字节数组
     * @param sKey 密钥
     * @return 解密后的字节数组
     */
    private byte[] decrypt(byte[] sSrc, String sKey, String method) throws Exception {
        if (StringUtils.isNotEmpty(method) && !method.contains("AES"))
            throw new M3u8Exception("未知的算法");
        // 判断Key是否正确
        if (StringUtils.isEmpty(sKey)) {
            return sSrc;
        }
        // 判断Key是否为16位
        if (sKey.length() != 16) {
            System.out.print("Key长度不是16位");
            return null;
        }
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
        SecretKeySpec keySpec = new SecretKeySpec(sKey.getBytes("utf-8"), "AES");
        //如果m3u8有IV标签，那么IvParameterSpec构造函数就把IV标签后的内容转成字节数组传进去
        AlgorithmParameterSpec paramSpec = new IvParameterSpec(new byte[16]);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, paramSpec);
        return cipher.doFinal(sSrc);
    }

    /**
     * 开启下载线程
     *
     * @param urls ts片段链接
     * @param i    ts片段序号
     * @return 线程
     */
    private Thread getThread0(String urls, int i) {
        return new Thread(() -> {
            if (closed()) return; // 标记为关闭的线程不再执行了
            int count = 1;
            HttpURLConnection httpURLConnection = null;
            //xy为未解密的ts片段，如果存在，则删除
            File file2 = new File(supDir + "/m3u8/" + fileName + "/" + i + ".xy");
            if (file2.exists())
                file2.delete();
            String fName = supDir + "/m3u8/" + fileName + "/" + i + ".xyz";
            OutputStream outputStream = null;
            InputStream inputStream1 = null;
            FileOutputStream outputStream1 = null;
            //重试次数判断
            while (count <= retryCount) {
                if (closed()) return; // 标记为关闭的线程不再执行了
                try {
                    //模拟http请求获取ts片段文件
                    URL url = new URL(urls);
                    httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setConnectTimeout((int) timeoutMillisecond);
                    httpURLConnection.setUseCaches(false);
                    httpURLConnection.setReadTimeout((int) timeoutMillisecond);
                    httpURLConnection.setDoInput(true);
                    InputStream inputStream = httpURLConnection.getInputStream();
                    outputStream = new FileOutputStream(file2);
                    int len;
                    byte[] bytes = new byte[1024 * 4];
                    //将未解密的ts片段写入文件
                    while ((len = inputStream.read(bytes)) != -1) {
                        if (closed()) return; // 标记为关闭的线程不再执行了
                        outputStream.write(bytes, 0, len);
                    }
                    outputStream.flush();
                    inputStream.close();
                    inputStream1 = new FileInputStream(file2);
                    byte[] bytes1 = new byte[inputStream1.available()];
                    inputStream1.read(bytes1);
                    File file = new File(fName);
                    outputStream1 = new FileOutputStream(file);
                    //开始解密ts片段
                    byte[] decrypt = decrypt(bytes1, key, method);
                    outputStream1.write(decrypt);

                    // 破解伪装的非ts文件 向后读取获取真正的ts视频文件
                    if (!urls.endsWith(".ts")) {
                        byte[] b = new byte[4096];
                        int l;
                        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(supDir + "/m3u8/" + fileName + "/" + i + ".xyz2"));
                             RandomAccessFile raFile = new RandomAccessFile(file, "rw");) {
                            raFile.seek(getTsNum(file));
                            while ((l = raFile.read(b)) != -1) {
                                if (closed()) return; // 标记为关闭的线程不再执行了
                                bos.write(b, 0, l);
                            }
                            file.delete();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    file2.delete();
                    break;
                } catch (Exception e) {
                    try {
                        new File(fName).createNewFile();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    System.out.println("第" + count + "获取链接重试！\t" + urls);
                    count++;
                    e.printStackTrace();
                } finally {
                    try {
                        if (inputStream1 != null)
                            inputStream1.close();
                        if (outputStream1 != null)
                            outputStream1.close();
                        if (outputStream != null)
                            outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }
                }
            }
            if (count > retryCount) {
                System.out.println("----------连接超时！-------");
                return;
            }
            // 保存下载进度
            finishedCount++;
            notificationBean.setHlsFinishedCount(finishedCount);
            notificationBean.setHlsFinishedNum(i);
            notificationBean.setTsList(tsList);
            // System.out.println(urls + "下载完毕！\t已完成" + finishedCount + "个，还剩" + (tsSet.size() - finishedCount) + "个");
        });
    }

    /**
     * 获取伪png这种ts文件实际字节开始下标
     */
    private int getTsNum(File file) {
        // 方式1、只计算一遍 效率但不够准确
        // if (num > 0) {
        //     return num;
        // }
        // 方式2、每次从新计算 准确但不够效率
        int num = 0;
        int value = 0;
        List<String> sbHexList = new ArrayList<>();
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            while ((value = is.read()) != -1) {
                // 转换16进制字符
                sbHexList.add(String.format("%02X ", value));
                if (num >= 1) {
                    String flag = sbHexList.get(num - 1) + sbHexList.get(num);
                    flag = flag.toUpperCase();
                    if (flag.equals(TS_FLAG)) { // ts文件16进制关键标志
                        // System.out.println((num - 1) + "----------获取伪png这种ts文件实际字节开始下标--------" + flag);
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

    /**
     * 下载存盘所以ts片段
     */
    private void startDownload0() {
        int i = 0;
        // 如果生成目录不存在，则创建
        File file1 = new File(supDir + "/m3u8/" + fileName);
        // System.out.println("生成目录==========" + file1.getAbsolutePath());
        if (!file1.exists())
            file1.mkdirs();
        // 执行多线程下载
        for (String s : tsList) {
            i++;
            if (notificationBean.getHlsFinishedNumList().contains(i)) {
                continue;
            }
            fixedThreadPool.execute(getThread0(s, i));
        }
        // 关闭线程池
        fixedThreadPool.shutdown();
        // 下载过程监视
        try {
            //轮询是否下载成功
            while (!fixedThreadPool.isTerminated() && "暂停".equals(notificationBean.getState())) {
                try {
                    if (closed()) return; // 标记为关闭的线程不再执行了
                    Thread.sleep(1000L);
                    if (tsList.size() != 0) {
                        String[] arr = new String[]{CommonUtils.getPercentage(finishedCount, tsList.size()) + "", id + "", fileName};
                        Message msg = handler.obtainMessage(3, arr);
                        handler.sendMessage(msg);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    String[] arr = new String[]{e.getMessage(), id + ""};
                    Message msg = handler.obtainMessage(2, arr);
                    handler.sendMessage(msg);
                }
            }
            if (fixedThreadPool.isTerminated() && "暂停".equals(notificationBean.getState()) && tsList.size() > finishedCount) {
                // 部分下载完成提示
                String str = "部分下载完成！下载" + finishedCount + "个ts文件，实际" + tsList.size() + "个，可继续尝试下载";
                stopAndSendMsg(str, 0, 0);
                System.out.println(str);
            }
            if ("暂停".equals(notificationBean.getState()) && tsList.size() <= finishedCount) {
                // 下载成功提示
                String str = "下载完成！共" + finishedCount + "个ts文件";
                sendMsg(str, 2);
                System.out.println(str);
            }
        } catch (Exception e) {
            e.printStackTrace();
            String[] arr = new String[]{e.getMessage(), id + ""};
            Message msg = handler.obtainMessage(2, arr);
            handler.sendMessage(msg);
        }
    }

    /**
     * 开始下载视频
     */
    public void start() {
        if (notificationBean == null) return;
        TaskExecutionManager.getInstance().executeTask(id, () -> {
            System.out.println("下载文件原始链接：" + DOWNLOADURL);
            if (DOWNLOADURL.endsWith(".m3u8")) {
                try {
                    // 创建并记录线程池
                    this.fixedThreadPool = Executors.newFixedThreadPool(threadCount);
                    // notificationBean.setFixedThreadPool(fixedThreadPool);
                    // 获取并整理所有ts片段链接
                    if (notificationBean.getHlsFinishedNumList().isEmpty()) {
                        getTsUrl(); // 首次获取ts片段
                    } else {
                        finishedCount = notificationBean.getHlsFinishedCount(); // 非首次继续上次进度
                        tsList = notificationBean.getTsList();
                    }
                    // 下载存盘所以ts片段
                    startDownload0();
                } catch (Exception e) {
                    e.printStackTrace();
                    String[] arr = new String[]{e.getMessage(), id+""};
                    Message msg= handler.obtainMessage(4, arr);
                    handler.sendMessage(msg);
                }
            } else {
                /*if (notificationBean.getRangeRequest() == null) {
                    notificationBean.setRangeRequest(supportRR() + "");
                }*/
                getUrlContentFile();
            }
        });
    }

    /**
     * 模拟http请求获取内容
     *
     * @return 内容
     */
    private void getUrlContentFile() {
        HttpURLConnection httpURLConnection = null;
        File dir = new File(supDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        int bytesum = 0;
        if (!"false".equals(notificationBean.getRangeRequest())) {
            bytesum = notificationBean.getBytesum(); // 尝试恢复进度
        }
        try {
            URL url = new URL(DOWNLOADURL);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setConnectTimeout((int) timeoutMillisecond);
            httpURLConnection.setReadTimeout((int) timeoutMillisecond);
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

            // 保存文件的绝对路径
            String absolutePath = notificationBean.getAbsolutePath();
            if (absolutePath == null) {
                absolutePath = supDir + "/" + fileName;
                if (!fileName.contains(".") || what != 4) {
                    // 获取格式
                    String contentType = httpURLConnection.getContentType();
                    String format = CommonUtils.getUrlFormat(DOWNLOADURL, contentType);
                    absolutePath = supDir + "/" + fileName + format;
                }
                notificationBean.setAbsolutePath(absolutePath);
            }

            // System.out.println("+++++++++++++++++++++++++++++++" + absolutePath);
            File file = new File(absolutePath);
            // 检查本地文件是否存在
            if (file.exists()) {
                if (notificationBean.getTotalSize() == 0) {
                    String m = "已存在相同文件";
                    stopAndSendMsg(m, 4, bytesum);
                    return;
                }
                if (bytesum > 0) {
                    bytesum = (int) file.length();
                    httpURLConnection.setRequestProperty("Range", "bytes=" + bytesum + "-");
                }
            }

            // 连接并判断请求状态
            httpURLConnection.connect();
            int responseCode = httpURLConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK && bytesum > 0) {
                String m = "不支持断点续传，请勿点击暂停";
                // file.delete();
                stopAndSendMsg(m, 10, 0);
                return;
            }
            if (responseCode != HttpURLConnection.HTTP_PARTIAL && responseCode != HttpURLConnection.HTTP_OK) {
                String m = "网络请求错误：" + responseCode;
                stopAndSendMsg(m, 0, bytesum);
                return;
            }

            // 获取文件长度
            int contentLength = httpURLConnection.getContentLength();
            if (bytesum == 0) {
                notificationBean.setTotalSize(contentLength > 0 ? contentLength : 1);
            }
            if (bytesum > 0 && contentLength == -1) {
                String m = "不支持断点续传，请勿点击暂停";
                // file.delete();
                stopAndSendMsg(m, 10, 0);
                return;
            }
            // System.out.println("=======contentLength=====" +contentLength);

            int len;
            byte[] buf = new byte[1024*8];
            // 获取文件流
            InputStream inputStream = httpURLConnection.getInputStream();
            try (BufferedInputStream bis = new BufferedInputStream(inputStream);
                 RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {
                randomAccessFile.seek(bytesum);
                long time = 0;
                String index = "0.00%";
                while ((len = bis.read(buf)) != -1 && "暂停".equals(notificationBean.getState())) {
                    randomAccessFile.write(buf, 0, len);
                    // 更新进度
                    bytesum += len;
                    notificationBean.setBytesum(bytesum); // 保存下载进度
                    index = String.format("%.2f", CommonUtils.getPercentage(bytesum, notificationBean.getTotalSize()));
                    if (len > 0 && System.currentTimeMillis() - time > 1000) {
                        time = System.currentTimeMillis();
                        String[] arr = new String[]{index, id+"", fileName};
                        Message msg0 = handler.obtainMessage(3, arr);
                        handler.sendMessage(msg0);
                    }
                }
                // 可能不到一秒间隔就被暂停了，再刷新下进度
                String[] arr = new String[]{index, id+"", fileName};
                Message msg0 = handler.obtainMessage(3, arr);
                handler.sendMessage(msg0);
                if ("暂停".equals(notificationBean.getState()) && bytesum >= notificationBean.getTotalSize()) {
                    String[] arr2 = new String[]{"下载文件成功", id+""};
                    Message msg = handler.obtainMessage(2, arr2);
                    handler.sendMessage(msg);
                }
                // notificationBean.setBytesum(bytesum);
            } catch (Exception e) {
                String eMsg = e.getMessage();
                if (eMsg != null && eMsg.contains("connection")) {
                    String m = "请检查网络：" + eMsg;
                    stopAndSendMsg(m, 0, bytesum);
                } else {
                    String m = "下载异常：" + eMsg;
                    stopAndSendMsg(m, 0, bytesum);
                }
                e.printStackTrace();
            }
        } catch (Exception e) {
            String m = "请检查网络：" + e.getMessage();
            stopAndSendMsg(m, 0, bytesum);
            e.printStackTrace();
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
    }

    /**
     * 判断是否支持断点续传
     * Support Range Request
     * @return 内容
     */
    private boolean supportRR() {
        HttpURLConnection httpURLConnection = null;
        File dir = new File(supDir);
        if (!dir.exists()) dir.mkdirs();
        try {
            URL url = new URL(DOWNLOADURL);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setConnectTimeout(2000);
            httpURLConnection.setReadTimeout(2000);
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
            // 断点开始位置 调试
            httpURLConnection.setRequestProperty("Range", "bytes=1-");
            // 连接
            httpURLConnection.connect();
            // 获取文件长度
            int contentLength = httpURLConnection.getContentLength();
            // 获取响应状态
            int responseCode = httpURLConnection.getResponseCode();
            // 判断是否支持断点续传
            if (responseCode == HttpURLConnection.HTTP_PARTIAL && contentLength > 0) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
        return false;
    }

    private boolean supportRR2() {
        HttpURLConnection httpURLConnection = null;
        File dir = new File(supDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        int bytesum = 1000;
        try {
            URL url = new URL(DOWNLOADURL);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setConnectTimeout((int) timeoutMillisecond);
            httpURLConnection.setReadTimeout((int) timeoutMillisecond);
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

            // 保存文件的绝对路径
            /*String absolutePath = supDir + "/" + fileName;
            if (!fileName.contains(".") || what != 4) {
                // 获取格式
                String contentType = httpURLConnection.getContentType();
                String format = CommonUtils.getUrlFormat(DOWNLOADURL, contentType);
                absolutePath = supDir + "/" + fileName + format;
            }
            File file = new File(absolutePath);*/
            httpURLConnection.setRequestProperty("Range", "bytes=" + bytesum + "-");

            // 连接并判断请求状态
            httpURLConnection.connect();

            // 获取文件长度
            int contentLength = httpURLConnection.getContentLength();
            if (bytesum == 0) {
                notificationBean.setTotalSize(contentLength > 0 ? contentLength : 1);
            }
            /*if (bytesum > 0 && contentLength == -1) {
                String m = "不支持断点续传，请勿点击暂停";
                // file.delete();
                stopAndSendMsg(m, 10, 0);
                return;
            }*/
            // System.out.println("=======contentLength=====" +contentLength);

            int len;
            byte[] buf = new byte[1024*8];
            // 获取文件流
            InputStream inputStream = httpURLConnection.getInputStream();
        } catch (Exception e) {
            if ("Cannot set request property after connection is made".equals(e.getMessage())) {
                String m = "不支持断点续传，请勿点击暂停";
                // file.delete();
                // stopAndSendMsg(m, 10, 0);
            } else {
                String m = "请检查网络：" + e.getMessage();
                // stopAndSendMsg(m, 0, bytesum);
            }
            e.printStackTrace();
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
        return false;
    }

    // 暂停下载消息，发送指定提示
    private void stopAndSendMsg(String m, int w, int bytesum) {
        // 保存下载进度
        notificationBean.setBytesum(bytesum);
        // 消息提示
        String[] arr = new String[]{m, id+"", w+""};
        Message msg= handler.obtainMessage(w%10, arr);
        handler.sendMessage(msg);
    }
    private void sendMsg(String m, int w) {
        // 消息提示
        String[] arr = new String[]{m, id+""};
        Message msg= handler.obtainMessage(w, arr);
        handler.sendMessage(msg);
    }

    /**
     * 模拟http请求获取下载文件名及文件大小
     *
     * @return 文件名及文件大小
     */
    public static String getUrlContentFileSize(String downLoadUrl, String fileName) {
        String title = fileName == null ? "未命名" : fileName;
        HttpURLConnection httpURLConnection = null;
        try {
            URL url = new URL(downLoadUrl);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setConnectTimeout((int) 2000L);
            httpURLConnection.setReadTimeout((int) 2000L);
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
            // 获取长度
            String fileSize = CommonUtils.getSize(httpURLConnection.getContentLength());
            // System.out.println("=======contentLength=====" + contentLength);
            if (title.contains(".")) {
                title += " / " + fileSize;
            } else {
                // 获取格式
                String contentType = httpURLConnection.getContentType();
                String format = CommonUtils.getUrlFormat(downLoadUrl, contentType);
                if (fileName == null) {
                    fileName = URLUtil.guessFileName(downLoadUrl, "", contentType);
                    format = "";
                }
                title = fileName + format + " / " + fileSize;
            }
            // System.out.println("+++++++++++++++++++++++++++++++" + title);
        } catch (Exception e) {
            CommonUtils.saveLog("getUrlContentFileSize：" + e.getMessage());
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
        return title;
    }

    public static String getBase64FileSize(String downLoadUrl, String fileName) {
        String title = fileName == null ? "未命名" : fileName;
        String base64Str = downLoadUrl.substring(downLoadUrl.indexOf(",")+1);
        byte[] decode = Base64.decode(base64Str, Base64.DEFAULT);
        String fileSize = CommonUtils.getSize(decode.length);
        return title + " / " + fileSize;
    }

    public static void test(String str, Handler handler) {
        new Thread(()-> {
            long thatTime = System.currentTimeMillis();
            while (System.currentTimeMillis() -1000*15 < thatTime) {
                System.out.println("======" + System.currentTimeMillis());
            }
            String[] arr = new String[]{"下载文件成功", 1+""};
            Message msg= handler.obtainMessage(2, arr);
            handler.sendMessage(msg);
            System.out.println("====================   " + str);
        }).start();
    }

    class M3u8Exception extends Exception{
        public M3u8Exception(String message) {
            super(message);
        }
    }

    // 判断是否暂停
    private boolean closed() {
        return !"暂停".equals(notificationBean.getState());
    }

}

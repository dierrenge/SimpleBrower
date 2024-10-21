package cn.cheng.simpleBrower.custom;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.webkit.URLUtil;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
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
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

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


    //要下载的m3u8链接
    private final String DOWNLOADURL;

    // 通知id
    private final int id;

    // 获取伪png这种ts文件实际字节开始下标
    static int num = 0;

    //线程数
    private int threadCount = 1;

    // 线程池
    private ExecutorService fixedThreadPool;

    //重试次数
    private int retryCount = 30;

    //链接连接超时时间（单位：毫秒）
    private long timeoutMillisecond = 1000L;

    //合并后的文件存储目录
    private String dir;

    // dir的上级目录
    private String supDir;

    //合并后的视频文件名称
    private String fileName;

    //已完成ts片段个数
    private int finishedCount = 0;

    //解密算法名称
    private String method;

    //密钥
    private String key = "";

    //所有ts片段下载链接
    private Set<String> tsSet = new LinkedHashSet<>();

    //m3u8文本行
    private ArrayList<String> m3u8Lines = new ArrayList<>();

    //解密后的片段
    private Set<File> finishedFiles = new ConcurrentSkipListSet<>(Comparator.comparingInt(o -> Integer.parseInt(o.getName().replace(".xyz", ""))));

    //已经下载的文件大小
    private BigDecimal downloadBytes = new BigDecimal(0);

    // m3u8是否转MP4
    private boolean m3u8ToMp4 = false;

    public void setM3u8ToMp4(boolean m3u8ToMp4) {
        this.m3u8ToMp4 = m3u8ToMp4;
    }

    public M3u8DownLoader(String m3U8URL, int notificationId) {
        DOWNLOADURL = m3U8URL;
        id = notificationId;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public void setFixedThreadPool(ExecutorService fixedThreadPool) {
        this.fixedThreadPool = fixedThreadPool;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public void setTimeoutMillisecond(long timeoutMillisecond) {
        this.timeoutMillisecond = timeoutMillisecond;
    }

    public void setDir(String dir, String supDir) {
        this.dir = dir;
        this.supDir = supDir;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
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
    private String getTsUrl() throws M3u8Exception, URISyntaxException {
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
        if (StringUtils.isNotEmpty(key1))
            key = key1;
        else key = null;
        return key;
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
        for (String s : split) {
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
                break;
            } else if (!m3u8ToMp4)  {
                // 保存m3u8文本行
                if (!isTsUrl) {
                    m3u8Lines.add(s);
                    if (s.contains("#EXTINF")) {
                        isTsUrl = true;
                    }
                } else {
                    n++;
                    if (!s.endsWith("ts")) {
                        m3u8Lines.add(supDir + "/m3u8/" + fileName + "/" + n + ".xyz2");
                    } else {
                        m3u8Lines.add(supDir + "/m3u8/" + fileName + "/" + n + ".xyz");
                    }
                    isTsUrl = false;
                }
            }
        }
        if (!m3u8ToMp4) {
            // 开始整理m3u8文本
            new Thread(() -> {
                File dir = new File(supDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File file = new File(supDir + "/" + fileName + ".m3u8");
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
        }
        String relativeUrl = url.substring(0, url.lastIndexOf("/") + 1);
        String relativeUrl2 = DOWNLOADURL.split("//")[0] + "//" +new URI(DOWNLOADURL).getHost();
        //将ts片段链接加入set集合
        for (int i = 0; i < split.length; i++) {
            String s = split[i];
            if (s.contains("#EXTINF")) {
                String ts = split[++i];
                if (ts.startsWith("http://") || ts.startsWith("https://")) {
                    tsSet.add(ts);
                } else {
                    if (ts.startsWith("/")) {
                        tsSet.add(relativeUrl2 + ts);
                    } else {
                        tsSet.add(relativeUrl + ts);
                    }
                }
            }
        }
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
    private Thread getThread(String urls, int i) {
        return new Thread(() -> {
            int count = 1;
            HttpURLConnection httpURLConnection = null;
            //xy为未解密的ts片段，如果存在，则删除
            File file2 = new File(dir + "/" + i + ".xy");
            if (file2.exists())
                file2.delete();
            OutputStream outputStream = null;
            InputStream inputStream1 = null;
            FileOutputStream outputStream1 = null;
            //重试次数判断
            while (count <= retryCount) {
                try {
                    //模拟http请求获取ts片段文件
                    URL url = new URL(urls);
                    httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setConnectTimeout((int) timeoutMillisecond);
                    httpURLConnection.setUseCaches(false);
                    httpURLConnection.setReadTimeout((int) timeoutMillisecond);
                    httpURLConnection.setDoInput(true);
                    InputStream inputStream = httpURLConnection.getInputStream();
                    try {
                        outputStream = new FileOutputStream(file2);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    int len;
                    byte[] bytes = new byte[1024 * 4];
                    //将未解密的ts片段写入文件
                    while ((len = inputStream.read(bytes)) != -1) {
                        outputStream.write(bytes, 0, len);
                        synchronized (this) {
                            downloadBytes = downloadBytes.add(new BigDecimal(len));
                        }
                    }
                    outputStream.flush();
                    inputStream.close();
                    inputStream1 = new FileInputStream(file2);
                    byte[] bytes1 = new byte[inputStream1.available()];
                    inputStream1.read(bytes1);
                    File file = new File(dir + "/" + i + ".xyz");
                    outputStream1 = new FileOutputStream(file);
                    //开始解密ts片段
                    outputStream1.write(decrypt(bytes1, key, method));
                    finishedFiles.add(file);
                    file2.delete();
                    break;
                } catch (Exception e) {
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
                //自定义异常
                System.out.println("----------连接超时！-------");
                return;
            }
            finishedCount++;
            // System.out.println(urls + "下载完毕！\t已完成" + finishedCount + "个，还剩" + (tsSet.size() - finishedCount) + "个");
        });
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
            RandomAccessFile raFile = null;
            BufferedOutputStream bos = null;
            //重试次数判断
            while (count <= retryCount) {
                try {
                    //模拟http请求获取ts片段文件
                    URL url = new URL(urls);
                    httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setConnectTimeout((int) timeoutMillisecond);
                    httpURLConnection.setUseCaches(false);
                    httpURLConnection.setReadTimeout((int) timeoutMillisecond);
                    httpURLConnection.setDoInput(true);
                    InputStream inputStream = httpURLConnection.getInputStream();
                    try {
                        outputStream = new FileOutputStream(file2);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    int len;
                    byte[] bytes = new byte[1024 * 4];
                    //将未解密的ts片段写入文件
                    while ((len = inputStream.read(bytes)) != -1) {
                        outputStream.write(bytes, 0, len);
                        synchronized (this) {
                            downloadBytes = downloadBytes.add(new BigDecimal(len));
                        }
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
                    if(!urls.endsWith(".ts")) {
                        int l;
                        byte[] b = new byte[4096];
                        bos = new BufferedOutputStream(new FileOutputStream(supDir + "/m3u8/" + fileName + "/" + i + ".xyz2"));
                        raFile = new RandomAccessFile(file, "rw");
                        raFile.seek(getTsNum(file));
                        while ((l = raFile.read(b)) != -1) {
                            bos.write(b, 0, l);
                        }
                        file.delete();
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
                        if (bos != null)
                            bos.close();
                        if (raFile != null)
                            raFile.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }
                }
            }
            if (count > retryCount) {
                //自定义异常
                System.out.println("----------连接超时！-------");
                return;
            }
            finishedCount++;
            // System.out.println(urls + "下载完毕！\t已完成" + finishedCount + "个，还剩" + (tsSet.size() - finishedCount) + "个");
        });
    }

    /**
     * 合并下载好的ts片段
     */
    private void mergeTs(BufferedOutputStream bos, BufferedInputStream bis, RandomAccessFile raFile) throws Exception {
        File file = new File(supDir + "/" + fileName + ".mp4");
        if (file.exists())
            file.delete();
        else file.createNewFile();
        bos = new BufferedOutputStream(new FileOutputStream(file));
        byte[] b = new byte[4096];
        for (File f : finishedFiles) {
            int len;
            if (!tsSet.iterator().next().endsWith(".ts")) {
                // 破解伪装的非ts文件 向后读取获取真正的ts视频文件
                raFile = new RandomAccessFile(f, "rw");
                raFile.seek(getTsNum(f));
                while ((len = raFile.read(b)) != -1) {
                    bos.write(b, 0, len);
                }
                raFile.close();
            } else {
                bis = new BufferedInputStream(new FileInputStream(f));
                while ((len = bis.read(b)) > 0) {
                    bos.write(b, 0, len);
                }
                bis.close();
            }
            bos.flush();
            f.delete();
        }
        bos.close();
    }

    /**
     * 获取伪png这种ts文件实际字节开始下标
     */
    private int getTsNum(File file) {
        if (num > 0) {
            return num;
        }
        int value = 0;
        List<String> sbHexList = new ArrayList<>();
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            while ((value = is.read()) != -1) {
                // 转换16进制字符
                sbHexList.add(String.format("%02X ", value));
                if (num >= 2) {
                    String flag = sbHexList.get(num-2) + sbHexList.get(num-1) + sbHexList.get(num);
                    flag = flag.toUpperCase();
                    if (flag.equals("FF 47 40 ")) { // ts文件16进制关键标志
                        // System.out.println((num + 1) + "----------获取伪png这种ts文件实际字节开始下标--------" + flag);
                        return num + 1;
                    }
                }
                num++;
                // 兜底 防止一直循环
                if (num >= 2000) {
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

    private int getTsNum(InputStream is) {
        if (num > 0) {
            return num;
        }
        int value = 0;
        List<String> sbHexList = new ArrayList<>();
        try {
            while ((value = is.read()) != -1) {
                // 转换16进制字符
                sbHexList.add(String.format("%02X ", value));
                if (num >= 2) {
                    String flag = sbHexList.get(num-2) + sbHexList.get(num-1) + sbHexList.get(num);
                    flag = flag.toUpperCase();
                    if (flag.equals("FF 47 40 ")) { // ts文件16进制关键标志
                        // System.out.println((num + 1) + "----------获取伪png这种ts文件实际字节开始下标--------" + flag);
                        return num + 1;
                    }
                }
                num++;
                // 兜底 防止一直循环
                if (num >= 2000) {
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
     * 下载视频
     */
    private void startDownload(Handler handler) {
        // 线程池
        if (fixedThreadPool == null) {
            fixedThreadPool = Executors.newFixedThreadPool(threadCount);
        }
        int i = 0;
        // 如果生成目录不存在，则创建
        File file1 = new File(dir);
        // System.out.println("生成目录==========" + file1.getAbsolutePath());
        if (!file1.exists())
            file1.mkdirs();
        // 执行多线程下载
        for (String s : tsSet) {
            i++;
            fixedThreadPool.execute(getThread(s, i));
        }
        // 关闭线程池
        fixedThreadPool.shutdown();
        // 下载过程监视
        new Thread(() -> {
            BufferedOutputStream bos = null;
            BufferedInputStream bis = null;
            RandomAccessFile raFile = null;
            try {
                int consume = 0;
                //轮询是否下载成功
                while (!fixedThreadPool.isTerminated()) {
                    try {
                        consume++;
                        BigDecimal bigDecimal = new BigDecimal(downloadBytes.toString());
                        Thread.sleep(1000L);
                        if (tsSet.size() != 0) {
                            String[] arr = new String[]{new BigDecimal(finishedCount).divide(new BigDecimal(tsSet.size()), 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(100)).setScale(2, BigDecimal.ROUND_HALF_UP) + "", id+""};
                            Message msg = handler.obtainMessage(3, arr);
                            handler.sendMessage(msg);
                        }
                        // System.out.print("已用时" + consume + "秒！\t下载速度：" /*+ StringUtils.convertToDownloadSpeed(new BigDecimal(downloadBytes.toString()).subtract(bigDecimal), 3) + "/s"*/);
                        // System.out.print("\t已完成" + finishedCount + "个，还剩" + (tsSet.size() - finishedCount) + "个");
                        // System.out.println(new BigDecimal(finishedCount).divide(new BigDecimal(tsSet.size()), 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(100)).setScale(2, BigDecimal.ROUND_HALF_UP) + "%");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        String[] arr = new String[]{e.getMessage(), id+""};
                        Message msg= handler.obtainMessage(4, arr);
                        handler.sendMessage(msg);
                    }
                }
                String str = finishedFiles.size() + "个ts文件";
                if (tsSet.size() == finishedFiles.size()) {
                    str = "下载完成，正在合并文件！共" + str;
                } else {
                    str = "部分下载完成，正在合并文件！共" + str + "，实际" + tsSet.size() + "个ts文件";
                }
                String[] arr = new String[]{str, id+""};
                Message msg0 = handler.obtainMessage(3, arr);
                handler.sendMessage(msg0);
                // System.out.println(str /*+ StringUtils.convertToDownloadSpeed(downloadBytes, 3)*/);
                // 开始合并视频
                mergeTs(bos, bis, raFile);
                // 下载成功提示
                String[] arr2 = new String[]{"下载文件成功", id+""};
                Message msg = handler.obtainMessage(2, arr2);
                handler.sendMessage(msg);
                new File(dir).delete();
                System.out.println("视频合并完成，欢迎使用!");
            } catch (Exception e) {
                e.printStackTrace();
                String[] arr = new String[]{e.getMessage(), id+""};
                Message msg= handler.obtainMessage(4, arr);
                handler.sendMessage(msg);
            } finally {
                try {
                    if (bis != null) {
                        bis.close();
                    }
                    if (raFile != null) {
                        raFile.close();
                    }
                    if (bos != null) {
                        bos.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    /**
     * 下载视频
     */
    private void startDownload0(Handler handler) {
        // 线程池
        if (fixedThreadPool == null) {
            fixedThreadPool = Executors.newFixedThreadPool(threadCount);
        }
        int i = 0;
        // 如果生成目录不存在，则创建
        File file1 = new File(supDir + "/m3u8/" + fileName);
        // System.out.println("生成目录==========" + file1.getAbsolutePath());
        if (!file1.exists())
            file1.mkdirs();
        // 执行多线程下载
        for (String s : tsSet) {
            i++;
            fixedThreadPool.execute(getThread0(s, i));
        }
        // 关闭线程池
        fixedThreadPool.shutdown();
        // 下载过程监视
        new Thread(() -> {
            try {
                int consume = 0;
                //轮询是否下载成功
                while (!fixedThreadPool.isTerminated()) {
                    try {
                        consume++;
                        BigDecimal bigDecimal = new BigDecimal(downloadBytes.toString());
                        Thread.sleep(1000L);
                        if (tsSet.size() != 0) {
                            String[] arr = new String[]{new BigDecimal(finishedCount).divide(new BigDecimal(tsSet.size()), 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(100)).setScale(2, BigDecimal.ROUND_HALF_UP) + "", id+""};
                            Message msg = handler.obtainMessage(3, arr);
                            handler.sendMessage(msg);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        String[] arr = new String[]{e.getMessage(), id+""};
                        Message msg= handler.obtainMessage(4, arr);
                        handler.sendMessage(msg);
                    }
                }
                // 下载成功提示
                String str = finishedCount + "个ts文件";
                if (tsSet.size() == finishedCount) {
                    str = "下载完成！共" + str;
                } else {
                    str = "部分下载完成！共" + str + "，实际" + tsSet.size() + "个ts文件";
                }
                String[] arr2 = new String[]{str, id+""};
                Message msg = handler.obtainMessage(2, arr2);
                handler.sendMessage(msg);
                System.out.println(str);
            } catch (Exception e) {
                e.printStackTrace();
                String[] arr = new String[]{e.getMessage(), id+""};
                Message msg= handler.obtainMessage(4, arr);
                handler.sendMessage(msg);
            }
        }).start();
    }

    /**
     * 开始下载视频
     */
    public void start(Handler handler) {
        // checkField();
        new Thread(()->{
            System.out.println(DOWNLOADURL + "   =============");
            if (DOWNLOADURL.endsWith(".m3u8")) {
                try {
                    String tsUrl = getTsUrl();
                    System.out.println("----" + tsUrl);
                    if(StringUtils.isEmpty(tsUrl)) {
                        System.out.println("不需要解密");
                    }
                    if (!m3u8ToMp4) {
                        startDownload0(handler);
                    } else {
                        startDownload(handler);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    String[] arr = new String[]{e.getMessage(), id+""};
                    Message msg= handler.obtainMessage(4, arr);
                    handler.sendMessage(msg);
                }
            } else {
                getUrlContentFile(handler);
            }

        }).start();
    }

    /**
     * 模拟http请求获取内容
     *
     * @return 内容
     */
    private void getUrlContentFile(Handler handler) {
        int count = 1;
        HttpURLConnection httpURLConnection = null;
        File dir = new File(supDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        while (count <= retryCount) {
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
                // 获取文件流
                InputStream inputStream = httpURLConnection.getInputStream();
                // 获取长度
                int contentLength = httpURLConnection.getContentLength();
                // System.out.println("=======contentLength=====" +contentLength);
                // 获取格式
                String contentType = httpURLConnection.getContentType();
                String format = CommonUtils.getUrlFormat(DOWNLOADURL);
                if ("".equals(format)) {
                    if (contentType.contains("text/plain")) {
                        format = ".txt";
                    } else {
                        String[] s = contentType.split("/");
                        if (s.length > 0) {
                            format = "." + s[s.length - 1];
                        } else {
                            format = ".未知格式";
                        }
                    }
                }
                if (fileName == null) {
                    fileName = URLUtil.guessFileName(DOWNLOADURL, "", contentType);
                }
                // System.out.println("+++++++++++++++++++++++++++++++" + supDir + "/" + fileName + format);
                File file = new File(supDir + "/" + fileName + format);
                int len, bytesum = 0;
                byte[] buf = new byte[1024*8];
                try (BufferedInputStream bis = new BufferedInputStream(inputStream);
                     BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {

                    long time = 0;
                    while ((len = bis.read(buf)) != -1) {
                        bos.write(buf, 0, len);
                        // 更新进度
                        bytesum += len;
                        String index = String.format("%.2f", bytesum * 100F / contentLength);
                        if (len > 0 && System.currentTimeMillis() - time > 1000) {
                            time = System.currentTimeMillis();
                            String[] arr = new String[]{index, id+""};
                            Message msg0 = handler.obtainMessage(3, arr);
                            handler.sendMessage(msg0);
                        }
                    }
                    inputStream.close();
                    String[] arr2 = new String[]{"下载文件成功", id+""};
                    Message msg = handler.obtainMessage(2, arr2);
                    handler.sendMessage(msg);
                } catch (Exception e) {
                   e.printStackTrace();
                    String[]  arr = new String[]{"", id+""};
                    Message msg= handler.obtainMessage(4, arr);
                    handler.sendMessage(msg);
                }
                break;
            } catch (Exception e) {
                System.out.println("第" + count + "获取链接重试！\t" + DOWNLOADURL);
                count++;
                e.printStackTrace();
                String[]  arr = new String[]{"", id+""};
                Message msg= handler.obtainMessage(4, arr);
                handler.sendMessage(msg);
            } finally {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
            }
        }
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
            float contentLength = httpURLConnection.getContentLength()/1024F/1024F;
            // System.out.println("=======contentLength=====" + contentLength);
            // 获取格式
            String contentType = httpURLConnection.getContentType();
            String format = CommonUtils.getUrlFormat(downLoadUrl);
            if ("".equals(format)) {
                if (contentType.contains("text/plain")) {
                    format = ".txt";
                } else {
                    String[] s = contentType.split("/");
                    if (s.length > 0) {
                        format = "." + s[s.length - 1];
                    } else {
                        format = ".未知格式";
                    }
                }
            }
            if (fileName == null) {
                fileName = URLUtil.guessFileName(downLoadUrl, "", contentType);
            }
            title = fileName  + format + " / " + String.format("%.2f", contentLength) + "M";
            // System.out.println("+++++++++++++++++++++++++++++++" + title);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
        return title;
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

}

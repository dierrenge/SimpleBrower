package cn.cheng.simpleBrower.bean;

import android.app.Notification;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import cn.cheng.simpleBrower.custom.M3u8DownLoader;

/**
 * 下载任务消息
 */
public class NotificationBean {
    private Notification notification;
    private String title;
    private String url;
    private String state; // 表示点击后呈现的状态

    private int bytesum; // 记录已下载进度

    private int totalSize; // 文件总大小

    private M3u8DownLoader m3u8Download;

    private ExecutorService fixedThreadPool; // 线程池

    private List<Integer> hlsFinishedNumList = new ArrayList<>();

    private int hlsFinishedCount;

    public Notification getNotification() {
        return notification;
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public int getBytesum() {
        return bytesum;
    }

    public void setBytesum(int bytesum) {
        this.bytesum = bytesum;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

    public M3u8DownLoader getM3u8Download() {
        return m3u8Download;
    }

    public void setM3u8Download(M3u8DownLoader m3u8Download) {
        this.m3u8Download = m3u8Download;
    }

    public ExecutorService getFixedThreadPool() {
        return fixedThreadPool;
    }

    public void setFixedThreadPool(ExecutorService fixedThreadPool) {
        this.fixedThreadPool = fixedThreadPool;
    }

    public List<Integer> getHlsFinishedNumList() {
        return hlsFinishedNumList;
    }

    public void setHlsFinishedNum(int hlsFinishedNum) {
        this.hlsFinishedNumList.add(hlsFinishedNum);
    }

    public int getHlsFinishedCount() {
        return hlsFinishedCount;
    }

    public void setHlsFinishedCount(int hlsFinishedCount) {
        this.hlsFinishedCount = hlsFinishedCount;
    }
}

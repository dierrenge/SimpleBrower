package cn.cheng.simpleBrower.bean;

import android.app.Notification;

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

    private M3u8DownLoader m3u8Download;

    private ExecutorService fixedThreadPool; // 线程池

    private int bytesum; // 记录下载进度

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

    public int getBytesum() {
        return bytesum;
    }

    public void setBytesum(int bytesum) {
        this.bytesum = bytesum;
    }
}

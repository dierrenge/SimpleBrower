package cn.cheng.simpleBrower.bean;

import android.app.Notification;

import java.io.Serializable;

/**
 * 下载任务消息
 */
public class NotificationBean implements Serializable {
    private Notification notification;
    private String title;
    private String url;
    private String state; // 表示点击后呈现的下载状态

    private int bytesum; // 记录下载进度

    private int what; // 用于区分下载文件类别

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

    public int getWhat() {
        return what;
    }

    public void setWhat(int what) {
        this.what = what;
    }
}

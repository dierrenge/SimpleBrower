package cn.cheng.simpleBrower.bean;

import android.app.Notification;

/**
 * 下载任务消息
 */
public class NotificationBean {
    private Notification notification;
    private String title;
    private String url;
    private String state; // 表示点击后呈现的状态

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
}

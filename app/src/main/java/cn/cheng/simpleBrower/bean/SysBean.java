package cn.cheng.simpleBrower.bean;

public class SysBean {

    // 是否展示检查影音下载的提示框
    private boolean flagVideo;
    // 是否开启动图过滤
    private boolean flagGif;

    public boolean isFlagVideo() {
        return flagVideo;
    }

    public void setFlagVideo(boolean flagVideo) {
        this.flagVideo = flagVideo;
    }

    public boolean isFlagGif() {
        return flagGif;
    }

    public void setFlagGif(boolean flagGif) {
        this.flagGif = flagGif;
    }

    @Override
    public String toString() {
        return "SysBean{" +
                "flagVideo=" + flagVideo +
                ", flagGif=" + flagGif +
                '}';
    }
}

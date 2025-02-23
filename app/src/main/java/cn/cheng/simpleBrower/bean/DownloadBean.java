package cn.cheng.simpleBrower.bean;

/**
 * 下载对象
 */
public class DownloadBean {
    private int what;
    private String fileType;
    private String title;
    private String url;

    public int getWhat() {
        return what;
    }

    public void setWhat(int what) {
        this.what = what;
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

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
}

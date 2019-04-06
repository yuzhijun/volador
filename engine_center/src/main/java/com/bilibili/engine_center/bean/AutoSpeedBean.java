package com.bilibili.engine_center.bean;

public class AutoSpeedBean {
    private long defaultReportKey;//
    private String pageName;//页面名称
    private long apiLoadTime;//网络请求的时间
    private long pageStartupTime;//页面启动时间
    private long finalDrawTime;//第二次渲染时间

    public long getDefaultReportKey() {
        return defaultReportKey;
    }

    public void setDefaultReportKey(long defaultReportKey) {
        this.defaultReportKey = defaultReportKey;
    }

    public String getPageName() {
        return pageName;
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    public long getApiLoadTime() {
        return apiLoadTime;
    }

    public void setApiLoadTime(long apiLoadTime) {
        this.apiLoadTime = apiLoadTime;
    }

    public long getPageStartupTime() {
        return pageStartupTime;
    }

    public void setPageStartupTime(long pageStartupTime) {
        this.pageStartupTime = pageStartupTime;
    }

    public long getFinalDrawTime() {
        return finalDrawTime;
    }

    public void setFinalDrawTime(long finalDrawTime) {
        this.finalDrawTime = finalDrawTime;
    }
}

package com.bilibili.engine_center.bean;

import java.util.List;

public class ConfigModel {
    private String pageName;
    private String tag;
    private List<String> api;

    public String getPageName() {
        return pageName;
    }

    public String getTag() {
        return tag;
    }

    public List<String> getApi() {
        return api;
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void setApi(List<String> api) {
        this.api = api;
    }
}

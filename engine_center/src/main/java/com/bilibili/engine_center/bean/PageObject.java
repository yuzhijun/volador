package com.bilibili.engine_center.bean;

import android.os.SystemClock;
import android.util.ArrayMap;

import com.bilibili.engine_center.startup.AutoSpeed;
import com.bilibili.engine_center.util.BaseUtility;

public class PageObject {
    private ArrayMap<Integer, RequestStatus> apiStatusMap = new ArrayMap<>();//url->status
    private long pageCreateTime;//页面创建时间
    private long apiLoadStartTime;//请求开始时间
    private long apiLoadEndTime;//请求结束时间
    private long finalDrawEndTime;//页面绘制完成
    private long initialDrawEndTime;//初次渲染结束时间
    private long pageObjKey;//页面key
    private AutoSpeed.PageCallBack callBack;//页面首次绘制完成回调
    private ConfigModel configModel;
    private long defaultReportKey;

    enum RequestStatus{
        NONE,
        LOADING,
        LOADED
    }

    public PageObject(long pageObjKey, ConfigModel configModel, long defaultReportKey, AutoSpeed.PageCallBack callBack) {
        this.pageObjKey = pageObjKey;
        this.configModel = configModel;
        this.defaultReportKey = defaultReportKey;
        this.callBack = callBack;
    }

    public ConfigModel getConfigModel() {
        return configModel;
    }

    public void onCreate(){
        if(pageCreateTime > 0){
            return;
        }
        pageCreateTime = SystemClock.elapsedRealtime();
    }

    public void onPageDrawEnd(){
        if (initialDrawEndTime <= 0) {//初次渲染还没有完成
            initialDrawEndTime = SystemClock.elapsedRealtime();
            if (!AutoSpeed.getInstance().hasApiConfig() || allApiLoaded()) {//如果没有请求配置或者请求已完成，则没有二次渲染时间，即初次渲染时间即为页面整体时间，且可以上报结束页面了
                finalDrawEndTime = -1;
                reportIfNeed();
            }
            //页面初次展示，回调，用于统计冷启动结束
            this.callBack.onPageShow(this);
            return;
        }
        //如果二次渲染没有完成，且所有请求已经完成，则记录二次渲染时间并结束测速，上报数据
        if (finalDrawEndTime <= 0 && (!AutoSpeed.getInstance().hasApiConfig() || allApiLoaded())) {
            finalDrawEndTime = SystemClock.elapsedRealtime();
            reportIfNeed();
        }
    }

    //请求开始的时间记录
    public boolean onApiLoadStart(String url){
        String relUrl = BaseUtility.getRelativeUrl(url);
        if (!AutoSpeed.getInstance().hasApiConfig() || !AutoSpeed.getInstance().hasUrl(relUrl) || apiStatusMap.get(relUrl.hashCode()) != RequestStatus.NONE) {
            return false;
        }

        //改变Url的状态为执行中
        apiStatusMap.put(relUrl.hashCode(), RequestStatus.LOADING);
        //第一个请求开始时记录起始点
        if (apiLoadStartTime <= 0) {
            apiLoadStartTime = SystemClock.elapsedRealtime();
        }

        return true;
    }

    //请求结束的时间记录
    public boolean onApiLoadEnd(String url){
        String relUrl = BaseUtility.getRelativeUrl(url);
        if (!AutoSpeed.getInstance().hasApiConfig() || !AutoSpeed.getInstance().hasUrl(relUrl) || apiStatusMap.get(relUrl.hashCode()) != RequestStatus.LOADING) {
            return false;
        }
        //改变Url的状态为执行结束
        apiStatusMap.put(relUrl.hashCode(), RequestStatus.LOADED);
        //全部请求结束后记录时间
        if (apiLoadEndTime <= 0 && allApiLoaded()) {
            apiLoadEndTime = SystemClock.elapsedRealtime();
        }
        return true;
    }

    private boolean allApiLoaded(){
        if (!AutoSpeed.getInstance().hasApiConfig()) return true;
        int size = apiStatusMap.size();
        for (int i = 0; i < size; ++i) {
            if (apiStatusMap.valueAt(i) != RequestStatus.LOADED) {
                return false;
            }
        }
        return true;
    }

    //计算网络请求时间
    long getApiLoadTime() {
        if (!AutoSpeed.getInstance().hasApiConfig() || apiLoadEndTime <= 0 || apiLoadStartTime <= 0) {
            return -1;
        }
        return apiLoadEndTime - apiLoadStartTime;
    }

    //页面启动时间
    long getPageStartupTime() {
        if (!AutoSpeed.getInstance().hasApiConfig() || pageCreateTime <= 0 || finalDrawEndTime <= 0) {
            return -1;
        }
        return finalDrawEndTime - pageCreateTime;
    }

    public long getInitialDrawEndTime() {
        return initialDrawEndTime;
    }

    private void reportIfNeed(){
        //TODO
    }
}

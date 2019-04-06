package com.bilibili.engine_center.bean;

import android.os.SystemClock;
import android.util.SparseIntArray;

import com.bilibili.engine_center.startup.AutoSpeed;
import com.bilibili.engine_center.util.BaseUtility;

public class PageObject {
    private SparseIntArray apiStatusMap = new SparseIntArray();//url->status
    private long pageCreateTime;//页面创建时间
    private long apiLoadStartTime;//请求开始时间
    private long apiLoadEndTime;//请求结束时间
    private long finalDrawEndTime;//页面绘制完成
    private long initialDrawEndTime;//初次渲染结束时间
    private long pageObjKey;//页面key
    private AutoSpeed.PageCallBack callBack;//页面首次绘制完成回调
    private long selectedTime;//fragment切换的时间
    private long viewCreatedTime;//fragment onViewCreated的时间
    private long scrollToTime;//viewpager开始滚动时间
    private ConfigModel configModel;
    private long defaultReportKey;

    private static final Integer NONE = 0;
    private static final Integer LOADING = 1;
    private static final Integer LOADED = 2;

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

    public void onPageViewCreate(){
        if (viewCreatedTime > 0){
            return;
        }
        viewCreatedTime = SystemClock.elapsedRealtime();
    }

    public void onPageSelect(){
        if (selectedTime > 0){
            return;
        }
        selectedTime = SystemClock.elapsedRealtime();
    }

    public void onPageScrolled(){

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
        if (!AutoSpeed.getInstance().hasApiConfig()
                || !AutoSpeed.getInstance().hasUrl(relUrl)
                || apiStatusMap.get(relUrl.hashCode()) != NONE) {
            return false;
        }

        //改变Url的状态为执行中
        apiStatusMap.put(relUrl.hashCode(), LOADING);
        //第一个请求开始时记录起始点
        if (apiLoadStartTime <= 0) {
            apiLoadStartTime = SystemClock.elapsedRealtime();
        }

        return true;
    }

    //请求结束的时间记录
    public boolean onApiLoadEnd(String url){
        String relUrl = BaseUtility.getRelativeUrl(url);
        if (!AutoSpeed.getInstance().hasApiConfig()
                || !AutoSpeed.getInstance().hasUrl(relUrl)
                || apiStatusMap.get(relUrl.hashCode()) != LOADING) {
            return false;
        }
        //改变Url的状态为执行结束
        apiStatusMap.put(relUrl.hashCode(), LOADED);
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
            if (apiStatusMap.valueAt(i) != LOADED) {
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
        if (pageCreateTime <= 0) {
            return -1;
        }

        if (finalDrawEndTime > 0) {//有二次渲染时间
            long totalTime = finalDrawEndTime - pageCreateTime;
            //如果有等待时间，则减掉这段多余的时间
            if (selectedTime > 0 && selectedTime > viewCreatedTime && selectedTime < finalDrawEndTime) {
                totalTime -= (selectedTime - viewCreatedTime);
            }
            return totalTime;
        } else {//以初次渲染时间为整体时间
            return getInitialDrawEndTime();
        }
    }

    private long getFinalDrawTime() {
        if (finalDrawEndTime <= 0 || apiLoadEndTime <= 0) {
            return -1;
        }
        //延迟二次渲染，需要减去等待时间(apiLoadEnd->scrollToTime)
        if (scrollToTime > 0 && scrollToTime > apiLoadEndTime && scrollToTime <= finalDrawEndTime) {
            return finalDrawEndTime - apiLoadEndTime - (scrollToTime - apiLoadEndTime);
        } else {//正常二次渲染
            return finalDrawEndTime - apiLoadEndTime;
        }
    }

    public long getInitialDrawEndTime() {
        if (pageCreateTime <= 0 || initialDrawEndTime <= 0) {
            return -1;
        }
        if (scrollToTime > 0 && scrollToTime > viewCreatedTime && scrollToTime <= initialDrawEndTime) {//延迟初次渲染，需要减去等待的时间(viewCreated->changeToPage)
            return initialDrawEndTime - pageCreateTime - (scrollToTime - viewCreatedTime);
        } else {//正常初次渲染
            return initialDrawEndTime - pageCreateTime;
        }
    }

    private void reportIfNeed(){
        //TODO
        long apiLoadTime = getApiLoadTime();
        long pageStartupTime = getPageStartupTime();

    }
}

package com.bilibili.engine_center.startup;

import android.content.Context;
import android.util.ArrayMap;
import android.view.View;

import com.bilibili.engine_center.bean.ConfigModel;
import com.bilibili.engine_center.bean.PageObject;
import com.bilibili.engine_center.util.BaseUtility;
import com.bilibili.engine_center.util.Const;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;

public class AutoSpeed {

    private static AutoSpeed mInstance;
    private List<ConfigModel> mConfigModels; //startup配置
    private ArrayMap<Integer, PageObject> activePages = new ArrayMap<>();//活跃的页面
    private long coldStartTime;//冷启动开始时间
    private long coldStartTotalTime;//冷启动花费所有时间
    private Context application;

    public interface PageCallBack{
        void onPageShow(PageObject pageObject);
    }

    private AutoSpeed(){}

    public static AutoSpeed getInstance(){
        if (mInstance == null){
            synchronized (AutoSpeed.class){
                if (mInstance == null){
                    mInstance = new AutoSpeed();
                }
            }
        }
        return mInstance;
    }


    public void init(Context context, long coldStartTime){
        this.application = context.getApplicationContext();
        if (this.coldStartTime < 0){
            this.coldStartTime = coldStartTime;
        }

        loadStartupXmlConfiguration(context);
    }

    private void loadStartupXmlConfiguration(Context context) {
        if (BaseUtility.isStartupXMLExists(context)){
            try {
                StartupParser.parseStartupConfiguration(context);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }
        }
    }

    public void onPageCreate(Object page){
        Integer pageObjKey = BaseUtility.getPageObjKey(page);
        PageObject pageObject = activePages.get(pageObjKey);
        ConfigModel configModel = getConfigModel(page);//获取该页面的配置
        if (pageObject == null && configModel != null){//有配置且没有测速过则测速
            pageObject = new PageObject(pageObjKey, configModel, BaseUtility.getDefaultReportKey(page), new PageCallBack() {
                @Override
                public void onPageShow(PageObject pageObject) {
                    if (Const.TAG.equals(pageObject.getConfigModel().getTag())){
                        long initialDrawEndTime = pageObject.getInitialDrawEndTime();
                        if (coldStartTotalTime < 0){
                            coldStartTotalTime = initialDrawEndTime - coldStartTime;
                        }
                    }
                }
            });
            pageObject.onCreate();
            activePages.put(pageObjKey, pageObject);
        }
    }

    public View createPageView(Context context, View child){
        Integer pageObjKey = BaseUtility.getPageObjKey(context);
        return AutoSpeedFrameLayout.wrap(pageObjKey, child);
    }

    public void onPageDrawEnd(Integer pageObjectKey){
        PageObject pageObject = activePages.get(pageObjectKey);
        if (pageObject != null) {
            pageObject.onPageDrawEnd();
        }
    }

    public boolean onApiLoadStart(String url){
        PageObject pageObject = getPageObjectByUrl(url);
        if (pageObject != null){
            return pageObject.onApiLoadStart(url);
        }

        return false;
    }

    //请求结束的时间记录
    public boolean onApiLoadEnd(String url){
        PageObject pageObject = getPageObjectByUrl(url);
        if (pageObject != null){
            return pageObject.onApiLoadEnd(url);
        }
        return false;
    }

    private ConfigModel getConfigModel(Object object){
        if (null != object){
            String objFullName = object.getClass().getName();
            String objName = object.getClass().getSimpleName();
            if (hasApiConfig()){
                for (ConfigModel configModel : mConfigModels){
                    if (configModel.getPageName().equals(objFullName) || configModel.getPageName().equals(objName)){
                        return configModel;
                    }
                }
            }
        }
        return null;
    }

    public void setConfigModels(List<ConfigModel> configModels) {
        this.mConfigModels = configModels;
    }

    public boolean hasApiConfig(){
        return mConfigModels != null && mConfigModels.size() > 0;
    }

    public boolean hasUrl(String relUrl){
        if (hasApiConfig()){
            for (ConfigModel configModel : mConfigModels){
                for (String url : configModel.getApi()){
                    if (url.equals(relUrl)){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public PageObject getPageObjectByUrl(String relUrl){
        try{
            ConfigModel configModel = getConfigModelByUrl(relUrl);
            if (configModel == null) return null;
            for (PageObject pageObject : activePages.values()){
               ConfigModel cModel = pageObject.getConfigModel();
               if (cModel.getPageName().equals(configModel.getPageName())){
                   return pageObject;
               }
           }
        }catch (Exception e){
           return null;
        }
       return null;
    }

    private ConfigModel getConfigModelByUrl(String relUrl){
        if (hasApiConfig()){
            for (ConfigModel configModel : mConfigModels){
                for (String url : configModel.getApi()){
                    if (url.equals(relUrl)){
                        return configModel;
                    }
                }
            }
        }
        return null;
    }
}

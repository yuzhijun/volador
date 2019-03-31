package com.bilibili.engine_center.startup;

import android.content.Context;
import android.content.res.AssetManager;
import android.text.TextUtils;

import com.bilibili.engine_center.bean.ConfigModel;
import com.bilibili.engine_center.util.Const;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class StartupParser {
    private static final String PAGE = "page";
    private static final String API = "api";

    private static final String ID = "id";
    private static final String TAG = "tag";

    private static StartupParser parser;
    public static void parseStartupConfiguration(Context context) throws IOException, XmlPullParserException {
        if (parser == null) {
            parser = new StartupParser();
        }
        parser.usePullParse(context);
    }

    private void usePullParse(Context context) throws XmlPullParserException, IOException {
        List<ConfigModel> configModels = null;
        List<String> api = null;
        ConfigModel configModel = null;
        try{
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser xmlPullParser = factory.newPullParser();
            xmlPullParser.setInput(getConfigInputStream(context), "UTF-8");
            int eventType = xmlPullParser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String nodeName = xmlPullParser.getName();
                switch (eventType){
                    case XmlPullParser.START_DOCUMENT:
                        configModels = new ArrayList<>();
                        api = new ArrayList<>();
                        break;
                    case XmlPullParser.START_TAG:
                        if(PAGE.equalsIgnoreCase(nodeName)){
                            configModel = new ConfigModel();
                            String pageName = xmlPullParser.getAttributeValue("",ID);
                            String tag = xmlPullParser.getAttributeValue("",TAG);
                            if (!TextUtils.isEmpty(pageName) && !TextUtils.isEmpty(tag)){
                                configModel.setPageName(pageName);
                                configModel.setTag(tag);
                            }
                        }else if (API.equalsIgnoreCase(nodeName)){
                            String id = xmlPullParser.getAttributeValue("", ID);
                            if (!TextUtils.isEmpty(id)){
                                api.add(id);
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if (PAGE.equalsIgnoreCase(nodeName) && configModel != null){
                            configModel.setApi(api);
                            configModels.add(configModel);
                            configModel = null;
                        }
                        break;
                }
                eventType = xmlPullParser.next();
            }

            if (configModels != null && configModels.size() > 0){
                AutoSpeed.getInstance().setConfigModels(configModels);
            }
        }catch (XmlPullParserException e){
            throw new XmlPullParserException("配置文件startup.xml解析失败");
        }catch (IOException e){
            throw new IOException("读取文件失败");
        }
    }

    private InputStream getConfigInputStream(Context context) throws IOException {
        AssetManager assetManager = context.getAssets();
        String[] fileNames = assetManager.list("");
        if (fileNames != null && fileNames.length > 0) {
            for (String fileName : fileNames) {
                if (Const.CONFIGURATION_FILE_NAME.equalsIgnoreCase(fileName)) {
                    return assetManager.open(fileName, AssetManager.ACCESS_BUFFER);
                }
            }
        }
        throw new FileNotFoundException("文件未找到");
    }
}

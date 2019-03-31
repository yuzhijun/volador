package com.bilibili.engine_center.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;

import java.io.IOException;

public class BaseUtility {


    public static boolean isStartupXMLExists(Context context) {
        try {
            AssetManager assetManager = context.getAssets();
            String[] fileNames = assetManager.list("");
            if (fileNames != null && fileNames.length > 0) {
                for (String fileName : fileNames) {
                    if (Const.CONFIGURATION_FILE_NAME.equalsIgnoreCase(fileName)) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
        }
        return false;
    }

    public static Integer getPageObjKey(Object object){
        return object.hashCode();
    }

    public static Integer getDefaultReportKey(Object object){
        //TODO
        return 0;
    }

    public static String getRelativeUrl(String url){
       try{
           Uri uri = Uri.parse(url);
           if (uri != null){
               return uri.getPath();
           }
       }catch (Exception e){
           e.printStackTrace();
           return "";
       }
        return "";
    }
}

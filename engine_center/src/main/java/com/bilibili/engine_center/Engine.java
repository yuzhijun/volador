package com.bilibili.engine_center;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.util.ArrayMap;

import com.bilibili.engine_center.sniffer.SnifferTrack;
import com.bilibili.engine_center.startup.AutoSpeed;

import java.util.Map;

public class Engine {
    @SuppressLint("StaticFieldLeak")
    private static Engine sInstance;
    private Context mContext;

    private Engine(Context context){
        this.mContext = context;
    }

    public static Engine getInstance(Context context){
        if (null == sInstance){
            synchronized (Engine.class){
                if (null == sInstance){
                    sInstance = new Engine(context);
                }
            }
        }
        return sInstance;
    }

    private Map<Class, Object> mCachedModules = new ArrayMap<>();
    @SuppressWarnings("unchecked")
    public <T> T getModule(Class<T> clz) {
        Object module = mCachedModules.get(clz);
        if (module != null) {
            if (!clz.isInstance(module)) {
                throw new IllegalStateException(clz.getName() + " must be instance of " + String.valueOf(module));
            }
            return (T) module;
        }
        try {
            T createdModule;
            if (AutoSpeed.class.equals(clz)){
                createdModule = (T) AutoSpeed.getInstance();
            }else if(SnifferTrack.class.equals(clz)){
                createdModule = (T) SnifferTrack.getInstance();
            }else{
                createdModule = clz.newInstance();
            }
            mCachedModules.put(clz, createdModule);
            return createdModule;
        } catch (Throwable e) {
            throw new IllegalStateException("Can not create instance of " + clz.getName() + ", " + String.valueOf(e));
        }
    }
}

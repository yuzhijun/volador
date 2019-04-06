package com.bilibili.engine_center.sniffer;

import com.bilibili.engine_center.GeneratorSubject;

public class SnifferTrack extends GeneratorSubject {
    private static SnifferTrack mInstance;

    private SnifferTrack(){}

    public static SnifferTrack getInstance(){
        if (mInstance == null){
            synchronized (SnifferTrack.class){
                if (mInstance == null){
                    mInstance = new SnifferTrack();
                }
            }
        }
        return mInstance;
    }


}

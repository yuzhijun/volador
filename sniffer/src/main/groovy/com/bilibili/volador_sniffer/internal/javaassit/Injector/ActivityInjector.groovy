package com.bilibili.volador_sniffer.internal.javaassit.Injector

import com.android.build.api.transform.Context
import com.android.build.api.transform.TransformOutputProvider
import javassist.ClassPool
import javassist.CtClass

class ActivityInjector extends BaseInjector{

    @Override
    CtClass injectClass(ClassPool pool, String dir, TransformOutputProvider outputProvider, Context context) {

        return null
    }

    @Override
    CtClass injectJar(ClassPool pool, String dir, TransformOutputProvider outputProvider, Context context) {
        return null
    }
}
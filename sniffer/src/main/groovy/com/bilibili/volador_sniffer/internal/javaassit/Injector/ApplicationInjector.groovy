package com.bilibili.volador_sniffer.internal.javaassit.Injector

import com.android.build.api.transform.Context
import com.android.build.api.transform.TransformOutputProvider
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod

class ApplicationInjector extends BaseInjector{

    def private static hookApplication = ['android.app.Application']

    @Override
    CtClass injectClass(ClassPool pool, String dir, TransformOutputProvider outputProvider, Context context) {
        def ctCls
        try {
            ctCls = pool.makeClass(dir)
            def originSuperCls = ctCls.superclass

            if (originSuperCls.name in hookApplication){
                if (ctCls.isFrozen()){
                    ctCls.defrost()
                }

                CtMethod ctMethod =  ctCls.getDeclaredMethod("onCreate")
                ctMethod.insertBefore("${Constants.AUTO_SPEED_CLASSNAME}.getInstance().init(this,0);")

                return ctCls
            }

            return null
        }catch (Throwable t) {
            println "    [Warning] --> ${t.toString()}"
        } finally {
            if (ctCls != null) {
                ctCls.detach()
            }
        }
        return null
    }

    @Override
    CtClass injectJar(ClassPool pool, String dir, TransformOutputProvider outputProvider, Context context) {
        //doNothing
        return null
    }
}
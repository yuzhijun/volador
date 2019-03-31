package com.bilibili.volador_sniffer.internal.javaassit.Injector

import com.android.build.api.transform.Context
import com.android.build.api.transform.TransformOutputProvider
import com.bilibili.volador_sniffer.internal.javaassit.Const
import com.bilibili.volador_sniffer.internal.util.Logger
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod

class ApplicationInjector extends BaseInjector{

    def private static hookApplication = ['android.app.Application']

    @Override
    CtClass injectClass(ClassPool pool, String dir, TransformOutputProvider outputProvider, Context context) {
        def ctCls
        try {
            ctCls = pool.getCtClass(dir)
            def originSuperCls = ctCls.superclass
            if (originSuperCls != null && (originSuperCls.name in hookApplication)){
                Logger.info("||-->处理的文件为: ${dir}")
                if (ctCls.isFrozen()){
                    ctCls.defrost()
                }

                CtMethod ctMethod =  ctCls.getDeclaredMethod("onCreate")
                long time = 1L
                ctMethod.insertBefore("${Const.AUTO_SPEED_CLASSNAME}.getInstance().init(this,${time});")

                return ctCls
            }

            return null
        }catch (Exception t) {
            t.printStackTrace()
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
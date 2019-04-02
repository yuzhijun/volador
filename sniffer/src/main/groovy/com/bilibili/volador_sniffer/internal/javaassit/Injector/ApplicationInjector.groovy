package com.bilibili.volador_sniffer.internal.javaassit.Injector

import com.android.build.api.transform.Context
import com.android.build.api.transform.TransformOutputProvider
import com.bilibili.volador_sniffer.internal.javaassit.Const
import javassist.*

class ApplicationInjector extends BaseInjector{

    def private static hookApplication = ['android.app.Application']

    @Override
    CtClass injectClass(ClassPool pool, String dir, TransformOutputProvider outputProvider, Context context) {
        def ctCls
        try {
            ctCls = pool.getCtClass(dir)
            def originSuperCls = ctCls.superclass
            if (originSuperCls != null && (originSuperCls.name in hookApplication)){
                if (ctCls.isFrozen()){
                    ctCls.defrost()
                }

                CtField ctField = CtField.make("private long coldStartTime;", ctCls)
                ctCls.addField(ctField)

                CtConstructor[] constructors = ctCls.getConstructors()
                CtConstructor ctConstructor = constructors[0]
                ctConstructor.insertAfter("${ctField.name}=android.os.SystemClock.elapsedRealtime();")

                CtMethod ctMethod =  ctCls.getDeclaredMethod("onCreate")
                ctMethod.insertBefore("${Const.AUTO_SPEED_CLASSNAME}.getInstance().init(this,coldStartTime);")

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
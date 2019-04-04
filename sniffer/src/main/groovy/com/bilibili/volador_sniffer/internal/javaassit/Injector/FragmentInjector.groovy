package com.bilibili.volador_sniffer.internal.javaassit.Injector

import com.android.build.api.transform.Context
import com.android.build.api.transform.TransformOutputProvider
import com.bilibili.volador_sniffer.internal.javaassit.Const
import javassist.ClassPool
import javassist.CtClass
import javassist.CtField
import javassist.CtMethod

class FragmentInjector extends BaseInjector{
    def private static hookFragment = ['android.support.v4.app.Fragment']

    @Override
    CtClass injectClass(ClassPool pool, String dir, TransformOutputProvider outputProvider, Context context) {
        def ctCls
        try{
            ctCls = pool.getCtClass(dir)
            def originSuperCls = ctCls.superclass
            if (originSuperCls != null && (originSuperCls.name in hookFragment)){
                if (ctCls.isFrozen()){
                    ctCls.defrost()
                }

                CtMethod ctMethod =  ctCls.getDeclaredMethod("onCreate")
                ctMethod.insertBefore("${Const.AUTO_SPEED_CLASSNAME}.getInstance().onPageCreate(this);")

                CtField ctField = CtField.make("protected static boolean AUTO_SPEED_FRAGMENT_CREATE_VIEW_FLAG = true;", ctCls)
                ctCls.addField(ctField)

                CtMethod ctCreateViewMethod =  ctCls.getDeclaredMethod("onCreateView")
                ctCreateViewMethod.insertBefore("if(AUTO_SPEED_FRAGMENT_CREATE_VIEW_FLAG) {" +
                        "AUTO_SPEED_FRAGMENT_CREATE_VIEW_FLAG = false;" +
                        "android.view.View var4 = ${Const.AUTO_SPEED_CLASSNAME}.getInstance().createPageView(this, this.onCreateView(\$\$));" +
                        "AUTO_SPEED_FRAGMENT_CREATE_VIEW_FLAG = true;" +
                        "return var4;}")
                return ctCls
            }

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
        //do nothing
        return null
    }
}
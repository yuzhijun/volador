package com.bilibili.volador_sniffer.internal.javaassit.Injector

import com.android.build.api.transform.Context
import com.android.build.api.transform.TransformOutputProvider
import com.bilibili.volador_sniffer.internal.javaassit.Const
import javassist.*
import javassist.bytecode.AccessFlag

class FragmentInjector extends BaseInjector{
    def private static hookFragment = ['android.support.v4.app.Fragment']

    def private static hookViewPager = ['android.support.v4.view.ViewPager']
    def private static hookInnerItemInfo = ['android.support.v4.view.ViewPager$ItemInfo']
    def private static hookOnPageLoadListener = ['android.support.v4.view.ViewPager$OnPageChangeListener']

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
                ctCreateViewMethod.insertBefore("${Const.AUTO_SPEED_CLASSNAME}.getInstance().onPageViewCreate(this);")
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
        def ctCls
        try {
            ctCls = pool.getCtClass(dir)

            if (ctCls.name in hookViewPager){
                if (ctCls.isFrozen()){
                    ctCls.defrost()
                }

                ctCls.getConstructors().each { CtConstructor ctConstructor->
                    ctConstructor.insertAfter("this.addOnPageChangeListener(new ${Const.AUTO_SPEED_LAYZY_LOAD_LISTENER}(this.mItems));")
                }

                return ctCls
            }

            if (ctCls.name in hookInnerItemInfo){
                if (ctCls.isFrozen()){
                    ctCls.defrost()
                }

                ctCls.setModifiers(AccessFlag.setPublic(ctCls.getModifiers()))

                return ctCls
            }

            def originSuperInf = ctCls.interfaces
            if (originSuperInf != null){
                CtClass ctClass =  originSuperInf.find {
                    it.name in hookOnPageLoadListener
                }

                if (ctClass != null && ctCls.name == Const.AUTO_SPEED_LAYZY_LOAD_LISTENER){
                    CtField ctField = CtField.make("private java.util.List items;", ctCls)
                    ctCls.addField(ctField)

                    CtConstructor[] constructors = ctCls.getConstructors()
                    CtConstructor ctConstructor = constructors[0]
                    ctConstructor.insertAfter("{${ctField.name}=\$1;}")

                    CtMethod ctMethod = ctCls.getDeclaredMethod("onPageSelected")
                    CtMethod ctMethod1 = ctCls.getDeclaredMethod("onPageScrolled")
                    if (ctMethod != null && ctMethod1 != null){
                        ctMethod.insertAfter("if(items != null) {" +
                                "int var2 = items.size();" +
                                "for(int var3 = 0; var3 < var2; ++var3) {" +
                                "java.lang.Object var4 = items.get(var3);" +
                                "if(var4 instanceof android.support.v4.view.ViewPager.ItemInfo) {" +
                                "android.support.v4.view.ViewPager.ItemInfo var5 = (android.support.v4.view.ViewPager.ItemInfo)var4;" +
                                "if(var5.position == \$1 && var5.object instanceof android.support.v4.app.Fragment) {" +
                                "${Const.AUTO_SPEED_CLASSNAME}.getInstance().onPageSelect(var5.object);" +
                                " break;}}}}")

                        ctMethod1.insertAfter(" if(items != null) {" +
                                " int var4 = java.lang.Math.round(\$2);" +
                                " int var5 = \$2 != (float)0 && var4 != 1?(var4 == 0?\$1 + 1:-1):\$1;" +
                                " int var6 = items.size();" +
                                "for(int var7 = 0; var7 < var6; ++var7) {" +
                                " Object var8 = items.get(var7);" +
                                " if(var8 instanceof android.support.v4.view.ViewPager.ItemInfo) {" +
                                "android.support.v4.view.ViewPager.ItemInfo var9 = (android.support.v4.view.ViewPager.ItemInfo)var8;" +
                                " if(var9.position == var5 && var9.object instanceof android.support.v4.app.Fragment) {" +
                                "${Const.AUTO_SPEED_CLASSNAME}.getInstance().onPageScrolled(var9.object);" +
                                " break;}}}}")

                        return ctCls
                    }
                }
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
}
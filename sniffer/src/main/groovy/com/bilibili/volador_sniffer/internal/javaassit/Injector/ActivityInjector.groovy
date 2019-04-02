package com.bilibili.volador_sniffer.internal.javaassit.Injector

import com.android.build.api.transform.Context
import com.android.build.api.transform.TransformOutputProvider
import com.bilibili.volador_sniffer.internal.javaassit.Const
import javassist.*
import javassist.expr.ExprEditor
import javassist.expr.MethodCall

class ActivityInjector extends BaseInjector{

    def private static hookActivity = [
            'android.support.v4.app.FragmentActivity',
            'android.support.v7.app.AppCompatActivity'
    ]

    def private static hookSuperActivity = ['android.support.v4.app.FragmentActivity']
    def private static hookAppCompatActivity = ['android.support.v7.app.AppCompatActivity']

    @Override
    CtClass injectClass(ClassPool pool, String dir, TransformOutputProvider outputProvider, Context context) {
        //do nothing
        return null
    }

    @Override
    CtClass injectJar(ClassPool pool, String dir, TransformOutputProvider outputProvider, Context context) {
        def ctCls
        try{
            ctCls = pool.getCtClass(dir)
            if (ctCls != null && (ctCls.name in hookActivity)){
                if (ctCls.isFrozen()){
                    ctCls.defrost()
                }
                CtMethod ctMethod =  ctCls.getDeclaredMethod("onCreate")
                ctMethod.insertBefore("${Const.AUTO_SPEED_CLASSNAME}.getInstance().onPageCreate(this);")

                if (ctCls.name in hookSuperActivity){
                    CtMethod ctNewMethod = CtNewMethod.make("public void setContentView(android.view.View view){super.setContentView(view);}",
                            ctCls)
                    ctCls.addMethod(ctNewMethod)

                    CtMethod ctNewMethod2 = CtNewMethod.make("public void setContentView(int layoutResID){super.setContentView(layoutResID);}",ctCls)
                    ctCls.addMethod(ctNewMethod2)

                    ctCls.getDeclaredMethods().each { CtMethod outerMethod ->
                        outerMethod.instrument(new ExprEditor(){
                            @Override
                            void edit(MethodCall call) throws CannotCompileException {
                                if (call.isSuper() && call.getMethod().name == "setContentView" && call.getMethod().signature == "(Landroid/view/View;)V"){
                                    if (call.getMethod().getReturnType().getName() == 'void') {
                                        call.replace('{$1 = com.bilibili.engine_center.startup.AutoSpeed.getInstance().createPageView(this,$1); $proceed($$);}')
                                    }
                                }else if(call.isSuper() && call.getMethod().name == "setContentView" && call.getMethod().signature == "(I)V"){
                                    if (call.getMethod().getReturnType().getName() == 'void') {
                                        call.replace('{ android.view.View view=android.view.LayoutInflater.from(this).inflate($1, null);$1 = com.bilibili.engine_center.startup.AutoSpeed.getInstance().createPageView(this,view); $proceed($$);}')
                                    }
                                }
                            }
                        })
                    }
                }else if(ctCls.name in hookAppCompatActivity){
                    ctCls.getDeclaredMethods().each { CtMethod outerMethod ->
                        if (outerMethod.name == "setContentView"){
                            outerMethod.instrument(new ExprEditor(){
                                @Override
                                void edit(MethodCall call) throws CannotCompileException {
                                    if (call.getMethod().name == "setContentView" && call.getMethod().signature == "(Landroid/view/View;)V"){
                                        if (call.getMethod().getReturnType().getName() == 'void') {
                                            call.replace('{$1 = com.bilibili.engine_center.startup.AutoSpeed.getInstance().createPageView(this,$1); $proceed($$);}')
                                        }
                                    }else if(call.getMethod().name == "setContentView" && call.getMethod().signature == "(I)V"){
                                        if (call.getMethod().getReturnType().getName() == 'void') {
                                            call.replace('{ android.view.View view=android.view.LayoutInflater.from(this).inflate($1, null);$1 = com.bilibili.engine_center.startup.AutoSpeed.getInstance().createPageView(this,view); $proceed($$);}')
                                        }
                                    }
                                }
                            })
                        }
                    }
                }

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
}
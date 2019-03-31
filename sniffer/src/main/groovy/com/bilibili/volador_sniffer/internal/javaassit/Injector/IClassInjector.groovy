package com.bilibili.volador_sniffer.internal.javaassit.Injector

import com.android.build.api.transform.Context
import com.android.build.api.transform.TransformOutputProvider
import javassist.ClassPool
import org.gradle.api.Project

interface IClassInjector {

    /**
     * 设置project对象
     * @param project
     */
    void setProject(Project project)

    /**
     * 设置variant目录关键串
     * @param variantDir
     */
    void setVariantDir(String variantDir)
    /**
     * 注入器名称
     */
    def name()

    /**
     * 对 dir 目录中的 Class 进行注入
     */
    def injectClass(ClassPool pool, String dir, TransformOutputProvider outputProvider, Context context)
    /**
     * 对 jar文件进行class注入
     * */
    def injectJar(ClassPool pool, String dir, TransformOutputProvider outputProvider, Context context )
}

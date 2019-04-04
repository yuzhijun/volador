package com.bilibili.volador_sniffer

import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import com.bilibili.volador_sniffer.internal.cache.GlobalConfig
import com.bilibili.volador_sniffer.internal.procedure.*
import com.bilibili.volador_sniffer.internal.util.Logger
import com.google.common.collect.ImmutableSet
import org.gradle.api.Project

class SnifferStartupTransform extends Transform{

    SnifferStartupProcedure snifferProcedure

    SnifferStartupTransform(Project project){
        snifferProcedure = new SnifferStartupProcedure(project)
    }

    @Override
    String getName() {
        return "sniffer_startup"
    }
    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return ImmutableSet.of(QualifiedContent.DefaultContentType.CLASSES)
    }
    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        //是否支持增量编译
        return false
    }
    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        Logger.info("||-->开始插件处理")
        Project project = GlobalConfig.project

        if(transformInvocation.isIncremental() && isIncremental()){
            //TODO
        }else {
            snifferProcedure.with(new StartupTimeConsumeProcedure(project, transformInvocation))
        }

        snifferProcedure.with(new SnifferOnFinishedProcedure(project, transformInvocation))

        snifferProcedure.doWorkContinuously()
    }
}
package com.bilibili.volador_sniffer

import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import com.bilibili.volador_sniffer.internal.cache.GlobalConfig
import com.bilibili.volador_sniffer.internal.procedure.AutoSnifferEventProcedure
import com.bilibili.volador_sniffer.internal.procedure.AutoSnifferEventUpdateProcedure
import com.bilibili.volador_sniffer.internal.procedure.SnifferOnFinishedProcedure
import com.bilibili.volador_sniffer.internal.procedure.SnifferProcedure
import com.bilibili.volador_sniffer.internal.procedure.StartupTimeConsumeProcedure
import com.google.common.collect.ImmutableSet
import org.gradle.api.Project

class SnifferTransform extends Transform{

    SnifferProcedure snifferProcedure
    SnifferTransform (Project project){
        snifferProcedure = new SnifferProcedure(project)
    }

    @Override
    String getName() {
        return "sniffer"
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
        return true
    }
    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        Project project = GlobalConfig.project

        if(transformInvocation.isIncremental() && isIncremental()){
            snifferProcedure.with(new AutoSnifferEventUpdateProcedure(project, transformInvocation))
        }else {
            snifferProcedure.with(new AutoSnifferEventProcedure(project, transformInvocation))
            snifferProcedure.with(new StartupTimeConsumeProcedure(project, transformInvocation))
        }

        snifferProcedure.with(new SnifferOnFinishedProcedure(project, transformInvocation))

        snifferProcedure.doWorkContinuously()
    }
}
package com.bilibili.volador_sniffer.internal.procedure

import com.android.build.api.transform.TransformInvocation
import org.gradle.api.Project

abstract class AbsProcedure {
    List<? extends AbsProcedure> procedures = new ArrayList<>()
    Project project
    TransformInvocation transformInvocation

    AbsProcedure(Project project, TransformInvocation transformInvocation){
        this.project = project
        if (null != transformInvocation){
            this.transformInvocation = transformInvocation
        }
    }

    public <T extends AbsProcedure> AbsProcedure with(T procedure) {
        if (procedure != null) {
            procedures << procedure
        }

        return this
    }

    boolean doWorkContinuously() {
        for (AbsProcedure procedure : procedures) {
            if (!procedure.doWorkContinuously()) {
                break
            }
        }
        return true
    }
}
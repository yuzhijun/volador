package com.bilibili.volador_sniffer.internal.procedure

import com.android.build.api.transform.TransformInvocation
import org.gradle.api.Project

class SnifferOnFinishedProcedure extends  AbsProcedure{

    SnifferOnFinishedProcedure(Project project, TransformInvocation transformInvocation) {
        super(project, transformInvocation)
    }

    @Override
    boolean doWorkContinuously() {
        return super.doWorkContinuously()
    }
}
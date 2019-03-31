package com.bilibili.volador_sniffer.internal.procedure


import com.bilibili.volador_sniffer.internal.cache.GlobalConfig
import org.gradle.api.Project

class SnifferProcedure extends  AbsProcedure{
    GlobalConfig mGlobalConfig

    SnifferProcedure(Project project) {
        super(project, null)
        mGlobalConfig = new GlobalConfig(project)
    }
}
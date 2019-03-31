package com.bilibili.volador_sniffer

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.bilibili.volador_sniffer.internal.util.Logger
import com.bilibili.volador_sniffer.internal.util.TimeTrace
import org.gradle.api.Plugin
import org.gradle.api.Project

class SnifferPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.extensions.create("sniffer", SnifferExtension)

        Logger.setDebug(project.sniffer.isDebug)

        if (project.plugins.hasPlugin(AppPlugin)){
            //build time trace
            project.gradle.addListener(new TimeTrace())
            //使用Transform实行遍历
            AppExtension android = project.extensions.getByType(AppExtension)
            android.registerTransform(new SnifferTransform(project))
        }
    }
}

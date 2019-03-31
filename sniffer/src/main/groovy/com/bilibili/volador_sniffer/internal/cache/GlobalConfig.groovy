package com.bilibili.volador_sniffer.internal.cache

import com.bilibili.volador_sniffer.SnifferExtension
import com.bilibili.volador_sniffer.internal.util.Logger
import org.gradle.api.Project

class GlobalConfig {
    private static Project project
    private static HashSet<String> exclude
    private static HashSet<String> include

    GlobalConfig(Project project){
       GlobalConfig.@project = project
        project.afterEvaluate {
            Logger.info("开始解析用户配置项目")
            parseUserConfig()
        }
    }

    static Project getProject() {
        return project
    }

    static SnifferExtension getExtension() {
        return project.sniffer
    }

    static HashSet<String> getExclude() {
        return exclude
    }

    static HashSet<String> getInclude() {
        return include
    }

    static boolean getIsDebug() {
        return extension.isDebug
    }

    static boolean getIsOpenLogTrack() {
        return extension.isOpenLogTrack
    }

    private static void parseUserConfig() {
        // 需要手动添加的包
        List<String> includePackages = extension.include
        HashSet<String> include = new HashSet<>()
        if (includePackages != null) {
            include.addAll(includePackages)
        }
        GlobalConfig.@include = include

        // 添加需要过滤的包
        List<String> excludePackages = extension.exclude
        HashSet<String> exclude = new HashSet<>()
        // 不对系统类进行操作，避免非预期错误
        exclude.add('android.support')
        exclude.add('androidx')
        if (excludePackages != null) {
            exclude.addAll(excludePackages)
        }
        GlobalConfig.@exclude = exclude
    }
}

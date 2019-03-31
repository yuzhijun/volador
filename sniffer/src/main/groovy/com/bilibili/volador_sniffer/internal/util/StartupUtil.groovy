package com.bilibili.volador_sniffer.internal.util

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.TransformInput
import org.gradle.api.Project

public class StartupUtil {

    /** 生成 ClassPool 使用的 ClassPath 集合，同时将要处理的 jar 写入 includeJars */
    def
    static getClassPaths(Project project ,Collection<TransformInput> inputs) {
        def classpathList = []

        // 原始项目中引用的 classpathList
        getProjectClassPath(project, inputs).each {
            classpathList.add(it)
        }

        classpathList
    }

    /** 获取原始项目中的 ClassPath */
    def private static getProjectClassPath(Project project,
                                           Collection<TransformInput> inputs) {
        def classPath = []

        inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput directoryInput ->
                def dir = directoryInput.file.absolutePath
                Logger.info("遍历目录${dir.toString()}")
                classPath << dir
            }
            input.jarInputs.each { JarInput jarInput ->
                File jarFile = jarInput.file
                Logger.info("遍历jar目录${jarFile.absolutePath.toString()}")
                classPath << jarFile.absolutePath
            }
        }
        return classPath
    }
}

package com.bilibili.volador_sniffer.internal.procedure

import com.android.build.api.transform.*
import com.android.utils.FileUtils
import com.bilibili.volador_sniffer.internal.asm.SnifferEventModify
import com.bilibili.volador_sniffer.internal.concurrent.BatchTaskScheduler
import com.bilibili.volador_sniffer.internal.concurrent.ITask
import com.bilibili.volador_sniffer.internal.util.Logger
import groovy.io.FileType
import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.Project

class AutoSnifferEventProcedure extends  AbsProcedure{

    AutoSnifferEventProcedure(Project project, TransformInvocation transformInvocation) {
        super(project, transformInvocation)
    }

    @Override
    boolean doWorkContinuously() {
        project.logger.debug("begin to sniffer event")

        BatchTaskScheduler taskScheduler = new BatchTaskScheduler()

        transformInvocation.getOutputProvider().deleteAll()

        transformInvocation.inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput dirInput->
                taskScheduler.addTask(new ITask(){
                    @Override
                    Object call() throws Exception {
                        File dest = transformInvocation.getOutputProvider().getContentLocation(dirInput.name, dirInput.contentTypes, dirInput.scopes, Format.DIRECTORY)
                        Logger.info("||-->开始遍历特定目录  ${dest.absolutePath}")
                        File dir = dirInput.file
                        if (dir) {
                            HashMap<String, File> modifyMap = new HashMap<>()
                            dir.traverse(type: FileType.FILES, nameFilter: ~/.*\.class/) {
                                File classFile ->
                                    File modified = SnifferEventModify.modifyClassFile(dir, classFile, transformInvocation.context.getTemporaryDir())
                                    if (modified != null) {
                                        //key为相对路径
                                        modifyMap.put(classFile.absolutePath.replace(dir.absolutePath, ""), modified)
                                    }
                            }
                            FileUtils.copyDirectory(dirInput.file, dest)
                            modifyMap.entrySet().each {
                                Map.Entry<String, File> en ->
                                    File target = new File(dest.absolutePath + en.getKey())
                                    Logger.info(target.getAbsolutePath())
                                    if (target.exists()) {
                                        target.delete()
                                    }
                                    FileUtils.copyFile(en.getValue(), target)
                                    en.getValue().delete()
                            }
                        }
                        Logger.info("||-->结束遍历特定目录  ${dest.absolutePath}")
                        return null
                    }
                })
            }

            input.jarInputs.each { JarInput jarInput ->
                    taskScheduler.addTask(new ITask(){
                        @Override
                        Object call() throws Exception {
                            String destName = jarInput.file.name
                            /** 截取文件路径的md5值重命名输出文件,因为可能同名,会覆盖*/
                            def hexName = DigestUtils.md5Hex(jarInput.file.absolutePath).substring(0, 8)
                            if (destName.endsWith(".jar")) {
                                destName = destName.substring(0, destName.length() - 4)
                            }
                            /** 获得输出文件*/
                            File dest = transformInvocation.getOutputProvider().getContentLocation(destName + "_" + hexName, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                            Logger.info("||-->开始遍历特定jar ${dest.absolutePath}")
                            def modifiedJar = SnifferEventModify.modifyJarFile(jarInput.file, transformInvocation.context.getTemporaryDir())
                            Logger.info("||-->结束遍历特定jar ${dest.absolutePath}")
                            if (modifiedJar == null) {
                                modifiedJar = jarInput.file
                            }
                            FileUtils.copyFile(modifiedJar, dest)
                            return null
                        }
                    })
            }
        }

        taskScheduler.execute()
    }
}
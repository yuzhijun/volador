package com.bilibili.volador_sniffer.internal.procedure

import com.android.SdkConstants
import com.android.build.api.transform.*
import com.android.utils.FileUtils
import com.bilibili.volador_sniffer.internal.asm.SnifferEventModify
import com.bilibili.volador_sniffer.internal.concurrent.BatchTaskScheduler
import com.bilibili.volador_sniffer.internal.concurrent.ITask
import com.bilibili.volador_sniffer.internal.util.Logger
import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.Project

class AutoSnifferEventUpdateProcedure extends  AbsProcedure{

    AutoSnifferEventUpdateProcedure(Project project, TransformInvocation transformInvocation) {
        super(project, transformInvocation)
    }

    @Override
    boolean doWorkContinuously() {
        project.logger.debug("begin to sniffer update event")

        BatchTaskScheduler taskScheduler = new BatchTaskScheduler()

        transformInvocation.inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput dirInput->
                taskScheduler.addTask(new ITask(){
                    @Override
                    Object call() throws Exception {
                        File dest = transformInvocation.getOutputProvider().getContentLocation(dirInput.name, dirInput.contentTypes, dirInput.scopes, Format.DIRECTORY)
                        HashMap<String, File> modifyMap = new HashMap<>()
                        dirInput.changedFiles.each { File file, Status status ->
                            if (file.getName().endsWith(SdkConstants.DOT_CLASS)) {
                                switch (status) {
                                    case Status.NOTCHANGED:
                                        //doNothing
                                        break
                                    case Status.REMOVED:
                                        String removeFileName = FileUtils.relativePossiblyNonExistingPath(file, dirInput.file)
                                        File removeFile = new File(dest, removeFileName)
                                        if (removeFile.exists()) {
                                            if (removeFile.isDirectory()) {
                                                FileUtils.deletePath(removeFile)
                                            } else {
                                                FileUtils.deleteIfExists(removeFile)
                                            }
                                        }
                                        break
                                    case Status.CHANGED:
                                    case Status.ADDED:
                                        File modified = SnifferEventModify.modifyClassFile(dirInput.file, file, transformInvocation.context.getTemporaryDir())
                                        if (modified != null) {
                                            //key为相对路径
                                            modifyMap.put(file.absolutePath.replace(dirInput.file.absolutePath, ""), modified)
                                        }
                                        break
                                    default:
                                        break
                                }
                            }
                        }

                        if (modifyMap != null){
                            modifyMap.entrySet().each {
                                Map.Entry<String, File> en ->
                                    File target = new File(dest.absolutePath + en.getKey())
                                    if (target.exists()) {
                                        target.delete()
                                    }
                                    FileUtils.copyFile(en.getValue(), target)
                                    en.getValue().delete()
                            }
                        }
                        return null
                    }
                })
            }

            input.jarInputs.each { JarInput jarInput ->
                String destName = jarInput.file.name
                /** 截取文件路径的md5值重命名输出文件,因为可能同名,会覆盖*/
                def hexName = DigestUtils.md5Hex(jarInput.file.absolutePath).substring(0, 8)
                if (destName.endsWith(".jar")) {
                    destName = destName.substring(0, destName.length() - 4)
                }
                /** 获得输出文件*/
                File dest = transformInvocation.getOutputProvider().getContentLocation(destName + "_" + hexName, jarInput.contentTypes, jarInput.scopes, Format.JAR)
               if (jarInput.status == Status.ADDED || jarInput.status == Status.CHANGED){
                   taskScheduler.addTask(new ITask(){
                       @Override
                       Object call() throws Exception {
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
               }else if(jarInput.status != Status.REMOVED){
                   FileUtils.deleteIfExists(dest)
               }
            }
        }

        taskScheduler.execute()
    }
}
package com.bilibili.volador_sniffer.internal.procedure

import com.android.SdkConstants
import com.android.build.api.transform.*
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.internal.TaskManager
import com.android.utils.FileUtils
import com.bilibili.volador_sniffer.internal.javaassit.Injector.IClassInjector
import com.bilibili.volador_sniffer.internal.javaassit.Injector.Injectors
import com.bilibili.volador_sniffer.internal.util.AutoTextUtil
import com.bilibili.volador_sniffer.internal.util.Logger
import com.bilibili.volador_sniffer.internal.util.StartupUtil
import groovy.io.FileType
import javassist.ClassPool
import javassist.CtClass
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.IOUtils
import org.gradle.api.Project

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

class StartupTimeConsumeProcedure extends  AbsProcedure{
    private def globalScope

    StartupTimeConsumeProcedure(Project project, TransformInvocation transformInvocation) {
        super(project, transformInvocation)
        def appPlugin = project.plugins.getPlugin(AppPlugin)
        // taskManager 在 2.1.3 中为 protected 访问类型的，在之后的版本为 private 访问类型的，
        // 使用反射访问
        TaskManager taskManager = BasePlugin.metaClass.getProperty(appPlugin, "taskManager")
        this.globalScope = taskManager.globalScope
    }

    @Override
    boolean doWorkContinuously() {
        Logger.info("||-->开始处理页面启动时间")
        def injectors = includedInjectors()
        if (injectors.isEmpty()){
            copyResult(transformInvocation.inputs, transformInvocation.outputProvider) // 跳过
        }else{
            doTransform(transformInvocation.inputs, transformInvocation.outputProvider, transformInvocation.context) // 执行 reclass
        }
    }

    /**
     * ###################预留############
     * 返回用户未忽略的注入器的集合
     */
    def includedInjectors() {
        def injectors = []
        Injectors.values().each {
            it.injector.setProject(project)
            injectors << it.nickName
        }
        return injectors
    }

    /**
     * 拷贝处理结果
     */
    def copyResult(def inputs, def outputs) {
        inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput dirInput ->
                copyDir(outputs, dirInput)
            }
            input.jarInputs.each { JarInput jarInput ->
                copyJar(outputs, jarInput)
            }
        }
    }

    /**
     * 执行 Transform
     */
    def doTransform(Collection<TransformInput> inputs,
                    TransformOutputProvider outputProvider, Context context) {
//        BatchTaskScheduler taskScheduler = new BatchTaskScheduler()
//        taskScheduler.addTask(new ITask(){
//            @Override
//            Object call() throws Exception {
                /* 初始化 ClassPool */
                Object pool = initClassPool(inputs)
                Logger.info("||-->初始化classpool结束")
                doInject(inputs, pool, outputProvider, context)
//                return null
//            }
//        })
    }

    /**
     * 执行注入操作
     */
    static def doInject(Collection<TransformInput> inputs, ClassPool pool,
                         TransformOutputProvider outputProvider, Context context) {
        try {
            inputs.each { TransformInput input ->
                input.directoryInputs.each { DirectoryInput dirInput ->
                        handleDir(pool, dirInput, outputProvider, context)
                }
                input.jarInputs.each { JarInput jarInput ->
                    String destName = jarInput.file.name
                    /** 截取文件路径的md5值重命名输出文件,因为可能同名,会覆盖*/
                    def hexName = DigestUtils.md5Hex(jarInput.file.absolutePath).substring(0, 8)
                    if (destName.endsWith(".jar")) {
                        destName = destName.substring(0, destName.length() - 4)
                    }
                    /** 获得输出文件*/
                    File dest = outputProvider.getContentLocation(destName + "_" + hexName, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                    File modifiedJar = null
                    File inputFile = null
                    Injectors.values().each {
                        inputFile = modifiedJar == null ? jarInput.file : modifiedJar
                        modifiedJar = handleJar(pool, inputFile, it.injector, outputProvider, context)
                        if (modifiedJar == null) {
                            modifiedJar = jarInput.file
                        }
                    }
                    Logger.info("||-->处理完要拷贝的jar${modifiedJar.absolutePath}: ${modifiedJar.name}")
                    FileUtils.copyFile(modifiedJar, dest)
                }
            }
        } catch (Throwable t) {
            println t.toString()
        }
    }

    /**
     * 初始化 ClassPool
     */
    def initClassPool(Collection<TransformInput> inputs) {
        Logger.info("||-->初始化classpool开始")
        def pool = new ClassPool(true)

        // 添加编译时需要引用的到类到 ClassPool, 同时记录要修改的 jar 到 includeJars
        StartupUtil.getClassPaths(project, inputs, globalScope).each {
            Logger.info("||-->开始添加路径到classpool${it}")
            pool.insertClassPath(it)
        }
        pool
    }

    /**
     * 处理 jar
     */
    static def handleJar(ClassPool pool, File jarFile, IClassInjector injector, TransformOutputProvider outputProvider, Context context) {
        JarFile jar = new JarFile(jarFile)
        def hexName = DigestUtils.md5Hex(jarFile.absolutePath).substring(0, 8)
        def outputJar = new File(context.temporaryDir, hexName + jarFile.name)
        Logger.info("||-->jar包中的文件: ${outputJar.absolutePath}")
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(outputJar))
        Enumeration<JarEntry> jarEntries = jar.entries()
        while (jarEntries.hasMoreElements()) {
            JarEntry jarEntry = jarEntries.nextElement()
            InputStream inputStream = jar.getInputStream(jarEntry)
            byte[] sourceClassBytes = IOUtils.toByteArray(inputStream)

            String entryName = jarEntry.getName()
            ZipEntry zipEntry = new ZipEntry(entryName)
            jarOutputStream.putNextEntry(zipEntry)

            CtClass ctCls = null
            if (entryName.endsWith(SdkConstants.DOT_CLASS)) {
                Logger.info("||-->jar包中的文件: ${entryName}")
                String className = entryName.replace("/", ".").replace(".class", "")
                try {
                    Logger.info("||-->jar包中要处理的文件: ${className}")
                    ctCls= injector.injectJar(pool, className, outputProvider, context)
                } catch (Exception e) {
                    //ignore.
                    e.printStackTrace()
                }
            }

            if (ctCls == null) {
                jarOutputStream.write(sourceClassBytes)
            } else {
                jarOutputStream.write(ctCls.toBytecode())
            }

            jarOutputStream.closeEntry()
        }

        jarOutputStream.close()
        jar.close()

        return outputJar
    }

    /**
     * 处理目录中的 class 文件
     */
    static def handleDir(ClassPool pool, DirectoryInput input, TransformOutputProvider outputProvider, Context context) {
        FileOutputStream outputStream = null
        CtClass ctCls = null
        File dest = outputProvider.getContentLocation(input.name, input.contentTypes, input.scopes, Format.DIRECTORY)
        def dir = input.file
        FileUtils.copyDirectory(dir, dest)
            if (dir){
                dir.traverse (type: FileType.FILES, nameFilter: ~/.*\.class/){
                    File classFile ->
                        File modified = null
                        Injectors.values().each {//由於inject只会操作不同的文件,所以这里不会出现操作被覆盖问题
                            try{
                                String className = AutoTextUtil.path2ClassName(classFile.absolutePath.replace(dir.absolutePath + File.separator, ""))
                                ctCls = it.injector.injectClass(pool, className, outputProvider, context)
                                if (ctCls == null){
                                    //do nothing
                                }else{
                                    modified = new File(context.temporaryDir, className.replace('.', '') + '.class')
                                    if (modified.exists()) {
                                        modified.delete()
                                    }
                                    modified.createNewFile()
                                    outputStream = new FileOutputStream(modified)
                                    outputStream.write(ctCls.toBytecode())
                                }
                            }catch(Exception e){
                                e.printStackTrace()
                            }finally{
                                try {
                                    if (outputStream != null) {
                                        outputStream.close()
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace()
                                }
                            }

                            if (ctCls != null){
                                String fullClassName = classFile.absolutePath.replace(dir.absolutePath, "")
                                File target = new File(dest.absolutePath + fullClassName)
                                if (target.exists()) {
                                    target.delete()
                                }
                                if(modified != null){
                                    FileUtils.copyFile(modified, target)
                                }
                            }
                        }
                }
            }
    }

    /**
     * 拷贝目录
     */
    def copyDir(TransformOutputProvider output, DirectoryInput input) {
        File dest = output.getContentLocation(input.name, input.contentTypes, input.scopes, Format.DIRECTORY)
        FileUtils.copyDirectory(input.file, dest)
    }

    /**
     * 拷贝 Jar
     */
    def copyJar(TransformOutputProvider output, JarInput input) {
        File jar = input.file
        String destName = input.name
        def hexName = DigestUtils.md5Hex(jar.absolutePath)
        if (destName.endsWith('.jar')) {
            destName = destName.substring(0, destName.length() - 4)
        }
        File dest = output.getContentLocation(destName + '_' + hexName, input.contentTypes, input.scopes, Format.JAR)
        FileUtils.copyFile(jar, dest)
    }
}
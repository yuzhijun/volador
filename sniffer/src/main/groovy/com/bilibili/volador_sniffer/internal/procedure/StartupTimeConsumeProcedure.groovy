package com.bilibili.volador_sniffer.internal.procedure


import com.android.build.api.transform.*
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.internal.TaskManager
import com.android.utils.FileUtils
import com.bilibili.volador_sniffer.internal.javaassit.Injector.Injectors
import com.bilibili.volador_sniffer.internal.util.Logger
import com.bilibili.volador_sniffer.internal.util.StartupUtil
import javassist.ClassPool
import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.Project

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
           /* 初始化 ClassPool */
           Object pool = initClassPool(inputs)

           doInject(inputs, pool, outputProvider, context)
    }

    /**
     * 执行注入操作
     */
    static def doInject(Collection<TransformInput> inputs, ClassPool pool,
                         TransformOutputProvider outputProvider, Context context) {
        try {
            inputs.each { TransformInput input ->
                input.directoryInputs.each { DirectoryInput dirInput ->
                        StartupUtil.handleDir(pool, dirInput, outputProvider, context)
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
                        modifiedJar = StartupUtil.handleJar(pool, inputFile, it.injector, outputProvider, context)
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
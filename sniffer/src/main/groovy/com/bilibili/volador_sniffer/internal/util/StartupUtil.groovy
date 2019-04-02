package com.bilibili.volador_sniffer.internal.util

import com.android.SdkConstants
import com.android.build.api.transform.Context
import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.internal.scope.GlobalScope
import com.android.sdklib.IAndroidTarget
import com.android.utils.FileUtils
import com.bilibili.volador_sniffer.internal.javaassit.Injector.IClassInjector
import com.bilibili.volador_sniffer.internal.javaassit.Injector.Injectors
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

public class StartupUtil {

    /** 生成 ClassPool 使用的 ClassPath 集合，同时将要处理的 jar 写入 includeJars */
    def
    static getClassPaths(Project project, Collection<TransformInput> inputs, GlobalScope scope) {
        def classpathList = []

        // android.jar
        classpathList.add(getAndroidJarPath(scope))

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
                classPath << dir
            }
            input.jarInputs.each { JarInput jarInput ->
                File jarFile = jarInput.file
                classPath << jarFile.absolutePath
            }
        }
        return classPath
    }

    /**
     * 编译环境中 android.jar 的路径
     */
    def static getAndroidJarPath(GlobalScope globalScope) {
        return globalScope.getAndroidBuilder().getTarget().getPath(IAndroidTarget.ANDROID_JAR)
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
     * 处理 jar
     */
    static def handleJar(ClassPool pool, File jarFile, IClassInjector injector, TransformOutputProvider outputProvider, Context context) {
        JarFile jar = new JarFile(jarFile)
        def hexName = DigestUtils.md5Hex(jarFile.absolutePath).substring(0, 8)
        def outputJar = new File(context.temporaryDir, hexName + jarFile.name)
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
                String className = entryName.replace("/", ".").replace(".class", "")
                try {
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
}

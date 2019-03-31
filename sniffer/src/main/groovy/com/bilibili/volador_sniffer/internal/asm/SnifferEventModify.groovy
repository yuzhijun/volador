package com.bilibili.volador_sniffer.internal.asm

import com.bilibili.volador_sniffer.internal.util.AutoMatchUtil
import com.bilibili.volador_sniffer.internal.util.AutoTextUtil
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.IOUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

class SnifferEventModify{

    static File modifyJarFile(File jarFile, File tempDir) {
        if (jarFile) {
            return modifyJar(jarFile, tempDir, true)
        }
        return null
    }

    static File modifyClassFile(File dir, File classFile, File tempDir) {
        File modified = null
        FileOutputStream outputStream = null
        try {
            String className = AutoTextUtil.path2ClassName(classFile.absolutePath.replace(dir.absolutePath + File.separator, ""))
//            Logger.info("File:className:" + className)
            if (AutoMatchUtil.isShouldModifyClass(className)) {
                byte[] sourceClassBytes = IOUtils.toByteArray(new FileInputStream(classFile))
                byte[] modifiedClassBytes = modifyClasses(className, sourceClassBytes)
                if (modifiedClassBytes) {
                    modified = new File(tempDir, className.replace('.', '') + '.class')
                    if (modified.exists()) {
                        modified.delete()
                    }
                    modified.createNewFile()
                    outputStream = new FileOutputStream(modified)
                    outputStream.write(modifiedClassBytes)
                }
            } else {
                return classFile
            }
        } catch (Exception e) {
            e.printStackTrace()
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close()
                }
            } catch (Exception e) {
            }
        }
        return modified
    }

    private static byte[] modifyClasses(String className, byte[] srcByteCode) {
        byte[] classBytesCode = null
        try {
            classBytesCode = modifyClass(srcByteCode)
            return classBytesCode
        } catch (Exception e) {
            e.printStackTrace()
        }
        if (classBytesCode == null) {
            classBytesCode = srcByteCode
        }
        return classBytesCode
    }

    /**
     * 真正修改类中方法字节码
     */
    private static byte[] modifyClass(byte[] srcClass) throws IOException {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS)
        ClassVisitor adapter = new SnifferEventVisitor(classWriter)
        ClassReader cr = new ClassReader(srcClass)
        cr.accept(adapter, ClassReader.EXPAND_FRAMES)
        return classWriter.toByteArray()
    }

    private static File modifyJar(File jarFile, File tempDir, boolean nameHex) {
        def file = new JarFile(jarFile)
        def hexName = ""
        if (nameHex) {
            hexName = DigestUtils.md5Hex(jarFile.absolutePath).substring(0, 8)
        }
        def outputJar = new File(tempDir, hexName + jarFile.name)
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(outputJar))
        Enumeration enumeration = file.entries()

        while (enumeration.hasMoreElements()) {
            JarEntry jarEntry = (JarEntry) enumeration.nextElement()
            InputStream inputStream = file.getInputStream(jarEntry)

            String entryName = jarEntry.getName()
            String className

            ZipEntry zipEntry = new ZipEntry(entryName)
            jarOutputStream.putNextEntry(zipEntry)

            byte[] modifiedClassBytes = null
            byte[] sourceClassBytes = IOUtils.toByteArray(inputStream)
            if (entryName.endsWith(".class")) {
                className = entryName.replace("/", ".").replace(".class", "")
                if (AutoMatchUtil.isShouldModifyClass(className)) {
                    modifiedClassBytes = modifyClasses(className, sourceClassBytes)
                }
            }
            if (modifiedClassBytes == null) {
                jarOutputStream.write(sourceClassBytes)
            } else {
                jarOutputStream.write(modifiedClassBytes)
            }
            jarOutputStream.closeEntry()
        }
        jarOutputStream.close()
        file.close()
        return outputJar
    }
}
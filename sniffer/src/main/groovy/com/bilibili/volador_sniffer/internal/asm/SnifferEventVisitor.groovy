package com.bilibili.volador_sniffer.internal.asm

import com.bilibili.volador_sniffer.internal.bean.SnifferHookConfig
import com.bilibili.volador_sniffer.internal.bean.SnifferMethodCell
import com.bilibili.volador_sniffer.internal.cache.GlobalConfig
import com.bilibili.volador_sniffer.internal.util.Logger
import com.bilibili.volador_sniffer.internal.util.SnifferAnalyticsUtil
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class SnifferEventVisitor extends ClassVisitor{
    private HashSet<String> visitedFragMethods = new HashSet<>()
    private ClassVisitor classVisitor
    private String mClassName
    private String mSuperName
    private String[] mInterfaces

    SnifferEventVisitor(final ClassVisitor classVisitor) {
        super(Opcodes.ASM6, classVisitor)
        this.classVisitor = classVisitor
    }

    @Override
    void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.mClassName = name
        this.mSuperName = superName
        this.mInterfaces = interfaces

        // 打印调试信息
        Logger.info("\n||---开始扫描类：${mClassName}")
        Logger.info("||---类详情：version=${version};\taccess=${Logger.accCode2String(access)};\tname=${name};\tsignature=${signature};\tsuperName=${superName};\tinterfaces=${interfaces.toArrayString()}")

        super.visit(version, access, name, signature, superName, interfaces)
    }

    @Override
    void visitInnerClass(String name, String outerName, String innerName, int access) {
        super.visitInnerClass(name, outerName, innerName, access)
    }

    @Override
    MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions)
        MethodVisitor adapter = null
        //采集日志埋点
        if (GlobalConfig.isOpenLogTrack){
            MethodVisitor androidMethodVisitor = new AndroidMethodVisitor(methodVisitor, access, name, desc, mSuperName, mClassName, mInterfaces, visitedFragMethods)
            adapter = androidMethodVisitor
        }

        if (adapter != null) {
            return adapter
        }
        return methodVisitor
    }

    @Override
    void visitEnd() {
        if (GlobalConfig.isOpenLogTrack && SnifferAnalyticsUtil.isInstanceOfFragment(mSuperName)) {
            MethodVisitor mv
            // 添加剩下的方法，确保super.onHiddenChanged(hidden);等先被调用
            Iterator<Map.Entry<String, SnifferMethodCell>> iterator = SnifferHookConfig.sFragmentMethods.entrySet().iterator()
//            Logger.info("visitedFragMethods:" + visitedFragMethods)
            while (iterator.hasNext()) {
                Map.Entry<String, SnifferMethodCell> entry = iterator.next()
                String key = entry.getKey()
                SnifferMethodCell methodCell = entry.getValue()
                if (visitedFragMethods.contains(key)) {
                    continue
                }
                Logger.info("||Hooked class:injected method:" + methodCell.agentName)
                mv = classVisitor.visitMethod(Opcodes.ACC_PUBLIC, methodCell.name, methodCell.desc, null, null)
                mv.visitCode()
                // call super
                SnifferAnalyticsUtil.visitMethodWithLoadedParams(mv, Opcodes.INVOKESPECIAL, mSuperName, methodCell.name, methodCell.desc, methodCell.paramsStart, methodCell.paramsCount, methodCell.opcodes)
                // call injected method
                SnifferAnalyticsUtil.visitMethodWithLoadedParams(mv, Opcodes.INVOKESTATIC, SnifferHookConfig.LOG_ANALYTICS_BASE, methodCell.agentName,
                        methodCell.agentDesc, methodCell.paramsStart, methodCell.paramsCount, methodCell.opcodes)
                mv.visitInsn(Opcodes.RETURN)
                mv.visitMaxs(methodCell.paramsCount, methodCell.paramsCount)
                mv.visitEnd()
                mv.visitAnnotation("Lcom/bilibili/engine_center/sniffer/AutoDataInstrumented;", false)
            }
        }
        Logger.info("||---结束扫描类：${mClassName}\n")
        super.visitEnd()
    }
}
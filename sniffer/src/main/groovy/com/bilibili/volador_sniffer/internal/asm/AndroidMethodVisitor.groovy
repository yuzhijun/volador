package com.bilibili.volador_sniffer.internal.asm

import com.bilibili.volador_sniffer.internal.bean.SnifferHookConfig
import com.bilibili.volador_sniffer.internal.bean.SnifferMethodCell
import com.bilibili.volador_sniffer.internal.util.Logger
import com.bilibili.volador_sniffer.internal.util.SnifferAnalyticsUtil
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter


class AndroidMethodVisitor extends AdviceAdapter{

    public HashSet<String> visitedFragMethods
    String methodName
    int access
    MethodVisitor methodVisitor
    String methodDesc
    String superName
    String className
    String[] interfaces

    AndroidMethodVisitor(MethodVisitor methodVisitor, int access, String name, String desc,
                            String superName, String className, String[] interfaces, HashSet<String> visitedFragMethods) {
        super(Opcodes.ASM6, methodVisitor, access, name, desc)
        this.methodName = name
        this.access = access
        this.methodVisitor = methodVisitor
        this.methodDesc = desc
        this.superName = superName
        this.className = className
        this.interfaces = interfaces
        this.visitedFragMethods = visitedFragMethods
        Logger.info("||开始扫描方法：${Logger.accCode2String(access)} ${methodName}${desc}")
    }

    @Override
    void visitEnd() {
        super.visitEnd()
        if (isHasTracked) {
            visitAnnotation("Lcom/bilibili/engine_center/sniffer/AutoDataInstrumented;", false)
            Logger.info("||Hooked method: ${methodName}${methodDesc}")
        }
        Logger.info("||结束扫描方法：${methodName}")
    }

    boolean isAutoTrackViewOnClickAnnotation = false
    boolean isAutoTrackIgnoreTrackOnClick = false
    boolean isHasInstrumented = false
    boolean isHasTracked = false
    @Override
    protected void onMethodEnter() {
        super.onMethodEnter()
        if (isAutoTrackViewOnClickAnnotation){
            return
        }

        /**
         * 在 android.gradle 的 3.2.1 版本中，针对 view 的 setOnClickListener 方法 的 lambda 表达式做特殊处理。
         */
        if (methodName.trim().startsWith('lambda$') && SnifferAnalyticsUtil.isPrivate(access) && SnifferAnalyticsUtil.isSynthetic(access)) {
            SnifferMethodCell snifferMethodCell = SnifferHookConfig.sLambdaMethods.get(desc)
            if (snifferMethodCell != null) {
                int paramStart = snifferMethodCell.paramsStart
                if (SnifferAnalyticsUtil.isStatic(access)) {
                    paramStart = paramStart - 1
                }
                SnifferAnalyticsUtil.visitMethodWithLoadedParams(methodVisitor, Opcodes.INVOKESTATIC, SnifferHookConfig.LOG_ANALYTICS_BASE,
                        snifferMethodCell.agentName, snifferMethodCell.agentDesc,
                        paramStart, snifferMethodCell.paramsCount, snifferMethodCell.opcodes)
                isHasTracked = true
                return
            }
        }

        if (!(SnifferAnalyticsUtil.isPublic(access) && !SnifferAnalyticsUtil.isStatic(access))) {
            return
        }

        //之前已经添加过埋点代码，忽略
        if (isHasInstrumented) {
            return
        }

        //Method 描述信息
        String methodNameDesc = methodName + methodDesc

        /**
         * Fragment
         * 目前支持 android/support/v4/app/ListFragment 和 android/support/v4/app/Fragment
         */
        if (SnifferAnalyticsUtil.isInstanceOfFragment(superName)) {
            SnifferMethodCell snifferMethodCell = SnifferHookConfig.sFragmentMethods.get(methodNameDesc)
//            Log.info("fragment:methodNameDesc:" + methodNameDesc)
//            Log.info("fragment:logMethodCell:" + logMethodCell)
            if (snifferMethodCell != null) {
                visitedFragMethods.add(methodNameDesc)
                SnifferAnalyticsUtil.visitMethodWithLoadedParams(methodVisitor, Opcodes.INVOKESTATIC, SnifferHookConfig.LOG_ANALYTICS_BASE, snifferMethodCell.agentName, snifferMethodCell.agentDesc, snifferMethodCell.paramsStart, snifferMethodCell.paramsCount, snifferMethodCell.opcodes)
                isHasTracked = true
            }
        }

        /**
         * Menu
         * 目前支持 onContextItemSelected(MenuItem item)、onOptionsItemSelected(MenuItem item)
         */
        if (SnifferAnalyticsUtil.isTargetMenuMethodDesc(methodNameDesc)) {
            methodVisitor.visitVarInsn(ALOAD, 0)
            methodVisitor.visitVarInsn(ALOAD, 1)
            methodVisitor.visitMethodInsn(INVOKESTATIC, SnifferHookConfig.LOG_ANALYTICS_BASE, "trackMenuItem", "(Ljava/lang/Object;Landroid/view/MenuItem;)V", false)
            isHasTracked = true
            return
        }

        if (methodNameDesc == 'onDrawerOpened(Landroid/view/View;)V') {
            methodVisitor.visitVarInsn(ALOAD, 1)
            methodVisitor.visitMethodInsn(INVOKESTATIC, SnifferHookConfig.LOG_ANALYTICS_BASE, "trackDrawerOpened", "(Landroid/view/View;)V", false)
            isHasTracked = true
            return
        } else if (methodNameDesc == 'onDrawerClosed(Landroid/view/View;)V') {
            methodVisitor.visitVarInsn(ALOAD, 1)
            methodVisitor.visitMethodInsn(INVOKESTATIC, SnifferHookConfig.LOG_ANALYTICS_BASE, "trackDrawerClosed", "(Landroid/view/View;)V", false)
            isHasTracked = true
            return
        }

        if (className == 'android/databinding/generated/callback/OnClickListener') {
            if (methodNameDesc == 'onClick(Landroid/view/View;)V') {
                methodVisitor.visitVarInsn(ALOAD, 1)
                methodVisitor.visitMethodInsn(INVOKESTATIC, SnifferHookConfig.LOG_ANALYTICS_BASE, "trackViewOnClick", "(Landroid/view/View;)V", false)
                isHasTracked = true
                return
            }
        }

        if (className.startsWith('android') || className.startsWith('androidx')) {
            return
        }

        if (methodNameDesc == 'onItemSelected(Landroid/widget/AdapterView;Landroid/view/View;IJ)V' || methodNameDesc == "onListItemClick(Landroid/widget/ListView;Landroid/view/View;IJ)V") {
            methodVisitor.visitVarInsn(ALOAD, 1)
            methodVisitor.visitVarInsn(ALOAD, 2)
            methodVisitor.visitVarInsn(ILOAD, 3)
            methodVisitor.visitMethodInsn(INVOKESTATIC, SnifferHookConfig.LOG_ANALYTICS_BASE, "trackListView", "(Landroid/widget/AdapterView;Landroid/view/View;I)V", false)
            isHasTracked = true
            return
        }

        if (isAutoTrackViewOnClickAnnotation) {
            if (methodDesc == '(Landroid/view/View;)V') {
                methodVisitor.visitVarInsn(ALOAD, 1)
                methodVisitor.visitMethodInsn(INVOKESTATIC, SnifferHookConfig.LOG_ANALYTICS_BASE, "trackViewOnClick", "(Landroid/view/View;)V", false)
                isHasTracked = true
                return
            }
        }

        if (interfaces != null && interfaces.length > 0) {
            SnifferMethodCell snifferMethodCell = SnifferHookConfig.sInterfaceMethods.get(methodNameDesc)
            if (snifferMethodCell != null && interfaces.contains(snifferMethodCell.parent)) {
                SnifferAnalyticsUtil.visitMethodWithLoadedParams(methodVisitor, Opcodes.INVOKESTATIC, SnifferHookConfig.LOG_ANALYTICS_BASE
                        , snifferMethodCell.agentName, snifferMethodCell.agentDesc, snifferMethodCell.paramsStart, snifferMethodCell.paramsCount, snifferMethodCell.opcodes)
                isHasTracked = true
            }
        }

        if (!isHasTracked) {
            if (methodNameDesc == 'onClick(Landroid/view/View;)V') {
                methodVisitor.visitVarInsn(ALOAD, 1)
                methodVisitor.visitMethodInsn(INVOKESTATIC, SnifferHookConfig.LOG_ANALYTICS_BASE, "trackViewOnClick", "(Landroid/view/View;)V", false)
                isHasTracked = true
            }
        }
    }

    @Override
    protected void onMethodExit(int opcode) {
        super.onMethodExit(opcode)
    }

    @Override
    AnnotationVisitor visitAnnotation(String s, boolean b) {
        if (s == 'Lcom/bilibili/engine_center/sniffer/AutoTrackDataViewOnClick;') {
            isAutoTrackViewOnClickAnnotation = true
            Logger.info("||发现 ${methodName}${methodDesc} 有注解 @AutoTrackDataViewOnClick")
        }

        if (s == 'Lcom/bilibili/engine_center/sniffer/AutoIgnoreTrackDataOnClick;') {
            isAutoTrackIgnoreTrackOnClick = true
            Logger.info("||发现 ${methodName}${methodDesc} 有注解 @AutoIgnoreTrackDataOnClick")
        }

        if (s == 'Lcom/bilibili/engine_center/sniffer/AutoDataInstrumented;') {
            isHasInstrumented = true
        }

        return super.visitAnnotation(s, b)
    }
}
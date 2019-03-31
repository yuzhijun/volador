package com.bilibili.volador_sniffer.internal.util


import com.bilibili.volador_sniffer.internal.cache.GlobalConfig

public class AutoMatchUtil {
    /**
     * 是否对扫描类进行修改
     * @param className 扫描到的类名
     * @param exclude 过滤掉的类
     */
    static boolean isShouldModifyClass(String className) {
        if (className.contains('R$') ||
                className.contains('R2$') ||
                className.endsWith('R') ||
                className.endsWith('R2') ||
                className.endsWith('BuildConfig')) {
            return false
        }

        // 1、用户自定义的优先通过
        Iterator<String> includeIterator = GlobalConfig.include.iterator()
        while (includeIterator.hasNext()) {
            String packageName = includeIterator.next()

            if (className.startsWith(packageName)) {
                return true
            }
        }

        // 2、不允许通过的包，包括用户自定义的
        Iterator<String> iterator = GlobalConfig.exclude.iterator()
        while (iterator.hasNext()) {
            String packageName = iterator.next()
            if (className.startsWith(packageName)) {
                return false
            }
        }
        return true
    }
}

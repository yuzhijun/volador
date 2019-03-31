package com.bilibili.volador_sniffer.internal.javaassit.Injector

import org.gradle.api.Project

abstract class BaseInjector implements IClassInjector {
    protected Project project

    protected String variantDir

    @Override
    public Object name() {
        return getClass().getSimpleName()
    }

    public void setProject(Project project) {
        this.project = project
    }

    public void setVariantDir(String variantDir) {
        this.variantDir = variantDir
    }
}

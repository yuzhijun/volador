package com.bilibili.volador_sniffer.internal.javaassit.Injector;

enum Injectors {
    APPLICATION_INJECTOR('ApplicationInjector', new ApplicationInjector(), '用于Application的注入'),
    ACTIVITY_INJECTOR('ActivityInjector', new ActivityInjector(),'用于activity的注入'),
    FRAGMENT_INJECTOR('FragmentInjector',new FragmentInjector(),'用于fragment的注入')

    IClassInjector injector
    String nickName
    String desc

    Injectors(String nickName, IClassInjector injector, String desc) {
        this.injector = injector
        this.nickName = nickName
        this.desc = desc
    }
}

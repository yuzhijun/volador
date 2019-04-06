package com.bilibili.engine_center;


import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * Data generation subject
 * Created by yuzhijun on 2018/3/27.
 */
public class GeneratorSubject<T> implements SubjectSupport<T>,Generator<T>{
    private Subject<T> mSubject;
    public GeneratorSubject() {
        mSubject = createSubject();
    }

    protected Subject<T> createSubject() {
        return PublishSubject.create();
    }

    @Override
    public void generate(T data) {
        mSubject.onNext(data);
    }

    @Override
    public Observable<T> subject() {
        return mSubject;
    }
}

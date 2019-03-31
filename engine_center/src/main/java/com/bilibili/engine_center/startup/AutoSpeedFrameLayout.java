package com.bilibili.engine_center.startup;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class AutoSpeedFrameLayout extends FrameLayout {

    public AutoSpeedFrameLayout(@NonNull Context context){
        super(context);
    }

    public AutoSpeedFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoSpeedFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private int pageObjectKey;//关联的页面key
    public AutoSpeedFrameLayout(@NonNull Context context, int pageObjectKey) {
        super(context);
        this.pageObjectKey = pageObjectKey;
    }

    public static View wrap(int pageObjectKey, @NonNull View child) {
        //将页面根View作为子View，其他参数保持不变
        ViewGroup vg = new AutoSpeedFrameLayout(child.getContext(), pageObjectKey);
        if (child.getLayoutParams() != null) {
            vg.setLayoutParams(child.getLayoutParams());
        }
        vg.addView(child, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return vg;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        AutoSpeed.getInstance().onPageDrawEnd(pageObjectKey);
    }
}

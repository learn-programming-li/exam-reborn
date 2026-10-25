package com.example.examreborn.util;

import android.app.Activity;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * EdgeToEdge 之后，页面内容会顶到状态栏/导航栏底下，用这几个方法把系统栏高度补成内边距。
 * 都是"在原有 padding 基础上叠加"，不会覆盖 XML 里写的 padding。
 */
public final class InsetUtils {

    private InsetUtils() {
    }

    /** 状态栏图标颜色：浅色背景的页面传 true（深色图标） */
    public static void lightStatusBar(Activity activity, boolean light) {
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(light);
    }

    /** 顶栏专用：状态栏高度加到顶部，顶栏颜色就能铺到状态栏底下 */
    public static void padTop(View view) {
        final int left = view.getPaddingLeft();
        final int top = view.getPaddingTop();
        final int right = view.getPaddingRight();
        final int bottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(left, top + bars.top, right, bottom);
            return windowInsets;
        });
    }

    /** 底部区域专用：留出导航栏高度 */
    public static void padBottom(View view) {
        final int left = view.getPaddingLeft();
        final int top = view.getPaddingTop();
        final int right = view.getPaddingRight();
        final int bottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(left, top, right, bottom + bars.bottom);
            return windowInsets;
        });
    }

    /** 整页内容：四边都留出系统栏 */
    public static void padAll(View view) {
        final int left = view.getPaddingLeft();
        final int top = view.getPaddingTop();
        final int right = view.getPaddingRight();
        final int bottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom);
            return windowInsets;
        });
    }
}

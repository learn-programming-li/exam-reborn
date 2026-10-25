package com.example.examreborn.util;

import android.app.Activity;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;

import com.example.examreborn.R;

/**
 * 毛玻璃效果。
 * <p>
 * Android 上"模糊窗口背后的内容"（blur behind）依赖设备支持，
 * {@code Window#setBackgroundBlurRadius} 在很多 ROM 上被关掉，不可靠。
 * 这里的做法是：对布局里的背景网格层 {@code R.id.bgMesh} 做真实高斯模糊，
 * 半透明玻璃卡叠在它上面 —— 效果确定，不依赖 ROM。
 */
public final class GlassUtils {

    /** 背景层模糊半径（px）。数值越大雾感越强，太大会糊成一团 */
    private static final float BLUR_RADIUS = 72f;

    private GlassUtils() {
    }

    /** 给背景网格层加高斯模糊（API 31+，本项目 minSdk 33） */
    public static void blurMesh(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return;
        }
        View mesh = activity.findViewById(R.id.bgMesh);
        if (mesh == null) {
            return;
        }
        mesh.setRenderEffect(RenderEffect.createBlurEffect(
                BLUR_RADIUS, BLUR_RADIUS, Shader.TileMode.CLAMP));
    }
}

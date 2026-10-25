package com.example.examreborn;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.examreborn.util.GlassUtils;
import com.example.examreborn.util.InsetUtils;

public class MainActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 1500L; //启动页面停留1.5秒

    private final Handler handler = new Handler(Looper.getMainLooper());

    // 到点要执行的事情: 跳转到主界面并销毁自己
    private final Runnable jumpToHome = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        InsetUtils.lightStatusBar(this, false);
        GlassUtils.blurMesh(this);
        handler.postDelayed(jumpToHome, SPLASH_DELAY_MS);
    }

    @Override
    protected  void onDestroy(){
        handler.removeCallbacks(jumpToHome);
        super.onDestroy();
    }
}
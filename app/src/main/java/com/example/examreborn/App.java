package com.example.examreborn;

import android.app.Application;

import com.example.examreborn.data.DataStore;

/** 在 Application 里初始化数据库，保证任何界面打开前数据源都可用 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        DataStore.init(this);
    }
}

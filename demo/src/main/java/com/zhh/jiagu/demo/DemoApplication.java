package com.zhh.jiagu.demo;

import android.app.Application;
import android.content.Context;

import com.tencent.mmkv.MMKV;

public class DemoApplication extends Application {

    private static DemoApplication instance = null;

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;
        initMMKV();

    }


    public static Context getApplication(){
        return instance;
    }

    private void initMMKV() {
        MMKV.initialize(this);
    }
}

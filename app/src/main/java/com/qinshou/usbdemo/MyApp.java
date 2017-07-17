package com.qinshou.usbdemo;

import android.app.Application;
import android.content.Context;

/**
 * Description:
 * Created on 2017/7/14
 */

public class MyApp extends Application {

    public static Context mContext;

    private static MyApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        instance = this;

    }
}

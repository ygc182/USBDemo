package com.qinshou.usbdemo;

import android.app.Application;
import android.content.Context;

/**
 * Description:
 * Created on 2017/7/14
 */

public class MyApp extends Application {

    public static final String TAG = "MyApp";
    public static Context mContext;

    public static Context getInstance() {
        return mContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;

//        Intent usbServiceIntent = new Intent(mContext, UsbService.class);
//        startService(usbServiceIntent);

    }
}

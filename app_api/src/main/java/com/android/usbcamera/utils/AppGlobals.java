package com.android.usbcamera.utils;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;

/**
 * @author lhzheng@grandstream.cn
 * @version 1.0 20-12-2
 * 反射方式获取Application，避免模块间强依赖
 */
public class AppGlobals {
    private static final String TAG = "AppGlobals";
    private static Application sApplication;

    /**
     * 反射方式获取Application对象
     * @return Application
     */
    public static Application getApplication() {
        if (sApplication == null) {
            try {
                sApplication = (Application) Class.forName("android.app.ActivityThread")
                        .getMethod("currentApplication")
                        .invoke(null, (Object[]) null);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return sApplication;
    }

    /**
     * 获取App的上下文
     * @return ApplicationContext
     */
    public static Context getAppContext(){
        Application application = getApplication();
        if (application!=null) {
           return application.getApplicationContext();
        }
        Log.e(TAG, "getAppConext return null.");
        return null;
    }
}

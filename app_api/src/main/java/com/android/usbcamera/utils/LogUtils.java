package com.android.usbcamera.utils;

import android.util.Log;

public class LogUtils {

    private static final String TAG = "confcamera"; // tag标签
    private static String className; // 类名
    private static String methodName; // 方法名
    private static int lineNumber; // 所在行数

    /**
     * 获取方法名,类名,行数
     * @param elements
     */
    private static void getMethodName(StackTraceElement[] elements) {
        className = elements[1].getClassName();
        methodName = elements[1].getMethodName();
        lineNumber = elements[1].getLineNumber();
    }

    /**
     * 创建msg
     * @param msg
     * @return
     */
    private static String createMsg(String msg) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(methodName);
        buffer.append("(").append(className).append(":").append(lineNumber).append(")=====");
        buffer.append(msg);
        return buffer.toString();
    }

    /**
     * 检查msg是否为null
     * @param msg
     * @return
     */
    private static Object handleMsg(Object msg) {
        if (msg == null) {
            msg = "[null]";
        } else if (msg.toString().trim().length() == 0) {
            msg = "[\"\"]";
        } else {
            msg = msg.toString().trim();
        }
        return msg;
    }

    /**
     * 打印verbose日志
     * @param msg
     */
    public static void v(Object msg) {
        msg = handleMsg(msg);
        getMethodName(new Throwable().getStackTrace());
        Log.v(TAG, createMsg(msg.toString()));
    }

    /**
     * 打印debug日志
     * @param msg
     */
    public static void d(Object msg) {
        msg = handleMsg(msg);
        getMethodName(new Throwable().getStackTrace());
        Log.d(TAG, createMsg(msg.toString()));
    }

    /**
     * 打印info日志
     * @param msg
     */
    public static void i(Object msg) {
        msg = handleMsg(msg);
        getMethodName(new Throwable().getStackTrace());
        Log.i(TAG, createMsg(msg.toString()));
    }

    /**
     * 打印warn日志
     * @param msg
     */
    public static void w(Object msg) {
        msg = handleMsg(msg);
        getMethodName(new Throwable().getStackTrace());
        Log.w(TAG, createMsg(msg.toString()));
    }

    /**
     * 打印error日志
     * @param msg
     */
    public static void e(Object msg) {
        msg = handleMsg(msg);
        getMethodName(new Throwable().getStackTrace());
        Log.e(TAG, createMsg(msg.toString()));
    }
}
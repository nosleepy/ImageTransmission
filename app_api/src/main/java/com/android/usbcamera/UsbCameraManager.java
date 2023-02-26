package com.android.usbcamera;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.android.usbcamera.utils.AppGlobals;
import com.android.usbcamera.utils.LogUtils;

public class UsbCameraManager {

    private static ICameraService cameraService;
    private static Activity activity;
    private static CameraServiceCallback callback;
    private static Context context;

    // 服务开启状态
    private static boolean status = false;

    /**
     * 初始化摄像头预览配置
     * @param usbCameraConfig
     */
    public static void init(UsbCameraConfig usbCameraConfig) {
        LogUtils.d("init UsbCameraManager");
        CameraService.setPreviewView(usbCameraConfig.getPreviewView());
        callback = usbCameraConfig.getCameraServiceCallback();
        activity = usbCameraConfig.getActivity();
        CameraService.setResolution(usbCameraConfig.getResolution());
        CameraService.setPreviewSize(usbCameraConfig.getPreviewSize());
        context = AppGlobals.getAppContext();
    }

    /**
     * 开启摄像头预览
     */
    public static void startUsbCameraPreview() {
        if (status) {
            stopUsbCameraPreview();
        }
        LogUtils.d("start usb camera preview");
        bindCameraService();
        status = true;
    }

    /**
     * 停止摄像头预览
     */
    public static void stopUsbCameraPreview() {
        if (!status) {
            return;
        }
        LogUtils.d("stop usb camera preview");
        try {
            cameraService.stopUsbCameraPreview();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        unBindCameraService();
        status = false;
    }

    // 调整锐化值
    public void sharpenChange(Integer progress) {
        CameraService.sharpenChange(progress);
    }

    // 绑定服务
    private static void bindCameraService() {
        LogUtils.d("bind camera service");
        Intent intent = new Intent(context, CameraService.class);
        context.startService(intent);
        boolean flag = context.bindService(intent, cameraServiceConnection, Context.BIND_AUTO_CREATE);
        LogUtils.d("isBind -> " + flag);

        if (flag && callback != null) {
            callback.onServiceStart();
        }
    }

    // 解绑服务
    private static void unBindCameraService() {
        LogUtils.d("unBind camera service");
        Intent intent = new Intent(context, CameraService.class);
        context.unbindService(cameraServiceConnection);
        context.stopService(intent);

        if (callback != null) {
            callback.onServiceStop();
        }
    }

    private static ServiceConnection cameraServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            LogUtils.d("onServiceConnected");
            cameraService = ICameraService.Stub.asInterface(iBinder);

            // 开启摄像头预览
            try {
                cameraService.startUsbCameraPreview();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            LogUtils.d("onServiceDisconnected");

            // 停止摄像头预览
            try {
                cameraService.stopUsbCameraPreview();
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            cameraService = null;
        }
    };

    /**
     * 服务启动/停止状态回调接口
     */
    public interface CameraServiceCallback {

        /**
         * CameraService服务启动
         */
        void onServiceStart();

        /**
         * CameraService服务停止
         */
        void onServiceStop();
    }
}
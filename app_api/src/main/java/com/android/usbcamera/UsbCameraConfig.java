package com.android.usbcamera;

import android.app.Activity;
import android.view.TextureView;
import android.view.View;

import jp.co.cyberagent.android.gpuimage.GPUImageView;

public class UsbCameraConfig {

    private View previewView;
    private UsbCameraManager.CameraServiceCallback cameraServiceCallback;
    private Activity activity;
    private String resolution;
    private String previewSize;

    private UsbCameraConfig(Builder builder) {
        previewView = builder.previewView;
        cameraServiceCallback = builder.cameraServiceCallback;
        activity = builder.activity;
        resolution = builder.resolution;
        previewSize = builder.previewSize;
    }

    public View getPreviewView() {
        return previewView;
    }

    public UsbCameraManager.CameraServiceCallback getCameraServiceCallback() {
        return cameraServiceCallback;
    }

    public Activity getActivity() {
        return activity;
    }

    public String getResolution() {
        return resolution;
    }

    public String getPreviewSize() {
        return previewSize;
    }

    public static class Builder {

        private View previewView; // 预览画面显示位置
        private UsbCameraManager.CameraServiceCallback cameraServiceCallback; // 服务启动/停止状态回调
        private Activity activity; // 上下文对象
        private String resolution = UsbCameraConstant.RESOLUTION_1080; // 分辨率
        private String previewSize = UsbCameraConstant.SIZE_SMALL; // 预览尺寸

        public Builder() {}

        /**
         * 使用TextureView作为画面预览(必须设置)
         * @param previewView
         * @return Builder
         */
        public Builder setPreviewView(TextureView previewView) {
            this.previewView = previewView;
            return this;
        }

        /**
         * 使用GPUImageView作为画面预览(必须设置)
         * @param previewView
         * @return Builder
         */
        public Builder setPreviewView(GPUImageView previewView) {
            this.previewView = previewView;
            return this;
        }

        /**
         * 设置服务启动停止回调
         * @param cameraServiceCallback
         * @return Builder
         */
        public Builder setCameraServiceCallback(UsbCameraManager.CameraServiceCallback cameraServiceCallback) {
            this.cameraServiceCallback = cameraServiceCallback;
            return this;
        }

        /**
         * 设置Activity上下文(必须设置)
         * @param activity
         * @return Builder
         */
        public Builder setActivity(Activity activity) {
            this.activity = activity;
            return this;
        }

        /**
         * 设置分辨率,默认"720"
         * “720”低分辨率,“1080”高分辨率
         * @param resolution
         * @return Builder
         */
        public Builder setResolution(String resolution) {
            this.resolution = resolution;
            return this;
        }

        /**
         * 设置预览尺寸,默认"small"
         * "small"小尺寸,"large"大尺寸
         * @param previewSize
         * @return Builder
         */
        public Builder setPreviewSize(String previewSize) {
            this.previewSize = previewSize;
            return this;
        }

        /**
         * 返回UsbCameraConfig对象
         * @return UsbCameraConfig
         */
        public UsbCameraConfig build() {
            return new UsbCameraConfig(this);
        }
    }
}
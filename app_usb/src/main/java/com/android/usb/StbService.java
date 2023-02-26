package com.android.usb;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.List;

public class StbService extends Service {

    private static final String TAG = "wlzhou";

    private static SurfaceTexture surfaceTexture;

    private static Activity activity;

    private Camera mCamera;
    private Camera.CameraInfo mFrontCameraInfo;
    private int mFrontCameraId = -1;
    private Camera.CameraInfo mBackCameraInfo;
    private int mBackCameraId = -1;

    private int degrees = 270;

    public AudioRecord mAudioRecord;
    public AudioTrack mAudioTrack;
    private int recordBufferSize;
    private int trackBufferSize;
    private boolean start;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "[StbService] onBind");
        StbBinder stbBinder = new StbBinder();
        return stbBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "[StbService] onUnbind");
        return super.onUnbind(intent);
    }

    class StbBinder extends IStbService.Stub {

        @Override
        public void startC03Preview() throws RemoteException {
            initData();
            initCameraInfo();
            openCamera();
            setPreviewSize(1200, 1600);
            setPreviewSurface(surfaceTexture);
            startPreview();
        }

        @Override
        public void stopC03Preview() throws RemoteException {
            stopPreview();
            closeCamera();
        }

        @Override
        public void rotateC03Preview() throws RemoteException {
            mCamera.setDisplayOrientation(degrees);
            degrees = degrees + 90;
            if (degrees == 360) {
                degrees = 0;
            }
        }
    }

    public static void setSurfaceTexture(SurfaceTexture st) {
        surfaceTexture = st;
    }

    public static void setActivity(Activity a) {
        activity = a;
    }

    public void initData() {
        recordBufferSize = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                8000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                recordBufferSize * 2);
        trackBufferSize = AudioTrack.getMinBufferSize(8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                8000,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                trackBufferSize * 2,
                AudioTrack.MODE_STREAM);
    }

    // 初始化摄像头信息
    private void initCameraInfo() {
        int numberOfCameras = Camera.getNumberOfCameras();// 获取摄像头个数
        Log.d("wlzhou", "[initCameraInfo] numberOfCameras = " + numberOfCameras);
        for (int cameraId = 0; cameraId < numberOfCameras; cameraId++) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                // 后置摄像头信息
                mBackCameraId = cameraId;
                mBackCameraInfo = cameraInfo;
                Log.d("wlzhou", "[initCameraInfo] mBackCameraId = " + mBackCameraId + ",mBackCameraInfo = " + mBackCameraInfo);
            } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
                // 前置摄像头信息
                mFrontCameraId = cameraId;
                mFrontCameraInfo = cameraInfo;
                Log.d("wlzhou", "[initCameraInfo] mFrontCameraId = " + mFrontCameraId + ",mFrontCameraInfo = " + mFrontCameraInfo);
            }
        }
    }

    // 开启指定摄像头
    private void openCamera() {
        if (mCamera != null) {
            throw new RuntimeException("相机已经被开启，无法同时开启多个相机实例！");
        }
        if (hasFrontCamera()) {
            // 优先开启前置摄像头
            Log.d(TAG, "[openCamera] open FrontCamera");
            mCamera = Camera.open(mFrontCameraId);
            mCamera.setDisplayOrientation(180);
        } else if (hasBackCamera()) {
            // 没有前置，就尝试开启后置摄像头
            Log.d(TAG, "[openCamera] open BackCamera");
            mCamera = Camera.open(mBackCameraId);
            mCamera.setDisplayOrientation(180);
        } else {
            Log.d(TAG, "no Camera");
            throw new RuntimeException("没有任何相机可以开启！");
        }
    }

    private boolean hasFrontCamera() {
        return mFrontCameraId != -1 && mFrontCameraInfo != null;
    }

    private boolean hasBackCamera() {
        return mBackCameraId != -1 && mBackCameraInfo != null;
    }

    private int getCameraDisplayOrientation(Camera.CameraInfo cameraInfo) {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (cameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (cameraInfo.orientation - degrees + 360) % 360;
        }
        return result;
    }

    // 设置预览尺寸
    private void setPreviewSize(int shortSide, int longSide) {
        if (mCamera != null && shortSide != 0 && longSide != 0) {
            float aspectRatio = (float) longSide / shortSide;
            Log.d(TAG, "[setPreviewSize] aspectRatio = " + aspectRatio);
            Camera.Parameters parameters = mCamera.getParameters();
            List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
            Log.d(TAG, "[setPreviewSize] supportedPreviewSizes = " + supportedPreviewSizes.size());
            for (Camera.Size previewSize : supportedPreviewSizes) {
                Log.d(TAG, "[setPreviewSize] width = " + previewSize.width + ",height = " + previewSize.height);
                if ((float) previewSize.width / previewSize.height == aspectRatio && previewSize.height <= shortSide && previewSize.width <= longSide) {
                    Log.d(TAG, "[setPreviewSize] setPreviewSize -> yes");
                    parameters.setPreviewSize(previewSize.width, previewSize.height);
                    mCamera.setParameters(parameters);
                    break;
                }
            }
        }
        Log.d(TAG, "[setPreviewSize] setPreviewSize -> no");
    }

    // 设置预览 Surface
    private void setPreviewSurface(SurfaceTexture surfaceTexture) {
        if (mCamera != null && surfaceTexture != null) {
            try {
                mCamera.setPreviewTexture(surfaceTexture);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 开始预览
    private void startPreview() {
        startPlay();
        if (mCamera != null) {
            mCamera.startPreview();
            Log.d(TAG, "[startPreview] -> yes");
        }
    }

    // 停止预览
    private void stopPreview() {
        stopPlay();
        if (mCamera != null) {
            mCamera.stopPreview();
            Log.d(TAG, "[stopPreview] -> yes");
        }
    }

    // 关闭相机
    private void closeCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    // 录制音频
    public void startPlay() {
        start = true;
        Log.d(TAG, "[startPlay] -> yes");
        new Thread(new Runnable() {
            @Override
            public void run() {
                mAudioRecord.startRecording();
                mAudioTrack.play();
                byte[] bytes = new byte[2048];
                while (start) {
                    int len = mAudioRecord.read(bytes, 0, bytes.length);
                    Log.d(TAG, "[mAudioRecord] read -> yes,len = " + len);
                    byte[] temp = new byte[2048];
                    System.arraycopy(bytes, 0, temp, 0, len);
                    mAudioTrack.write(temp, 0, len);
                    Log.d(TAG, "[mAudioTrack] write -> yes,len = " + len);
                }
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioTrack.stop();
                mAudioTrack.release();
            }
        }).start();
    }

    // 播放音频
    public void stopPlay() {
        start = false;
        Log.d(TAG, "[startPlay] -> false");
    }
}

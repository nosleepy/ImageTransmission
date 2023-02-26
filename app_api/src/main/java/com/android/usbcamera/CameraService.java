package com.android.usbcamera;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Range;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.usbcamera.utils.GPUImageFilterTools;
import com.android.usbcamera.utils.ImageUtils;
import com.android.usbcamera.utils.LogUtils;

import java.util.Arrays;

import jp.co.cyberagent.android.gpuimage.GPUImageView;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSharpenFilter;

public class CameraService extends Service implements UsbDeviceReceiver.UsbDeviceChangeListener {

    private CameraManager cameraManager; //摄像头管理
    private CameraDevice.StateCallback deviceCallback; //摄像头监听
    private CameraCaptureSession.CaptureCallback captureCallback; // 预览拍照监听
    private CameraCaptureSession cameraCaptureSession; // 控制摄像头预览或拍照
    private CameraDevice cameraDevice;

    private HandlerThread handlerThread;
    private Handler cameraHandler; // 后台处理图片传输帧
    private ImageReader imageReader;

    private static volatile View previewView;
    private static volatile GPUImageView gpuImageView;
    private static volatile TextureView textureView;
    private static GPUImageFilterTools.FilterAdjuster filterAdjuster;

    private UsbDeviceReceiver usbDeviceReceiver;

    public AudioRecord mAudioRecord;
    public AudioTrack mAudioTrack;
    private int recordBufferSize;
    private int trackBufferSize;
    private volatile boolean start;

    private static int width;
    private static int height;

    private static final Range<Integer> fpsRange = new Range<>(30, 30); // 预览画面帧率

    // 预览尺寸
    private static final int[] smallSize = {960, 540};
    private static final int[] largeSize = {1920, 1080};
    private static int[] curSize;

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtils.d("onCreate");
        // 注册usb设备监听
        registerUsbDeviceReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.d("onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtils.d("onDestroy");
        // 取消usb设备监听
        unregisterUsbDeviceReceiver();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        LogUtils.d("CameraService onBind");
        CameraBinder cameraBinder = new CameraBinder();
        return cameraBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LogUtils.d("CameraService onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onUsbDeviceAttached(UsbDevice device) {
        initHandler();
        initData();
        openCamera();
    }

    @Override
    public void onUsbDeviceDetached(UsbDevice device) {
        destroyHandler();
        closeCamera();
        stopPlay();
    }

    class CameraBinder extends ICameraService.Stub {

        @Override
        public void startUsbCameraPreview() throws RemoteException {
            initHandler();
            initData();
            openCamera();
        }

        @Override
        public void stopUsbCameraPreview() throws RemoteException {
            destroyHandler();
            closeCamera();
            stopPlay();
        }
    }

    // 设置预览画面显示位置
    public static void setPreviewView(View view) {
        LogUtils.d("set preview view -> " + view.getClass().getSimpleName());
        previewView = view;
        String className = previewView.getClass().getSimpleName();
        if (className.equals("GPUImageView")) {
            gpuImageView = (GPUImageView) view;
            gpuImageView.setRenderMode(GPUImageView.RENDERMODE_CONTINUOUSLY);
            imageSharpen();
        } else if (className.equals("TextureView")) {
            textureView = (TextureView) view;
            textureView.setScaleX(-1f);
        }
    }

    // 设置显示分辨率
    public static void setResolution(String resolution) {
        LogUtils.d("set resolution -> " + resolution);
        if (UsbCameraConstant.RESOLUTION_1080.equals(resolution)) {
            width = 1920;
            height = 1080;
        } else if (UsbCameraConstant.RESOLUTION_720.equals(resolution)) {
            width = 1280;
            height = 720;
        }
    }

    // 设置预览尺寸
    public static void setPreviewSize(String previewSize) {
        LogUtils.d("set preview size -> " + previewSize);
        if (UsbCameraConstant.SIZE_SMALL.equals(previewSize)) {
            curSize = smallSize;
        } else if (UsbCameraConstant.SIZE_LARGE.equals(previewSize)) {
            curSize = largeSize;
        }
    }

    // 改变预览大小
    private static void changePreviewSize() {
        ViewGroup.LayoutParams layoutParams = previewView.getLayoutParams();
        layoutParams.width = curSize[0];
        layoutParams.height = curSize[1];
        previewView.setLayoutParams(layoutParams);
        LogUtils.d("preview size change");
    }

    // 数据初始化
    @SuppressLint("MissingPermission")
    private void initData() {
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

        cameraManager = (CameraManager) CameraService.this.getSystemService(Context.CAMERA_SERVICE);

        deviceCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                cameraDevice = camera;
                String className = previewView.getClass().getSimpleName();
                if (className.equals("GPUImageView")) {
                    Surface surface = imageReader.getSurface();
                    takePreview(surface);
                } else if (className.equals("TextureView")) {
                    SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
                    surfaceTexture.setDefaultBufferSize(width, height);
                    Surface surface = new Surface(surfaceTexture);
                    takePreview(surface);
                }
                startPlay();
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                closeCamera();
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                closeCamera();
                LogUtils.e("open camera error");
                Toast.makeText(CameraService.this, "open camera error", Toast.LENGTH_SHORT).show();
            }
        };

        captureCallback = new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                super.onCaptureStarted(session, request, timestamp, frameNumber);
            }
        };

        imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 1);
        imageReader.setOnImageAvailableListener(reader -> {
            try {
                cameraHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Image image = reader.acquireLatestImage();
                        if (image == null) {
                            return;
                        }
                        byte[] bytes = ImageUtils.generateNV21Data(image);
                        gpuImageView.updatePreviewFrame(bytes, width, height);
                        image.close();
                    }
                });
            } catch (Exception e) {
                LogUtils.e("sending message to a Handler on a dead thread");
            }
        }, cameraHandler);

        changePreviewSize(); // 设置预览尺寸大小
    }

    // 为摄像头开一个线程
    private void initHandler() {
        handlerThread = new HandlerThread("camera");
        handlerThread.start();
        cameraHandler = new Handler(handlerThread.getLooper()); // handler与线程进行绑定
    }

    // 使用后置摄像头
    public void openCamera() {
        try {
            String[] cameraIdList = cameraManager.getCameraIdList();
            LogUtils.d("CameraIdList,length = " + cameraIdList.length);
            // 获取可用相机设备列表
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraIdList[0]);
            // 在这里可以通过CameraCharacteristics设置相机的功能,当然必须检查是否支持
            characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            String cameraId = cameraIdList[0]; // 得到后摄像头编号
            LogUtils.d("cameraId = " + cameraId);
            cameraManager.openCamera(cameraId, deviceCallback, cameraHandler);
        } catch (Exception e) {
            Toast.makeText(CameraService.this, "please connect usb camera", Toast.LENGTH_SHORT).show();
            LogUtils.e("please connect usb camera");
        }
    }

    // 开启预览
    public void takePreview(Surface surface) {
        try {
            CaptureRequest.Builder previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // 将预览数据传递
            previewRequestBuilder.addTarget(surface);
            // 自动对焦
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 打开闪光灯
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            // 设置预览画面的帧率
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    cameraCaptureSession = session;
                    CaptureRequest captureRequest = previewRequestBuilder.build();
                    try {
                        cameraCaptureSession.setRepeatingRequest(captureRequest, captureCallback, cameraHandler);
                    } catch (CameraAccessException e) {
                        Toast.makeText(CameraService.this, "camera access exception", Toast.LENGTH_SHORT).show();
                        LogUtils.e("camera access exception");
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, null);
        } catch (CameraAccessException e) {
            Toast.makeText(CameraService.this, "camera access exception", Toast.LENGTH_SHORT).show();
            LogUtils.e("camera access exception");
        }
    }

    // 关闭摄像头
    public void closeCamera() {
        if (cameraDevice == null) {
            return;
        }
        cameraDevice.close();
        cameraDevice = null;
    }

    // 关闭线程
    private void destroyHandler() {
        if (handlerThread == null) {
            return;
        }
        handlerThread.quitSafely();
        try {
            handlerThread.join(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // 音频播放
    private void startPlay() {
        start = true;
        LogUtils.d("set play start");
        new Thread(new Runnable() {
            @Override
            public void run() {
                mAudioRecord.startRecording();
                mAudioTrack.play();
                byte[] bytes = new byte[2048];
                LogUtils.d("record,track -> start");
                while (start) {
                    int len = mAudioRecord.read(bytes, 0, bytes.length);
                    byte[] temp = new byte[2048];
                    System.arraycopy(bytes, 0, temp, 0, len);
                    mAudioTrack.write(temp, 0, len);
                }
                LogUtils.d("record,track -> stop");
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioTrack.stop();
                mAudioTrack.release();
            }
        }).start();
    }

    // 音频停止
    private void stopPlay() {
        start = false;
        LogUtils.d("set play stop");
    }

    // 注册广播监听取消
    private void registerUsbDeviceReceiver() {
        if (usbDeviceReceiver == null) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
            intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
            usbDeviceReceiver = new UsbDeviceReceiver(CameraService.this);
            registerReceiver(usbDeviceReceiver, intentFilter);
            LogUtils.d("registerUsbDeviceReceiver");
        }
    }

    // 取消广播监听
    private void unregisterUsbDeviceReceiver() {
        if (usbDeviceReceiver != null) {
            unregisterReceiver(usbDeviceReceiver);
            LogUtils.d("unregisterUsbDeviceReceiver");
        }
    }

    // 图像锐化
    private static void imageSharpen()  {
        GPUImageSharpenFilter sharpenFilter = new GPUImageSharpenFilter();
        sharpenFilter.setSharpness(2.0f);
        gpuImageView.setFilter(sharpenFilter);
        filterAdjuster = new GPUImageFilterTools.FilterAdjuster(sharpenFilter);
        filterAdjuster.adjust(47);
        gpuImageView.requestRender();
    }

    // 调整锐化值
    public static void sharpenChange(Integer progress) {
        filterAdjuster.adjust(progress);
        gpuImageView.requestRender();
    }
}
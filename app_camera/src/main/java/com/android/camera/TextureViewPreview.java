package com.android.camera;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.usbcamera.UsbCameraConfig;
import com.android.usbcamera.UsbCameraConstant;
import com.android.usbcamera.UsbCameraManager;

public class TextureViewPreview extends Activity {

    private TextureView textureView;
    private Button startBtn;
    private Button stopBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // 设置窗口没有标题
        setContentView(R.layout.preview_textureview);
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("wlzhou", "onResume");
    }

    @Override
    protected void onStop() {
        super.onStop();
        UsbCameraManager.stopUsbCameraPreview(); // 停止预览
    }

    private void initView() {
        startBtn = findViewById(R.id.start);
        stopBtn = findViewById(R.id.stop);
        startBtn.setOnClickListener(onClickListener);
        stopBtn.setOnClickListener(onClickListener);
        textureView = findViewById(R.id.tv);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                Log.d("wlzhou", "init");
                UsbCameraConfig usbCameraConfig = new UsbCameraConfig.Builder()
                        .setPreviewView(textureView) // 设置预览视图
//                        .setActivity(TextureViewPreview.this) // 设置当前activity上下文
                        .setResolution(UsbCameraConstant.RESOLUTION_1080) // 设置分辨率
                        .setPreviewSize(UsbCameraConstant.SIZE_SMALL) // 设置预览尺寸
                        .setCameraServiceCallback(callback) // 设置服务启动|停止状态回调
                        .build();
                UsbCameraManager.init(usbCameraConfig);
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

            }
        });
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.start:
                    UsbCameraManager.startUsbCameraPreview(); // 开启预览
                    break;
                case R.id.stop:
                    UsbCameraManager.stopUsbCameraPreview(); // 停止预览
                    break;
            }
        }
    };

    private UsbCameraManager.CameraServiceCallback callback = new UsbCameraManager.CameraServiceCallback() { // 服务启动|停止回调
        @Override
        public void onServiceStart() {
            Toast.makeText(TextureViewPreview.this, "service start", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceStop() {
            Toast.makeText(TextureViewPreview.this, "service stop", Toast.LENGTH_SHORT).show();
        }
    };
}
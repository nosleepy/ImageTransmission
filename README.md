## ImageTransmission

### MediaProjection + MediaCodec 实现简易投屏

#### 完整 Demo 下载

+ https://github.com/hiwlzhou/ImageTransmission/tree/master/app_host
+ https://github.com/hiwlzhou/ImageTransmission/tree/master/app_accessory
+ https://github.com/hiwlzhou/ImageTransmission/tree/master/app_sdk

### Camera1 实现镜像传输

#### 完整 Demo 下载

+ https://github.com/hiwlzhou/ImageTransmission/tree/master/app_stb
+ https://github.com/hiwlzhou/ImageTransmission/tree/master/app_usb

### Camera2 实现镜像传输

#### 一、功能简介

C03 充当 USB 摄像头，使用 HDMI - USB 转接线连接到机顶盒，机顶盒再通过显示器输出 C03 画面内容。使用安卓 Camera2 以及 AudioRecord 和 AudioTrack 完成视频和音频的传输。

#### 二、SDK 使用指南

+ 导入 SDK

将 classes.jar 拷贝至 Android 工程的 libs 目录下，修改 build.gradle 文件，编译项目。

```gradle
dependencies {
    implementation files('libs/classes.jar') // add
}
```

+ 主要接口

```java
public class UsbCameraManager {

    /**
     * 设置预览画面显示
     * @param TextureView实例
     */
    public static void setTextureView(TextureView tv) {}
    
    /**
     * 开启摄像头预览
     * @param Activity实例
     */
    public static void startUsbCameraPreview(Activity a) {}
    
    /**
     * 停止摄像头预览
     */
    public static void stopUsbCameraPreview() {}
}
```

+ 添加用户权限

在工程 AndroidManifest.xml 文件中添加如下权限，注册 service。

```xml
<uses-permission android:name="android.permission.CAMERA"/>
<uses-permission android:name="android.permission.RECORD_AUDIO"/>

<service android:name="com.android.api.CameraService"/>
```

+ 布局文件添加 TextureView

android:layout_width 和 android:layout_height 指定画面显示大小。

```xml
<TextureView
    android:id="@+id/tv"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

+ 示例 Activity

```java
public class CameraActivity extends AppCompatActivity {

    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
    private static final int REQUEST_PERMISSIONS_CODE = 1;

    private TextureView textureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtils.d("onCreate");
        hideTitle();
        setContentView(R.layout.activity_camera);
        initView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        LogUtils.d("onStart");
        initPermission();
    }

    @Override
    protected void onStop() {
        super.onStop();
        LogUtils.d("onStop");
        // 停止预览
        UsbCameraManager.stopUsbCameraPreview();
    }

    // 设置窗口没有标题
    private void hideTitle() {
        if (getSupportActionBar() != null){
            getSupportActionBar().hide();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    LogUtils.d("no permission");
                    Toast.makeText(CameraActivity.this, "no permission", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
        // 开启预览
        UsbCameraManager.startUsbCameraPreview(CameraActivity.this);
    }

    private void initView() {
        textureView = findViewById(R.id.tv);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
                LogUtils.d("texture view created");
                // 预览显示
                UsbCameraManager.setTextureView(textureView);
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

            }
        });
    }

    // 判断权限
    private void initPermission() {
        LogUtils.d("initPermission");
        if (!hasPermissions()) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_PERMISSIONS_CODE);
        } else {
            // 开启预览
            UsbCameraManager.startUsbCameraPreview(CameraActivity.this);
        }
    }

    private boolean hasPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(CameraActivity.this, permission) == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }
}
```

#### 三、功能操作步骤

使用 HDMI - USB 转接线连接 C03 和机顶盒，完成 C03 到机顶盒的镜像传输。注：HDMI 端连接 C03，USB 端连接机顶盒。

![](https://img-blog.csdnimg.cn/de46ec0fb9c0400e84bdfb39ddd378d0.png?x-oss-process=image/watermark,type_d3F5LXplbmhlaQ,shadow_50,text_Q1NETiBAXzEzMjg=,size_13,color_FFFFFF,t_70,g_se,x_16#pic_center)

#### 四、完整 Demo 下载

```
https://github.com/hiwlzhou/ImageTransmission/tree/master/app_camera
https://github.com/hiwlzhou/ImageTransmission/tree/master/app_api
```

### 相关流程图

+ 手机投屏原理
+ USB 通讯流程
+ MediaCodec 原理

### Android 音视频开发

+ MediaRecoder：音视频录制的上层API，通过一些简单的配置，就可以直接录制音视频保存到指定的文件路径。
+ MediaPlayer：音视频播放的上层API，可以用来播放音频和视频文件，特别常用的API。
+ AudioRecord：音频录制的API，通过流的形式输出的音频数据是未经过编码的，也就是PCM原数据，无法直接使用播放器进行播放。
+ AudioTrack：音频播放的API，可以播放未经过编码的的PCM原始音频数据。
+ MediaCodec：音视频的编码和解码器，应该是这个系列最重要的API了，为了配合他的使用，还得用到MediaFormat等其他的API。
+ MediaExtractor：音视频分提取器，例如把某视频文件中的音频提取出来保存成一个音频文件。
+ MediaMuxer：音视频合成器，视频音频合成，视频合成等等，常用与MediaExtractor一起使用。

### 参考

+ Android USB通讯(完整版)：https://blog.csdn.net/yaohui_/article/details/62435460?spm=1001.2014.3001.5502
+ USB 主机和配件概览：https://developer.android.com/guide/topics/connectivity/usb
+ 音视频系列--MediaProjection+MediaCodec制作简单投屏效果：https://blog.csdn.net/qq_18242391/article/details/111566304
+ 手机投屏H265，H264硬编码，局域网socket通信实现。：https://my.oschina.net/zemingzeng/blog/4817148
+ Android Camera1 教程 · 第一章 · 开启相机：https://www.jianshu.com/p/3440d82545f6
+ Android Camera1 教程 · 第二章 · 预览：https://www.jianshu.com/p/705d4792e836
+ Android：Camera2的简单使用：https://www.cnblogs.com/davidFB/p/15090897.html
+ android音视频知识一（AudioRecord与AudioTrack）：https://zhuanlan.zhihu.com/p/91139984

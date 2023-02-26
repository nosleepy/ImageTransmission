package com.android.sdk;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class AccessoryService extends Service implements UsbAccessoryReceiver.UsbAccessoryChangeListener {

    public static final String TAG = "wlzhou";
    public static final String ACTION_USB_PERMISSION_ACCESSORY = "com.android.accessory.USB_PERMISSION";

    private UsbAccessoryReceiver usbAccessoryReceiver;

    private volatile boolean connected_accessory;
    private static volatile boolean start;

    private UsbManager usbManager;

    private ParcelFileDescriptor parcelFileDescriptor;
    private FileInputStream fileInputStream;
    private FileOutputStream fileOutputStream;

    private MediaProjectionManager mediaProjectionManager;
    public static MediaProjection mediaProjection;
    private MediaCodec mediaCodec;
    private VirtualDisplay virtualDisplay;
    private byte[] key_config_bytes;
    private static final byte H264_KEY_FRAME_TYPE = 5;
    private static final byte H264_KEY_CONFIG_TYPE = 7;

    private static final int width = 640;
    private static final int height = 800;

    public static volatile Activity activity;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.d(TAG, "msg.what = " + msg.what);
            switch (msg.what) {
                case 200:
                    String content = (String) msg.obj;
                    Toast.makeText(AccessoryService.this, content, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    private void init() {
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
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
        Log.d(TAG, "[onBind]");
        AccessoryBinder testBinder = new AccessoryBinder();
        return testBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "[onUnbind]");
        return super.onUnbind(intent);
    }

    class AccessoryBinder extends IAccessoryService.Stub {

        @Override
        public void registerUsbAccessoryReceiver() throws RemoteException {
            if (usbAccessoryReceiver == null) {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
                intentFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
                intentFilter.addAction(ACTION_USB_PERMISSION_ACCESSORY);
                usbAccessoryReceiver = new UsbAccessoryReceiver(AccessoryService.this);
                registerReceiver(usbAccessoryReceiver, intentFilter);
                Toast.makeText(AccessoryService.this, "accessory register success", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void unregisterUsbAccessoryReceiver() throws RemoteException {
            unregisterReceiver(usbAccessoryReceiver);
        }

        @Override
        public void getAccessoryList() throws RemoteException {
            UsbAccessory[] accessories = usbManager.getAccessoryList();
            if (accessories != null) {
                for (UsbAccessory accessory : accessories) {
                    onUsbAccessoryAttached(accessory);
                }
            } else {
                Toast.makeText(AccessoryService.this, "accessories is null", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void transfer(String content) throws RemoteException {
            content = "accessory:" + content;
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.write(content.getBytes());
                    Log.d(TAG, "[transfer] content = " + content);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Log.d(TAG, "[transfer] fileOutputStream is null");
                Toast.makeText(AccessoryService.this, "fileOutputStream is null", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void createScreenCapture() throws RemoteException {
            Log.d(TAG, "[createScreenCapture]");
            mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            Intent intent = mediaProjectionManager.createScreenCaptureIntent();
            activity.startActivityForResult(intent, 100);
        }

        @Override
        public void startProjectionScreen() throws RemoteException {
            Log.d(TAG, "[startProjectionScreen]");
            // h264协议
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 60);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            // 初始化视频编码器
            try {
                mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            // surface通过MediaCodec提供
            Surface surface = mediaCodec.createInputSurface();
            // 创建场地
            virtualDisplay = mediaProjection.createVirtualDisplay(
                    "virtualDisplay", width, height, 1, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
                    , surface, null, null);
            mediaCodec.start();
            Log.d(TAG, "mediaCodec start");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    start = true;
                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    while (start) {
                        try {
                            // 获取输出队列的一个缓存区的索引，并将格式信息保存在info中
                            int outputBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
                            if (outputBufferId >= 0) {
                                Log.d(TAG, "[startProjectionScreen] outputBufferId = " + outputBufferId);
                                // 获取输出队列的一个缓存区
                                // 获取编码好的H264的数据
                                ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferId);
                                // 处理数据
                                dealBuffer(outputBuffer, bufferInfo);
                                // 清除index指向的缓存区中的数据
                                mediaCodec.releaseOutputBuffer(outputBufferId, false);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.d(TAG, "mediaCodec exception");
                            start = false;
                        }
                    }
                    Log.d(TAG, "mediaCodec stop -> exception");
                }
            }).start();
        }

        @Override
        public void stopProjectionScreen() throws RemoteException {
            start = false;
            // 结束录屏
            mediaProjection.stop();
            if (mediaCodec != null) {
                mediaCodec.stop();
                mediaCodec.release();
            }
            Log.d(TAG, "mediaCodec stop -> normal");
        }
    }

    private void dealBuffer(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo) {
        byte[] bytes = new byte[bufferInfo.size];
        outputBuffer.get(bytes);
        int typeByteOffset = 4;
        if (outputBuffer.get(2) == 0x01)
            typeByteOffset = 3;
        int typeByte = outputBuffer.get(typeByteOffset);

        byte[] temp = null;
        int type = typeByte & 0x1f;
        if (type == H264_KEY_CONFIG_TYPE) {
            key_config_bytes = bytes;
        } else if (type == H264_KEY_FRAME_TYPE) {
            temp = bytes;
            bytes = new byte[key_config_bytes.length + bytes.length];
            System.arraycopy(key_config_bytes, 0, bytes, 0, key_config_bytes.length);
            System.arraycopy(temp, 0, bytes, key_config_bytes.length, temp.length);
        }

        // 推送编码好的数据
        try {
            Log.d(TAG, "[dealBuffer] set data = " + Arrays.toString(bytes));
            fileOutputStream.write(bytes);
            Log.d(TAG, "[dealBuffer] write success");
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "[dealBuffer] fileOutputStream is exception");
        }
    }

    @Override
    public void onUsbAccessoryAttached(UsbAccessory accessory) {
        Log.d(TAG, "[onUsbAccessoryAttached] accessory = " + accessory.getModel() + ",connected = " + connected_accessory);
        if (!connected_accessory) {
            connectAccessory(accessory);
        }
    }

    private void connectAccessory(UsbAccessory accessory) {
        Log.d(TAG, "[connectAccessory] accessory = " + accessory.getModel() + ",connected = " + connected_accessory);
        if (connected_accessory) {
            disconnectAccessory();
        }

        if (!usbManager.hasPermission(accessory)) {
            Log.d(TAG, "[connectAccessory] requestPermission");
            Intent intent = new Intent(AccessoryService.ACTION_USB_PERMISSION_ACCESSORY);
            intent.setPackage(getPackageName());
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
            usbManager.requestPermission(accessory, pendingIntent);
            return;
        } else {
            Log.d(TAG, "[connectAccessory] hasPermission");
        }

        parcelFileDescriptor = usbManager.openAccessory(accessory);

        if (parcelFileDescriptor == null) {
            Toast.makeText(AccessoryService.this, "parcelFileDescriptor is null", Toast.LENGTH_SHORT).show();
            return;
        }

        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        fileInputStream = new FileInputStream(fileDescriptor);
        fileOutputStream = new FileOutputStream(fileDescriptor);

        if (fileInputStream != null && fileOutputStream != null) {
            connected_accessory = true;
            Log.d(TAG, "[connectAccessory] connect accessory success!!!");
            Toast.makeText(AccessoryService.this, "connect accessory success!!!", Toast.LENGTH_SHORT).show();
            parseHostData();
        } else {
            Log.d(TAG, "[connectAccessory] connect accessory fail!!!");
            Toast.makeText(AccessoryService.this, "connect accessory fail!!!", Toast.LENGTH_SHORT).show();
        }
    }

    private void parseHostData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] bytes = new byte[16384];
                int i = 0;
                while (i >= 0) {
                    try {
                        i = fileInputStream.read(bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                    if (i > 0) {
                        Message message = handler.obtainMessage(200, new String(bytes, 0, i));
                        handler.sendMessage(message);
                        Log.d(TAG, "[parseHostData] sendMessage");
                    }
                }
            }
        }).start();
    }

    private void disconnectAccessory() {
        connected_accessory = false;
        start = false;
        if (parcelFileDescriptor != null) {
            try {
                parcelFileDescriptor.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (fileInputStream != null) {
            try {
                fileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (fileOutputStream != null) {
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mediaCodec.stop();
        mediaCodec.release();
    }

    @Override
    public void onUsbAccessoryDetached(UsbAccessory accessory) {
        Log.d(TAG, "[onUsbAccessoryDetached] accessory = " + accessory.getModel());
        if (connected_accessory) {
            disconnectAccessory();
        }
    }
}
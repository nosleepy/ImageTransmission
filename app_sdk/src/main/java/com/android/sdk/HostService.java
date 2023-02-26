package com.android.sdk;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

public class HostService extends Service implements UsbDeviceReceiver.UsbDeviceChangeListener {

    public static final String TAG = "wlzhou";
    public static final String ACTION_USB_PERMISSION_HOST = "com.android.host.USB_PERMISSION";

    public static volatile Activity activity;

    private UsbDeviceReceiver usbDeviceReceiver;

    private volatile boolean connected_device;
    private static volatile boolean start;

    private UsbManager usbManager;
    private UsbDeviceConnection usbDeviceConnection;
    private UsbInterface usbInterface;
    private UsbEndpoint usbEndpointIn;
    private UsbEndpoint usbEndpointOut;

    private static final int width = 640;
    private static final int height = 800;

    public static volatile Surface surface;
    private MediaCodec mediaCodec;

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.d(TAG, "[handleMessage] msg.what = " + msg.what);
            switch (msg.what) {
                case 100:
                    String content = (String) msg.obj;
                    Toast.makeText(HostService.this, content, Toast.LENGTH_SHORT).show();
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
        usbManager = (UsbManager)getSystemService(Context.USB_SERVICE);
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
        Log.d(TAG, "[HostService] onBind");
        HostBinder hostBinder = new HostBinder();
        return hostBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "[HostService] onUnbind");
        return super.onUnbind(intent);
    }

    class HostBinder extends IHostService.Stub {

        @Override
        public void registerUsbDeviceReceiver() throws RemoteException {
            if (usbDeviceReceiver == null) {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
                intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
                intentFilter.addAction(ACTION_USB_PERMISSION_HOST);
                usbDeviceReceiver = new UsbDeviceReceiver(HostService.this);
                registerReceiver(usbDeviceReceiver, intentFilter);
                Log.d(TAG, "[registerUsbDeviceReceiver]");
                Toast.makeText(HostService.this, "host register success", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void unregisterUsbDeviceReceiver() throws RemoteException {
            Log.d(TAG, "[unregisterUsbDeviceReceiver]");
            unregisterReceiver(usbDeviceReceiver);
        }

        @Override
        public void getDeviceList() throws RemoteException {
            Map<String, UsbDevice> devices = usbManager.getDeviceList();
            if (devices != null) {
                for (UsbDevice device : devices.values()) {
                    onUsbDeviceAttached(device);
                }
            } else {
                Log.d(TAG, "[getDeviceList] devices is null");
                Toast.makeText(HostService.this, "devices is null", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void transfer(String content) throws RemoteException {
            content = "host:" + content;
            if (usbDeviceConnection != null && usbEndpointOut != null) {
                int i = usbDeviceConnection.bulkTransfer(usbEndpointOut, content.getBytes(), content.getBytes().length, 3000);
                if (i > 0) {
                    Log.d(TAG, "[transfer] content = " + content);
                }
            } else {
                Log.d(TAG, "[transfer] usbDeviceConnection or usbEndpointOut is null");
                Toast.makeText(HostService.this, "usbDeviceConnection or usbEndpointOut is null", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void startMediaCodec() throws RemoteException {
            // h264协议
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 60);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            // 初始化视频解码器
            try {
                mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mediaCodec.configure(mediaFormat, surface, null, 0);
            mediaCodec.start();
        }

        @Override
        public void stopMediaCodec() throws RemoteException {
            if (mediaCodec != null) {
                mediaCodec.stop();
                mediaCodec.release();
            }
        }
    }


    @Override
    public void onUsbDeviceAttached(UsbDevice device) {
        Log.d(TAG, "[onUsbDeviceAttached] device = " + device.getDeviceName() + ",connected = " + connected_device);
        if (!connected_device) {
            connectDevice(device);
        }
    }

    private void connectDevice(UsbDevice device) {
        Log.d(TAG, "[connectDevice] device = " + device.getDeviceName() + ",connected = " + connected_device);
        if (connected_device) {
            disconnectDevice(device);
        }

        if (!usbManager.hasPermission(device)) {
            Log.d(TAG, "[connect] requestPermission");
            Intent intent = new Intent(ACTION_USB_PERMISSION_HOST);
            intent.setPackage(getPackageName());
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
            usbManager.requestPermission(device, pendingIntent);
            return;
        } else {
            Log.d(TAG, "[connect] hasPermission");
        }

        usbDeviceConnection = usbManager.openDevice(device);
        if (usbDeviceConnection != null) {
            enterAccessoryMode();
            Log.d(TAG, "[connect] enterAccessoryMode");

            usbInterface = device.getInterface(0);
            int endpointCount = usbInterface.getEndpointCount();
            for (int i = 0; i < endpointCount; i++) {
                UsbEndpoint usbEndpoint = usbInterface.getEndpoint(i);
                if (usbEndpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (usbEndpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                        usbEndpointOut = usbEndpoint;
                        Log.d(TAG, "[connect] usbEndpointOut init");
                    } else if (usbEndpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                        usbEndpointIn = usbEndpoint;
                        Log.d(TAG, "[connect] usbEndpointIn init");
                    }
                }
            }
        }

        if (usbEndpointIn != null && usbEndpointOut != null) {
            connected_device = true;
            Toast.makeText(HostService.this, "connect device success!!!", Toast.LENGTH_SHORT).show();
            parseAccessoryData();
        } else {
            Toast.makeText(HostService.this, "connect device fail!!!", Toast.LENGTH_SHORT).show();
        }
    }

    private void parseAccessoryData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                start = true;
                while (start) {
                    try {
                        byte[] bytes = new byte[16384];
                        int i = usbDeviceConnection.bulkTransfer(usbEndpointIn, bytes, bytes.length, 3000);
                        if (i > 0) {
                            // 消息发送
                            String content = new String(bytes, 0, i);
                            if (content.startsWith("accessory:")) {
                                Message message = handler.obtainMessage(100, content);
                                handler.sendMessage(message);
                            }

                            // 进行解码
                            // 获取输入队列的一个空闲索引
                            int index = mediaCodec.dequeueInputBuffer(10000);
                            Log.d(TAG, "[dequeueInputBuffer] index = " + index);
                            if (index >= 0) {
                                // 获取输入队列的一个空闲缓存区
                                ByteBuffer buffer = mediaCodec.getInputBuffer(index);
                                buffer.clear();
                                buffer.put(bytes, 0, bytes.length);
                                // 提醒解码器或编码器处理数据
                                mediaCodec.queueInputBuffer(index, 0, bytes.length, System.currentTimeMillis(), 0);
                            }

                            // 获取数据
                            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                            // 获取输出队列的一个缓存区的索引，并将格式信息保存在info中
                            int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
                            while (outputBufferIndex >= 0) {
                                // 清除index指向的缓存区中的数据
                                // true的时候，解码的数据直接渲染到Surface
                                mediaCodec.releaseOutputBuffer(outputBufferIndex, true);
                                // 查询是否还有数据，因为服务端传输的时候I帧前面，拼接了vps帧
                                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        start = false;
                        Log.d(TAG, "mediaCodec stop -> exception");
                    }
                }
            }
        }).start();
    }

    private void enterAccessoryMode() {
        Log.d(TAG, "[enterAccessoryMode]");
        // 根据AOA协议打开Accessory模式
        initStringControlTransfer(usbDeviceConnection, 0, "Android"); // MANUFACTURER
        initStringControlTransfer(usbDeviceConnection, 1, "Host"); // MODEL
        initStringControlTransfer(usbDeviceConnection, 2, "HostDemo"); // DESCRIPTION
        initStringControlTransfer(usbDeviceConnection, 3, "1.0"); // VERSION
        initStringControlTransfer(usbDeviceConnection, 4, "http://www.android.com/"); // URI
        initStringControlTransfer(usbDeviceConnection, 5, "0000000012345678"); // SERIAL
        usbDeviceConnection.controlTransfer(0x40, 53, 0, 0, new byte[]{}, 0, 100);
    }

    private void initStringControlTransfer(UsbDeviceConnection deviceConnection, int index, String string) {
        deviceConnection.controlTransfer(0x40, 52, 0, index, string.getBytes(), string.length(), 100);
    }

    private void disconnectDevice(UsbDevice device) {
        Log.d(TAG, "disconnectDevice");
        connected_device = false;
        if (usbDeviceConnection != null) {
            usbDeviceConnection.releaseInterface(usbInterface);
            usbDeviceConnection.close();
            usbDeviceConnection = null;
        }
        usbEndpointIn = null;
        usbEndpointOut = null;
    }

    @Override
    public void onUsbDeviceDetached(UsbDevice device) {
        if (connected_device) {
            disconnectDevice(device);
        }
    }
}
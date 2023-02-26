package com.android.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

public class UsbDeviceReceiver extends BroadcastReceiver {

    private static final String TAG = "wlzhou";

    private UsbDeviceChangeListener usbDeviceChangeListener;

    public UsbDeviceReceiver(UsbDeviceChangeListener usbDeviceChangeListener) {
        this.usbDeviceChangeListener = usbDeviceChangeListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (device != null) {
            String action = intent.getAction();
            Log.d(TAG, "[onReceive] action = " + action);
            if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                usbDeviceChangeListener.onUsbDeviceAttached(device);
            } else if (action.equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                usbDeviceChangeListener.onUsbDeviceDetached(device);
            } else if (action.equals(HostService.ACTION_USB_PERMISSION_HOST)) {
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    Log.d(TAG, "[onReceive] Device permission granted: " + device);
                    usbDeviceChangeListener.onUsbDeviceAttached(device);
                } else {
                    Log.d(TAG, "[onReceive] Device permission denied: " + device);
                }
            }
        }
    }

    public interface UsbDeviceChangeListener {

        // 连接UsbDevice
        void onUsbDeviceAttached(UsbDevice device);

        // 断开UsbDevice
        void onUsbDeviceDetached(UsbDevice device);
    }
}
package com.android.usbcamera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.android.usbcamera.utils.LogUtils;

public class UsbDeviceReceiver extends BroadcastReceiver {

    private UsbDeviceChangeListener usbDeviceChangeListener;

    public UsbDeviceReceiver(UsbDeviceChangeListener usbDeviceChangeListener) {
        this.usbDeviceChangeListener = usbDeviceChangeListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (device != null) {
            String action = intent.getAction();
            LogUtils.d("action = " + action);
            LogUtils.d("device = " + device);
            if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                usbDeviceChangeListener.onUsbDeviceAttached(device);
            } else if (action.equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                usbDeviceChangeListener.onUsbDeviceDetached(device);
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
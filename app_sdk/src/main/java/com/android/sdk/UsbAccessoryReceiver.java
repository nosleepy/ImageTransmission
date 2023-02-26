package com.android.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.util.Log;

public class UsbAccessoryReceiver extends BroadcastReceiver {

    private static final String TAG = "wlzhou";

    private UsbAccessoryChangeListener usbAccessoryChangeListener;

    public UsbAccessoryReceiver(UsbAccessoryChangeListener usbAccessoryChangeListener) {
        this.usbAccessoryChangeListener = usbAccessoryChangeListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        UsbAccessory accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
        if (accessory != null) {
            String action = intent.getAction();
            Log.d(TAG, "[onReceive] action = " + action);
            if (action.equals(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)) {
                usbAccessoryChangeListener.onUsbAccessoryAttached(accessory);
            } else if (action.equals(UsbManager.ACTION_USB_ACCESSORY_DETACHED)) {
                usbAccessoryChangeListener.onUsbAccessoryDetached(accessory);
            } else if (action.equals(AccessoryService.ACTION_USB_PERMISSION_ACCESSORY)) {
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    Log.d(TAG, "[onReceive] Accessory permission granted: " + accessory);
                    usbAccessoryChangeListener.onUsbAccessoryAttached(accessory);
                } else {
                    Log.d(TAG, "[onReceive] Accessory permission denied: " + accessory);
                }
            }
        }
    }

    public interface UsbAccessoryChangeListener {

        // 连接UsbAccessory
        void onUsbAccessoryAttached(UsbAccessory accessory);

        // 断开UsbAccessory
        void onUsbAccessoryDetached(UsbAccessory accessory);
    }
}

package com.android.sdk;

interface IHostService {

    void registerUsbDeviceReceiver();

    void unregisterUsbDeviceReceiver();

    void getDeviceList();

    void transfer(String content);

    void startMediaCodec();

    void stopMediaCodec();
}
package com.android.sdk;

interface IAccessoryService {

    void registerUsbAccessoryReceiver();

    void unregisterUsbAccessoryReceiver();

    void getAccessoryList();

    void transfer(String content);

    void createScreenCapture();

    void startProjectionScreen();

    void stopProjectionScreen();
}
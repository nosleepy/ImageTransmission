package com.android.sdk;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;

public class TestService extends Service {

    private static final String TAG = "wlzhou";

    @Override
    public void onCreate() {
        super.onCreate();
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
        Log.d(TAG, "[TestService] onBind");
        TestBinder testBinder = new TestBinder();
        return testBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "[TestService] onUnbind");
        return super.onUnbind(intent);
    }

    class TestBinder extends ITestService.Stub {

        @Override
        public String getMsg() throws RemoteException {
            return "grandstream";
        }
    }
}
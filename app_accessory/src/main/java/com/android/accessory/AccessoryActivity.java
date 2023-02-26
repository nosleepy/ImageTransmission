package com.android.accessory;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.sdk.AccessoryService;
import com.android.sdk.IAccessoryService;

public class AccessoryActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "wlzhou";

    private IAccessoryService accessoryService;

    private Button connectHost;
    private Button send;
    private EditText et;
    private Button start;
    private Button stop;
    private TextView info;

    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "[onCreate]");
        setContentView(R.layout.activity_accessory);
        bindAccessoryService();
        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "[onStart]");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "[onResume]");
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "[onPause]");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "[onStop]");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "[onDestroy]");
        super.onDestroy();
        Intent intent = new Intent(this, AccessoryService.class);
        unbindService(accessoryServiceConnection);
        stopService(intent);
    }

    @Override
    public void finish() {
        Log.d(TAG, "[finish]");
        moveTaskToBack(true);//设置该activity永不过期，即不执行onDestroy()
    }

    private void init() {
        connectHost = (Button) findViewById(R.id.connectHost);
        connectHost.setOnClickListener(this);
        send = (Button) findViewById(R.id.send);
        send.setOnClickListener(this);
        et = (EditText) findViewById(R.id.et);
        start = (Button) findViewById(R.id.start);
        start.setOnClickListener(this);
        stop = (Button) findViewById(R.id.stop);
        stop.setOnClickListener(this);
        info = (TextView) findViewById(R.id.info);
    }

    private void bindAccessoryService() {
        Intent intent = new Intent(this, AccessoryService.class);
        startService(intent);
        boolean b = bindService(intent, accessoryServiceConnection, BIND_AUTO_CREATE);
        Log.d(TAG, "[bindAccessoryService] isBind -> " + b);
    }

    private ServiceConnection accessoryServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "[onServiceConnected]");
            accessoryService = IAccessoryService.Stub.asInterface(iBinder);

            AccessoryService.activity = AccessoryActivity.this;

            // 服务绑定成功后监听广播
            try {
                accessoryService.registerUsbAccessoryReceiver();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "[onServiceDisconnected]");

            // 服务解绑后取消监听广播
            try {
                accessoryService.unregisterUsbAccessoryReceiver();
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            accessoryService = null;
        }
    };

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.connectHost:
                try {
                    accessoryService.getAccessoryList();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.send:
                try {
                    String content = et.getText().toString();
                    et.setText("");
                    accessoryService.transfer(content);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.start:
                try {
                    accessoryService.createScreenCapture();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.stop:
                try {
                    accessoryService.stopProjectionScreen();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "[onActivityResult] createScreenCapture");
        if (resultCode == RESULT_OK && requestCode == 100) {
            mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
            if (null != mediaProjection) {
                Log.d(TAG, "[onActivityResult] mediaProjection is not null");
                AccessoryService.mediaProjection = mediaProjection;
                try {
                    accessoryService.startProjectionScreen();
                    Log.d(TAG, "[onActivityResult] openProjectionScreen");
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void show(String content) {
        info.setText(content);
    }
}
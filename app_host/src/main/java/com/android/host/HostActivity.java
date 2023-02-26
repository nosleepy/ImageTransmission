package com.android.host;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.sdk.HostService;
import com.android.sdk.IHostService;

public class HostActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "wlzhou";

    private IHostService hostService;

    private Button connectAccessory;
    private Button send;
    private EditText et;
    private SurfaceView sv;
    private TextView info;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "[onCreate]");
        setContentView(R.layout.activity_host);
        bindHostService();
        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "[onStart]");
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "[onResume]");
        super.onResume();
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
        Intent intent = new Intent(this, HostService.class);
        unbindService(hostServiceConnection);
        stopService(intent);
    }

    private void init() {
        connectAccessory = (Button) findViewById(R.id.connectAccessory);
        connectAccessory.setOnClickListener(this);
        send = (Button) findViewById(R.id.send);
        send.setOnClickListener(this);
        et = (EditText) findViewById(R.id.et);
        sv = (SurfaceView) findViewById(R.id.sv);
        SurfaceHolder holder = sv.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Toast.makeText(getBaseContext(), "surfaceCreated", Toast.LENGTH_SHORT).show();
                HostService.surface = holder.getSurface();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
        info = (TextView) findViewById(R.id.info);
    }

    private void bindHostService() {
        Intent intent = new Intent(this, HostService.class);
        startService(intent);
        boolean b = bindService(intent, hostServiceConnection, BIND_AUTO_CREATE);
        Log.d(TAG, "[bindHostService] isBind -> " + b);
    }

    private ServiceConnection hostServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "[onServiceConnected]");
            hostService = IHostService.Stub.asInterface(iBinder);

            HostService.activity = HostActivity.this;

            // 服务绑定成功后监听广播
            try {
                hostService.registerUsbDeviceReceiver();
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            // 开始解码视频
            try {
                hostService.startMediaCodec();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected");

            // 服务解绑后取消监听广播
            try {
                hostService.unregisterUsbDeviceReceiver();
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            // 停止解码视频
            try {
                hostService.stopMediaCodec();
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            hostService = null;
        }
    };

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.connectAccessory:
                try {
                    hostService.getDeviceList();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.send:
                try {
                    String content = et.getText().toString();
                    et.setText("");
                    hostService.transfer(content);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    private void show(String content) {
        info.setText(content);
    }
}
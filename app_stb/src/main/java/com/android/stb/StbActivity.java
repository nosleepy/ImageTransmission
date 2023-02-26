package com.android.stb;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.usb.IStbService;
import com.android.usb.StbService;

public class StbActivity extends AppCompatActivity {

    private static final String TAG = "wlzhou";
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
    private static final int REQUEST_PERMISSIONS_CODE = 1;

    private TextureView textureView;
    private Button rotate;

    private IStbService stbService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "[onCreate]");
        super.onCreate(savedInstanceState);
        hideTitle();
        setContentView(R.layout.activity_stb);
        initView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "[onStart]");
        // 绑定服务
        bindStbService();
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
        try {
            stbService.stopC03Preview();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        // 解绑服务
        unBindStbService();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "[onDestroy]");
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean flag = true;
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == -1) {
                    flag = false;
                    break;
                }
            }
        }
        Log.d(TAG, "[onRequestPermissionsResult] flag = " + flag);
        if (flag) {
            try {
                stbService.startC03Preview();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(getBaseContext(), "no permission", Toast.LENGTH_SHORT).show();
        }
    }

    // 设置窗口没有标题
    private void hideTitle() {
        if (getSupportActionBar() != null){
            getSupportActionBar().hide();
        }
    }

    private void initView() {
        textureView = (TextureView) findViewById(R.id.tv);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
                StbService.setSurfaceTexture(surfaceTexture);
                StbService.setActivity(StbActivity.this);
                textureView.setScaleY(-1f);
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

            }
        });
        rotate = (Button) findViewById(R.id.rotate);
        rotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    stbService.rotateC03Preview();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void bindStbService() {
        Intent intent = new Intent(this, StbService.class);
        startService(intent);
        boolean b = bindService(intent, stbServiceConnection, BIND_AUTO_CREATE);
        Log.d(TAG, "[bindStbService] isBind -> " + b);
    }

    private void unBindStbService() {
        Intent intent = new Intent(this, StbService.class);
        unbindService(stbServiceConnection);
        stopService(intent);
    }

    private ServiceConnection stbServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "[onServiceConnected]");
            stbService = IStbService.Stub.asInterface(iBinder);

            if (!hasPermissions()) {
                requestPermissions(REQUIRED_PERMISSIONS, REQUEST_PERMISSIONS_CODE);
            } else {
                try {
                    stbService.startC03Preview();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected");
            stbService = null;
        }
    };

    private boolean hasPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }
}
package com.one.audio_recorder_testing;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    static final int PERMISSION_REQUEST_CODE = 444;
    static final int MEDIA_PROJECTION_REQUEST_CODE = 445;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO,Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
            Toast.makeText(this,"Some features are not Supported on this Device",Toast.LENGTH_LONG).show();
        }

        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startServiceRequest();
            }
        });

        findViewById(R.id.stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isCaptureServiceRunning()) {
                    stopForegroundService();
                }
            }
        });
    }
    void startServiceRequest(){
        MediaProjectionManager manager =(MediaProjectionManager) getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        startActivityForResult(manager.createScreenCaptureIntent(),
                MEDIA_PROJECTION_REQUEST_CODE);
    }
    void stopForegroundService(){
        Intent intent = new Intent(this, RecorderService.class);
        intent.setAction(RecorderService.RECORDER_SERVICE_STOP);
        Log.d("myTest", "stopForegroundService: Stopping ForegroundService");
        startService(intent);
    }

    private boolean isCaptureServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (RecorderService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(requestCode == MEDIA_PROJECTION_REQUEST_CODE){
            if(resultCode == Activity.RESULT_OK){

                Intent intent = new Intent(this,RecorderService.class);
                intent.setAction(RecorderService.RECORDER_SERVICE_START);
                intent.putExtra(RecorderService.INTENT_DATA,data);
                startForegroundService(intent);

            }else{
                Toast.makeText(this,"Media Projection Request is Denied",Toast.LENGTH_SHORT).show();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
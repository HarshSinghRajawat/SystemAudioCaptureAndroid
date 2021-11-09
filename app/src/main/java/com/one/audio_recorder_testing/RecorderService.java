package com.one.audio_recorder_testing;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

public class RecorderService extends Service {

    private MediaProjection mProjection;
    private MediaProjectionManager manager;
    private Recorder mRecorder;

    static final String RECORDER_SERVICE_START = "Start Recorder Foreground Service";
    static final String RECORDER_SERVICE_STOP = "Stop Recorder Foreground Service";
    static final String INTENT_DATA = "PARSE DATA";
    private final int SERVICE_ID = 555;
    private final String NOTIFICATION_CHANNEL_ID = "Capturing System Audio";
    private final String NOTIFICATION_CHANNEL_NAME = "Capturing System Audio";

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onCreate() {
        createNotificationChannel();
        startForeground(SERVICE_ID,
                new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID).build());
        super.onCreate();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    void createNotificationChannel(){
        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT);

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(intent != null) {
            switch (intent.getAction()) {
                case RECORDER_SERVICE_START:
                    Toast.makeText(this,"Audio Capture: Recording Started",Toast.LENGTH_SHORT).show();
                    manager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                    mProjection = manager.getMediaProjection(Activity.RESULT_OK,intent.getParcelableExtra(INTENT_DATA));

                    record();

                    return START_STICKY;

                case RECORDER_SERVICE_STOP:
                    if(mRecorder != null){
                        mRecorder.startProcessing();
                        mRecorder.stop();
                        mProjection.stop();
                    }

                    stopSelf();
                    return START_NOT_STICKY;
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this,"Audio Capture: Recording Stopped",Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    void record(){
        boolean isSupported;
        mRecorder = new Recorder();

        isSupported = mRecorder.start(this, mProjection);

        if(!isSupported){
            mProjection.stop();
            stopSelf();
        }

    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

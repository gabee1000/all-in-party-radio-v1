package com.gabee.allinpartyradio;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.util.Random;

/**
 * Created by gabee1000 on 2016. 08. 07..
 */
public class StreamService extends Service {
    // Binder given to clients
    public static boolean isMainActivityDestroyed = false;
    public static boolean isMainActivityStopped = false;
    private final IBinder mBinder = new LocalBinder();
    public static boolean isStreamServiceDestroyed = false;
    public static boolean loadingStream = false;
    public static MediaPlayer mMediaPlayer;
    private NotificationCompat.Builder mBuilder;
    private static final int PAUSE_CODE = 1;
    private static final int STOP_CODE = 2;
    private static final int START_CODE = 3;
    private static final int PLAY = 1;
    private static final int PAUSE = 0;
    private PendingIntent mPausePendingIntent;
    private PendingIntent mStopPendingIntent;
    private PendingIntent mStartPendingIntent;
    private NotificationClickReceiver myReceiver;
    private ServiceCallbacks serviceCallbacks;

    public void setCallbacks(ServiceCallbacks callbacks) {
        serviceCallbacks = callbacks;
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        StreamService getService() {
            // Return this instance of StreamService so clients can call public methods
            return StreamService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void pauseMediaPlayerService() {
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
            setStartOrPauseIconNotification(PLAY);
        }
    }

    public void startMediaPlayerService() {
        if (mMediaPlayer != null) {
            mMediaPlayer.start();
            setStartOrPauseIconNotification(PAUSE);
        }
    }

    @Override
    public void onCreate() {
        Log.i("StreamService=", "onCreate()");
        isStreamServiceDestroyed = false;
        myReceiver = new NotificationClickReceiver();
        Intent pauseIntent = new Intent("custom_pause");
        pauseIntent.putExtra("com.gabee.allinpartyradio.pause", PAUSE_CODE);
        mPausePendingIntent = PendingIntent.getBroadcast(this, PAUSE_CODE, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent stopIntent = new Intent("custom_stop");
        stopIntent.putExtra("com.gabee.allinpartyradio.stop", STOP_CODE);
        mStopPendingIntent = PendingIntent.getBroadcast(this, STOP_CODE, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent startIntent = new Intent("custom_start");
        startIntent.putExtra("com.gabee.allinpartyradio.start", START_CODE);
        mStartPendingIntent = PendingIntent.getBroadcast(this, START_CODE, startIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        IntentFilter filter = new IntentFilter();
        filter.addAction("custom_pause");
        filter.addAction("custom_start");
        filter.addAction("custom_stop");
        this.registerReceiver(myReceiver, filter, null, null);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isStreamServiceDestroyed = false;
        if (mMediaPlayer != null){
            removeNotification();
        }
        Log.i("StreamService=", "onStartCommand()");
        startMediaStream(MainActivity.CURRENT_STREAM);
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        Log.i("StreamService=", "onDestroy()");
        unregisterReceiver(myReceiver);
        super.onDestroy();
    }

    public void startMediaStream(int stream) {
        loadingStream = true;
        serviceCallbacks.changePlayPauseStopButtonClickable();
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = (MainActivity.CURRENT_STREAM == 0 ? "http://adas.allinparty.hu:8430/hq" : "http://adas.allinparty.hu:8430/classic");
                    mMediaPlayer.setDataSource(url);
                    mMediaPlayer.prepare(); // might take long! (for buffering, etc)
                    mMediaPlayer.start();
                    serviceCallbacks.changePlayPauseStopButtonClickable();
                    setStartOrPauseIconNotification(0);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    loadingStream = false;
                }
            }
        }).start();

    }

    public void releaseRadioResources() {
        mMediaPlayer.release();
        mMediaPlayer = null;
    }

    public class NotificationClickReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            switch (action) {
                case "custom_pause":
                    mMediaPlayer.pause();
                    setStartOrPauseIconNotification(PLAY);
                    serviceCallbacks.updateState();
                    break;
                case "custom_start":
                    if (mMediaPlayer == null) {
                        startMediaStream(MainActivity.CURRENT_STREAM);
                    } else {
                        mMediaPlayer.start();
                        setStartOrPauseIconNotification(PAUSE);
                    }
                    serviceCallbacks.updateState();
                    break;
                case "custom_stop":
                    removeNotification();
                    if (isMainActivityDestroyed || isMainActivityStopped) {
                        stopSelf();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public void setStartOrPauseIconNotification(int state) {
        int drawable = (state == PAUSE ? R.drawable.ic_pause_white_48dp : R.drawable.ic_play_arrow_white_48dp);
        String playOrPause = (state == PAUSE ? "Pause" : "Play");
        PendingIntent tempPendingIntent = (state == PAUSE ? mPausePendingIntent : mStartPendingIntent);

        /*Intent notificationIntent = new Intent(this, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(notificationIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT);*/
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 1234, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        mBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.mipmap.ic_launcher)
                .addAction(drawable, playOrPause, tempPendingIntent)
                .addAction(R.drawable.ic_stop_white_48dp, "Stop", mStopPendingIntent)
                .setStyle(new NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1))
                .setShowWhen(false)
                .setContentTitle("All in Party Radio")
                .setContentIntent(resultPendingIntent);

        startForeground(MainActivity.NOTIFICATION_ID, mBuilder.build());
    }

    public void removeNotification() {
        if (!MainActivity.areButtonsClickable) {
            serviceCallbacks.changePlayPauseStopButtonClickable();
        }

        stopForeground(true);
        releaseRadioResources();
        if (!MainActivity.dialogFragmentCalling) {
            serviceCallbacks.updateState();
            MainActivity.dialogFragmentCalling = false;
        }
    }

    public interface ServiceCallbacks {
        void updateState();

        void changePlayPauseStopButtonClickable();
    }


}

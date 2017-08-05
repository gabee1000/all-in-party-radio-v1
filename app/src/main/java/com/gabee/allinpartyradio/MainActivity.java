package com.gabee.allinpartyradio;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class MainActivity extends AppCompatActivity implements StreamChooseDialogFragment.StreamChooseDialogListener, View.OnClickListener, StreamService.ServiceCallbacks {
    Map<String, Integer> map = new HashMap<String, Integer>();
    StreamService mService;
    protected int test;
    public static boolean dialogFragmentCalling = false;
    public static boolean mBound = false;
    public static String PACKAGE_NAME;
    public static final String START_SERVICE = "start_service";
    public static final String STOP_SERVICE = "stop_service";
    public static final String PAUSE_ACTION = "pause_action";
    public static final int NOTIFICATION_ID = 1246;
    private StreamChooseDialogFragment mChooseDialogFragment;
    private Animation mAnimClick;
    private Animation mFadeOut;
    private Animation mFadeIn;
    private Animation mSlideIn;
    private Animation mSlideOut;
    private ImageButton mImageButtonPlayPause;
    private ImageButton mImageButtonStop;
    private ProgressBar mProgressCircle;
    private TextView mTxtStreamTitle;
    private ImageView mImageView;
    public static int CURRENT_STREAM = 0;
    private int PLAY_DRAWABLE = R.drawable.ic_play_arrow_white_48dp;
    private int PAUSE_DRAWABLE = R.drawable.ic_pause_white_48dp;
    public static boolean areButtonsClickable = true;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("TEST", test);
        Log.i("onSaveInstance()", Integer.toString(test));
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        int test2 = savedInstanceState.getInt("TEST");
        Log.i("onRestore()", Integer.toString(test2));
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState == null)
            test = 15;
        StreamService.isMainActivityDestroyed = false;
        super.onCreate(savedInstanceState);
        // Bind to LocalService
        Intent intent = new Intent(this, StreamService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        loadAnimations();
        setContentView(R.layout.activity_main);
        PACKAGE_NAME = getApplicationContext().getPackageName();
        mChooseDialogFragment = new StreamChooseDialogFragment();
        mImageButtonPlayPause = (ImageButton) findViewById(R.id.imageButtonPlayPause);
        mImageButtonStop = (ImageButton) findViewById(R.id.imageButtonStop);
        mImageButtonPlayPause.setOnClickListener(this);
        mImageButtonStop.setOnClickListener(this);
        mProgressCircle = (ProgressBar) findViewById(R.id.progressBar);
        mProgressCircle.setIndeterminate(true);
        mProgressCircle.setVisibility(View.GONE);
        mTxtStreamTitle = (TextView) findViewById(R.id.txtStreamTitle);
        mImageView = (ImageView) findViewById(R.id.imageView);
        map.put("play", R.drawable.ic_play_arrow_white_48dp);
        map.put("pause", R.drawable.ic_pause_white_48dp);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("MainActivity=", "onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        StreamService.isMainActivityStopped = true;
        if (StreamService.mMediaPlayer == null) {
            stopService(new Intent(this, StreamService.class));
        }
        Log.i("MainActivity=", "onStop()");
    }

    @Override
    protected void onStart() {
        super.onStart();
        StreamService.isMainActivityStopped = false;
        Log.i("MainActivity=", "onStart()");
        updateState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("MainActivity=", "onResume()");
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            StreamService.LocalBinder binder = (StreamService.LocalBinder) iBinder;
            mService = binder.getService();
            mBound = true;
            mService.setCallbacks(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBound = false;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.stream_chooseId:
                mChooseDialogFragment.show(getSupportFragmentManager(), "choose_stream");
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        StreamService.isMainActivityDestroyed = true;
        Log.i("MainActivity=", "onDestroy()");
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
        if (StreamService.mMediaPlayer == null) {
            stopService(new Intent(this, StreamService.class));
        }
        Log.i("onDestroy()", Integer.toString(test));
    }

    @Override
    public void onDialogListener(DialogFragment dialog, int which) {
        dialogFragmentCalling = true;
        CURRENT_STREAM = which;
        callService(which);
    }

    public void loadAnimations() {
        mAnimClick = AnimationUtils.loadAnimation(this, R.anim.anim_alpha);
        mFadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        mFadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        mSlideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);
        mSlideOut = AnimationUtils.loadAnimation(this, R.anim.slide_out);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imageButtonPlayPause:
                if (StreamService.mMediaPlayer == null) {
                    Log.i("info=", "MediaPlayer null");
                    mImageButtonPlayPause.setImageResource(PAUSE_DRAWABLE);
                    callService(CURRENT_STREAM);
                } else if (StreamService.mMediaPlayer.isPlaying()) {
                    Log.i("info=", "MediaPlayer running and playing");
                    mImageButtonPlayPause.setImageResource(PLAY_DRAWABLE);
                    mService.pauseMediaPlayerService();
                } else if (!StreamService.mMediaPlayer.isPlaying()) {
                    mImageButtonPlayPause.setImageResource(PAUSE_DRAWABLE);
                    mService.startMediaPlayerService();
                }
                mImageButtonPlayPause.startAnimation(mAnimClick);
                break;
            case R.id.imageButtonStop:
                mImageButtonStop.startAnimation(mAnimClick);
                if (StreamService.mMediaPlayer != null) {
                    mService.removeNotification();
                }
                break;
            default:
                return;
        }
    }

    private void callService(int stream) {
        Intent startIntent = new Intent(this, StreamService.class);
        startService(startIntent);
    }

    @Override
    public void updateState() {
        if (StreamService.mMediaPlayer == null) {
            mImageButtonPlayPause.setImageResource(PLAY_DRAWABLE);
        } else if (StreamService.mMediaPlayer.isPlaying()) {
            mImageButtonPlayPause.setImageResource(PAUSE_DRAWABLE);
        } else if (!StreamService.mMediaPlayer.isPlaying()) {
            mImageButtonPlayPause.setImageResource(PLAY_DRAWABLE);
        }
    }

    @Override
    public void changePlayPauseStopButtonClickable() {
        if (areButtonsClickable) {
            areButtonsClickable = false;
            mImageButtonPlayPause.setClickable(false);
            mImageButtonPlayPause.setAlpha(0.3f);
            mImageButtonStop.setClickable(false);
            mImageButtonStop.setAlpha(0.3f);
        } else if (!areButtonsClickable){
            areButtonsClickable = true;
            mImageButtonPlayPause.setClickable(true);
            mImageButtonPlayPause.setAlpha(1f);
            mImageButtonStop.setClickable(true);
            mImageButtonStop.setAlpha(1f);
        }
    }
}

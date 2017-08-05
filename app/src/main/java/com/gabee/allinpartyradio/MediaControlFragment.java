package com.gabee.allinpartyradio;

import android.app.Fragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MediaControlFragment extends Fragment implements View.OnClickListener {
    Map<String, Integer> map = new HashMap<String, Integer>();

    MediaPlayer mMediaPlayer;
    private Animation mAnimClick;
    private Animation mFadeOut;
    private Animation mFadeIn;
    private Animation mSlideIn;
    private Animation mSlideOut;

    private ImageButton mImageButtonPlayPause;
    private ImageButton mImageButtonStop;
    private int mCurrStream = 0;
    private ProgressBar mProgressCircle;
    private TextView mTxtStreamTitle;
    private View myFragment;
    private ImageView mImageView;

    private RemoteViews mRemoteViews;
    private NotificationCompat.Builder mBuilder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadAnimations();
    }

    public void loadAnimations() {
        mAnimClick = AnimationUtils.loadAnimation(getActivity(), R.anim.anim_alpha);
        mFadeOut = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_out);
        mFadeIn = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in);
        mSlideIn = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_in);
        mSlideOut = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_out);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        // Inflate the layout for this fragment
        myFragment = inflater.inflate(R.layout.media_control_fragment, container, false);
        mImageButtonPlayPause = (ImageButton) myFragment.findViewById(R.id.imageButtonPlayPause);
        mImageButtonStop = (ImageButton) myFragment.findViewById(R.id.imageButtonStop);
        mImageButtonPlayPause.setOnClickListener(this);
        mImageButtonStop.setOnClickListener(this);
        mProgressCircle = (ProgressBar) myFragment.findViewById(R.id.progressBar);
        mProgressCircle.setIndeterminate(true);
        mProgressCircle.setVisibility(View.GONE);
        mTxtStreamTitle = (TextView) myFragment.findViewById(R.id.txtStreamTitle);
        map.put("play", R.drawable.ic_play_arrow_white_48dp);
        map.put("pause", R.drawable.ic_pause_white_48dp);
        return myFragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mImageView = (ImageView) getActivity().findViewById(R.id.imageView);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imageButtonPlayPause:
                mImageButtonPlayPause.startAnimation(mAnimClick);
                this.startMediaStream(mCurrStream);
                break;
            case R.id.imageButtonStop:
                mImageButtonStop.startAnimation(mAnimClick);
                if (mMediaPlayer != null) {
                    releaseRadioResources();
                    Intent stopIntent = new Intent(getActivity(), StreamService.class);
                    stopIntent.setAction(MainActivity.STOP_SERVICE);
                    getActivity().startService(stopIntent);
                }
                break;
            default:
                return;
        }
    }

    public void startMediaStream(int mCurrStream) {
        // Destroying remaining stream if persist or pause it if param: mCurrStream == this.mCurrStream
        if (mMediaPlayer != null && this.mCurrStream != mCurrStream) {
            releaseRadioResources();
        }
        else if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            releaseRadioResources();
            return;
        }
        // Initializing new stream
        mImageButtonPlayPause.setImageResource(map.get("pause"));
        this.mCurrStream = mCurrStream;
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTxtStreamTitle.setVisibility(View.GONE);
                            mProgressCircle.setVisibility(View.VISIBLE);
                        }
                    });
                    String url = (getCurr() == 0 ? "http://adas.allinparty.hu:8430/hq" : "http://adas.allinparty.hu:8430/classic");
                    mMediaPlayer.setDataSource(url);
                    mMediaPlayer.prepare(); // might take long! (for buffering, etc)
                    mMediaPlayer.start();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTxtStreamTitle.setTextColor(Color.WHITE);
                            // Error occured if branch
                            if (!mMediaPlayer.isPlaying()) {
                                mTxtStreamTitle.setText("Hiba a rádió betöltésekor");
                                mTxtStreamTitle.setTextColor(Color.RED);
                                mImageButtonPlayPause.setImageResource(map.get("play"));
                            }
                            // MediaPlayer service started
                            if (getCurr() == 0 && mMediaPlayer.isPlaying())
                                mTxtStreamTitle.setText("Playing: All in Party Radio");
                            else if (getCurr() == 1 && mMediaPlayer.isPlaying())
                                mTxtStreamTitle.setText("Playing: All in Classic Radio");
                            mProgressCircle.setVisibility(View.GONE);
                            mTxtStreamTitle.setVisibility(View.VISIBLE);
                            if (mMediaPlayer.isPlaying())
                                mImageView.startAnimation(mFadeOut);
                            mTxtStreamTitle.startAnimation(mSlideIn);

                            Intent startIntent = new Intent(getActivity(), StreamService.class);
                            startIntent.setAction(MainActivity.START_SERVICE);
                            getActivity().startService(startIntent);

                            // with Custom Notification Layout
                            /*mRemoteViews = new RemoteViews(MainActivity.PACKAGE_NAME, R.layout.notification_player_layout);
                            mRemoteViews.setImageViewResource(R.id.imageViewNotification, R.mipmap.ic_launcher);
                            mRemoteViews.setImageViewResource(R.id.imageButtonNotificationPlayPause, R.drawable.ic_play_arrow_white_48dp);
                            mRemoteViews.setImageViewResource(R.id.imageButtonNotificationStop, R.drawable.ic_stop_white_48dp);
                            mRemoteViews.setTextViewText(R.id.textViewNotification, "All in Party Radio");
                            mBuilder = new NotificationCompat.Builder(getActivity())
                                    .setSmallIcon(R.mipmap.ic_launcher)
                                    .setContent(mRemoteViews);
                            Intent resultIntent = new Intent(getActivity(), MainActivity.class);
                            TaskStackBuilder stackBuilder = TaskStackBuilder.create(getActivity());
                            stackBuilder.addParentStack(MainActivity.class);
                            stackBuilder.addNextIntent(resultIntent);
                            PendingIntent resultPendingIntent =
                                    stackBuilder.getPendingIntent(
                                            0,
                                            PendingIntent.FLAG_UPDATE_CURRENT);
                            mBuilder.setContentIntent(resultPendingIntent);
                            NotificationManager mNotificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
                            mNotificationManager.notify(MainActivity.NOTIFICATION_ID, mBuilder.build());*/

                            /*mBuilder = new NotificationCompat.Builder(getActivity())
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                    .setOngoing(true)
                                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                                    .setSmallIcon(R.mipmap.ic_launcher)
                                    .addAction(R.drawable.ic_pause_white_48dp, "Pause", pausePendingIntent)
                                    .addAction(R.drawable.ic_stop_white_48dp, "Stop", stopPendingIntent)
                                    .setStyle(new NotificationCompat.MediaStyle()
                                            .setShowActionsInCompactView(1)
                                            .setMediaSession(mMediaSession.getSessionToken()))
                                    .setContentTitle("Wonderful music")
                                    .setContentText("My Awesome Band");
                            Intent resultIntent = new Intent(getActivity(), MainActivity.class);
                            TaskStackBuilder stackBuilder = TaskStackBuilder.create(getActivity());
                            stackBuilder.addParentStack(MainActivity.class);
                            stackBuilder.addNextIntent(resultIntent);
                            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                                            0,
                                            PendingIntent.FLAG_UPDATE_CURRENT);
                            mBuilder.setContentIntent(resultPendingIntent);
                            NotificationManager mNotificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
                            mNotificationManager.notify(MainActivity.NOTIFICATION_ID, mBuilder.build());*/
                        }
                    });
                }
            }
        }).start();
    }

    public void releaseRadioResources() {
        if (mMediaPlayer.isPlaying()) {
            mImageView.startAnimation(mFadeIn);
            mTxtStreamTitle.startAnimation(mSlideOut);
            mTxtStreamTitle.setVisibility(View.GONE);
        }
        mProgressCircle.setVisibility(View.GONE);
        mMediaPlayer.release();
        mMediaPlayer = null;
        mImageButtonPlayPause.setImageResource(map.get("play"));
    }

    public int getCurr() {
        return this.mCurrStream;
    }
}

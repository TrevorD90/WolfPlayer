package com.wolfbyte.wolfplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;

/**
 * Foreground "media playback" service. While it runs, Android keeps WolfPlayer alive with the
 * screen off, and it supplies the notification, lock-screen and Bluetooth/headphone controls.
 * The actual audio is still played by the web player; this service just relays button presses.
 */
public class PlaybackService extends Service {
    static volatile PlaybackService instance;

    private static final String CHANNEL = "wolfplayer_playback";
    private static final int NOTIF_ID = 1;
    private static final String ACT_PLAY = "com.wolfbyte.wolfplayer.PLAY";
    private static final String ACT_PAUSE = "com.wolfbyte.wolfplayer.PAUSE";
    private static final String ACT_NEXT = "com.wolfbyte.wolfplayer.NEXT";
    private static final String ACT_PREV = "com.wolfbyte.wolfplayer.PREV";

    private final Handler main = new Handler(Looper.getMainLooper());
    private MediaSession session;
    private String title = "WolfPlayer", artist = "";
    private boolean playing = false;
    private long durationMs = 0, positionMs = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Now playing", NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
        session = new MediaSession(this, "WolfPlayer");
        session.setCallback(new MediaSession.Callback() {
            @Override public void onPlay() { MediaControlsPlugin.send("play", null); }
            @Override public void onPause() { MediaControlsPlugin.send("pause", null); }
            @Override public void onStop() { MediaControlsPlugin.send("pause", null); }
            @Override public void onSkipToNext() { MediaControlsPlugin.send("next", null); }
            @Override public void onSkipToPrevious() { MediaControlsPlugin.send("prev", null); }
            @Override public void onSeekTo(long pos) { MediaControlsPlugin.send("seekto", pos / 1000.0); }
        });
        session.setActive(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACT_PLAY.equals(action)) MediaControlsPlugin.send("play", null);
        else if (ACT_PAUSE.equals(action)) MediaControlsPlugin.send("pause", null);
        else if (ACT_NEXT.equals(action)) MediaControlsPlugin.send("next", null);
        else if (ACT_PREV.equals(action)) MediaControlsPlugin.send("prev", null);
        else if (intent != null) {
            title = intent.getStringExtra("title");
            artist = intent.getStringExtra("artist");
            playing = intent.getBooleanExtra("playing", false);
            durationMs = intent.getLongExtra("duration", 0);
            positionMs = intent.getLongExtra("position", 0);
        }
        refresh(); // always call startForeground promptly after a start request
        return START_NOT_STICKY;
    }

    /** Update what's playing (called from the plugin while the service is already running). */
    void apply(String t, String a, boolean isPlaying, long dur, long pos) {
        main.post(() -> {
            title = t; artist = a; playing = isPlaying; durationMs = dur; positionMs = pos;
            refresh();
        });
    }

    private void refresh() {
        if (title == null) title = "WolfPlayer";
        if (artist == null) artist = "";
        MediaMetadata.Builder meta = new MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, artist);
        if (durationMs > 0) meta.putLong(MediaMetadata.METADATA_KEY_DURATION, durationMs);
        session.setMetadata(meta.build());
        session.setPlaybackState(new PlaybackState.Builder()
            .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE
                | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_SEEK_TO)
            .setState(playing ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED,
                positionMs, playing ? 1f : 0f, SystemClock.elapsedRealtime())
            .build());

        Notification n = buildNotification();
        if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        else startForeground(NOTIF_ID, n);
    }

    private PendingIntent serviceIntent(String action, int code) {
        Intent i = new Intent(this, PlaybackService.class).setAction(action);
        return PendingIntent.getService(this, code, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private Notification buildNotification() {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        Intent open = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPi = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        b.setSmallIcon(R.drawable.ic_stat_music)
            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
            .setContentTitle(title)
            .setContentText(artist)
            .setContentIntent(openPi)
            .setOngoing(playing)
            .setShowWhen(false)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(new Notification.Action.Builder(
                Icon.createWithResource(this, android.R.drawable.ic_media_previous), "Previous", serviceIntent(ACT_PREV, 1)).build())
            .addAction(playing
                ? new Notification.Action.Builder(Icon.createWithResource(this, android.R.drawable.ic_media_pause), "Pause", serviceIntent(ACT_PAUSE, 2)).build()
                : new Notification.Action.Builder(Icon.createWithResource(this, android.R.drawable.ic_media_play), "Play", serviceIntent(ACT_PLAY, 3)).build())
            .addAction(new Notification.Action.Builder(
                Icon.createWithResource(this, android.R.drawable.ic_media_next), "Next", serviceIntent(ACT_NEXT, 4)).build())
            .setStyle(new Notification.MediaStyle()
                .setMediaSession(session.getSessionToken())
                .setShowActionsInCompactView(0, 1, 2));
        return b.build();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // App swiped away from recents: the web player is gone, so stop too.
        stopSelf();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        instance = null;
        if (session != null) { session.setActive(false); session.release(); }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}

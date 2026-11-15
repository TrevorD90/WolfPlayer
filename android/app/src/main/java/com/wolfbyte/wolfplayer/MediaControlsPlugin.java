package com.wolfbyte.wolfplayer;

import android.content.Intent;

import androidx.core.content.ContextCompat;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/** Bridge between the web player and PlaybackService (keeps music alive with the screen off). */
@CapacitorPlugin(name = "MediaControls")
public class MediaControlsPlugin extends Plugin {
    private static MediaControlsPlugin instance;

    @Override
    public void load() { instance = this; }

    @PluginMethod
    public void update(PluginCall call) {
        String title = call.getString("title", "WolfPlayer");
        String artist = call.getString("artist", "");
        boolean playing = Boolean.TRUE.equals(call.getBoolean("playing", false));
        Double dur = call.getDouble("duration", 0.0);
        Double pos = call.getDouble("position", 0.0);
        long durationMs = dur == null || dur.isNaN() || dur.isInfinite() ? 0 : (long) (dur * 1000);
        long positionMs = pos == null || pos.isNaN() ? 0 : (long) (pos * 1000);

        PlaybackService running = PlaybackService.instance;
        if (running != null) {
            running.apply(title, artist, playing, durationMs, positionMs);
            call.resolve();
            return;
        }
        if (!playing) { call.resolve(); return; } // only start the service when music actually starts
        Intent i = new Intent(getContext(), PlaybackService.class);
        i.putExtra("title", title);
        i.putExtra("artist", artist);
        i.putExtra("playing", true);
        i.putExtra("duration", durationMs);
        i.putExtra("position", positionMs);
        try {
            ContextCompat.startForegroundService(getContext(), i);
            call.resolve();
        } catch (Exception e) {
            call.reject("Could not start playback service: " + e.getMessage());
        }
    }

    @PluginMethod
    public void stop(PluginCall call) {
        getContext().stopService(new Intent(getContext(), PlaybackService.class));
        call.resolve();
    }

    /** Called by PlaybackService when a notification, lock-screen or headphone button is pressed. */
    static void send(String action, Double positionSeconds) {
        MediaControlsPlugin p = instance;
        if (p == null) return;
        JSObject data = new JSObject();
        data.put("action", action);
        if (positionSeconds != null) data.put("position", positionSeconds);
        p.notifyListeners("action", data);
    }
}

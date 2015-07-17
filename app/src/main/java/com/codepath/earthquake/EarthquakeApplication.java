package com.codepath.earthquake;

import android.app.Application;
import android.content.Intent;
import android.os.Handler;

public class EarthquakeApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        final Handler handler = new Handler();
        poll(handler, 1000 * 60); // Better to use Alarm Manager
    }

    void poll(final Handler handler, final long pollDelay) {
        Intent i = new Intent(this, EarthquakeService.class);
        i.putExtra(EarthquakeService.KEY_URL, "http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_hour.geojson");
        startService(i);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                poll(handler, pollDelay);
            }
        }, pollDelay);
    }
}

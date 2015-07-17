package com.codepath.earthquake;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EarthquakeService extends IntentService {
    static final String TAG = "EarthquakeService";
    public static final String KEY_URL = "KeyUrl";
    public static final String KEY_RESULTS = "KeyResults";
    public static final String KEY_RESULT_CODE = "KeyResultCode";
    public static final String ACTION = "com.codepath.earthquake.EarthquakeService";

    public EarthquakeService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final String urlString = intent.getStringExtra(KEY_URL);
        if (urlString != null && urlString.length() > 0) {
            final String jsonString = getJSON(urlString, 1000 * 15);
            // Construct an Intent tying it to the ACTION (arbitrary event namespace)
            Intent in = new Intent(ACTION);
            // Put extras into the intent as usual
            in.putExtra(KEY_RESULT_CODE, Activity.RESULT_OK);
            in.putExtra(KEY_RESULTS, jsonString);
            // Fire the broadcast with intent packaged
            LocalBroadcastManager.getInstance(this).sendBroadcast(in);
        }
    }


    public String getJSON(String url, int timeout) {
        HttpURLConnection c = null;
        try {
            URL u = new URL(url);
            c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.setRequestProperty("Content-length", "0");
            c.setUseCaches(false);
            c.setAllowUserInteraction(false);
            c.setConnectTimeout(timeout);
            c.setReadTimeout(timeout);
            c.connect();
            int status = c.getResponseCode();

            switch (status) {
                case 200:
                case 201:
                    BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line+"\n");
                    }
                    br.close();
                    return sb.toString();
            }

        } catch (MalformedURLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception ex) {
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return null;
    }
}

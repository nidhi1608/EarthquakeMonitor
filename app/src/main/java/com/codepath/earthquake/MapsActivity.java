package com.codepath.earthquake;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;

import com.codepath.earthquake.model.Earthquake;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.JsonSyntaxException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;

public class MapsActivity extends FragmentActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private LinkedHashMap<String, Earthquake.Feature> mFeatures = new LinkedHashMap<>();
    final LatLngBounds.Builder mBoundsBuilder = new LatLngBounds.Builder();
    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
        mPrefs = getSharedPreferences("Earthquake_v1", Context.MODE_PRIVATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        IntentFilter filter = new IntentFilter(EarthquakeService.ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int resultCode = intent.getIntExtra(EarthquakeService.KEY_RESULT_CODE, RESULT_CANCELED);
            if (resultCode == RESULT_OK) {
                String resultValue = intent.getStringExtra(EarthquakeService.KEY_RESULTS);
                mPrefs.edit().putString(EarthquakeService.KEY_RESULTS, resultValue).apply();
            }
        }
    };

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void addMarker(final String title, final String snippet, final double mag, final String url, final double lat, final double lon) {
        final GoogleMap map = mMap;
        final LatLng latlng = new LatLng(lat, lon);
        mBoundsBuilder.include(latlng);
        final Bitmap icon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_marker).copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(icon);
        Paint paint = new Paint();
        final StringBuilder colorString = new StringBuilder("#FF");
        // 00 = 3.0f & above, FF = 1.0f and below
        if (mag >= 3.0f) {
            colorString.append("00");
        } else if (mag <= 1.0f) {
            colorString.append("FF");
        } else {
            double normalized = 1.0f - ((mag - 1.0f) / 2);
            int scale = (int)(normalized * 255.0f);
            colorString.append(String.format("%02X", scale));
        }
        colorString.append("00");
        int color = Color.parseColor(colorString.toString());
        paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
        canvas.drawBitmap(icon, 0f, 0f, paint);
        map.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(icon))
                .snippet(snippet.toString())
                .title(title)
                .position(latlng));
        map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
    }

    private void didUpdateData(final String jsonString) {
        if (jsonString == null || jsonString.length() == 0) return;
        try {
            boolean updateMapCamera = mFeatures.size() == 0;
            final Earthquake earthquake = Earthquake.GSON.fromJson(jsonString, Earthquake.class);
            if (earthquake != null) {
                if (earthquake.metadata != null && earthquake.features != null) {
                    setTitle(earthquake.metadata.title + " (" + earthquake.metadata.count  + ")");
                    for (final Earthquake.Feature feature : earthquake.features) {
                        final String id = feature.id;
                        if (!mFeatures.containsValue(id)) {
                            mFeatures.put(id, feature);
                            addMarker(id);
                        }
                    }
                }
            }
            if (updateMapCamera && mMap != null) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(mBoundsBuilder.build(), (int)(50 * Resources.getSystem().getDisplayMetrics().density)));
            }
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
    }

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat();
    private void addMarker(final String id) {
        final Earthquake.Feature feature = mFeatures.get(id);
        if (feature != null && feature.geometry != null && feature.geometry.coordinates.size() >= 2) {
            final double lon = feature.geometry.coordinates.get(0);
            final double lat = feature.geometry.coordinates.get(1);
            final String title = feature.properties.title;
            final Date date = new Date(feature.properties.time);
            final String snippet = DATE_FORMAT.format(date);
            addMarker(title, snippet, feature.properties.mag, feature.properties.url, lat, lon);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key != null && key.equals(EarthquakeService.KEY_RESULTS)) {
            final String jsonString = mPrefs.getString(EarthquakeService.KEY_RESULTS, null);
            didUpdateData(jsonString);
        }
    }
}

package com.example.universalseismiclogger.locationprovider;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.core.app.ActivityCompat;

import java.util.Calendar;

import static com.example.universalseismiclogger.shared.DefaultStrings.GPS_LOCATION;
import static com.example.universalseismiclogger.shared.DefaultStrings.GPS_LOCATION_DEFAULT;

public class GpsLocationProvider implements LocationListener {
    private LocationManager mLocationManager;
    private SharedPreferences config;
    private boolean locationGet = false;
    public Handler mHandler;

    private Context activityContext;

    public boolean IsLocationGet(){
        return locationGet;
    }

    public GpsLocationProvider(Context activityContext, SharedPreferences config) {
        locationGet = false;
        this.config = config;
        this.activityContext = activityContext;
        mLocationManager = (LocationManager) activityContext.getSystemService(Context.LOCATION_SERVICE);

    }

    public void getLocation(){
        locationGet = false;



        if (ActivityCompat.checkSelfPermission(activityContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(activityContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {


            Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            //if the recent location calculated earlier than 120 seconds ago
            if(location != null && location.getTime() > Calendar.getInstance().getTimeInMillis() - 2 * 60 * 1000) {
                SharedPreferences.Editor configEditor = config.edit();
                configEditor.putString(GPS_LOCATION, location.getLatitude() + " " + location.getLongitude());
                configEditor.apply();
                locationGet = true;
                Looper.myLooper().quitSafely();
            }
            else {
                SharedPreferences.Editor configEditor = config.edit();
                configEditor.putString(GPS_LOCATION, GPS_LOCATION_DEFAULT);
                configEditor.apply();
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            SharedPreferences.Editor configEditor = config.edit();
            configEditor.putString(GPS_LOCATION, location.getLatitude() + " " + location.getLongitude());
            configEditor.apply();

            mLocationManager.removeUpdates(this);
            locationGet = true;
            Looper.myLooper().quitSafely();
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }


}

package com.example.joelang.appforslack;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.repackaged.retrofit_v1_9_0.retrofit.http.POST;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.UUID;

public class BackgroundMonitoring extends Application {
    private BeaconManager beaconManager;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPref = this.getSharedPreferences("userinfo",Context.MODE_PRIVATE);
        editor= sharedPref.edit();


        beaconManager = new BeaconManager(getApplicationContext());
        beaconManager.setMonitoringListener(new BeaconManager.MonitoringListener() {
            @Override
            public void onEnteredRegion(Region region, List<Beacon> list) {
                //TO CHANGE & send info to BOT
                if (sharedPref.getBoolean("registered",false)) {
                    editor.putString("status","available");
                    editor.commit();
                    Thread thread = new Thread(new Runnable(){
                        @Override
                        public void run() {
                            try {
                                MainActivity.POST(sharedPref.getString("webhook", ""), "available");

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    thread.start();
                }
            }
            @Override
            public void onExitedRegion(Region region) {
                // TO SEND INFO TO BOT
                if (sharedPref.getBoolean("registered", false)) {
                    editor.putString("status","out of office");
                    editor.commit();
                    Thread thread = new Thread(new Runnable(){
                        @Override
                        public void run() {
                            try {
                                MainActivity.POST(sharedPref.getString("webhook", ""),"out of office");

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    thread.start();


                }
            }
            }
            );
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                if (sharedPref.getBoolean("registered", false)) {
                    beaconManager.startMonitoring(new Region(
                            "monitored region",
                            UUID.fromString("B9407F30-F5F8-466E-AFF9-25556B57FE6D"),
                            sharedPref.getInt("beaconmajor", 0), sharedPref.getInt("beaconminor", 0)));
                }
            }
        });
    }


}

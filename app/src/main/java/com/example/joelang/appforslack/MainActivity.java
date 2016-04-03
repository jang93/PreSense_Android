package com.example.joelang.appforslack;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.SystemRequirementsChecker;


import org.json.JSONException;
import org.json.JSONObject;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {
    private BeaconManager beaconManager;
    private Region region;
    private Beacon nearestBeacon=null;
    private EditText Name;
    private EditText Webhook;
    public static EditText Status;
    private SharedPreferences.Editor editor;
    private SharedPreferences sharedPref;
    private static final String TAG = "MyActivity";
    public static final JSONObject object = new JSONObject();
    private boolean firstStart=true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //shared preferences used to store user info
        Context context = this.getApplicationContext();
        sharedPref = context.getSharedPreferences("userinfo", Context.MODE_PRIVATE);
        Status = (EditText)findViewById(R.id.editText3);

        if (Status != null) {
            Status.setFocusable(false);
        }
        editor= sharedPref.edit();
        if(!sharedPref.contains("registered")){
            editor.putBoolean("registered", false);
            editor.commit();
            Status.setText("Unregistered");
        }
        else {
            Status.setText("Registered");
        }


        Webhook = (EditText)findViewById(R.id.editText2);
        // TO BE REMOVED
        if (sharedPref.contains("webhook")) {
            Webhook.setText(sharedPref.getString("webhook",""));
            try {
                object.put("webhook", sharedPref.getString("webhook", ""));
            }catch(JSONException e){
                e.printStackTrace();
            }
        }else{
            Webhook.setText("https://presensebot.herokuapp.com/hubot/notify/presensetest");
        }
        Name =(EditText)findViewById(R.id.editText);
        if (sharedPref.contains("user")){
            Name.setText(sharedPref.getString("user",""));
            try {
                object.put("user", sharedPref.getString("user", ""));
            }catch(JSONException e){
                e.printStackTrace();
            }
        }else{
            Name.setText("Name");
        }

        beaconManager = new BeaconManager(this);
        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, List<Beacon> list) {

                if (firstStart && sharedPref.getBoolean("registered", false)) {
                    final String currentStatus;
                    if (list.isEmpty()) {
                        editor.putString("status", "out of office");
                        currentStatus="out of office";
                        Status.setText("Out of office");
                    } else {
                        nearestBeacon = list.get(0);
                        if (sharedPref.getInt("beaconmajor", 0) == nearestBeacon.getMajor() && sharedPref.getInt("beaconminor", 0) == nearestBeacon.getMinor()) {
                            editor.putString("status", "available");
                            currentStatus="available";
                            Status.setText("Available");
                        } else {
                            editor.putString("status", "out of office");
                            currentStatus="out of office";
                            Status.setText("Out of office");
                        }
                    }
                    Thread thread = new Thread(new Runnable(){
                        @Override
                        public void run() {
                            try {
                                POST(sharedPref.getString("webhook", ""), currentStatus);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    thread.start();
                    editor.commit();
                }

                firstStart = false;
                if (!list.isEmpty()) {
                    nearestBeacon = list.get(0);
                    editor.putBoolean("inrange",true);
                    editor.commit();
                }
                else{
                    editor.putBoolean("inrange",false);
                    editor.commit();
                }
            }
        });
        region = new Region("ranged region",
                UUID.fromString("B9407F30-F5F8-466E-AFF9-25556B57FE6D"), null, null);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SystemRequirementsChecker.checkWithDefaultDialogs(this);

        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                beaconManager.startRanging(region);
            }
        });
    }

    @Override
    protected void onPause() {
        beaconManager.stopRanging(region);

        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void Register(View view){
        String name = Name.getText().toString();
        String webhook = Webhook.getText().toString();
        if (sharedPref.getBoolean("inrange",false)&&nearestBeacon!=null) {
            editor.putString("user", name);
            editor.putString("webhook", webhook);
            editor.putInt("beaconmajor", nearestBeacon.getMajor());
            editor.putInt("beaconminor", nearestBeacon.getMinor());
            editor.putString("status", "available");
            editor.commit();
            Status.setText("Available");

            try {
                object.put("user", name);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Thread thread = new Thread(new Runnable(){
                @Override
                public void run() {
                    try {
                        POST(sharedPref.getString("webhook", ""), "available");

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
            Toast.makeText(getBaseContext(), "Registered Successfully!", Toast.LENGTH_LONG).show();

        }
        else{
            Toast.makeText(getBaseContext(), "Error. No beacons are in range." , Toast.LENGTH_LONG).show();
        }
        editor.putBoolean("registered", true);
        editor.commit();
    }

    public void Available (View view){
        if (sharedPref.getBoolean("registered",false)&&sharedPref.contains("status"))
        {
            if(sharedPref.getString("status","").equalsIgnoreCase("out of office")){
                Toast.makeText(getBaseContext(), "Status can only be changed in office!" , Toast.LENGTH_LONG).show();
            }
            else {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            POST(sharedPref.getString("webhook", ""), "available");

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();
                Status.setText("Available");


                Toast.makeText(getBaseContext(), "Status is now set to: Available.", Toast.LENGTH_LONG).show();
            }
        }
        else{
            Toast.makeText(getBaseContext(), "Error. You must be registered first.", Toast.LENGTH_LONG).show();
        }


    }

    public void Busy (View view){
        if (sharedPref.getBoolean("registered",false)&&sharedPref.contains("status")) {
            if (sharedPref.getString("status", "").equalsIgnoreCase("out of office")) {
                Toast.makeText(getBaseContext(), "Status can only be changed in office!", Toast.LENGTH_LONG).show();
            } else {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String result = POST(sharedPref.getString("webhook", ""), "busy");

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();
                Status.setText("Busy");
                Toast.makeText(getBaseContext(), "Status is now set to: Busy.", Toast.LENGTH_LONG).show();
            }
        }
        else{
            Toast.makeText(getBaseContext(), "Error. You must be registered first.", Toast.LENGTH_LONG).show();
        }

    }

    public static String POST(String url, String text) {
        InputStream inputStream;
        String result="";
        try {
            object.put("status", text);
        }catch (JSONException e){
            e.printStackTrace();
        }
        try {

            // 1. create HttpClient
            HttpClient httpclient = new DefaultHttpClient();

            // 2. make POST request to the given URL
            HttpPost httpPost = new HttpPost(url);

            String json;

            // 4. convert JSONObject to JSON to String
            json = object.toString();


            // 5. set json to StringEntity
            StringEntity se = new StringEntity(json);

            // 6. set httpPost Entity
            httpPost.setEntity(se);

            // 7. Set some headers to inform server about the type of the content
           // httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");

            // 8. Execute POST request to the given URL
            HttpResponse httpResponse = httpclient.execute(httpPost);

            // 9. receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();

            // 10. convert inputstream to string
            if(inputStream != null)
                result = inputStream.toString();
            else
                result = "Registration failed!";

        } catch (Exception e) {
            Log.d("InputStream","error!!!" );
            e.printStackTrace();
        }

        // 11. return result
        return result;
    }



}

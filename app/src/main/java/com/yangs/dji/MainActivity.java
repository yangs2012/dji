package com.yangs.dji;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;

import com.yangs.dji.activity.RealtimeVideo;
import com.yangs.dji.activity.WaypointActivity;

import dji.common.product.Model;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

public class MainActivity extends Activity implements View.OnClickListener {
    private FlightController controller;
    private Button bt_waypoint;
    private Button bt_realtime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
                            Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                            Manifest.permission.READ_PHONE_STATE,
                    }, 13);
        }
        setContentView(R.layout.activity_main);
        bt_waypoint = (Button) findViewById(R.id.main_bt_waypoint);
        bt_realtime = (Button) findViewById(R.id.main_bt_realtime);
        bt_realtime.setOnClickListener(this);
        bt_waypoint.setOnClickListener(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(AppApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(receiver, filter);
    }

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BaseProduct product = AppApplication.getProduct();
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                controller = ((Aircraft) product).getFlightController();
            }
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.main_bt_waypoint:
                startActivity(new Intent(MainActivity.this, WaypointActivity.class));
                break;
            case R.id.main_bt_realtime:
                startActivity(new Intent(MainActivity.this, RealtimeVideo.class));
                break;
        }
    }
}


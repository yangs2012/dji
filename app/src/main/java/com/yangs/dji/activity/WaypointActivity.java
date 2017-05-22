package com.yangs.dji.activity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.yangs.dji.AppApplication;
import com.yangs.dji.R;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

/**
 * Created by yangs on 2017/5/18 0018.
 */

public class WaypointActivity extends Activity implements View.OnClickListener, AMap.OnMapClickListener {
    @BindView(R.id.waypoint_status_level)
    public TextView status_level;
    @BindView(R.id.waypoint_status_location)
    public TextView status_location;
    @BindView(R.id.waypoint_mapView)
    public MapView mapView;
    @BindView(R.id.waypoint_locate)
    public Button locate;
    @BindView(R.id.waypoint_config)
    public Button config;
    @BindView(R.id.waypoint_start)
    public Button start;
    private AMap aMap;
    private List<Waypoint> waypointList;
    private WaypointMission.Builder waypointMissionBuilder;
    private WaypointMissionOperator waypointMissionOperator;
    private FlightController flightController;
    private double airCraftLocationLa;
    private double airCraftLocationLong;
    private float mSpeed;
    private WaypointMissionFinishedAction mFinishedAction;
    private WaypointMissionHeadingMode mHeadingMode;
    private float altitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.waypoint);
        ButterKnife.bind(this);
        mapView.onCreate(savedInstanceState);
        aMap = mapView.getMap();
        waypointList = new ArrayList<Waypoint>();
        MyLocationStyle myLocationStyle;
        myLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_SHOW);
        aMap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
        aMap.getUiSettings().setMyLocationButtonEnabled(true);//设置默认定位按钮是否显示
        aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
        locate.setOnClickListener(this);
        config.setOnClickListener(this);
        start.setOnClickListener(this);
        aMap.setOnMapClickListener(this);
        if (AppApplication.getProduct() != null) {
            flightController = ((Aircraft) AppApplication.getProduct()).getFlightController();
            flightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(@NonNull FlightControllerState flightControllerState) {
                    airCraftLocationLa = flightControllerState.getAircraftLocation().getLatitude();
                    airCraftLocationLong = flightControllerState.getAircraftLocation().getLongitude();
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(new LatLng(airCraftLocationLa, airCraftLocationLong));
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                    aMap.addMarker(markerOptions);
                }
            });
        } else {
            AppApplication.showToast("异常,没有连接到无人机!");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.waypoint_config:
                showSettingDialog();
                break;
            case R.id.waypoint_start:
                if (waypointMissionBuilder == null)
                    waypointMissionBuilder = new WaypointMission.Builder();
                if (waypointMissionOperator == null)
                    waypointMissionOperator = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
                waypointMissionBuilder.finishedAction(mFinishedAction)
                        .headingMode(mHeadingMode)
                        .autoFlightSpeed(mSpeed)
                        .maxFlightSpeed(mSpeed)
                        .flightPathMode(WaypointMissionFlightPathMode.NORMAL);
                waypointMissionOperator.loadMission(waypointMissionBuilder.build());
                waypointMissionOperator.uploadMission(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError == null) {
                            AppApplication.showToast("上传任务成功!");
                            waypointMissionOperator.startMission(new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError == null)
                                        AppApplication.showToast("开始执行任务....");
                                    else
                                        AppApplication.showToast("执行任务出错!");
                                }
                            });
                        } else
                            AppApplication.showToast("上传任务失败!");
                    }
                });
                break;
            case R.id.waypoint_locate:
                airCraftLocationLa = flightController.getState().getAircraftLocation().getLatitude();
                airCraftLocationLong = flightController.getState().getAircraftLocation().getLongitude();
                status_level.setText("信号: " + flightController.getState().getGPSSignalLevel());
                status_location.setText("纬度:" + airCraftLocationLa + "  经度:" + airCraftLocationLong);
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(new LatLng(airCraftLocationLa, airCraftLocationLong));
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                aMap.addMarker(markerOptions);
                new AlertDialog.Builder(WaypointActivity.this).setTitle("提示")
                        .setMessage("采集成功,是否加入任务?").setNegativeButton("是", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Waypoint waypoint = new Waypoint(airCraftLocationLa, airCraftLocationLong, altitude);
                        waypointList.add(waypoint);
                        dialog.dismiss();
                    }
                }).setPositiveButton("否", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create().show();
                break;

        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        AppApplication.showToast(latLng.latitude + "   " + latLng.longitude);
    }

    private void showSettingDialog() {
        LinearLayout wayPointSettings = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_waypointsetting, null);
        final TextView wpAltitude_TV = (TextView) wayPointSettings.findViewById(R.id.flyconfig);
        RadioGroup speed_RG = (RadioGroup) wayPointSettings.findViewById(R.id.speed);
        RadioGroup actionAfterFinished_RG = (RadioGroup) wayPointSettings.findViewById(R.id.actionAfterFinished);
        RadioGroup heading_RG = (RadioGroup) wayPointSettings.findViewById(R.id.heading);
        speed_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.lowSpeed) {
                    mSpeed = 3.0f;
                } else if (checkedId == R.id.MidSpeed) {
                    mSpeed = 5.0f;
                } else if (checkedId == R.id.HighSpeed) {
                    mSpeed = 10.0f;
                }
            }

        });

        actionAfterFinished_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.finishNone) {
                    mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
                } else if (checkedId == R.id.finishGoHome) {
                    mFinishedAction = WaypointMissionFinishedAction.GO_HOME;
                } else if (checkedId == R.id.finishAutoLanding) {
                    mFinishedAction = WaypointMissionFinishedAction.AUTO_LAND;
                } else if (checkedId == R.id.finishToFirst) {
                    mFinishedAction = WaypointMissionFinishedAction.GO_FIRST_WAYPOINT;
                }
            }
        });

        heading_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.headingNext) {
                    mHeadingMode = WaypointMissionHeadingMode.AUTO;
                } else if (checkedId == R.id.headingInitDirec) {
                    mHeadingMode = WaypointMissionHeadingMode.USING_INITIAL_DIRECTION;
                } else if (checkedId == R.id.headingRC) {
                    mHeadingMode = WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER;
                } else if (checkedId == R.id.headingWP) {
                    mHeadingMode = WaypointMissionHeadingMode.USING_WAYPOINT_HEADING;
                }
            }
        });
        new AlertDialog.Builder(this).setTitle("").setView(wayPointSettings)
                .setPositiveButton("完成", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String flyconfig = wpAltitude_TV.getText().toString();
                        double la = Double.parseDouble(flyconfig.split(",")[0]);
                        double lon = Double.parseDouble(flyconfig.split(",")[1]);
                        float alt = Float.parseFloat(flyconfig.split(",")[2]);
                        Waypoint waypoint = new Waypoint(la, lon, alt);
                        waypointList.add(waypoint);
                    }

                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }

                }).create().show();
    }

}

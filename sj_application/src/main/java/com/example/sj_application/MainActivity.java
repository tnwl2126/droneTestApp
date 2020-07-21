package com.example.sj_application;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.Overlay;

import com.o3dr.android.client.Drone;

import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.companion.solo.SoloAttributes;
import com.o3dr.services.android.lib.drone.companion.solo.SoloState;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.Home;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.model.SimpleCommandListener;

import java.util.List;

import static android.view.View.INVISIBLE;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,DroneListener, TowerListener, LinkListener {

    private NaverMap myMap;
    private Button btn_basic, btn_navi, btn_hybrid, btn_terrain, btn_satellite,
            btn_visible, btn_layout,btn_arm,btn_connect;

    private TableLayout btn_group;
    private TextView altitudeValue, speedValue, distanceValue, attitudeValue, gpsCount;
    private Marker marker;
    private CameraUpdate cameraUpdate;

    private Spinner selectMode;
//    private InfoWindow infoWindow;

    private LatLng latLng;
    private int num;
    private int droneType = Type.TYPE_UNKNOWN;
    private Drone drone;

    private ControlTower controlTower;
    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);

        final Context context = getApplicationContext();
        this.controlTower = new ControlTower(context);
        this.drone = new Drone(context);

        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map);

        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);

        btn_basic = (Button) findViewById(R.id.basic);
        btn_navi = (Button) findViewById(R.id.navi);
        btn_satellite = (Button) findViewById(R.id.satellite);
        btn_hybrid = (Button) findViewById(R.id.hybrid);
        btn_terrain = (Button) findViewById(R.id.terrain);

        btn_visible = (Button) findViewById(R.id.visible);
        btn_layout = (Button) findViewById(R.id.layoutBtn);
        btn_group = (TableLayout) findViewById(R.id.buttonGroup);
        btn_connect = (Button) findViewById(R.id.connectBtn);
        selectMode = (Spinner)findViewById(R.id.selectMode);

        selectMode.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onFlightModeSelected(view);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        num = 0;
        marker = new Marker();
//        infoWindow = new InfoWindow();
//
//        infoWindow.setAdapter(new InfoWindow.DefaultTextAdapter(context) {
//            @NonNull
//            @Override
//            public CharSequence getText(@NonNull InfoWindow infoWindow) {
//                return str;
//            }
//        });
        btn_group.setVisibility(INVISIBLE);
        this.listener();
        this.droneStart();
    }


    //드론 시동, 이륙, 착륙
    public void droneStart(){
        VehicleApi.getApi(this.drone).arm(true, false,new SimpleCommandListener(){
            @Override
            public void onError(int executionError){
                Toast.makeText(getApplicationContext(), "Unable to arm vehicle.", Toast.LENGTH_SHORT).show();
//                alertUser("Unable to arm vehicle.");
            }
            @Override
            public void onTimeout(){
                Toast.makeText(getApplicationContext(), "Arming operation timed out.", Toast.LENGTH_SHORT).show();
            }
        });

        ControlApi.getApi(this.drone).takeoff(10, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(getApplicationContext(), "Taking off...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(int executionError) {
                Toast.makeText(getApplicationContext(), "Unable to take off.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onTimeout() {
                Toast.makeText(getApplicationContext(), "Unable to take off.", Toast.LENGTH_SHORT).show();
            }
        });

        VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LAND, new SimpleCommandListener(){
            @Override
            public void onError(int executionError){
                Toast.makeText(getApplicationContext(), "Unable to land the vehicle.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onTimeout(){
                Toast.makeText(getApplicationContext(), "Unable to land the vehicle.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //버튼이벤트
    public void listener() {
        //일반지도
        btn_basic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myMap.setMapType(NaverMap.MapType.Basic);
                btn_group.setVisibility(INVISIBLE);
                btn_visible.setText(btn_basic.getText());
            }
        });
        //위성사진, 도로, 심벌 함께 노출
        btn_hybrid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myMap.setMapType(NaverMap.MapType.Hybrid);
                btn_group.setVisibility(INVISIBLE);
                btn_visible.setText(btn_hybrid.getText());
            }
        });
        //네비게이션지도
        btn_navi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myMap.setMapType(NaverMap.MapType.Navi);
                btn_group.setVisibility(INVISIBLE);
                btn_visible.setText(btn_navi.getText());
            }
        });
        //지형도
        btn_terrain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myMap.setMapType(NaverMap.MapType.Terrain);
                btn_group.setVisibility(INVISIBLE);
                btn_visible.setText(btn_terrain.getText());
            }
        });
        //위성지도
        btn_satellite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myMap.setMapType(NaverMap.MapType.Satellite);
                btn_group.setVisibility(INVISIBLE);
                btn_visible.setText(btn_satellite.getText());
            }
        });
        btn_visible.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (btn_group.getVisibility() == View.VISIBLE) btn_group.setVisibility(INVISIBLE);
                else btn_group.setVisibility(View.VISIBLE);
            }
        });

        btn_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (num == 0) {
                    myMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, true);
                    btn_layout.setText("지적도on");
                } else {
                    myMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, false);
                    num = -1;
                    btn_layout.setText("지적도off");
                }
                num++;
            }
        });
    }

        @Override
    public void onStart() {
        super.onStart();
        this.controlTower.connect(this);
        updateVehicleModesForType(this.droneType);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.drone.isConnected()) {
            this.drone.disconnect();
            connectBtn(false);
        }

        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();
    }

    //드론연결 및 해제
    public void droneConnectClick(View view){
        if (this.drone.isConnected()) {
           this.drone.disconnect();
            connectBtn(false);
        } else {
            ConnectionParameter connectionParams = ConnectionParameter.newUdpConnection(null);
            this.drone.connect(connectionParams);
                connectBtn(true);
        }
    }

    public void droneArmClick(View view){
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);

        if (vehicleState.isFlying()) {
            // Land
            VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LAND, new SimpleCommandListener() {
                @Override
                public void onError(int executionError) {
                    Toast.makeText(getApplicationContext(), "Unable to land the vehicle.", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onTimeout() {
                    Toast.makeText(getApplicationContext(), "Unable to land the vehicle.", Toast.LENGTH_SHORT).show();
                }
            });
        } else if (vehicleState.isArmed()) {
            // Take off
            ControlApi.getApi(this.drone).takeoff(10, new AbstractCommandListener() {

                @Override
                public void onSuccess() {
                    Toast.makeText(getApplicationContext(), "Taking off...", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(int i) {
                    Toast.makeText(getApplicationContext(), "Unable to take off.", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onTimeout() {
                    Toast.makeText(getApplicationContext(), "Unable to take off.", Toast.LENGTH_SHORT).show();
                }
            });
        } else if (!vehicleState.isConnected()) {
            // Connect
            Toast.makeText(getApplicationContext(), "Connect to a drone first", Toast.LENGTH_SHORT).show();
        } else {
            // Connected but not Armed
            VehicleApi.getApi(this.drone).arm(true, false, new SimpleCommandListener() {
                @Override
                public void onError(int executionError) {
                    Toast.makeText(getApplicationContext(), "Unable to arm vehicle.", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onTimeout() {
                    Toast.makeText(getApplicationContext(), "Arming operation timed out.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    //드론 모니터링
    @Override
    public void onDroneEvent(String event, Bundle extras) {
        switch (event){
            case AttributeEvent.STATE_CONNECTED:
                Toast.makeText(this.getApplicationContext(), "Drone Connected", Toast.LENGTH_SHORT).show();
                connectBtn(this.drone.isConnected());
                updateArmButton();
                checkSoloState();
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                connectBtn(this.drone.isConnected());
                updateArmButton();
                break;
            case AttributeEvent.STATE_UPDATED:
            case AttributeEvent.STATE_ARMING:
                updateArmButton();
                break;

            case AttributeEvent.TYPE_UPDATED:
                Type newDroneType = this.drone.getAttribute(AttributeType.TYPE);
                if(newDroneType.getDroneType() != this.droneType){
                    this.droneType = newDroneType.getDroneType();
                    updateVehicleModesForType(this.droneType);
                }
                break;

            case AttributeEvent.STATE_VEHICLE_MODE:
                updateVehicleMode();
                break;

            case AttributeEvent.SPEED_UPDATED:
                updateSpeed();
                break;
            case AttributeEvent.ALTITUDE_UPDATED:
                updateAltitude();
                break;
            case AttributeEvent.HOME_UPDATED:
                updateDistanceFromHome();
                break;
                //yaw 각도 머시기
            case AttributeEvent.ATTITUDE_UPDATED:
                updateAttitude();
                break;
                //위성수
//            case AttributeEvent.GPS_COUNT:
//                break;
            default:
                break;
        }
    }

    //지도
    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.myMap = naverMap;
        myMap.setMapType(NaverMap.MapType.Basic);

    }

    protected void connectBtn(Boolean isConnected){
        if (isConnected) {
            btn_connect.setText("연결끊기");
        } else {
            btn_connect.setText("연결하기");
        }
    }
//===========정보가져오기
    protected void updateAltitude() {
        altitudeValue = (TextView) findViewById(R.id.altitudeValue);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        altitudeValue.setText(String.format("%3.1f", droneAltitude.getAltitude()) + "m");
    }

    protected void updateSpeed() {
       speedValue = (TextView) findViewById(R.id.speedValue);
        Speed droneSpeed = this.drone.getAttribute(AttributeType.SPEED);
        speedValue.setText(String.format("%3.1f", droneSpeed.getGroundSpeed()) + "m/s");
    }

    protected void updateDistanceFromHome() {
        distanceValue = (TextView) findViewById(R.id.distanceValue);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        double vehicleAltitude = droneAltitude.getAltitude();
        Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
        LatLong vehiclePosition = droneGps.getPosition();

        double distanceFromHome = 0;

        if (droneGps.isValid()) {
            LatLongAlt vehicle3DPosition = new LatLongAlt(vehiclePosition.getLatitude(), vehiclePosition.getLongitude(), vehicleAltitude);
            Home droneHome = this.drone.getAttribute(AttributeType.HOME);
            distanceFromHome = distanceBetweenPoints(droneHome.getCoordinate(), vehicle3DPosition);
        } else {
            distanceFromHome = 0;
        }

        distanceValue.setText(String.format("%3.1f", distanceFromHome) + "m");
        Toast.makeText(this.getApplicationContext(), "위도 : "+vehiclePosition.getLatitude()+"    경도 : "+vehiclePosition.getLongitude(), Toast.LENGTH_LONG).show();
    }

    protected  void updateAttitude(){
        attitudeValue = (TextView) findViewById(R.id.altitudeValue);
        Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
        attitudeValue.setText(String.format("%3.1f", droneGps.getSatellitesCount()) + "deg");
    }

//    protected void updateGpsCount(){
//        gpsCount = (TextView) findViewById(R.id.gpsCount);
//        attitudeValue = (TextView) findViewById(R.id.altitudeValue);
//        Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
//    }
//============정보가져오기

    private void checkSoloState() {
        final SoloState soloState = drone.getAttribute(SoloAttributes.SOLO_STATE);
        if (soloState == null){
            Toast.makeText(this.getApplicationContext(), "Unable to retrieve the solo state.", Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(this.getApplicationContext(), "Solo state is up to date.", Toast.LENGTH_SHORT).show();
        }
    }

    protected void updateArmButton() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        btn_arm = (Button) findViewById(R.id.armBtn);

        if (!this.drone.isConnected()) {
           btn_arm.setVisibility(View.INVISIBLE);
        } else {
            btn_arm.setVisibility(View.VISIBLE);
        }

        if (vehicleState.isFlying()) {
            // Land
            btn_arm.setText("LAND");
        } else if (vehicleState.isArmed()) {
            // Take off
            btn_arm.setText("TAKE OFF");
        } else if (vehicleState.isConnected()) {
            // Connected but not Armed
            btn_arm.setText("ARM");
        }
    }

    protected void updateVehicleModesForType(int droneType) {
        List<VehicleMode> vehicleModes = VehicleMode.getVehicleModePerDroneType(droneType);
        ArrayAdapter<VehicleMode> vehicleModeArrayAdapter = new ArrayAdapter<VehicleMode>(this, android.R.layout.simple_spinner_item, vehicleModes);
        vehicleModeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.selectMode.setAdapter(vehicleModeArrayAdapter);
    }

    protected void updateVehicleMode() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        VehicleMode vehicleMode = vehicleState.getVehicleMode();
        ArrayAdapter arrayAdapter = (ArrayAdapter) this.selectMode.getAdapter();
        this.selectMode.setSelection(arrayAdapter.getPosition(vehicleMode));
    }

    protected double distanceBetweenPoints(LatLongAlt pointA, LatLongAlt pointB) {
        if (pointA == null || pointB == null) {
            return 0;
        }
        double dx = pointA.getLatitude() - pointB.getLatitude();
        double dy = pointA.getLongitude() - pointB.getLongitude();
        double dz = pointA.getAltitude() - pointB.getAltitude();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    // 비행모드 변경
    public void onFlightModeSelected(View view) {
        VehicleMode vehicleMode = (VehicleMode) this.selectMode.getSelectedItem();

        VehicleApi.getApi(this.drone).setVehicleMode(vehicleMode, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(getApplicationContext(), "Vehicle mode change successful.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(int executionError) {
                Toast.makeText(getApplicationContext(), "Vehicle mode  change failed: " + executionError, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onTimeout() {
                Toast.makeText(getApplicationContext(), "Vehicle mode change timed out.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }

    @Override
    public void onLinkStateUpdated(@NonNull LinkConnectionStatus connectionStatus) {

    }

    @Override
    public void onTowerConnected() {
        Toast.makeText(getApplicationContext(), "DroneKit-Android Connected.", Toast.LENGTH_SHORT).show();
        //        alertUser("DroneKit-Android Connected");
        this.controlTower.registerDrone(this.drone, this.handler);
        this.drone.registerDroneListener(this);
    }

    @Override
    public void onTowerDisconnected() {
        Toast.makeText(getApplicationContext(), "DroneKit-Android disConnected.", Toast.LENGTH_SHORT).show();
    }
}
package com.example.sj_application;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.android.client.Drone;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.property.GuidedState;
import com.o3dr.services.android.lib.model.AbstractCommandListener;

public class GuideMode {

    static LatLng mGuidPoint;
    static Marker mMarkerGuide = new com.naver.maps.map.overlay.Marker();
    static OverlayImage guideIcon = OverlayImage.fromResource(R.drawable.guide);
    static VehicleMode vehicleMode;

    static void DialogSimple(final Drone drone, final LatLong point, final Context context, final NaverMap myMap){

        AlertDialog.Builder alt_bld = new AlertDialog.Builder(context);
        mGuidPoint = new LatLng(point.getLatitude(), point.getLongitude());
        mMarkerGuide.setPosition(mGuidPoint);
        mMarkerGuide.setIcon(guideIcon);
        mMarkerGuide.setMap(myMap);

        State vehicleState = drone.getAttribute(AttributeType.STATE);

        if(vehicleMode == vehicleState.getVehicleMode()){
            VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_GUIDED, new AbstractCommandListener(){
                @Override
                public void onSuccess(){
                    vehicleMode = vehicleState.getVehicleMode();
                    ControlApi.getApi(drone).goTo(point, true, null);
                    Toast.makeText(context, "위도 : "+point.getLatitude()+"    경도 : "+point.getLongitude(), Toast.LENGTH_SHORT).show();
//                 CheckGoal(drone,mGuidPoint, context);
                }
                @Override
                public void onError(int i) {
                }
                @Override
                public void onTimeout() {
                }
            });
        }
        else{
            alt_bld.setMessage("확인하시면 가이드모드로 전환후 기체가 이동" +
                    "합니다.").setCancelable(false).setPositiveButton("확인",new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int id){
                    VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_GUIDED, new AbstractCommandListener(){
                        @Override
                        public void onSuccess(){
                            vehicleMode = vehicleState.getVehicleMode();
                            ControlApi.getApi(drone).goTo(point, true, null);
                            Toast.makeText(context, "위도 : "+ point.getLatitude()+"    경도 : "+point.getLongitude(), Toast.LENGTH_SHORT).show();
                        }
                        @Override
                        public void onError(int i) {
                        }
                        @Override
                        public void onTimeout() {
                        }
                    });
                }
            }).setNegativeButton("취소",new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int id){
                    dialog.cancel();
                }
            });

            AlertDialog alert = alt_bld.create();
            alert.setTitle("Title");
            alert.show();
        }


         }
        public static boolean CheckGoal(final Drone drone, LatLng recentLatLng, Context context){
            GuidedState guidedState = drone.getAttribute(AttributeType.GUIDED_STATE);
            LatLng target = new LatLng(guidedState.getCoordinate().getLatitude(),guidedState.getCoordinate().getLongitude());
            mMarkerGuide.setMap(null);
            Toast.makeText(context,"도착!!!",Toast.LENGTH_SHORT).show();
            return target.distanceTo(recentLatLng)<=1;
        }

}

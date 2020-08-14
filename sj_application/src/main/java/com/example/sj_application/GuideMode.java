package com.example.sj_application;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.android.client.Drone;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.property.GuidedState;
import com.o3dr.services.android.lib.model.AbstractCommandListener;

public class GuideMode {

    static LatLng mGuidPoint;
    static Marker mMarkerGuide = new com.naver.maps.map.overlay.Marker();
    //static OverlayImage guideIcon = OverlayImage.fromResource(R.drawable.);

    static void DialogSimple(final Drone drone, final LatLong point, final Context context){
        AlertDialog.Builder alt_bld = new AlertDialog.Builder(context);
        alt_bld.setMessage("확인하시면 가이드모드로 전환후 기체가 이동합니다.").setCancelable(false).setPositiveButton("확인",new DialogInterface.OnClickListener(){
        public void onClick(DialogInterface dialog, int id){
        // Action fon 'yes' button
         VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_GUIDED, new AbstractCommandListener(){

             @Override
         public void onSuccess(){
         ControlApi.getApi(drone).goTo(point, true, null);}
         @Override
         public void onError(int i){
         }

         @Override
         public void onTimeout(){
         }
         });
         }
         }).setNegativeButton("취소",new DialogInterface.OnClickListener(){
         public void onClick(DialogInterface dialog, int id){
         dialog.cancel();
         }
         });

         AlertDialog alert = alt_bld.create();
         //Title for AlertDialog
         alert.setTitle("Title");
         //Icon for AlertDialog
         //           alert.setIcon(R.drawable.);
         alert.show();
         }
        public static boolean CheckGoal(final Drone drone, LatLng recentLatLng){
            GuidedState guidedState = drone.getAttribute(AttributeType.GUIDED_STATE);
            LatLng target = new LatLng(guidedState.getCoordinate().getLatitude(),guidedState.getCoordinate().getLongitude());
            return target.distanceTo(recentLatLng)<=1;
        }

}

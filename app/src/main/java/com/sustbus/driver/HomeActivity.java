/*
 * Copyright (C) 2019-2020 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */
package com.sustbus.driver;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.messaging.FirebaseMessaging;
import com.here.sdk.core.GeoCoordinates;
import com.sustbus.driver.adminPanel.AdminPanelActivity;
import com.sustbus.driver.adminPanel.RouteManager;
import com.sustbus.driver.util.MapUtil;
import com.sustbus.driver.util.NotificationSender;
import com.sustbus.driver.util.PermissionsRequestor;
import com.sustbus.driver.util.ResultListener;
import com.sustbus.driver.util.UserInfo;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import static com.sustbus.driver.MapsActivity.MIN_DIST;
import static com.sustbus.driver.MapsActivity.MIN_TIME;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener, FirebaseAuth.AuthStateListener {
    private static final String TAG = "HomeActivity";
    private static final int LOCATION_PERMISSION_CODE = 1;
    UserInfo userInfo = null;
    Intent intent;
    private TextView userNameTv;
    private TextView driverOrStudent;
    private CardView shareRideTv;
    private CardView adminPanelCv;
    private ImageView dpEv;
    private DatabaseReference databaseReference,userLocationData, userPathReference;
    private FirebaseFirestore db;
    private ListenerRegistration listener;
    private DocumentReference userDoc;
    private FirebaseAuth mAuth;
    private ImageView rideShareIndicatorIV;
    private boolean isRideShareOn = false;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private String userUid,routeIdCurrentlySharing;
    private MapUtil mapUtil;
    private PermissionsRequestor permissionsRequestor;
    private List<String> pathString;
    private boolean pathOk;
    private int determineCallCount;
    private GeoCoordinates previousPosition;
    private Location ridersPreviousLocation = null;
    private NotificationSender notificationSender;
    private  CardView routeUploaderCv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        rideShareIndicatorIV = findViewById(R.id.ride_share_iv);

        CardView openMapBtn = findViewById(R.id.track_buses_cv);
        adminPanelCv = findViewById(R.id.admin_panel_cv);
        routeUploaderCv=findViewById(R.id.route_uploader);
        shareRideTv = findViewById(R.id.ride_on_cv);
        CardView scheduleBtn = findViewById(R.id.bus_schedule_cv);
        userNameTv = findViewById(R.id.row_item_user_name_tv);
        driverOrStudent = findViewById(R.id.driver_or_student_tv);
        CardView profileCv = findViewById(R.id.profile_cv);
        CardView signOut = findViewById(R.id.help_center_cv);
        dpEv = findViewById(R.id.home_user_image_ev);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        findViewById(R.id.home_notifications_tv).setOnClickListener(this);
        openMapBtn.setOnClickListener(this);
        scheduleBtn.setOnClickListener(this);
        shareRideTv.setOnClickListener(this);
        profileCv.setOnClickListener(this);
        signOut.setOnClickListener(this);
        routeUploaderCv.setOnClickListener(this);
        FirebaseAuth.getInstance().addAuthStateListener(this);
        adminPanelCv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, AdminPanelActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });

        userInfo = UserInfo.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mapUtil = MapUtil.getInstance();

    }

    private void loadImage() {
        try {
            String img = userInfo.getUrl();
            byte[] imageAsBytes = Base64.decode(img.getBytes(), Base64.DEFAULT);
            dpEv.setImageBitmap(BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length));
        }
        catch (NullPointerException e){
        }
        catch (IllegalArgumentException e){
            Log.d(TAG, "onSuccess: " + e.getMessage());
        }
    }

    private void updateDatabase() {
        /**
         * Database initialization
         * */
        Log.d(TAG, "updateDatabase: called");
        databaseReference = FirebaseDatabase.getInstance().getReference();
        userUid = mAuth.getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();
        userDoc = FirebaseFirestore.getInstance().collection("users").document(userUid);
        userLocationData = databaseReference.child("alive").child(userUid);
        userPathReference = databaseReference.child("destinations").child(userUid).child("path");
        userLocationData.onDisconnect().setValue(null);
        userPathReference.onDisconnect().setValue(null);

        /**
         * getting data from cloud firestore
         * */
       listener = userDoc.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }
                if (documentSnapshot != null || documentSnapshot.exists()) {
                    UserInfo.setInstance(documentSnapshot.toObject(UserInfo.class));
                    userInfo = UserInfo.getInstance();
                    Log.d(TAG, userInfo.toString());
                    dashboardSetup();
                    loadImage();
                    Log.d(TAG, "onEvent: " + userInfo.toString());
                   if(!userInfo.isProfileCompleted() || !userInfo.isPermitted()){
                        Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    }
                } else {
                    Log.d(TAG, "Current data: null");
                }
            }
        });

    }

    private void dashboardSetup() {
        Log.d(TAG, "dashboardSetup: called");
        /**
         * setting up dashboard for user (driver/student)
         * */
        userNameTv.setText(userInfo.getUserName());
        if (userInfo.isDriver()) {
            Log.d(TAG, "onEvent: " + " ashena?");
            driverOrStudent.setText("Driver");
        } else {
            driverOrStudent.setText("Student");
        }


        if(getIntent().getStringExtra("markerKey")!=null){

            Intent intent=new Intent(HomeActivity.this,MapsActivity.class);
            intent.putExtra("fromSchedule",true);
            intent.putExtra("markerToShow",getIntent().getStringExtra("markerKey"));
            startActivity(intent);
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: ");
        permissionsRequestor = new PermissionsRequestor(this);
        permissionsRequestor.request(new ResultListener() {
            @Override
            public void permissionsGranted() {
            }

            @Override
            public void permissionsDenied() {
                Snackbar.make(findViewById(R.id.home_scrollview), "Please grant all Permissions", Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");

    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: ");
    }

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        Log.d(TAG, "onAuthStateChanged: fired");
        if (firebaseAuth.getCurrentUser() == null) {
            Log.d(TAG, "onAuthStateChanged: " + "mAuth gets null");
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }
        else {
            FirebaseFirestore.getInstance().collection("admin")
                    .document(firebaseAuth.getCurrentUser().getUid())
                    .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task.isSuccessful() && task.getResult().exists() && task.getResult().getBoolean("active")){
                        adminPanelCv.setVisibility(View.VISIBLE);
                    }
                    else adminPanelCv.setVisibility(View.GONE);
                }
            });
            updateDatabase();
        }
    }

    public void turnOnRideShare() {

        MapUtil.rideShareStatus = true;
        isRideShareOn = true;
        rideShareIndicatorIV.setImageDrawable(getDrawable(R.drawable.end_ride));
        notificationSender=new NotificationSender(this,userUid);

        locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                userLocationData.child("lat").setValue(location.getLatitude());
                userLocationData.child("lng").setValue(location.getLongitude());
                float rotation = 0;
                if (ridersPreviousLocation != null) {
                    rotation = location.bearingTo(ridersPreviousLocation);
                }
                ridersPreviousLocation = location;
                userLocationData.child("rotation").setValue(rotation);
                handlePath(new GeoCoordinates(location.getLatitude(), location.getLongitude()));
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
            }

            @Override
            public void onProviderEnabled(String s) {
            }

            @Override
            public void onProviderDisabled(String s) {
                mapUtil.enableGPS(getApplicationContext(), getActivity(), 101);
                turnOffRideShare();
            }
        };


        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DIST, locationListener);
        } catch (SecurityException e) {
            Snackbar.make(findViewById(R.id.home_scrollview), e.getMessage(), Snackbar.LENGTH_SHORT).show();
        }
    }

    public void determineCurrentLocation(GeoCoordinates latLng) {

        if (determineCallCount == 0) previousPosition = latLng;
        else if (latLng.distanceTo(previousPosition) >= 5) {
            int rem = 0;
            for (int i = 0; i + 1 < pathString.size(); i++) {
                GeoCoordinates co1 = MapUtil.GeoCoordinatesMap.get(pathString.get(i));
                GeoCoordinates co2 = MapUtil.GeoCoordinatesMap.get(pathString.get(i + 1));

                if (Math.abs(latLng.distanceTo(co2) + previousPosition.distanceTo(co2) - previousPosition.distanceTo(latLng)) <= 1) {

                    if (co1.distanceTo(previousPosition) < co1.distanceTo(latLng)) {
                        rem = i + 2;
                        break;
                    }

                } else if (Math.abs(latLng.distanceTo(co1) + previousPosition.distanceTo(co1) - previousPosition.distanceTo(latLng)) <= 1) {

                    if (co2.distanceTo(previousPosition) > co2.distanceTo(latLng)) {
                        rem = i + 1;
                        break;
                    }

                } else if (co1.distanceTo(previousPosition) < co1.distanceTo(latLng) && co2.distanceTo(previousPosition) > co2.distanceTo(latLng)) {
                    rem = i + 1;
                    break;

                }


            }

            if (rem == 0) {
                determineCallCount = 0;
                return;
            } else {
                for (int i = 0; i < rem; i++) pathString.remove(0);
                pathOk = true;
                updatePath();
            }
        }

        determineCallCount = 1;

    }

    public void handlePath(GeoCoordinates newLatLng) {

        if (!pathOk) {
            determineCurrentLocation(newLatLng);
            return;
        }


        GeoCoordinates toCheck = MapUtil.GeoCoordinatesMap.get(pathString.get(0));

        if (newLatLng.distanceTo(toCheck) <= 100) {
            GeoCoordinates campus=MapUtil.GeoCoordinatesMap.get(MapUtil.CAMPUS);
            if(toCheck.distanceTo(campus)>newLatLng.distanceTo(campus)){
                notificationSender.send(pathString.get(0),"away");
            }
            else notificationSender.send(pathString.get(0),"towards");

            if(pathString.size()==1)return;
            pathString.remove(0);
            updatePath();
        }


    }

    public void initializePath() {
        startActivityForResult(new Intent(this,ScheduleActivity.class).putExtra("forRideShare",true),100);
    }

    public void updatePath() {
        String path = "";
        for (String s : pathString) {
            path += (s + ";");
        }
        userPathReference.setValue(path);
    }

    public Activity getActivity() {
        return this;
    }

    public void turnOffRideShare() {
        notificationSender.destroy();
        notificationSender=null;
        MapUtil.rideShareStatus = isRideShareOn = false;
        rideShareIndicatorIV.setImageDrawable(getDrawable(R.drawable.start_ride));
        locationManager.removeUpdates(locationListener);
        locationListener = null;
        userLocationData.setValue(null);
        userPathReference.setValue(null);
        databaseReference.child("busesOnRoad").child(routeIdCurrentlySharing).setValue(null);
    }


    @Override
    public void onClick(View view) {
        int i = view.getId();

        if (i == R.id.ride_on_cv) {
            if (userInfo.isDriver()) {
                if (!isRideShareOn){
                    boolean isGps = false;
                    try {
                        isGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                    } catch (Exception e) {}

                    if (!isGps) {
                        mapUtil.enableGPS(getApplicationContext(), this, 101);
                    }
                    else initializePath();

                }
                else turnOffRideShare();
            } else {
                Snackbar.make(findViewById(R.id.ride_on_cv), "You're not a Driver!", Snackbar.LENGTH_SHORT).show();
            }
        } else if (i == R.id.help_center_cv) {
            userInfo.reset();
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);

            finish();

        } else if (i == R.id.track_buses_cv) {
            startActivity(new Intent(HomeActivity.this, MapsActivity.class));
        } else if (i == R.id.profile_cv && userInfo != null) {
            startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
        } else if (i == R.id.bus_schedule_cv) {
            startActivity(new Intent(HomeActivity.this, ScheduleActivity.class));
        }
        else if(i==R.id.home_notifications_tv){
            startActivity(new Intent(HomeActivity.this, NotificationSettings.class));
        }
        else if(i==R.id.route_uploader){
            startActivity(new Intent(HomeActivity.this, RouteManager.class));
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101) {
            if (resultCode == Activity.RESULT_OK) initializePath();
        }
        else if(requestCode==100 && data!=null){


            pathString = new ArrayList<>();
            String path=data.getStringExtra("path");
            String routeId=data.getStringExtra("routeId");
            String title=data.getStringExtra("title");
            if(path==null || routeId==null || title==null)return;

            int last=0;
            for (int i = 0; i <path.length(); i++) {
                if(path.charAt(i)==';'){
                    pathString.add(path.substring(last,i));
                    Log.d("DEB",path.substring(last,i));
                    last=i+1;
                }
            }



            userLocationData.child("title").setValue(title);
            databaseReference.child("busesOnRoad").child(routeId).onDisconnect().setValue(null);
            databaseReference.child("busesOnRoad").child(routeId).child("key").setValue(userUid);
            routeIdCurrentlySharing=routeId;
            userPathReference.setValue("NA;");
            pathOk = false;
            determineCallCount = 0;

            turnOnRideShare();

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionsRequestor.onRequestPermissionsResult(requestCode, grantResults);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: ");
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        super.onDestroy();
        if (isRideShareOn) {
            turnOffRideShare();
        }
        if(listener != null) listener.remove();
        FirebaseAuth.getInstance().removeAuthStateListener(this);
    }
}

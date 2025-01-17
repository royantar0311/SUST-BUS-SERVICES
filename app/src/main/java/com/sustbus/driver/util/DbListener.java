package com.sustbus.driver.util;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.sustbus.driver.ProfileActivity;

import io.grpc.Context;

public class DbListener {
    private static final String TAG = "getFromDB";
    UserInfo userInfo =  UserInfo.getInstance();
    CallBack callBack;
    public DbListener(CallBack callBack){
        this.callBack = callBack;
        run();
    }
    private void run() {
        Log.d(TAG, "run: " + Thread.currentThread().getName());
        Log.d(TAG, "run: " + userInfo.getuId());
        FirebaseFirestore.getInstance().collection("users").document(userInfo.getuId())
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }
                        if (snapshot != null || snapshot.exists()) {
                            UserInfo.setInstance(snapshot.toObject(UserInfo.class));

                            userInfo = UserInfo.getInstance();
                            callBack.ok();
                            //Log.d(TAG, userInfo.toString());
                            Log.d("DEB", "onEvent: " + userInfo.toString());
                            if (!userInfo.isProfileCompleted() || !userInfo.isPermitted()) {
                                callBack.notOk();
                            }
                        } else {
                            Log.d(TAG, "Current data: null");
                        }
                    }
                });
    }
}
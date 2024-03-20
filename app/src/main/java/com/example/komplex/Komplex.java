package com.example.komplex;

import android.app.Application;

import com.google.firebase.FirebaseApp;

public class Komplex extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }
}

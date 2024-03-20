package com.example.komplex;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class LocationPickerActivity extends AppCompatActivity {

    private GoogleMap mMap;
    private Marker marker;
    Button add_marker_button;
    String sightName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_picker);

        Intent intent = getIntent();
        sightName = intent.getStringExtra("sightName");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
                LatLng markerLocation = new LatLng(47.087961097750714, 17.908053120015623);
                marker = mMap.addMarker(new MarkerOptions().position(markerLocation));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markerLocation, 12f));

                mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng latLng) {
                        Marker newMarker = mMap.addMarker(new MarkerOptions().position(latLng));
                        marker.remove();
                        marker = newMarker;
                    }
                });
            }
        });

        add_marker_button = findViewById(R.id.add_marker_button);
        add_marker_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("markerLocation", marker.getPosition());
                resultIntent.putExtra("sightName", sightName);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        });
    }
}
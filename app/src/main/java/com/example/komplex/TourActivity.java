package com.example.komplex;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TourActivity extends AppCompatActivity {

    Button add_button;
    Button delete_button;
    TextView title;
    String tourName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour);
        Intent intent = getIntent();
        tourName = intent.getStringExtra("tourName");

        ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            if (data != null && data.hasExtra("markerLocation")) {
                                LatLng markerLocation = data.getParcelableExtra("markerLocation");
                                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("/Users/admin");
                                DatabaseReference newChildRef = reference.child("tours").child(tourName).child(data.getStringExtra("sightName"));
                                Map<String, Object> sightData = new HashMap<>();
                                sightData.put("desc", "");
                                sightData.put("location", markerLocation.toString());
                                sightData.put("visited", false);
                                newChildRef.removeValue();
                                newChildRef.setValue(sightData);
                            }
                        }
                    }
                });

        title = findViewById(R.id.tour_title);
        title.setText(tourName);
        title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(TourActivity.this);
                View dialogView = getLayoutInflater().inflate(R.layout.rename_sight_popup, null);
                builder.setView(dialogView);
                EditText edit_text = dialogView.findViewById(R.id.popup_edit);
                Button ok_button = dialogView.findViewById(R.id.ok_button);
                AlertDialog dialog = builder.create();
                edit_text.requestFocus();
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                dialog.show();
                ok_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String newTourName = edit_text.getText().toString().trim();
                        if (!newTourName.isEmpty()) {
                            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("/Users/admin/tours");
                            reference.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.hasChild(newTourName)) {
                                        Toast.makeText(TourActivity.this, "Tour name already exists", Toast.LENGTH_LONG).show();
                                    } else {
                                        String oldKey = tourName;
                                        tourName = newTourName;
                                        title.setText(tourName);
                                        DatabaseReference oldRef = reference.child(oldKey);
                                        oldRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                if (snapshot.exists()) {
                                                    Object data = snapshot.getValue();
                                                    DatabaseReference newRef = reference.child(tourName);
                                                    newRef.setValue(data);
                                                    snapshot.getRef().removeValue();
                                                } else {
                                                    Toast.makeText(TourActivity.this, "Old tour name does not exist", Toast.LENGTH_LONG).show();
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {

                                            }
                                        });
                                        dialog.dismiss();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                        } else {
                            Toast.makeText(TourActivity.this, "Please enter a valid tour name", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });


        add_button = findViewById(R.id.add_sight_button);
        add_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(TourActivity.this);
                View dialogView = getLayoutInflater().inflate(R.layout.rename_sight_popup, null);
                builder.setView(dialogView);
                EditText popupEditText = dialogView.findViewById(R.id.popup_edit);
                Button okButton = dialogView.findViewById(R.id.ok_button);
                AlertDialog dialog = builder.create();
                popupEditText.requestFocus();
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                dialog.show();
                okButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String newSightName = popupEditText.getText().toString();
                        if (!newSightName.isEmpty()) {
                            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("/Users/admin/tours/" + tourName + "/" + newSightName);
                            reference.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        Toast.makeText(TourActivity.this, "Sight name already exists", Toast.LENGTH_LONG).show();
                                    } else {
                                        Intent intent = new Intent(TourActivity.this, LocationPickerActivity.class);
                                        Map<String, Object> sightData = new HashMap<>();
                                        sightData.put("desc", "");
                                        sightData.put("location", "");
                                        sightData.put("visited", false);
                                        reference.setValue(sightData);
                                        intent.putExtra("tourName", tourName);
                                        intent.putExtra("sightName", newSightName);
                                        activityResultLauncher.launch(intent);
                                        dialog.dismiss();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        } else {
                            Toast.makeText(TourActivity.this, "Please enter a valid sight name", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });


        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("/Users/admin");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                LinearLayout containerLayout = findViewById(R.id.scrollable_sights);
                containerLayout.removeViews(0, containerLayout.getChildCount()-1);
                for (DataSnapshot tourSnapshot : dataSnapshot.child("tours").child(tourName).getChildren()) {
                    String sightName = tourSnapshot.getKey();
                    String latlng_str = tourSnapshot.child("location").getValue().toString();
                    if (!latlng_str.equals("")) {
                        createContainerView(LatLngParse(latlng_str), sightName);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {

            }
        });

        delete_button = findViewById(R.id.delete_button);
        delete_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseReference newChildRef = databaseReference.child("tours").child(tourName);
                newChildRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot sightSnapshot : snapshot.getChildren()) {
                            boolean v = sightSnapshot.child("visited").getValue(Boolean.class);
                            if (v) {
                                String filename = sightSnapshot.child("image").getValue(String.class);
                                StorageReference storageReference = FirebaseStorage.getInstance().getReference("images/" + filename);
                                storageReference.delete();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
                databaseReference.child("tours").child(tourName).removeValue();
                finish();
            }
        });
    }

    private void createContainerView(LatLng markerLocation, String sightName) {
        LinearLayout containerLayout = findViewById(R.id.scrollable_sights);

        RelativeLayout relativeLayout = new RelativeLayout(this);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        relativeLayout.setLayoutParams(layoutParams);

        View locationContainer = getLayoutInflater().inflate(R.layout.sight_container, containerLayout, false);
        locationContainer.setId(View.generateViewId());
        Button editButton = locationContainer.findViewById(R.id.edit_button);
        TextView text = locationContainer.findViewById(R.id.sight_name);
        text.setText(sightName);
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TourActivity.this, EditSightActivity.class);
                intent.putExtra("latlng", markerLocation);
                intent.putExtra("tourName", tourName);
                intent.putExtra("sightName", sightName);
                startActivity(intent);
            }
        });
        relativeLayout.addView(locationContainer);

        containerLayout.addView(relativeLayout, containerLayout.getChildCount() - 1);
    }

    private LatLng LatLngParse(String latLngString) {
        String coords = latLngString.substring(latLngString.indexOf("(") + 1, latLngString.indexOf(")"));
        String[] parts = coords.split(",");
        double latitude = Double.parseDouble(parts[0]);
        double longitude = Double.parseDouble(parts[1]);
        return new LatLng(latitude, longitude);
    }
}
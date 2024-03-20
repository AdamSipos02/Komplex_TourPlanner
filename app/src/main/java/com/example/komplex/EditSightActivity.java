package com.example.komplex;

import android.Manifest;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EditSightActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 1;

    TextView sight_title;
    EditText desc;
    Button save_sight;
    Button delete_sight;
    String sightName;
    String tourName;
    GoogleMap mMap;
    Marker marker;
    String latlng;
    Button tick;
    ActivityResultLauncher<Intent> activityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_tour);

        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            Bundle extras = data.getExtras();
                            Bitmap imageBitmap = (Bitmap) extras.get("data");
                            showImage(imageBitmap);
                            uploadImageToStorage(imageBitmap);
                        }
                    }
                });

        Intent intent = getIntent();
        sightName = intent.getStringExtra("sightName");
        tourName = intent.getStringExtra("tourName");
        latlng = intent.getParcelableExtra("latlng").toString();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("/Users/admin");
        DatabaseReference newChildRef = reference.child("tours").child(tourName).child(sightName);
        newChildRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean v = snapshot.child("visited").getValue(Boolean.class);
                if (v) {
                    String filename = snapshot.child("image").getValue(String.class);
                    StorageReference storageReference = FirebaseStorage.getInstance().getReference("images/" + filename);
                    try {
                        File localfile = File.createTempFile("tempfile", ".jpg");
                        storageReference.getFile(localfile)
                                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                        Bitmap bitmap = BitmapFactory.decodeFile(localfile.getAbsolutePath());
                                        showImage(bitmap);
                                    }
                                });
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        sight_title = findViewById(R.id.sight_title);
        desc = findViewById(R.id.desc);
        sight_title.setText(sightName);
        DatabaseReference desc_reference = reference.child("tours").child(tourName).child(sightName).child("desc");
        desc_reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                desc.setText(snapshot.getValue().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
                LatLng markerLocation = LatLngParse(latlng);
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

        sight_title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(EditSightActivity.this);
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
                        String newSightName = edit_text.getText().toString().trim();
                        if (!newSightName.isEmpty()) {
                            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("/Users/admin/tours").child(tourName);
                            reference.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.hasChild(newSightName)) {
                                        Toast.makeText(EditSightActivity.this, "Sight name already exists", Toast.LENGTH_LONG).show();
                                    } else {
                                        String oldKey = sightName;
                                        sightName = newSightName;
                                        sight_title.setText(sightName);
                                        DatabaseReference oldRef = reference.child(oldKey);
                                        oldRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                if (snapshot.exists()) {
                                                    Object data = snapshot.getValue();
                                                    DatabaseReference newRef = reference.child(sightName);
                                                    newRef.setValue(data);
                                                    snapshot.getRef().removeValue();
                                                } else {
                                                    Toast.makeText(EditSightActivity.this, "Old sight name does not exist", Toast.LENGTH_LONG).show();
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
                            Toast.makeText(EditSightActivity.this, "Please enter a valid sight name", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });

        save_sight = findViewById(R.id.save_button);
        save_sight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("/Users/admin");
                DatabaseReference newChildRef = reference.child("tours").child(tourName).child(sightName);
                newChildRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Map<String, Object> sightData = new HashMap<>();
                        sightData.put("desc", ((EditText)findViewById(R.id.desc)).getText().toString());
                        sightData.put("location", marker.getPosition().toString());
                        boolean v = snapshot.child("visited").getValue(Boolean.class);
                        sightData.put("visited", v);
                        if (v) {
                            sightData.put("image", snapshot.child("image").getValue(String.class));
                        }
                        newChildRef.setValue(sightData);
                        finish();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });

        delete_sight = findViewById(R.id.delete_button);
        delete_sight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseReference newChildRef = reference.child("tours").child(tourName).child(sightName);
                newChildRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean v = snapshot.child("visited").getValue(Boolean.class);
                        if (v) {
                            String filename = snapshot.child("image").getValue(String.class);
                            StorageReference storageReference = FirebaseStorage.getInstance().getReference("images/" + filename);
                            storageReference.delete();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
                reference.child("tours").child(tourName).child(sightName).removeValue();
                finish();
            }
        });

        tick = findViewById(R.id.tick_button);
        tick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });
    }

    private void showImage(Bitmap imageBitmap) {
        ImageView imageView = new ImageView(this);
        imageView.setId(View.generateViewId());
        imageView.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int mapWidth = findViewById(R.id.map).getWidth();
        int imageWidth = imageBitmap.getWidth();
        int imageHeight = imageBitmap.getHeight();
        float scaleFactor = (float) mapWidth / imageWidth;
        int newHeight = Math.round(imageHeight * scaleFactor);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, mapWidth, newHeight, true);
        imageView.setImageBitmap(resizedBitmap);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(mapWidth, newHeight);
        layoutParams.addRule(RelativeLayout.BELOW, R.id.map);
        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        layoutParams.setMargins(0, 15, 0, 0);
        imageView.setLayoutParams(layoutParams);
        LinearLayout parentLayout = findViewById(R.id.scrollable);
        int tickButtonIndex = parentLayout.indexOfChild(tick);
        parentLayout.removeView(tick);
        parentLayout.addView(imageView, tickButtonIndex, layoutParams);
    }

    private void uploadImageToStorage(Bitmap imageBitmap) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference imagesRef = storageRef.child("images");
        String imageName = "image_" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = imagesRef.child(imageName);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageData = baos.toByteArray();
        imageRef.putBytes(imageData)
                .addOnSuccessListener(taskSnapshot -> {
                    // Image uploaded successfully, get the download URL
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        storeImageUrlInDatabase(imageName);
                    });
                });
    }

    private void storeImageUrlInDatabase(String imageName) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("/Users/admin").child("tours").child(tourName).child(sightName);
        databaseReference.child("visited").setValue(true);
        databaseReference.child("image").setValue(imageName);
    }

    private LatLng LatLngParse(String latLngString) {
        String coords = latLngString.substring(latLngString.indexOf("(") + 1, latLngString.indexOf(")"));
        String[] parts = coords.split(",");
        double latitude = Double.parseDouble(parts[0]);
        double longitude = Double.parseDouble(parts[1]);
        return new LatLng(latitude, longitude);
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
    }

    private void dispatchTakePictureIntent() {
        if (checkCameraPermission()) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            activityResultLauncher.launch(takePictureIntent);
            mMap.getUiSettings().setAllGesturesEnabled(false);
            mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {

                }
            });
        } else {
            requestCameraPermission();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                // Camera permission denied, show a message or take appropriate action

            }
        }
    }
}
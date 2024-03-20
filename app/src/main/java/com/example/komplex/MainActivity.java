package com.example.komplex;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class MainActivity extends AppCompatActivity {
    Button new_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);

        new_button = findViewById(R.id.add_button);
        new_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
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
                        String newTourName = popupEditText.getText().toString();
                        if (!newTourName.isEmpty()) {
                            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("/Users/admin/tours/" + newTourName);
                            reference.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        Toast.makeText(MainActivity.this, "Tour name already exists", Toast.LENGTH_LONG).show();
                                    } else {
                                        Intent intent = new Intent(MainActivity.this, TourActivity.class);
                                        DatabaseReference newChildRef = reference;
                                        newChildRef.setValue(0);
                                        intent.putExtra("tourName", newTourName);
                                        startActivity(intent);
                                        dialog.dismiss();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        } else {
                            Toast.makeText(MainActivity.this, "Please enter a tour name", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("/Users/admin");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                LinearLayout container = findViewById(R.id.scrollable_tours);
                container.removeAllViews();
                boolean empty = true;
                for (DataSnapshot tourSnapshot : snapshot.child("tours").getChildren()) {
                    String tourName = tourSnapshot.getKey();
                    createContainerView(tourName);
                    empty = false;
                }
                if (empty) {
                    TextView dynamicTextView = new TextView(MainActivity.this);
                    dynamicTextView.setText("Empty");

                    dynamicTextView.setTextSize(50);
                    dynamicTextView.setGravity(Gravity.CENTER);

                    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

                    dynamicTextView.setLayoutParams(layoutParams);
                    LinearLayout parentLayout = findViewById(R.id.scrollable_tours);
                    parentLayout.addView(dynamicTextView);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void createContainerView(String name) {
        LinearLayout containerLayout = findViewById(R.id.scrollable_tours);

        View container = getLayoutInflater().inflate(R.layout.sight_container, containerLayout, false);
        container.setId(View.generateViewId());
        TextView text = container.findViewById(R.id.sight_name);
        text.setText(name);
        Button editButton = container.findViewById(R.id.edit_button);
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TourActivity.class);
                intent.putExtra("tourName", name);
                startActivity(intent);
            }
        });

        containerLayout.addView(container);
    }

}
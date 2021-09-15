package com.example.indoorpositioning;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.model.LatLng;

public class MapActivity  extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState){
        Button button;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_activity);


        button = findViewById(R.id.back_button);
        button.setOnClickListener(v -> {
            startActivity(new Intent(MapActivity.this, MainActivity.class));

        });

        LatLng coordinates;
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            double[] value = extras.getDoubleArray("latlon");
            System.out.println("Value is: " + value[0]);
            //The key argument here must match that used in the other activity

        }


    }
}

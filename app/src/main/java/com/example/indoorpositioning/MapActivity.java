package com.example.indoorpositioning;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapActivity  extends AppCompatActivity implements OnMapReadyCallback {
    private Double lat;
    private Double lon;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        Button button;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_activity);


//        Here we initialise the map
        // Get the SupportMapFragment and request notification when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        //assert mapFragment != null;
        assert mapFragment != null;
        mapFragment.getMapAsync(this);


        button = findViewById(R.id.back_button);
        button.setOnClickListener(v -> {
            startActivity(new Intent(MapActivity.this, MainActivity.class));

        });

        LatLng coordinates;
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            double[] value = extras.getDoubleArray("latlon");
            lat = value[0];
            lon = value[1];
            System.out.println("Value is: " + value[0]);
            //The key argument here must match that used in the other activity
//            this.onMapReady(mapFragment.getMapAsync());
        }


    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        LatLng sydney = new LatLng(lat, lon);
        googleMap.addMarker(new MarkerOptions()
                .position(sydney)
                .title("Marker in Sydney"));
    }
}

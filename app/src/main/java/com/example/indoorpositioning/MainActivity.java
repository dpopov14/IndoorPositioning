package com.example.indoorpositioning;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Observer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.indoorpositioning.R;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import aga.android.luch.Beacon;
import aga.android.luch.BeaconScanner;
import aga.android.luch.Ranger;
import aga.android.luch.parsers.BeaconParserFactory;
import aga.android.luch.parsers.IBeaconParser;


public class MainActivity extends AppCompatActivity {
    Button button;
    // Layout for iBeacon
    String beaconLayout = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";
    // Create parser from layout
    IBeaconParser beaconParser = BeaconParserFactory.createFromLayout(beaconLayout);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        List<Beacon> beaconList = new ArrayList<>(); // External list for storing found beacons

        BeaconScanner beaconScanner = new BeaconScanner.Builder(this).setBeaconBatchListener(beacons -> {
            System.out.println("Scanning...");
            // Add to external list in order to pass beacons to the range finder (Hacky, fix later)
            beaconList.addAll(beacons);
            for (Beacon b : beacons) {
                System.out.println("Beacon with hardware address: "
                        + b.getHardwareAddress()
                        + "found");
            }
        })
                .setBeaconParser(beaconParser) // Set this scanners parser to the parser we created
                .build();
        // Create ranger
        Ranger ranger = beaconScanner.getRanger();

        // Check if app has permission for fine location and coarse location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Ask for permission in case app does not have it yet
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 42);
            return;
        }
        // Start the beaconScanner
        beaconScanner.start();

        // Find distance to all beacons found (Idk if this works, still needs to be tested on actual
        // beacons...)
        for (Beacon b : beaconList) {
            System.out.println("distance: " + ranger.calculateDistance(b));
        }





    }


}
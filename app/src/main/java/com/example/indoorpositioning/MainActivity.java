package com.example.indoorpositioning;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Pair;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.model.LatLng;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import aga.android.luch.Beacon;
import aga.android.luch.BeaconScanner;
import aga.android.luch.Ranger;
import aga.android.luch.parsers.BeaconParserFactory;
import aga.android.luch.parsers.IBeaconParser;
import aga.android.luch.rssi.ArmaFilter;


public class MainActivity extends AppCompatActivity {
    Button button;
    // Layout for iBeacon
    String beaconLayout = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";
    // Create parser from layout
    IBeaconParser beaconParser = BeaconParserFactory.createFromLayout(beaconLayout);
    Map<String, LatLng> beaconInfo = new HashMap<>();
    List<Beacon> beaconList = new ArrayList<>(); // External list for storing found beacons

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BeaconScanner beaconScanner = new BeaconScanner.Builder(this).setBeaconBatchListener(beacons -> {
            System.out.println("Scanning...");
            // Add to external list in order to pass beacons to the range finder (Hacky, fix later)
            for(Beacon b : beacons){
                boolean flag = true;
                for(Beacon b2 : beaconList){
                    if (b2.getIdentifierAsUuid(1).equals(b.getIdentifierAsUuid(1))){
                        flag = false;
                    }
                }
                if(flag){
                    beaconList.add(b);
                }
            }
           beaconList.addAll(beacons);
            for (Beacon b : beacons) {
                System.out.println("Beacon with hardware address: "
                        + b.getIdentifierAsUuid(1)
                        + "found");
            }
        })
                .setRangingEnabled(
                        new ArmaFilter.Builder()
                )
                .setBeaconParser(beaconParser)
                .build();
        // Check if app has permission for fine location and coarse location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Ask for permission in case app does not have it yet
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 42);
//            return;
        }

        button = findViewById(R.id.start_scan);
        button.setOnClickListener(v -> {
            // Start the beaconScanner
            beaconList.clear();
            beaconScanner.start();
        });

        button = findViewById(R.id.stop_scan);
        button.setOnClickListener(v -> {
            beaconScanner.stop();
            System.out.println("Stopped");
        });

        button = findViewById(R.id.print_button);
        button.setOnClickListener(v -> {

            System.out.println(beaconList);
            // Find distance to all beacons found (Idk if this works, still needs to be tested on actual
            // beacons...)
            Ranger ranger = beaconScanner.getRanger();
            if(beaconList.isEmpty()){
                System.out.println("No beacons detected yet.");
            }
            else {
                for (Beacon b : beaconList) {
                    System.out.println("distance: " + ranger.calculateDistance(b));
                }
            }

        });

        button = findViewById(R.id.result_button);
        button.setOnClickListener(v -> {
            Ranger ranger = beaconScanner.getRanger();
            Intent intent = new Intent(MainActivity.this, MapActivity.class);
            ArrayList<Double> sortedDistances = new ArrayList();
            Map<Double, LatLng> ourSortedCoordinates = this.getBestCoordinates(ranger);
            for (Double dist : ourSortedCoordinates.keySet()) {
                System.out.println("Sorted " + dist);
                sortedDistances.add(dist);
            }
            if(sortedDistances.size() < 3){
                System.out.println("Not enough beacons to determine location");
                return;
            }
            LatLng myLocation = getLocationByTrilateration(ourSortedCoordinates.get(sortedDistances.get(0)), sortedDistances.get(0), ourSortedCoordinates.get(sortedDistances.get(1)), sortedDistances.get(1),ourSortedCoordinates.get(sortedDistances.get(2)), sortedDistances.get(2));
            intent.putExtra("latlon", myLocation);
            System.out.println("\n \n \n"+ "Your location is: " + myLocation.toString());
            startActivity(intent);
        });

    }

    public Map<Double, LatLng> getBestCoordinates(Ranger ranger){
        Map<String,LatLng> hardwareAndCoordinates = null;
        Map<String, Double> hardwareAndDistance= new HashMap<>();
        Map<Double,LatLng> ourCoordinates = new HashMap<>();

        Intent intent = new Intent(MainActivity.this, MapActivity.class);


        for (Beacon b : beaconList) {
            hardwareAndDistance.put(b.getHardwareAddress(), ranger.calculateDistance(b));
        }

        try {
            hardwareAndCoordinates = this.getHardwareAndCoordinates();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(hardwareAndCoordinates != null){
            for(String key: hardwareAndCoordinates.keySet()){
                if(hardwareAndDistance.containsKey(key)){
                    ourCoordinates.put(hardwareAndDistance.get(key),hardwareAndCoordinates.get(key));
                }
            }
        }

        Map<Double, LatLng> ourSortedCoordinates = new TreeMap(ourCoordinates);
        return  ourSortedCoordinates;
    }


    public Map<String,LatLng> getHardwareAndCoordinates() throws IOException {
        Map<String,LatLng> hardCoordinates = new HashMap<>();

        InputStream myInput = getAssets().open("beacons-Ravelijn.xls");

        POIFSFileSystem myFileSystem = new POIFSFileSystem(myInput);

        HSSFWorkbook myWorkBook = new HSSFWorkbook(myFileSystem);

        HSSFSheet mySheet = myWorkBook.getSheetAt(0);
        int rowno =0;

        Iterator<Row> rowIter = mySheet.rowIterator();

        while (rowIter.hasNext()) {
            String currentAddress = null;
            String currentLat  = null;
            String currentLon = null;
            System.out.println("Row no " + rowno);
            HSSFRow myRow = (HSSFRow) rowIter.next();
            if(rowno !=0) {
                Iterator<Cell> cellIter = myRow.cellIterator();
                int colno =0;
                while (cellIter.hasNext()) {
                    HSSFCell myCell = (HSSFCell) cellIter.next();
                    if (colno==2){
                        currentAddress = myCell.toString();
                        System.out.println("YESSSSSSSS " + currentAddress);
                    }else if (colno==3){
                        currentLon = myCell.toString();
                        System.out.println(currentLon);
                    }else if (colno==4){
                        currentLat = myCell.toString();
                        System.out.println(currentLat);
                    }
                    if (currentLat != null && currentLon != null){
                        hardCoordinates.put(currentAddress,new LatLng(Double.parseDouble(currentLat),Double.parseDouble(currentLon)));
                    }
                    colno++;
                }
            }
            rowno++;
        }
        return hardCoordinates;
    }


    public LatLng getLocationByTrilateration(
            LatLng location1, double distance1,
            LatLng location2, double distance2,
            LatLng location3, double distance3){

        //DECLARE VARIABLES

        double[] P1   = new double[2];
        double[] P2   = new double[2];
        double[] P3   = new double[2];
        double[] ex   = new double[2];
        double[] ey   = new double[2];
        double[] p3p1 = new double[2];
        double jval  = 0;
        double temp  = 0;
        double ival  = 0;
        double p3p1i = 0;
        double triptx;
        double tripty;
        double xval;
        double yval;
        double t1;
        double t2;
        double t3;
        double t;
        double exx;
        double d;
        double eyy;

        //TRANSALTE POINTS TO VECTORS
        //POINT 1
        P1[0] = location1.latitude;
        P1[1] = location1.longitude;
        //POINT 2
        P2[0] = location2.latitude;
        P2[1] = location2.longitude;
        //POINT 3
        P3[0] = location3.latitude;
        P3[1] = location3.longitude;

        //TRANSFORM THE METERS VALUE FOR THE MAP UNIT
        //DISTANCE BETWEEN POINT 1 AND MY LOCATION
        distance1 = (distance1 / 100000);
        //DISTANCE BETWEEN POINT 2 AND MY LOCATION
        distance2 = (distance2 / 100000);
        //DISTANCE BETWEEN POINT 3 AND MY LOCATION
        distance3 = (distance3 / 100000);

        for (int i = 0; i < P1.length; i++) {
            t1   = P2[i];
            t2   = P1[i];
            t    = t1 - t2;
            temp += (t*t);
        }
        d = Math.sqrt(temp);
        for (int i = 0; i < P1.length; i++) {
            t1    = P2[i];
            t2    = P1[i];
            exx   = (t1 - t2)/(Math.sqrt(temp));
            ex[i] = exx;
        }
        for (int i = 0; i < P3.length; i++) {
            t1      = P3[i];
            t2      = P1[i];
            t3      = t1 - t2;
            p3p1[i] = t3;
        }
        for (int i = 0; i < ex.length; i++) {
            t1 = ex[i];
            t2 = p3p1[i];
            ival += (t1*t2);
        }
        for (int  i = 0; i < P3.length; i++) {
            t1 = P3[i];
            t2 = P1[i];
            t3 = ex[i] * ival;
            t  = t1 - t2 -t3;
            p3p1i += (t*t);
        }
        for (int i = 0; i < P3.length; i++) {
            t1 = P3[i];
            t2 = P1[i];
            t3 = ex[i] * ival;
            eyy = (t1 - t2 - t3)/Math.sqrt(p3p1i);
            ey[i] = eyy;
        }
        for (int i = 0; i < ey.length; i++) {
            t1 = ey[i];
            t2 = p3p1[i];
            jval += (t1*t2);
        }
        xval = (Math.pow(distance1, 2) - Math.pow(distance2, 2) + Math.pow(d, 2))/(2*d);
        yval = ((Math.pow(distance1, 2) - Math.pow(distance3, 2) + Math.pow(ival, 2) + Math.pow(jval, 2))/(2*jval)) - ((ival/jval)*xval);

        t1 = location1.latitude;
        t2 = ex[0] * xval;
        t3 = ey[0] * yval;
        triptx = t1 + t2 + t3;

        t1 = location1.longitude;
        t2 = ex[1] * xval;
        t3 = ey[1] * yval;
        tripty = t1 + t2 + t3;


        return new LatLng(triptx,tripty);

    }


}
package com.example.offsetcalculator.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.offsetcalculator.model.emission.CarEmission;
import com.example.offsetcalculator.model.route.Coordinate;
import com.example.offsetcalculator.model.route.Route;
import com.example.offsetcalculator.rep.CarEmissionRepository;
import com.example.offsetcalculator.rep.RouteRepository;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LocationService extends Service {

    private FusedLocationProviderClient mFusedLocationClient;
    //TODO change it back to five
    private final static long UPDATE_INTERVAL = (60 * 1000) * 1;  /* UPDATE EVERY 5 MINS */
    private final static long FASTEST_INTERVAL = 2000; /* 2 sec */
    private final static double MILES_CONVERSATION = 0.00062137;
    private Route route;
    private RouteRepository routeRepository;
    private List<Coordinate> mCoordinates;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        routeRepository = new RouteRepository(getApplication());
        route = new Route(routeRepository.generateId(), new Date().toString()); //initialize a route here because it needs be initialized before the coordinates this method increments the id

        if (Build.VERSION.SDK_INT >= 26) { // android demands that api levels above 26 require that the user is notified with the service.
            String CHANNEL_ID = "my_channel_01";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "My Channel",
                    NotificationManager
                            .IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("")
                    .setContentText("").build();

            startForeground(1, notification);
        }

    }

    //TODO is this method called when the user cancels the service?
    @Override
    public void onDestroy() {
        super.onDestroy();
        //save the coords in the route
        routeRepository.insert(route, mCoordinates);
        Log.d("Service", "Service has stopped");
        for(Coordinate coordinate : mCoordinates) {
            Log.d("Service", coordinate.toString());
            Log.d("Route ID", String.valueOf(route.getId()));
        }

        //create some emissions to test the service
        Double distanceResult  = routeRepository.calculateDistance(mCoordinates); //distance is on meters and needs to be converted to miles. m * 0.00062137 to get miles
        Double distance = distanceResult * MILES_CONVERSATION;

        Log.d("@@@ Distance", distance.toString());

        CarEmissionRepository carEmissionRepository = new CarEmissionRepository(getApplication());
        carEmissionRepository.insert(new CarEmission(distance, 2.0));

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("OnStartCommand", "Service has started!");
        getLocation();
        return START_NOT_STICKY;
    }

    private void getLocation() {

        // ---------------------------------- LocationRequest ------------------------------------
        // Create the location request to start receiving updates
        LocationRequest mLocationRequestHighAccuracy = new LocationRequest();
        //@@@ might want to change as it uses a lot of battery
        mLocationRequestHighAccuracy.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequestHighAccuracy.setInterval(UPDATE_INTERVAL);
        mLocationRequestHighAccuracy.setFastestInterval(FASTEST_INTERVAL);


        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("Location", "getLocation: stopping the location service.");
            stopSelf();
            return;
        }
        Log.d("Location", "getLocation: getting location information.");

        final List<Coordinate> coordinates = new ArrayList<>();

        mFusedLocationClient.requestLocationUpdates(mLocationRequestHighAccuracy, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {

                        Log.d("Result", "onLocationResult: got location result.");

                        Location location = locationResult.getLastLocation();
                        if (location != null) {
                            Coordinate coordinate = new Coordinate(location.getLatitude(), location.getLongitude(), route.getId());
                            Log.d("Location", coordinate.toString());
                            coordinates.add(coordinate);
                        }
                    }
                },
                Looper.myLooper()); // Looper.myLooper tells this to repeat forever until thread is destroyed

        //set the global scope list to be equal to the generated list on the method
        mCoordinates = coordinates;
    }
}

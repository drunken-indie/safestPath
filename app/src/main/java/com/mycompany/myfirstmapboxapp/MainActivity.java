//NOTE: Some of the MainActivity.java code was from an android tutorial provided by Mapbox
// with some modifications to fit our project's requirements

package com.mycompany.myfirstmapboxapp;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.*;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.graphics.Color;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Toast;

import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationSource;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.services.android.ui.geocoder.GeocoderAutoCompleteView;
import com.mapbox.services.api.ServicesException;
import com.mapbox.services.api.directions.v5.DirectionsCriteria;
import com.mapbox.services.api.directions.v5.MapboxDirections;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.api.geocoding.v5.GeocodingCriteria;
import com.mapbox.services.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.models.Position;
import static com.mapbox.services.Constants.PRECISION_6;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MainActivity extends Activity {

    private MapView mapView;
    private FloatingActionButton floatingActionButton;
    private LocationEngine locationServices;
    private MapboxMap map;
    private Location lastLocation;
    private DirectionsRoute currentRoute;
    private MapboxDirections client;
    private Button searchButton, newButton;
    private GeocoderAutoCompleteView autocompleteStart, autocompleteEnd;
    private Position origin, destination;
    private Client c;
    private boolean searchBox1, searchBox2;

    private static final String TAG = "MainActivity";
    private static final int PERMISSIONS_LOCATION = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        new Thread(new Runnable() {
            public void run() {
                setSearch();
            }
        }).start();

        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_location);

        locationServices = LocationSource.getLocationEngine(this);
        locationServices.activate();

        newButton = (Button) findViewById(R.id.NewButton);
        searchButton = (Button) findViewById(R.id.searchButton);
        searchButton.setVisibility(View.INVISIBLE);

        // Create a mapView
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        // Add a MapboxMap
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                map = mapboxMap;
                autocompleteStart = (GeocoderAutoCompleteView) findViewById(R.id.start);
                autocompleteStart.setAccessToken(Mapbox.getAccessToken());
                autocompleteStart.setType(GeocodingCriteria.TYPE_ADDRESS);
                autocompleteEnd = (GeocoderAutoCompleteView) findViewById(R.id.end);
                autocompleteEnd.setAccessToken(Mapbox.getAccessToken());
                autocompleteEnd.setType(GeocodingCriteria.TYPE_ADDRESS);


                newButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        searchButton.setVisibility(View.VISIBLE);
                        autoSearch();
                    }
                });
                searchButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //if Both search box is filled, draw a route, and marker
                        if (searchBox1 && searchBox2)
                            drawIt(map, origin, destination);
                        //if not, pop up a message to fill it up
                        else
                            Toast.makeText(
                                    MainActivity.this,
                                    "Please set Origin and Destination!",
                                    Toast.LENGTH_SHORT).show();
                    }

                });
            }

        });

        // Location button
        floatingActionButton = (FloatingActionButton) findViewById(R.id.location_toggle_fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (map != null) {
                    try {
                        toggleGps(!map.isMyLocationEnabled());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
    private void setSearch(){
        c = new Client(this, 0.005);
        new Thread(new Runnable() {
            public void run() {
                try {
                    c.readFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }

    private void setMarker(double latitude, double longitude) throws IOException {
        double latitude1 = latitude;
        double longitude1 = longitude;
        c.setLatLon(latitude1, longitude1);
        c.Search();

        ArrayList<Double> longResults = c.getLongInRange();
        ArrayList<Double> latResults = c.getLatInRange();
        ArrayList<String> titles = c.getTitles();
        ArrayList<String> snippets = c.getSnippets();


        for (int i = 0; i < longResults.size(); i++){
            map.addMarker(new MarkerOptions()
                    .position(new LatLng(latResults.get(i), longResults.get(i)))
                    .title(titles.get(i))
                    .snippet(snippets.get(i)));
        }


    }
    private void autoSearch(){
        searchBox1 = false;
        searchBox2 = false;
        newButton.setVisibility(View.INVISIBLE);
        autocompleteStart.setVisibility(View.VISIBLE);
        autocompleteEnd.setVisibility(View.VISIBLE);
        autocompleteStart.setOnFeatureListener(new GeocoderAutoCompleteView.OnFeatureListener() {
            @Override
            public void onFeatureClick(CarmenFeature feature) {
                hideOnScreenKeyboard();
                origin = feature.asPosition();
                searchBox1 = true;
            }
        });
        autocompleteEnd.setOnFeatureListener(new GeocoderAutoCompleteView.OnFeatureListener() {
            @Override
            public void onFeatureClick(CarmenFeature feature) {
                hideOnScreenKeyboard();
                destination = feature.asPosition();
                searchBox2 = true;
            }
        });
    }

    private void getRoute(Position origin, Position destination) throws ServicesException {

        client = new MapboxDirections.Builder()
                .setOrigin(origin)
                .setDestination(destination)
                .setOverview(DirectionsCriteria.OVERVIEW_FULL)
                .setProfile(DirectionsCriteria.PROFILE_CYCLING)
                .setAccessToken(Mapbox.getAccessToken())
                .build();

        client.enqueueCall(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                // You can get the generic HTTP info about the response
                Log.d(TAG, "Response code: " + response.code());
                if (response.body() == null) {
                    Log.e(TAG, "No routes found, make sure you set the right user and access token.");
                    return;
                } else if (response.body().getRoutes().size() < 1) {
                    Log.e(TAG, "No routes found");
                    return;
                }

                // Print some info about the route
                currentRoute = response.body().getRoutes().get(0);
                Log.d(TAG, "Distance: " + currentRoute.getDistance());
                Toast.makeText(
                        MainActivity.this,
                        "Route is " + currentRoute.getDistance() + " meters long.",
                        Toast.LENGTH_SHORT).show();

                // Draw the route on the map
                try {
                    drawRoute(currentRoute);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                Log.e(TAG, "Error: " + throwable.getMessage());
                Toast.makeText(MainActivity.this, "Error: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void drawIt(MapboxMap mapboxMap, Position origin, Position destination){
        autocompleteStart.setVisibility(View.INVISIBLE);
        autocompleteEnd.setVisibility(View.INVISIBLE);
        // Add origin and destination to the map
        mapboxMap.addMarker(new MarkerOptions()
                .position(new LatLng(origin.getLatitude(), origin.getLongitude()))
                .title("Origin"));
        //private String snippet;
        mapboxMap.addMarker(new MarkerOptions()
                .position(new LatLng(destination.getLatitude(), destination.getLongitude()))
                .title("Destination"));

        // Get route from API
        try {
            getRoute(origin, destination);
        } catch (ServicesException servicesException) {
            servicesException.printStackTrace();
        }
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng((origin.getLatitude()+destination.getLatitude())/2, (origin.getLongitude()+destination.getLongitude())/2), 10));
        searchButton.setVisibility(View.INVISIBLE);
        newButton.setVisibility(View.VISIBLE);
    }

    private void drawRoute(DirectionsRoute route) throws IOException {
        // Convert LineString coordinates into LatLng[]
        LineString lineString = LineString.fromPolyline(route.getGeometry(), PRECISION_6);
        List<Position> coordinates = lineString.getCoordinates();
        LatLng[] points = new LatLng[coordinates.size()];
        for (int i = 0; i < coordinates.size(); i++) {
            points[i] = new LatLng(
                    coordinates.get(i).getLatitude(),
                    coordinates.get(i).getLongitude());
            setMarker(coordinates.get(i).getLatitude(), coordinates.get(i).getLongitude());
        }



        // Draw Points on MapView
        map.addPolyline(new PolylineOptions()
                .add(points)
                .color(Color.parseColor("#009688"))
                .width(5));



    }

    private void hideOnScreenKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (getCurrentFocus() != null) {
                imm.hideSoftInputFromWindow(MainActivity.this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        } catch (Exception exception) {

            throw new RuntimeException(exception);
        }

    }

    private void toggleGps(boolean enableGps) throws IOException {
        boolean enabled = false;
        double lat;
        double lon;
        if (enableGps) {
            // Check if user has granted location permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_LOCATION);
            } else {
                enabled = true;
                // If we have the last location of the user, we can move the camera to that position.
                lastLocation = locationServices.getLastLocation();

                if (lastLocation != null) {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation), 16));
                    lat = lastLocation.getLatitude();
                    lon = lastLocation.getLongitude();
                    Location(lat, lon);
                }
                locationServices.addLocationEngineListener(new LocationEngineListener() {
                    @Override
                    public void onConnected() {

                    }

                    @Override
                    public void onLocationChanged(Location location){
                        if (location != null) {
                            // Move the map camera to where the user location is and then remove the
                            // listener so the camera isn't constantly updating when the user location
                            // changes. When the user disables and then enables the location again, this
                            // listener is registered again and will adjust the camera once again.
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location), 16));
                            locationServices.removeLocationEngineListener(this);
                            try {
                                Location(location.getLatitude(), location.getLongitude());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                });
                floatingActionButton.setImageResource(R.drawable.ic_location_disabled_24dp);
            }
        } else {
            floatingActionButton.setImageResource(R.drawable.ic_my_location_24dp);
        }
        // Enable or disable the location layer on the map
        map.setMyLocationEnabled(enabled);
    }

    public void Location(double latitude, double longitude) throws IOException{
        //creates markers for every crime within +/- 0.005 the latitude and longitude
        setMarker(latitude, longitude);
        c.Search();
        //returns a boolean if the number of crimes surrounding the user's location exceed a certain threshold
        boolean search = c.getCheck();
        if (search){
            //Create a popup message
            Snackbar msg = Snackbar.make(mapView, "AREA NOT SAFE",Snackbar.LENGTH_LONG);
            msg.show();
        }
        else if (!search){
            //Create a popup message
            Snackbar msg = Snackbar.make(mapView, "Area is safe",Snackbar.LENGTH_LONG);
            msg.show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                try {
                    toggleGps(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
}

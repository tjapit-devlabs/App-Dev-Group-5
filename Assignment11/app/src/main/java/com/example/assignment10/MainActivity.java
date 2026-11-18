package com.example.assignment10;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CustomCap;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
GoogleMap.OnPolylineClickListener, GoogleMap.OnPolygonClickListener,
        LoginFragment.LoginListener,
        RegisterFragment.RegisterListener {

    private final OkHttpClient client = new OkHttpClient();
    private ArrayList<LatLng> points = new ArrayList();
    private GoogleMap googleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(FirebaseAuth.getInstance().getCurrentUser() == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main, new LoginFragment())
                    .commit();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main, new CreateTripFragment())
                    .commit();
        }

        /*

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        HttpUrl url = HttpUrl.parse("https://www.theappsdr.com/map/route").newBuilder()
                .build();

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()) {
                    String body = response.body().string();
                    Log.d("test", body);

                    try {
                        JSONObject jsonObject = new JSONObject(body);
                        JSONArray pathJson = jsonObject.getJSONArray("path");
                        for(int i = 0; i < pathJson.length(); i++) {
                            JSONObject latlonJson = pathJson.getJSONObject(i);

                            double lat = latlonJson.getDouble("latitude");
                            double lon = latlonJson.getDouble("longitude");

                            Log.d("test", lat + " lat, " + lon + " lon");

                            points.add(new LatLng(lat, lon));
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                plotTrip();
                            }
                        });
                    } catch(JSONException e) {

                    }
                }
            }
        });
         */
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
    }

    private void plotTrip() {
        if (googleMap == null || points.isEmpty()) {
            return;
        }

        // Draw route
        googleMap.addPolyline(
                new PolylineOptions()
                        .addAll(points)
                        .width(12)
                        .color(android.graphics.Color.BLUE)
        );

        // START marker
        googleMap.addMarker(
                new MarkerOptions()
                        .position(points.get(0))
                        .title("START")
                        .icon(BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_GREEN))
        );

        // END marker
        googleMap.addMarker(
                new MarkerOptions()
                        .position(points.get(points.size() - 1))
                        .title("END")
                        .icon(BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_RED))
        );

        // Calculate map boundaries
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for (LatLng point : points) {
            builder.include(point);
        }

        // Zoom to include all trip points
        googleMap.animateCamera(
                com.google.android.gms.maps.CameraUpdateFactory
                        .newLatLngBounds(builder.build(), 100)
        );
    }

    @Override
    public void onPolygonClick(@NonNull Polygon polygon) {

    }

    @Override
    public void onPolylineClick(@NonNull Polyline polyline) {

    }

    @Override
    public void onLoginSuccess() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main, new CreateTripFragment())
                .commit();
    }

    @Override
    public void onRegisterSuccess() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main, new CreateTripFragment())
                .commit();
    }

    @Override
    public void gotoLoginAccount() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main, new LoginFragment())
                .commit();
    }

    @Override
    public void gotoCreateAccount() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main, new RegisterFragment())
                .commit();
    }
}
package com.example.assignment10;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.assignment10.databinding.FragmentCreateTripBinding;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.PlaceAutocomplete;
import com.google.android.libraries.places.widget.PlaceAutocompleteActivity;
import com.google.common.base.Preconditions;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CreateTripFragment extends Fragment implements OnMapReadyCallback {

    public CreateTripFragment() {
        // Required empty public constructor
    }

    FragmentCreateTripBinding binding;
    private PlacesClient placesClient;
    private Place startPlace, endPlace;
    private LatLng startLatLng, endLatLng;
    private String startAddress, endAddress;
    private GoogleMap googleMap;
    private final OkHttpClient client = new OkHttpClient();

    private boolean selectingStartLoc;

    private ActivityResultLauncher<Intent> placeAutocompleteActivityResultLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCreateTripBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            startLatLng = savedInstanceState.getParcelable("startLatLng");
            endLatLng = savedInstanceState.getParcelable("endLatLng");
            startAddress = savedInstanceState.getString("startAddress");
            endAddress = savedInstanceState.getString("endAddress");
            if (startAddress != null) binding.editTextStartLoc.setText(startAddress);
            if (endAddress != null) binding.editTextEndLoc.setText(endAddress);
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Provide the API key that has enabled "Places API (New)" in the Google Cloud Console.
        Places.initializeWithNewPlacesApiEnabled(
                requireContext().getApplicationContext(),
                BuildConfig.MAPS_API_KEY);

        // Create session token
        AutocompleteSessionToken sessionToken = AutocompleteSessionToken.newInstance();

        Intent autocompleteIntent =
                new PlaceAutocomplete.IntentBuilder()
                        // ... set input params for origin, countries, types filter ...
                        .setAutocompleteSessionToken(sessionToken) // optional
                        .build(requireActivity());

        placeAutocompleteActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        Intent intent = result.getData();
                        if (result.getResultCode() == PlaceAutocompleteActivity.RESULT_OK) {
                            // get prediction object
                            AutocompletePrediction prediction =
                                    PlaceAutocomplete.getPredictionFromIntent(
                                            Preconditions.checkNotNull(intent));

                            // get session token
                            AutocompleteSessionToken sessionToken =
                                    PlaceAutocomplete.getSessionTokenFromIntent(
                                            Preconditions.checkNotNull(intent));

                            // create PlacesClient to make FetchPlace request (optional)
                            PlacesClient placesClient = Places.createClient(requireContext());
                            FetchPlaceRequest request =
                                    FetchPlaceRequest.builder(prediction.getPlaceId(),
                                                    Arrays.asList(Place.Field.DISPLAY_NAME,
                                                            Place.Field.LOCATION))
                                            .setSessionToken(sessionToken).build();

                            Task<FetchPlaceResponse> task = placesClient.fetchPlace(request);

                            task.addOnSuccessListener(new OnSuccessListener<FetchPlaceResponse>() {
                                @Override
                                public void onSuccess(FetchPlaceResponse response) {
                                    Place place = response.getPlace();

                                    Log.d("test", place.toString());

                                    String address = place.getDisplayName();
                                    LatLng location = place.getLocation();

                                    if(selectingStartLoc) {
                                        startPlace = place;
                                        startLatLng = location;
                                        startAddress = address;
                                        binding.editTextStartLoc.setText(address);
                                    } else {
                                        endPlace = place;
                                        endLatLng = location;
                                        endAddress = address;
                                        binding.editTextEndLoc.setText(address);
                                    }
                                }
                            });
                        }
                    }
                }
        );

        binding.editTextStartLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectingStartLoc = true;
                // Launch Activity
                placeAutocompleteActivityResultLauncher.launch(autocompleteIntent);
            }
        });

        binding.editTextEndLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectingStartLoc = false;
                // Launch Activity
                placeAutocompleteActivityResultLauncher.launch(autocompleteIntent);
            }
        });

        binding.buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (startLatLng != null && endLatLng != null) {
                    fetchRouteAndPlot();
                } else {
                    Toast.makeText(requireContext(), "Please select both start and end locations", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("startLatLng", startLatLng);
        outState.putParcelable("endLatLng", endLatLng);
        outState.putString("startAddress", startAddress);
        outState.putString("endAddress", endAddress);
    }

    private void fetchRouteAndPlot() {
        if (startLatLng == null || endLatLng == null) {
            return;
        }

        try {
            JSONObject originJson = new JSONObject();
            JSONObject originLocation = new JSONObject();
            JSONObject originLatLngObj = new JSONObject();
            originLatLngObj.put("latitude", startLatLng.latitude);
            originLatLngObj.put("longitude", startLatLng.longitude);
            originLocation.put("latLng", originLatLngObj);
            originJson.put("location", originLocation);

            JSONObject destJson = new JSONObject();
            JSONObject destLocation = new JSONObject();
            JSONObject destLatLngObj = new JSONObject();
            destLatLngObj.put("latitude", endLatLng.latitude);
            destLatLngObj.put("longitude", endLatLng.longitude);
            destLocation.put("latLng", destLatLngObj);
            destJson.put("location", destLocation);

            JSONObject requestBodyJson = new JSONObject();
            requestBodyJson.put("origin", originJson);
            requestBodyJson.put("destination", destJson);
            requestBodyJson.put("travelMode", "DRIVE");

            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(requestBodyJson.toString(), mediaType);

            Request request = new Request.Builder()
                    .url("https://routes.googleapis.com/directions/v2:computeRoutes")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-Goog-Api-Key", BuildConfig.MAPS_API_KEY)
                    .addHeader("X-Goog-FieldMask", "routes.polyline.encodedPolyline")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("CreateTripFragment", "Failed to fetch route from Routes API", e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        Log.d("test", responseBody);

                        try {
                            JSONObject jsonObject = new JSONObject(responseBody);
                            JSONArray routes = jsonObject.optJSONArray("routes");
                            if (routes != null && routes.length() > 0) {
                                JSONObject route = routes.getJSONObject(0);
                                JSONObject polyline = route.optJSONObject("polyline");
                                if (polyline != null) {
                                    String encodedPolyline = polyline.optString("encodedPolyline");
                                    if (encodedPolyline != null && !encodedPolyline.isEmpty()) {
                                        List<LatLng> points = PolyUtil.decode(encodedPolyline);
                                        if (points != null && !points.isEmpty()) {
                                            if (getActivity() != null) {
                                                getActivity().runOnUiThread(() -> plotTrip(new ArrayList<>(points)));
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (JSONException e) {
                            Log.e("CreateTripFragment", "JSON parsing error", e);
                        }
                    } else {
                        Log.e("CreateTripFragment", "Routes API error response: " + response.code() + " " + response.message());
                        if (response.body() != null) {
                            Log.e("CreateTripFragment", "Error body: " + response.body().string());
                        }
                    }
                }
            });

        } catch (JSONException e) {
            Log.e("CreateTripFragment", "Error building JSON request for Routes API", e);
        }
    }

    private void plotTrip(ArrayList<LatLng> points) {
        if (googleMap == null || points.isEmpty()) {
            return;
        }

        googleMap.clear();

        // Draw route polyline
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

        googleMap.animateCamera(
                com.google.android.gms.maps.CameraUpdateFactory
                        .newLatLngBounds(builder.build(), 100)
        );
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
    }
}

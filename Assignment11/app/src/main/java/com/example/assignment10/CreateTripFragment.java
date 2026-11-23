package com.example.assignment10;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.example.assignment10.databinding.FragmentCreateTripBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
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
    private Place startPlace, endPlace;
    private LatLng startLatLng, endLatLng;
    private String startAddress, endAddress;
    private GoogleMap googleMap;
    private final OkHttpClient client = new OkHttpClient();

    private String encodedPolyline = "";
    private List<GasStation> currentGasStations = new ArrayList<>();
    private int fetchedCount = 0;

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

        Places.initializeWithNewPlacesApiEnabled(
                requireContext().getApplicationContext(),
                BuildConfig.MAPS_API_KEY);

        binding.editTextStartLoc.setOnClickListener(v -> showPlaceAutocompleteDialog(true));

        binding.editTextEndLoc.setOnClickListener(v -> showPlaceAutocompleteDialog(false));

        binding.buttonSubmit.setOnClickListener(v -> {
            if (startLatLng != null && endLatLng != null) {
                fetchRouteAndPlot();
            } else {
                Toast.makeText(requireContext(), "Please select both start and end locations", Toast.LENGTH_SHORT).show();
            }
        });

        binding.buttonSave.setOnClickListener(v -> saveTripToFirebase());

        binding.buttonViewTrips.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main, new TripsFragment())
                    .commit();
        });

        binding.buttonLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main, new LoginFragment())
                    .commit();
        });

        if (getArguments() != null && getArguments().containsKey("selectedTrip")) {
            Trip selectedTrip = (Trip) getArguments().getSerializable("selectedTrip");
            if (selectedTrip != null) {
                loadSelectedTrip(selectedTrip);
            }
        }
    }

    private void showPlaceAutocompleteDialog(boolean isStart) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_place_autocomplete, null);
        builder.setView(view);

        EditText editTextSearch = view.findViewById(R.id.editTextSearchQuery);
        ListView listView = view.findViewById(R.id.listViewPredictions);

        AlertDialog dialog = builder.create();

        List<String> predictionDescriptions = new ArrayList<>();
        List<AutocompletePrediction> predictionList = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, predictionDescriptions);
        listView.setAdapter(adapter);

        PlacesClient placesClient = Places.createClient(requireContext());

        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                            .setQuery(s.toString())
                            .build();

                    placesClient.findAutocompletePredictions(request).addOnSuccessListener(response -> {
                        predictionList.clear();
                        predictionList.addAll(response.getAutocompletePredictions());
                        predictionDescriptions.clear();
                        for (AutocompletePrediction p : predictionList) {
                            predictionDescriptions.add(p.getFullText(null).toString());
                        }
                        adapter.notifyDataSetChanged();
                    }).addOnFailureListener(e -> {
                        Log.e("CreateTripFragment", "Autocomplete error: " + e.getMessage(), e);
                    });
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        listView.setOnItemClickListener((parent, view1, position, id) -> {
            if (position >= 0 && position < predictionList.size()) {
                AutocompletePrediction prediction = predictionList.get(position);
                FetchPlaceRequest fetchRequest = FetchPlaceRequest.builder(
                        prediction.getPlaceId(),
                        Arrays.asList(Place.Field.DISPLAY_NAME, Place.Field.LOCATION))
                        .build();

                placesClient.fetchPlace(fetchRequest).addOnSuccessListener(fetchResponse -> {
                    Place place = fetchResponse.getPlace();
                    String address = place.getDisplayName();
                    LatLng location = place.getLocation();

                    if (isStart) {
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
                    dialog.dismiss();
                }).addOnFailureListener(e -> {
                    Log.e("CreateTripFragment", "Fetch place error: " + e.getMessage(), e);
                });
            }
        });

        dialog.show();
    }

    private void loadSelectedTrip(Trip trip) {
        startAddress = trip.getStartAddress();
        endAddress = trip.getEndAddress();
        binding.editTextStartLoc.setText(startAddress);
        binding.editTextEndLoc.setText(endAddress);
        startLatLng = new LatLng(trip.getStartLat(), trip.getStartLng());
        endLatLng = new LatLng(trip.getEndLat(), trip.getEndLng());
        encodedPolyline = trip.getEncodedPolyline();
        currentGasStations = trip.getGasStations() != null ? trip.getGasStations() : new ArrayList<>();

        if (encodedPolyline != null && !encodedPolyline.isEmpty()) {
            List<LatLng> points = PolyUtil.decode(encodedPolyline);
            if (googleMap != null) {
                plotTripWithGasStations(new ArrayList<>(points), currentGasStations);
            }
        }
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
                        try {
                            JSONObject jsonObject = new JSONObject(responseBody);
                            JSONArray routes = jsonObject.optJSONArray("routes");
                            if (routes != null && routes.length() > 0) {
                                JSONObject route = routes.getJSONObject(0);
                                JSONObject polyline = route.optJSONObject("polyline");
                                if (polyline != null) {
                                    encodedPolyline = polyline.optString("encodedPolyline");
                                    if (encodedPolyline != null && !encodedPolyline.isEmpty()) {
                                        List<LatLng> points = PolyUtil.decode(encodedPolyline);
                                        if (points != null && !points.isEmpty()) {
                                            findGasStationsAndPlot(new ArrayList<>(points));
                                        }
                                    }
                                }
                            }
                        } catch (JSONException e) {
                            Log.e("CreateTripFragment", "JSON parsing error", e);
                        }
                    }
                }
            });

        } catch (JSONException e) {
            Log.e("CreateTripFragment", "Error building JSON request for Routes API", e);
        }
    }

    private void findGasStationsAndPlot(ArrayList<LatLng> points) {
        if (points.isEmpty()) return;

        LatLng start = points.get(0);
        LatLng end = points.get(points.size() - 1);
        LatLng mid = points.get(points.size() / 2);

        List<GasStation> stations = new ArrayList<>();
        fetchedCount = 0;

        fetchGasStationForLocation(start, "START", stations, points);
        fetchGasStationForLocation(mid, "MID", stations, points);
        fetchGasStationForLocation(end, "END", stations, points);
    }

    private void fetchGasStationForLocation(LatLng latLng, String type, List<GasStation> stations, ArrayList<LatLng> points) {
        try {
            JSONObject requestBodyJson = new JSONObject();
            JSONArray includedTypes = new JSONArray();
            includedTypes.put("gas_station");
            requestBodyJson.put("includedTypes", includedTypes);
            requestBodyJson.put("maxResultCount", 1);

            JSONObject circle = new JSONObject();
            JSONObject center = new JSONObject();
            center.put("latitude", latLng.latitude);
            center.put("longitude", latLng.longitude);
            circle.put("center", center);
            circle.put("radius", 5000.0);

            JSONObject locationRestriction = new JSONObject();
            locationRestriction.put("circle", circle);
            requestBodyJson.put("locationRestriction", locationRestriction);

            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(requestBodyJson.toString(), mediaType);

            Request request = new Request.Builder()
                    .url("https://places.googleapis.com/v1/places:searchNearby")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-Goog-Api-Key", BuildConfig.MAPS_API_KEY)
                    .addHeader("X-Goog-FieldMask", "places.displayName,places.location")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("CreateTripFragment", "Failed to fetch gas station for " + type, e);
                    checkAndPlot(stations, points);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        String resStr = response.body().string();
                        try {
                            JSONObject jsonObject = new JSONObject(resStr);
                            JSONArray places = jsonObject.optJSONArray("places");
                            if (places != null && places.length() > 0) {
                                JSONObject place = places.getJSONObject(0);
                                JSONObject nameObj = place.optJSONObject("displayName");
                                String name = nameObj != null ? nameObj.optString("text", "Gas Station") : "Gas Station";
                                JSONObject location = place.optJSONObject("location");
                                double lat = location != null ? location.optDouble("latitude") : 0.0;
                                double lng = location != null ? location.optDouble("longitude") : 0.0;

                                synchronized (stations) {
                                    stations.add(new GasStation(name, lat, lng, type));
                                }
                            }
                        } catch (JSONException e) {
                            Log.e("CreateTripFragment", "JSON parse error for gas station", e);
                        }
                    }
                    checkAndPlot(stations, points);
                }
            });
        } catch (Exception e) {
            Log.e("CreateTripFragment", "Error building gas station request", e);
            checkAndPlot(stations, points);
        }
    }

    private synchronized void checkAndPlot(List<GasStation> stations, ArrayList<LatLng> points) {
        fetchedCount++;
        if (fetchedCount >= 3) {
            fetchedCount = 0;
            currentGasStations = stations;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> plotTripWithGasStations(points, stations));
            }
        }
    }

    private void plotTripWithGasStations(ArrayList<LatLng> points, List<GasStation> stations) {
        if (googleMap == null || points.isEmpty()) {
            return;
        }

        googleMap.clear();

        // Draw route polyline
        googleMap.addPolyline(
                new PolylineOptions()
                        .addAll(points)
                        .width(12)
                        .color(Color.BLUE)
        );

        // START marker
        googleMap.addMarker(
                new MarkerOptions()
                        .position(points.get(0))
                        .title("START")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        );

        // END marker
        googleMap.addMarker(
                new MarkerOptions()
                        .position(points.get(points.size() - 1))
                        .title("END")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        );

        // Gas Station markers
        for (GasStation station : stations) {
            googleMap.addMarker(
                    new MarkerOptions()
                            .position(new LatLng(station.getLatitude(), station.getLongitude()))
                            .title("Gas Station (" + station.getLocationType() + "): " + station.getName())
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            );
        }

        // Calculate map boundaries
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : points) {
            builder.include(point);
        }
        for (GasStation station : stations) {
            builder.include(new LatLng(station.getLatitude(), station.getLongitude()));
        }

        googleMap.animateCamera(
                CameraUpdateFactory
                        .newLatLngBounds(builder.build(), 100)
        );
    }

    private void saveTripToFirebase() {
        if (encodedPolyline == null || encodedPolyline.isEmpty()) {
            Toast.makeText(requireContext(), "Please compute route first", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        Trip trip = new Trip();
        trip.setUserId(user.getUid());
        trip.setTripName((startAddress != null ? startAddress : "Start") + " to " + (endAddress != null ? endAddress : "End"));
        trip.setStartAddress(startAddress);
        trip.setEndAddress(endAddress);
        trip.setStartLat(startLatLng.latitude);
        trip.setStartLng(startLatLng.longitude);
        trip.setEndLat(endLatLng.latitude);
        trip.setEndLng(endLatLng.longitude);
        trip.setEncodedPolyline(encodedPolyline);
        trip.setGasStations(currentGasStations);
        trip.setTimestamp(System.currentTimeMillis());

        FirebaseFirestore.getInstance().collection("trips")
                .add(trip)
                .addOnSuccessListener(documentReference -> {
                    if (!isAdded() || getContext() == null) return;
                    trip.setId(documentReference.getId());
                    documentReference.update("id", documentReference.getId());
                    Toast.makeText(requireContext(), "Trip saved to Firebase successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || getContext() == null) return;
                    Toast.makeText(requireContext(), "Failed to save trip: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        if (getArguments() != null && getArguments().containsKey("selectedTrip")) {
            Trip selectedTrip = (Trip) getArguments().getSerializable("selectedTrip");
            if (selectedTrip != null && selectedTrip.getEncodedPolyline() != null) {
                List<LatLng> points = PolyUtil.decode(selectedTrip.getEncodedPolyline());
                plotTripWithGasStations(new ArrayList<>(points), selectedTrip.getGasStations());
            }
        }
    }
}

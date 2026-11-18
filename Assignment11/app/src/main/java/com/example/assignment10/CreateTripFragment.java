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

import com.example.assignment10.databinding.FragmentCreateTripBinding;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
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

import java.util.Arrays;

public class CreateTripFragment extends Fragment implements OnMapReadyCallback {

    public CreateTripFragment() {
        // Required empty public constructor
    }

    FragmentCreateTripBinding binding;
    private PlacesClient placesClient;
    private Place startPlace, endPlace;
    private GoogleMap googleMap;

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
                                        binding.editTextStartLoc.setText(address);
                                    } else {
                                        endPlace = place;
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

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
    }
}
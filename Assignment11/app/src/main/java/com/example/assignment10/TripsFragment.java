package com.example.assignment10;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.assignment10.databinding.FragmentTripsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TripsFragment extends Fragment {

    public TripsFragment() {
        // Required empty public constructor
    }

    FragmentTripsBinding binding;
    private TripsAdapter adapter;
    private List<Trip> tripList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentTripsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.recyclerViewTrips.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TripsAdapter(tripList, trip -> {
            CreateTripFragment fragment = new CreateTripFragment();
            Bundle args = new Bundle();
            args.putSerializable("selectedTrip", trip);
            fragment.setArguments(args);
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main, fragment)
                    .commit();
        });
        binding.recyclerViewTrips.setAdapter(adapter);

        binding.buttonBack.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main, new CreateTripFragment())
                    .commit();
        });

        loadTripsFromFirebase();
    }

    private void loadTripsFromFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance().collection("trips")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    tripList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Trip trip = document.toObject(Trip.class);
                        tripList.add(trip);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to load trips: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}

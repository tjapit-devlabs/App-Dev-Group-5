package com.example.assignment10;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.assignment10.databinding.FragmentTripsBinding;

public class TripsFragment extends Fragment {

    public TripsFragment() {
        // Required empty public constructor
    }

    FragmentTripsBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentTripsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
}
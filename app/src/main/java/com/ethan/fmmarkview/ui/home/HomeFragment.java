package com.ethan.fmmarkview.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.ethan.fmmarkview.R;
import com.ethan.fmmarkview.databinding.FragmentHomeBinding;
import com.ethan.fmmarkview.ui.view.FMMarkView;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;
    private FragmentHomeBinding binding;
    private FMMarkView fmMarkView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textHome;
        fmMarkView = binding.idMarkView;
        fmMarkView.setGuideLineColor(R.color.green);
        fmMarkView.setMarkLineColor(R.color.black);

        fmMarkView.setOnFMChangeListener((double fm) -> {
            Log.d("HomeFragment", "fm = " + fm);
        });

        textView.setOnClickListener((View view) -> {
            onClick();
        });

        homeViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void onClick(){
        fmMarkView.setFM(99.5f);
    }
}
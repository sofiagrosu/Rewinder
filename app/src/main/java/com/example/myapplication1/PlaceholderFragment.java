package com.example.myapplication1;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

/**
 * Temporary screen for tabs that nobody has built yet ("Materii: în lucru").
 * Delete this file when every tab has its real fragment.
 */
public class PlaceholderFragment extends Fragment {

    private static final String ARG_TITLE = "title";

    public static PlaceholderFragment newInstance(String title) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        TextView text = new TextView(requireContext());
        text.setGravity(Gravity.CENTER);
        text.setTextSize(18);
        text.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        String title = requireArguments().getString(ARG_TITLE, "");
        text.setText(getString(R.string.placeholder_coming_soon, title));
        return text;
    }
}

package com.yulink.nas.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.yulink.nas.R;
import com.yulink.nas.data.repository.LocalFileRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

public class SettingsFragment extends Fragment {
    private TextInputEditText etDownloadLocation;
    private AutoCompleteTextView spinnerTheme;
    private TextInputEditText etMaxTransfers;
    private TextInputEditText etTimeout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        etDownloadLocation = view.findViewById(R.id.et_download_location);
        spinnerTheme = view.findViewById(R.id.spinner_theme);
        etMaxTransfers = view.findViewById(R.id.et_max_transfers);
        etTimeout = view.findViewById(R.id.et_timeout);

        setupThemeSpinner();
        loadSettings();
    }

    private void setupThemeSpinner() {
        String[] themes = {
                getString(R.string.theme_system),
                getString(R.string.theme_light),
                getString(R.string.theme_dark)
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, themes);
        spinnerTheme.setAdapter(adapter);
        spinnerTheme.setText(themes[0], false);
    }

    private void loadSettings() {
        LocalFileRepository localRepo = new LocalFileRepository();
        etDownloadLocation.setText(localRepo.getDownloadsPath());
    }
}

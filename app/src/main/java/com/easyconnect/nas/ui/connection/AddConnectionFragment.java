package com.easyconnect.nas.ui.connection;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.easyconnect.nas.R;
import com.easyconnect.nas.data.model.ProtocolType;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class AddConnectionFragment extends Fragment {
    private AddConnectionViewModel viewModel;
    private TextInputEditText etName, etHost, etPort, etUsername, etPassword, etShareName, etDefaultPath;
    private AutoCompleteTextView spinnerProtocol;
    private TextInputLayout tilShareName, tilPort;
    private MaterialSwitch switchPassive, switchFtps, switchSmbEncryption;
    private MaterialButton btnTest, btnSave, btnCancel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_connection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AddConnectionViewModel.class);

        initViews(view);
        setupProtocolSpinner();
        setupListeners();
        setupObservers();
    }

    private void initViews(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        etName = view.findViewById(R.id.et_name);
        spinnerProtocol = view.findViewById(R.id.spinner_protocol);
        etHost = view.findViewById(R.id.et_host);
        etPort = view.findViewById(R.id.et_port);
        etUsername = view.findViewById(R.id.et_username);
        etPassword = view.findViewById(R.id.et_password);
        etShareName = view.findViewById(R.id.et_share_name);
        etDefaultPath = view.findViewById(R.id.et_default_path);
        tilShareName = view.findViewById(R.id.til_share_name);
        tilPort = view.findViewById(R.id.til_port);
        switchPassive = view.findViewById(R.id.switch_passive);
        switchFtps = view.findViewById(R.id.switch_ftps);
        switchSmbEncryption = view.findViewById(R.id.switch_smb_encryption);
        btnTest = view.findViewById(R.id.btn_test);
        btnSave = view.findViewById(R.id.btn_save);
        btnCancel = view.findViewById(R.id.btn_cancel);
    }

    private void setupProtocolSpinner() {
        ProtocolType[] protocols = ProtocolType.values();
        String[] protocolNames = new String[protocols.length];
        for (int i = 0; i < protocols.length; i++) {
            protocolNames[i] = protocols[i].getDisplayName();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, protocolNames);
        spinnerProtocol.setAdapter(adapter);
        spinnerProtocol.setText(protocols[0].getDisplayName(), false);
        updateProtocolFields(protocols[0]);

        spinnerProtocol.setOnItemClickListener((parent, view, position, id) -> {
            updateProtocolFields(protocols[position]);
        });
    }

    private void updateProtocolFields(ProtocolType protocol) {
        switch (protocol) {
            case SMB:
                tilShareName.setVisibility(View.VISIBLE);
                switchSmbEncryption.setVisibility(View.VISIBLE);
                switchPassive.setVisibility(View.GONE);
                switchFtps.setVisibility(View.GONE);
                etPort.setText(String.valueOf(ProtocolType.SMB.getDefaultPort()));
                break;
            case FTP:
                tilShareName.setVisibility(View.GONE);
                switchSmbEncryption.setVisibility(View.GONE);
                switchPassive.setVisibility(View.VISIBLE);
                switchFtps.setVisibility(View.VISIBLE);
                etPort.setText(String.valueOf(ProtocolType.FTP.getDefaultPort()));
                break;
            case SFTP:
                tilShareName.setVisibility(View.GONE);
                switchSmbEncryption.setVisibility(View.GONE);
                switchPassive.setVisibility(View.GONE);
                switchFtps.setVisibility(View.GONE);
                etPort.setText(String.valueOf(ProtocolType.SFTP.getDefaultPort()));
                break;
        }
    }

    private void setupListeners() {
        btnTest.setOnClickListener(v -> {
            if (validateInputs()) {
                btnTest.setEnabled(false);
                btnTest.setText("测试中...");
                ProtocolType protocol = getSelectedProtocol();
                viewModel.testConnection(
                        etName.getText().toString(),
                        protocol,
                        etHost.getText().toString(),
                        Integer.parseInt(etPort.getText().toString()),
                        etUsername.getText().toString(),
                        etPassword.getText().toString(),
                        etShareName.getText() != null ? etShareName.getText().toString() : "",
                        switchPassive.isChecked(),
                        switchFtps.isChecked(),
                        switchSmbEncryption.isChecked()
                );
            }
        });

        btnSave.setOnClickListener(v -> {
            if (validateInputs()) {
                ProtocolType protocol = getSelectedProtocol();
                viewModel.saveConnection(
                        etName.getText().toString(),
                        protocol,
                        etHost.getText().toString(),
                        Integer.parseInt(etPort.getText().toString()),
                        etUsername.getText().toString(),
                        etPassword.getText().toString(),
                        etShareName.getText() != null ? etShareName.getText().toString() : "",
                        etDefaultPath.getText() != null ? etDefaultPath.getText().toString() : "/",
                        switchPassive.isChecked(),
                        switchFtps.isChecked(),
                        switchSmbEncryption.isChecked()
                );
            }
        });

        btnCancel.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
    }

    private void setupObservers() {
        viewModel.getTestResult().observe(getViewLifecycleOwner(), success -> {
            btnTest.setEnabled(true);
            btnTest.setText(R.string.test_connection);
            if (success) {
                Snackbar.make(requireView(), R.string.connection_test_success, Snackbar.LENGTH_SHORT).show();
            }
        });

        viewModel.getSaveResult().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                Snackbar.make(requireView(), R.string.connection_saved, Snackbar.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).navigateUp();
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.connection_test_failed)
                        .setMessage(errorMsg)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        });
    }

    private boolean validateInputs() {
        boolean valid = true;

        if (etName.getText() == null || etName.getText().toString().trim().isEmpty()) {
            etName.setError("请输入连接名称");
            valid = false;
        }

        if (etHost.getText() == null || etHost.getText().toString().trim().isEmpty()) {
            etHost.setError("请输入主机地址");
            valid = false;
        }

        if (etPort.getText() == null || etPort.getText().toString().trim().isEmpty()) {
            etPort.setError("请输入端口");
            valid = false;
        }

        if (etUsername.getText() == null || etUsername.getText().toString().trim().isEmpty()) {
            etUsername.setError("请输入用户名");
            valid = false;
        }

        if (etPassword.getText() == null || etPassword.getText().toString().trim().isEmpty()) {
            etPassword.setError("请输入密码");
            valid = false;
        }

        return valid;
    }

    private ProtocolType getSelectedProtocol() {
        String selected = spinnerProtocol.getText().toString();
        for (ProtocolType type : ProtocolType.values()) {
            if (type.getDisplayName().equals(selected)) {
                return type;
            }
        }
        return ProtocolType.SMB;
    }
}

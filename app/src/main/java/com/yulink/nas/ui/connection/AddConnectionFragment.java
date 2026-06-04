package com.yulink.nas.ui.connection;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.yulink.nas.R;
import com.yulink.nas.data.db.entity.ConnectionEntity;
import com.yulink.nas.data.model.ProtocolType;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;

public class AddConnectionFragment extends Fragment {
    private AddConnectionViewModel viewModel;
    private EditText etName, etHost, etPort, etUsername, etPassword, etShareName, etDefaultPath;
    private TextView spinnerProtocol;
    private LinearLayout layoutShareName;
    private MaterialCardView cardOptions;
    private MaterialSwitch switchPassive, switchFtps, switchSmbEncryption;
    private TextView btnSave, tvTitle;
    private long editConnectionId = -1;

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

        // Check if editing existing connection
        if (getArguments() != null) {
            editConnectionId = getArguments().getLong("connectionId", -1);
            if (editConnectionId > 0) {
                loadConnection(editConnectionId);
            }
        }
    }

    private void initViews(View view) {
        ImageButton btnBack = view.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> dismiss());

        tvTitle = view.findViewById(R.id.tv_title);
        btnSave = view.findViewById(R.id.btn_save);

        etName = view.findViewById(R.id.et_name);
        spinnerProtocol = view.findViewById(R.id.spinner_protocol);
        etHost = view.findViewById(R.id.et_host);
        etPort = view.findViewById(R.id.et_port);
        etUsername = view.findViewById(R.id.et_username);
        etPassword = view.findViewById(R.id.et_password);
        etShareName = view.findViewById(R.id.et_share_name);
        etDefaultPath = view.findViewById(R.id.et_default_path);

        layoutShareName = view.findViewById(R.id.layout_share_name);
        cardOptions = view.findViewById(R.id.card_options);

        switchPassive = view.findViewById(R.id.switch_passive);
        switchFtps = view.findViewById(R.id.switch_ftps);
        switchSmbEncryption = view.findViewById(R.id.switch_smb_encryption);
    }

    private void setupProtocolSpinner() {
        ProtocolType[] protocols = ProtocolType.values();
        spinnerProtocol.setText(protocols[0].getDisplayName());
        updateProtocolFields(protocols[0]);

        // iOS style - show bottom sheet picker on click
        spinnerProtocol.setOnClickListener(v -> showProtocolPicker(protocols));
        spinnerProtocol.setFocusable(false);
        spinnerProtocol.setCursorVisible(false);
    }

    private void showProtocolPicker(ProtocolType[] protocols) {
        String[] protocolNames = new String[protocols.length];
        for (int i = 0; i < protocols.length; i++) {
            protocolNames[i] = protocols[i].getDisplayName() + " (端口 " + protocols[i].getDefaultPort() + ")";
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("选择协议")
                .setItems(protocolNames, (dialog, which) -> {
                    spinnerProtocol.setText(protocols[which].getDisplayName());
                    updateProtocolFields(protocols[which]);
                })
                .show();
    }

    private void updateProtocolFields(ProtocolType protocol) {
        // Show options section
        cardOptions.setVisibility(View.VISIBLE);

        switch (protocol) {
            case SMB:
                layoutShareName.setVisibility(View.VISIBLE);
                switchSmbEncryption.setVisibility(View.VISIBLE);
                switchPassive.setVisibility(View.GONE);
                switchFtps.setVisibility(View.GONE);
                etPort.setText(String.valueOf(ProtocolType.SMB.getDefaultPort()));
                break;
            case FTP:
                layoutShareName.setVisibility(View.GONE);
                switchSmbEncryption.setVisibility(View.GONE);
                switchPassive.setVisibility(View.VISIBLE);
                switchFtps.setVisibility(View.VISIBLE);
                etPort.setText(String.valueOf(ProtocolType.FTP.getDefaultPort()));
                break;
            case SFTP:
                layoutShareName.setVisibility(View.GONE);
                switchSmbEncryption.setVisibility(View.GONE);
                switchPassive.setVisibility(View.GONE);
                switchFtps.setVisibility(View.GONE);
                etPort.setText(String.valueOf(ProtocolType.SFTP.getDefaultPort()));
                break;
        }
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> {
            if (validateInputs()) {
                ProtocolType protocol = getSelectedProtocol();
                String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

                if (editConnectionId > 0) {
                    viewModel.updateConnection(
                            editConnectionId,
                            etName.getText().toString(),
                            protocol,
                            etHost.getText().toString(),
                            Integer.parseInt(etPort.getText().toString()),
                            etUsername.getText().toString(),
                            password,
                            etShareName.getText() != null ? etShareName.getText().toString() : "",
                            etDefaultPath.getText() != null ? etDefaultPath.getText().toString() : "/",
                            switchPassive.isChecked(),
                            switchFtps.isChecked(),
                            switchSmbEncryption.isChecked()
                    );
                } else {
                    viewModel.saveConnection(
                            etName.getText().toString(),
                            protocol,
                            etHost.getText().toString(),
                            Integer.parseInt(etPort.getText().toString()),
                            etUsername.getText().toString(),
                            password,
                            etShareName.getText() != null ? etShareName.getText().toString() : "",
                            etDefaultPath.getText() != null ? etDefaultPath.getText().toString() : "/",
                            switchPassive.isChecked(),
                            switchFtps.isChecked(),
                            switchSmbEncryption.isChecked()
                    );
                }
            }
        });
    }

    private void setupObservers() {
        viewModel.getSaveResult().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                Snackbar.make(requireView(), R.string.connection_saved, Snackbar.LENGTH_SHORT).show();
                dismiss();
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("错误")
                        .setMessage(errorMsg)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        });

        viewModel.getConnection().observe(getViewLifecycleOwner(), connection -> {
            if (connection != null) {
                populateFields(connection);
            }
        });
    }

    private void loadConnection(long connectionId) {
        viewModel.loadConnection(connectionId);
        tvTitle.setText(R.string.edit_connection);
    }

    private void populateFields(ConnectionEntity connection) {
        etName.setText(connection.name);
        etHost.setText(connection.host);
        etPort.setText(String.valueOf(connection.port));
        etUsername.setText(connection.username);
        etShareName.setText(connection.shareName);
        etDefaultPath.setText(connection.defaultPath);
        switchPassive.setChecked(connection.passiveMode);
        switchFtps.setChecked(connection.useFtps);
        switchSmbEncryption.setChecked(connection.useSmbEncryption);

        // Set protocol
        for (ProtocolType type : ProtocolType.values()) {
            if (type == connection.protocol) {
                spinnerProtocol.setText(type.getDisplayName());
                updateProtocolFields(type);
                break;
            }
        }

        // Update password hint
        etPassword.setHint("留空表示不修改密码");
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

        // Password is required only for new connections
        if (editConnectionId <= 0) {
            if (etPassword.getText() == null || etPassword.getText().toString().trim().isEmpty()) {
                etPassword.setError("请输入密码");
                valid = false;
            }
        }

        return valid;
    }

    private void dismiss() {
        getParentFragmentManager().popBackStack();
        // Restore bottom nav visibility
        requireActivity().findViewById(R.id.bottom_navigation).setVisibility(View.VISIBLE);
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

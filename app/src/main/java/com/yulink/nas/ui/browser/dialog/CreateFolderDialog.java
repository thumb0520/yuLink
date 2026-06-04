package com.yulink.nas.ui.browser.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.yulink.nas.R;
import com.yulink.nas.ui.browser.FileBrowserViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

public class CreateFolderDialog extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_create_folder, null);

        TextInputEditText etFolderName = view.findViewById(R.id.et_folder_name);

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.create_folder)
                .setView(view)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String name = etFolderName.getText() != null ?
                            etFolderName.getText().toString().trim() : "";
                    if (!name.isEmpty()) {
                        FileBrowserViewModel viewModel = new ViewModelProvider(requireParentFragment())
                                .get(FileBrowserViewModel.class);
                        viewModel.createDirectory(name);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
    }
}

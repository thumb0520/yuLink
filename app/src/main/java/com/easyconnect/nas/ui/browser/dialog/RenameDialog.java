package com.easyconnect.nas.ui.browser.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.easyconnect.nas.R;
import com.easyconnect.nas.data.model.NasFile;
import com.easyconnect.nas.ui.browser.FileBrowserViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

public class RenameDialog extends DialogFragment {
    private static final String ARG_FILE_NAME = "file_name";
    private static final String ARG_FILE_PATH = "file_path";

    public static RenameDialog newInstance(NasFile file) {
        RenameDialog dialog = new RenameDialog();
        Bundle args = new Bundle();
        args.putString(ARG_FILE_NAME, file.getName());
        args.putString(ARG_FILE_PATH, file.getFullPath());
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_rename, null);

        TextInputEditText etNewName = view.findViewById(R.id.et_new_name);
        String currentName = getArguments() != null ? getArguments().getString(ARG_FILE_NAME) : "";
        etNewName.setText(currentName);
        etNewName.selectAll();

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.rename)
                .setView(view)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String newName = etNewName.getText() != null ?
                            etNewName.getText().toString().trim() : "";
                    if (!newName.isEmpty() && !newName.equals(currentName)) {
                        FileBrowserViewModel viewModel = new ViewModelProvider(requireParentFragment())
                                .get(FileBrowserViewModel.class);
                        // Create a NasFile with the old path for renaming
                        NasFile file = new NasFile();
                        file.setName(currentName);
                        file.setFullPath(getArguments().getString(ARG_FILE_PATH));
                        viewModel.renameFile(file, newName);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
    }
}

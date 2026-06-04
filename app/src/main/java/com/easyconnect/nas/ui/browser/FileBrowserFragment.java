package com.easyconnect.nas.ui.browser;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.easyconnect.nas.R;
import com.easyconnect.nas.data.model.NasFile;
import com.easyconnect.nas.ui.browser.adapter.FileListAdapter;
import com.easyconnect.nas.ui.browser.dialog.CreateFolderDialog;
import com.easyconnect.nas.ui.browser.dialog.RenameDialog;
import com.easyconnect.nas.ui.preview.ImagePreviewActivity;
import com.easyconnect.nas.ui.preview.VideoPlayerActivity;
import com.easyconnect.nas.ui.preview.AudioPlayerActivity;
import com.easyconnect.nas.util.MimeTypeHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FileBrowserFragment extends Fragment implements FileListAdapter.OnFileClickListener {
    private FileBrowserViewModel viewModel;
    private FileListAdapter adapter;
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private ProgressBar progressBar;
    private ChipGroup chipGroupBreadcrumb;
    private MaterialToolbar toolbar;
    private BottomAppBar bottomActionBar;
    private boolean selectionMode = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_file_browser, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(FileBrowserViewModel.class);

        initViews(view);
        setupRecyclerView();
        setupToolbar();
        setupObservers();

        // Get connection ID from arguments
        long connectionId = getArguments() != null ? getArguments().getLong("connectionId", -1) : -1;
        if (connectionId > 0) {
            viewModel.connectToServer(connectionId);
        }
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        recyclerView = view.findViewById(R.id.rv_files);
        tvEmpty = view.findViewById(R.id.tv_empty);
        progressBar = view.findViewById(R.id.progress_bar);
        chipGroupBreadcrumb = view.findViewById(R.id.chip_group_breadcrumb);
        bottomActionBar = view.findViewById(R.id.bottom_action_bar);
    }

    private void setupRecyclerView() {
        adapter = new FileListAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> {
            if (selectionMode) {
                exitSelectionMode();
            } else {
                viewModel.navigateUp();
            }
        });

        toolbar.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_browser, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.action_view_mode) {
                    viewModel.setGridView(!viewModel.isGridView());
                    // TODO: Switch between list and grid adapter
                    return true;
                } else if (id == R.id.action_sort) {
                    showSortDialog();
                    return true;
                } else if (id == R.id.action_refresh) {
                    viewModel.refresh();
                    return true;
                } else if (id == R.id.action_new_folder) {
                    showCreateFolderDialog();
                    return true;
                } else if (id == R.id.action_settings) {
                    // Navigate to settings
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.STARTED);
    }

    private void setupObservers() {
        viewModel.getFiles().observe(getViewLifecycleOwner(), files -> {
            adapter.submitList(files);
            tvEmpty.setVisibility(files == null || files.isEmpty() ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(files == null || files.isEmpty() ? View.GONE : View.VISIBLE);
        });

        viewModel.getCurrentPath().observe(getViewLifecycleOwner(), this::updateBreadcrumb);

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Snackbar.make(requireView(), error, Snackbar.LENGTH_LONG).show();
            }
        });

        viewModel.getSelectedFiles().observe(getViewLifecycleOwner(), selected -> {
            if (selected != null && !selected.isEmpty()) {
                enterSelectionMode(selected.size());
            } else if (selectionMode) {
                exitSelectionMode();
            }
        });
    }

    private void updateBreadcrumb(String path) {
        chipGroupBreadcrumb.removeAllViews();

        String[] segments = path.split("/");
        StringBuilder currentPath = new StringBuilder();

        // Root chip
        Chip rootChip = new Chip(requireContext());
        rootChip.setText("/");
        rootChip.setOnClickListener(v -> viewModel.navigateTo("/"));
        chipGroupBreadcrumb.addView(rootChip);

        for (String segment : segments) {
            if (segment.isEmpty()) continue;
            currentPath.append("/").append(segment);

            Chip chip = new Chip(requireContext());
            chip.setText(segment);
            String navigatePath = currentPath.toString();
            chip.setOnClickListener(v -> viewModel.navigateTo(navigatePath));
            chipGroupBreadcrumb.addView(chip);
        }

        // Update toolbar title
        toolbar.setTitle(segments.length > 0 ? segments[segments.length - 1] : "/");
    }

    private void enterSelectionMode(int count) {
        selectionMode = true;
        adapter.setSelectionMode(true);
        toolbar.setTitle(getString(R.string.selected_count, count));
        bottomActionBar.setVisibility(View.VISIBLE);
    }

    private void exitSelectionMode() {
        selectionMode = false;
        adapter.setSelectionMode(false);
        viewModel.clearSelection();
        bottomActionBar.setVisibility(View.GONE);
        String currentPath = viewModel.getCurrentPath().getValue();
        toolbar.setTitle(currentPath != null ? currentPath : "/");
    }

    private void showSortDialog() {
        String[] options = {
                getString(R.string.sort_by_name) + " (A-Z)",
                getString(R.string.sort_by_name) + " (Z-A)",
                getString(R.string.sort_by_size) + " (小到大)",
                getString(R.string.sort_by_size) + " (大到小)",
                getString(R.string.sort_by_date) + " (旧到新)",
                getString(R.string.sort_by_date) + " (新到旧)"
        };

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("排序方式")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: viewModel.setSortMode(FileBrowserViewModel.SortMode.NAME_ASC); break;
                        case 1: viewModel.setSortMode(FileBrowserViewModel.SortMode.NAME_DESC); break;
                        case 2: viewModel.setSortMode(FileBrowserViewModel.SortMode.SIZE_ASC); break;
                        case 3: viewModel.setSortMode(FileBrowserViewModel.SortMode.SIZE_DESC); break;
                        case 4: viewModel.setSortMode(FileBrowserViewModel.SortMode.DATE_ASC); break;
                        case 5: viewModel.setSortMode(FileBrowserViewModel.SortMode.DATE_DESC); break;
                    }
                })
                .show();
    }

    private void showCreateFolderDialog() {
        new CreateFolderDialog().show(getChildFragmentManager(), "create_folder");
    }

    @Override
    public void onFileClick(NasFile file) {
        if (file.isDirectory()) {
            viewModel.navigateTo(file.getFullPath());
        } else if (file.isPreviewable()) {
            openPreview(file);
        } else {
            // Prompt download
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(file.getName())
                    .setMessage("是否下载此文件？")
                    .setPositiveButton(R.string.download, (dialog, which) -> {
                        // TODO: Start download
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }
    }

    @Override
    public void onFileLongClick(NasFile file) {
        if (!selectionMode) {
            viewModel.clearSelection();
            viewModel.toggleFileSelection(file);
        }
    }

    @Override
    public void onFileSelectionChanged(Set<String> selectedPaths) {
        // Update ViewModel
    }

    private void openPreview(NasFile file) {
        Intent intent;
        if (file.isImage()) {
            intent = new Intent(requireContext(), ImagePreviewActivity.class);
        } else if (file.isVideo()) {
            intent = new Intent(requireContext(), VideoPlayerActivity.class);
        } else {
            intent = new Intent(requireContext(), AudioPlayerActivity.class);
        }

        intent.putExtra("filePath", file.getFullPath());
        intent.putExtra("fileName", file.getName());
        intent.putExtra("connectionId", viewModel.getConnectionInfo().getId());
        startActivity(intent);
    }
}

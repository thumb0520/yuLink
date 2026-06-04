package com.yulink.nas.ui.browser;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yulink.nas.R;
import com.yulink.nas.data.model.NasFile;
import com.yulink.nas.data.model.TransferTask;
import com.yulink.nas.transfer.TransferService;
import com.yulink.nas.ui.browser.adapter.FileListAdapter;
import com.yulink.nas.ui.browser.dialog.CreateFolderDialog;
import com.yulink.nas.ui.preview.ImagePreviewActivity;
import com.yulink.nas.ui.preview.VideoPlayerActivity;
import com.yulink.nas.ui.preview.AudioPlayerActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.Set;

public class FileBrowserFragment extends Fragment implements FileListAdapter.OnFileClickListener {
    private FileBrowserViewModel viewModel;
    private FileListAdapter adapter;
    private RecyclerView recyclerView;
    private TextView tvEmpty, tvTitle, tvSelectedCount;
    private ProgressBar progressBar;
    private ChipGroup chipGroupBreadcrumb;
    private LinearLayout bottomActionBar;
    private boolean selectionMode = false;
    private TransferService transferService;
    private boolean serviceBound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TransferService.TransferBinder binder = (TransferService.TransferBinder) service;
            transferService = binder.getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            transferService = null;
            serviceBound = false;
        }
    };

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
        setupButtons();
        setupObservers();

        // Bind transfer service (don't start foreground yet — only when a transfer is enqueued)
        Intent serviceIntent = new Intent(requireContext(), TransferService.class);
        requireContext().bindService(serviceIntent, serviceConnection, android.content.Context.BIND_AUTO_CREATE);

        // Get connection ID from arguments
        long connectionId = getArguments() != null ? getArguments().getLong("connectionId", -1) : -1;
        if (connectionId > 0) {
            viewModel.connectToServer(connectionId);
        } else {
            tvEmpty.setText("请先选择一个NAS连接");
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }
    }

    private void initViews(View view) {
        tvTitle = view.findViewById(R.id.tv_title);
        recyclerView = view.findViewById(R.id.rv_files);
        tvEmpty = view.findViewById(R.id.tv_empty);
        progressBar = view.findViewById(R.id.progress_bar);
        chipGroupBreadcrumb = view.findViewById(R.id.chip_group_breadcrumb);
        bottomActionBar = view.findViewById(R.id.bottom_action_bar);
        tvSelectedCount = view.findViewById(R.id.tv_selected_count);

        ImageButton btnBack = view.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> {
            if (selectionMode) {
                exitSelectionMode();
            } else {
                viewModel.navigateUp();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new FileListAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupButtons() {
        ImageButton btnViewMode = requireView().findViewById(R.id.btn_view_mode);
        btnViewMode.setOnClickListener(v -> {
            viewModel.setGridView(!viewModel.isGridView());
            // TODO: Switch between list and grid adapter
        });

        ImageButton btnMore = requireView().findViewById(R.id.btn_more);
        btnMore.setOnClickListener(v -> showPopupMenu(v));
    }

    private void showPopupMenu(View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenuInflater().inflate(R.menu.menu_browser, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_sort) {
                showSortDialog();
                return true;
            } else if (id == R.id.action_refresh) {
                viewModel.refresh();
                return true;
            } else if (id == R.id.action_new_folder) {
                showCreateFolderDialog();
                return true;
            }
            return false;
        });
        popup.show();
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

        // Update title
        tvTitle.setText(segments.length > 0 ? segments[segments.length - 1] : "/");
    }

    private void enterSelectionMode(int count) {
        selectionMode = true;
        adapter.setSelectionMode(true);
        tvTitle.setText(getString(R.string.selected_count, count));
        tvSelectedCount.setText(getString(R.string.selected_count, count));
        bottomActionBar.setVisibility(View.VISIBLE);
    }

    private void exitSelectionMode() {
        selectionMode = false;
        adapter.setSelectionMode(false);
        viewModel.clearSelection();
        bottomActionBar.setVisibility(View.GONE);
        String currentPath = viewModel.getCurrentPath().getValue();
        tvTitle.setText(currentPath != null ? currentPath : "/");
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
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(file.getName())
                    .setMessage("是否下载此文件？")
                    .setPositiveButton(R.string.download, (dialog, which) -> {
                        startDownload(file);
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

    private void startDownload(NasFile file) {
        if (!serviceBound || transferService == null) {
            Snackbar.make(requireView(), "传输服务未就绪", Snackbar.LENGTH_SHORT).show();
            return;
        }

        long connectionId = viewModel.getConnectionInfo().getId();
        java.io.File downloadDir = new java.io.File(requireContext().getExternalFilesDir(null), "downloads");
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }
        String destinationPath = new java.io.File(downloadDir, file.getName()).getAbsolutePath();

        TransferTask task = new TransferTask(
                connectionId,
                TransferTask.Direction.DOWNLOAD,
                file.getFullPath(),
                destinationPath,
                file.getName(),
                file.getSize()
        );
        task.setNotificationId((int) (System.currentTimeMillis() % Integer.MAX_VALUE));

        // Start foreground service now that we have an actual transfer
        Intent serviceIntent = new Intent(requireContext(), TransferService.class);
        requireContext().startService(serviceIntent);

        transferService.enqueueTransfer(task);
        Snackbar.make(requireView(), "开始下载: " + file.getName(), Snackbar.LENGTH_SHORT).show();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (serviceBound) {
            requireContext().unbindService(serviceConnection);
            serviceBound = false;
        }
    }
}

package com.yulink.nas.ui.transfer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yulink.nas.R;
import com.yulink.nas.data.model.TransferTask;
import com.yulink.nas.transfer.TransferService;
import com.yulink.nas.ui.transfer.adapter.TransferAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class TransferQueueFragment extends Fragment implements TransferAdapter.OnTransferActionListener {
    private TransferQueueViewModel viewModel;
    private TransferAdapter adapter;
    private RecyclerView recyclerView;
    private TextView tvEmpty;
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
        return inflater.inflate(R.layout.fragment_transfer_queue, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TransferQueueViewModel.class);

        recyclerView = view.findViewById(R.id.rv_transfers);
        tvEmpty = view.findViewById(R.id.tv_empty);
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);

        adapter = new TransferAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.clear_completed) {
                viewModel.clearCompleted();
                return true;
            }
            return false;
        });

        viewModel.getAllTransfers().observe(getViewLifecycleOwner(), this::updateList);

        // Bind transfer service for cancel/delete operations
        Intent serviceIntent = new Intent(requireContext(), TransferService.class);
        requireContext().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void updateList(List<TransferTask> tasks) {
        adapter.submitList(tasks);
        tvEmpty.setVisibility(tasks == null || tasks.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(tasks == null || tasks.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onCancelClick(TransferTask task) {
        if (serviceBound && transferService != null) {
            transferService.cancelTransfer(task.getTaskId());
        } else {
            // Fallback: cancel via repository only
            viewModel.getRepository().cancelTask(task.getTaskId());
        }
    }

    @Override
    public void onDeleteClick(TransferTask task) {
        boolean isActive = task.getStatus() == TransferTask.Status.RUNNING ||
                task.getStatus() == TransferTask.Status.QUEUED;

        String title = isActive ? "强制删除任务" : "删除任务";
        String message = isActive
                ? "任务 \"" + task.getFileName() + "\" 正在传输中，确定要强制删除吗？\n\n这将取消当前传输并删除任务记录。"
                : "确定要删除 \"" + task.getFileName() + "\" 的传输记录吗？";

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    if (serviceBound && transferService != null) {
                        transferService.forceDeleteTask(task.getTaskId());
                    } else {
                        viewModel.deleteTask(task);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
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

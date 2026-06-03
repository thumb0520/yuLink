package com.easyconnect.nas.ui.transfer;

import android.os.Bundle;
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

import com.easyconnect.nas.R;
import com.easyconnect.nas.data.model.TransferTask;
import com.easyconnect.nas.ui.transfer.adapter.TransferAdapter;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

public class TransferQueueFragment extends Fragment implements TransferAdapter.OnTransferActionListener {
    private TransferQueueViewModel viewModel;
    private TransferAdapter adapter;
    private RecyclerView recyclerView;
    private TextView tvEmpty;

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

        viewModel.getActiveTasks().observe(getViewLifecycleOwner(), this::updateList);
    }

    private void updateList(List<TransferTask> tasks) {
        adapter.submitList(tasks);
        tvEmpty.setVisibility(tasks == null || tasks.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(tasks == null || tasks.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onCancelClick(TransferTask task) {
        viewModel.cancelTask(task.getTaskId());
    }
}

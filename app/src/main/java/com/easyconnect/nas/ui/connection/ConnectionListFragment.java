package com.easyconnect.nas.ui.connection;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.easyconnect.nas.R;
import com.easyconnect.nas.data.db.entity.ConnectionEntity;
import com.easyconnect.nas.ui.connection.adapter.ConnectionAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

public class ConnectionListFragment extends Fragment implements ConnectionAdapter.OnConnectionClickListener {
    private ConnectionListViewModel viewModel;
    private ConnectionAdapter adapter;
    private RecyclerView recyclerView;
    private TextView tvEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_connection_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ConnectionListViewModel.class);

        recyclerView = view.findViewById(R.id.rv_connections);
        tvEmpty = view.findViewById(R.id.tv_empty);
        FloatingActionButton fab = view.findViewById(R.id.fab_add);

        adapter = new ConnectionAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        viewModel.getConnections().observe(getViewLifecycleOwner(), connections -> {
            adapter.submitList(connections);
            tvEmpty.setVisibility(connections == null || connections.isEmpty() ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(connections == null || connections.isEmpty() ? View.GONE : View.VISIBLE);
        });

        fab.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_connectionList_to_addConnection);
        });
    }

    @Override
    public void onConnectionClick(ConnectionEntity connection) {
        viewModel.updateLastConnected(connection.id);
        Bundle args = new Bundle();
        args.putLong("connectionId", connection.id);
        Navigation.findNavController(requireView()).navigate(R.id.action_connectionList_to_fileBrowser, args);
    }

    @Override
    public void onConnectionLongClick(ConnectionEntity connection) {
        String[] options = {getString(R.string.edit_connection), getString(R.string.delete)};
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(connection.name)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Edit
                            editConnection(connection);
                            break;
                        case 1: // Delete
                            confirmDeleteConnection(connection);
                            break;
                    }
                })
                .show();
    }

    private void editConnection(ConnectionEntity connection) {
        Bundle args = new Bundle();
        args.putLong("connectionId", connection.id);
        Navigation.findNavController(requireView()).navigate(R.id.action_connectionList_to_addConnection, args);
    }

    private void confirmDeleteConnection(ConnectionEntity connection) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete)
                .setMessage(getString(R.string.confirm_delete_message))
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    viewModel.deleteConnection(connection);
                    Snackbar.make(requireView(), R.string.connection_deleted, Snackbar.LENGTH_LONG)
                            .setAction(R.string.cancel, v -> {
                                // TODO: Re-insert connection
                            })
                            .show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}

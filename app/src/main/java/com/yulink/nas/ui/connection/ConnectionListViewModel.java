package com.yulink.nas.ui.connection;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.yulink.nas.data.db.entity.ConnectionEntity;
import com.yulink.nas.data.repository.ConnectionRepository;

import java.util.List;

public class ConnectionListViewModel extends AndroidViewModel {
    private final ConnectionRepository repository;
    private final LiveData<List<ConnectionEntity>> connections;

    public ConnectionListViewModel(@NonNull Application application) {
        super(application);
        repository = new ConnectionRepository(application);
        connections = repository.getAllConnections();
    }

    public LiveData<List<ConnectionEntity>> getConnections() {
        return connections;
    }

    public void deleteConnection(ConnectionEntity connection) {
        repository.deleteConnection(connection);
    }

    public void deleteConnectionById(long id) {
        repository.deleteConnectionById(id);
    }

    public void updateLastConnected(long id) {
        repository.updateLastConnected(id);
    }
}

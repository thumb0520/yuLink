package com.easyconnect.nas.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.easyconnect.nas.data.db.AppDatabase;
import com.easyconnect.nas.data.db.dao.ConnectionDao;
import com.easyconnect.nas.data.db.entity.ConnectionEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConnectionRepository {
    private final ConnectionDao connectionDao;
    private final ExecutorService executor;

    public ConnectionRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        connectionDao = db.connectionDao();
        executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<ConnectionEntity>> getAllConnections() {
        return connectionDao.getAllConnections();
    }

    public void getConnectionById(long id, Callback<ConnectionEntity> callback) {
        executor.execute(() -> {
            ConnectionEntity entity = connectionDao.getConnectionById(id);
            callback.onResult(entity);
        });
    }

    public void insertConnection(ConnectionEntity connection, Callback<Long> callback) {
        executor.execute(() -> {
            long id = connectionDao.insertConnection(connection);
            if (callback != null) callback.onResult(id);
        });
    }

    public void updateConnection(ConnectionEntity connection) {
        executor.execute(() -> connectionDao.updateConnection(connection));
    }

    public void deleteConnection(ConnectionEntity connection) {
        executor.execute(() -> connectionDao.deleteConnection(connection));
    }

    public void deleteConnectionById(long id) {
        executor.execute(() -> connectionDao.deleteConnectionById(id));
    }

    public void updateLastConnected(long id) {
        executor.execute(() -> connectionDao.updateLastConnected(id, System.currentTimeMillis()));
    }

    public interface Callback<T> {
        void onResult(T result);
    }
}

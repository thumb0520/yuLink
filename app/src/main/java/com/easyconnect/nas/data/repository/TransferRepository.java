package com.easyconnect.nas.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.easyconnect.nas.data.db.AppDatabase;
import com.easyconnect.nas.data.db.dao.TransferHistoryDao;
import com.easyconnect.nas.data.db.entity.TransferHistoryEntity;
import com.easyconnect.nas.data.model.TransferTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransferRepository {
    private final TransferHistoryDao transferHistoryDao;
    private final ExecutorService executor;
    private final ConcurrentHashMap<String, TransferTask> activeTasks = new ConcurrentHashMap<>();
    private final MutableLiveData<List<TransferTask>> activeTasksLiveData = new MutableLiveData<>();

    public TransferRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        transferHistoryDao = db.transferHistoryDao();
        executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<TransferHistoryEntity>> getAllTransfers() {
        return transferHistoryDao.getAllTransfers();
    }

    public LiveData<List<TransferTask>> getActiveTasks() {
        return activeTasksLiveData;
    }

    public void addTask(TransferTask task) {
        activeTasks.put(task.getTaskId(), task);
        updateActiveTasksLiveData();

        // Also persist to database
        executor.execute(() -> {
            TransferHistoryEntity entity = new TransferHistoryEntity();
            entity.connectionId = task.getConnectionId();
            entity.fileName = task.getFileName();
            entity.sourcePath = task.getSourcePath();
            entity.destinationPath = task.getDestinationPath();
            entity.fileSize = task.getTotalBytes();
            entity.direction = task.getDirection() == TransferTask.Direction.UPLOAD ? 0 : 1;
            entity.status = 0;
            entity.startedAt = System.currentTimeMillis();
            transferHistoryDao.insertTransfer(entity);
        });
    }

    public void updateTaskProgress(String taskId, long transferredBytes) {
        TransferTask task = activeTasks.get(taskId);
        if (task != null) {
            task.setTransferredBytes(transferredBytes);
            updateActiveTasksLiveData();
        }
    }

    public void updateTaskStatus(String taskId, TransferTask.Status status, String errorMessage) {
        TransferTask task = activeTasks.get(taskId);
        if (task != null) {
            task.setStatus(status);
            task.setErrorMessage(errorMessage);
            if (status == TransferTask.Status.COMPLETED || status == TransferTask.Status.FAILED) {
                activeTasks.remove(taskId);
            }
            updateActiveTasksLiveData();
        }
    }

    public void cancelTask(String taskId) {
        TransferTask task = activeTasks.get(taskId);
        if (task != null) {
            task.setStatus(TransferTask.Status.CANCELLED);
            activeTasks.remove(taskId);
            updateActiveTasksLiveData();
        }
    }

    public void deleteCompletedTransfers() {
        executor.execute(transferHistoryDao::deleteCompletedTransfers);
    }

    private void updateActiveTasksLiveData() {
        List<TransferTask> tasks = new ArrayList<>(activeTasks.values());
        activeTasksLiveData.postValue(tasks);
    }
}

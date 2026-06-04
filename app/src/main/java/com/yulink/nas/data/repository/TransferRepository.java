package com.yulink.nas.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.yulink.nas.data.db.AppDatabase;
import com.yulink.nas.data.db.dao.TransferHistoryDao;
import com.yulink.nas.data.db.entity.TransferHistoryEntity;
import com.yulink.nas.data.model.TransferTask;

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
    private final ConcurrentHashMap<String, Long> taskIdToDbId = new ConcurrentHashMap<>();

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
            long rowId = transferHistoryDao.insertTransfer(entity);
            taskIdToDbId.put(task.getTaskId(), rowId);
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
            // Don't override CANCELLED status (cancel causes disconnect → onTransferFailed)
            if (task.getStatus() == TransferTask.Status.CANCELLED) {
                return;
            }
            task.setStatus(status);
            task.setErrorMessage(errorMessage);
            // Keep in activeTasks so UUID→DB mapping survives for cancel/delete
            // Remove only on forceDeleteTask
            int dbStatus = status == TransferTask.Status.COMPLETED ? 2 : 3;
            updateDbStatus(taskId, task.getFileName(), task.getConnectionId(), dbStatus);
            updateActiveTasksLiveData();
        }
    }

    public void cancelTask(String taskId) {
        TransferTask task = activeTasks.get(taskId);
        if (task != null && task.getStatus() != TransferTask.Status.COMPLETED) {
            task.setStatus(TransferTask.Status.CANCELLED);
            updateDbStatus(taskId, task.getFileName(), task.getConnectionId(), 4);
            updateActiveTasksLiveData();
        }
    }

    public void forceDeleteTask(String taskId) {
        // Remove from active tasks (works for both active and completed tasks)
        activeTasks.remove(taskId);

        // Delete from database
        Long dbId = taskIdToDbId.remove(taskId);
        if (dbId != null && dbId > 0) {
            executor.execute(() -> transferHistoryDao.deleteTransferById(dbId));
        } else {
            // Fallback: taskId might be the DB row ID itself (from history records)
            executor.execute(() -> {
                try {
                    long id = Long.parseLong(taskId);
                    transferHistoryDao.deleteTransferById(id);
                } catch (NumberFormatException ignored) {
                }
            });
        }
        updateActiveTasksLiveData();
    }

    public void deleteCompletedTransfers() {
        executor.execute(transferHistoryDao::deleteCompletedTransfers);
    }

    private void updateDbStatus(String taskId, String fileName, long connectionId, int dbStatus) {
        executor.execute(() -> {
            Long dbId = taskIdToDbId.get(taskId);
            if (dbId != null && dbId > 0) {
                transferHistoryDao.updateStatus(dbId, dbStatus, System.currentTimeMillis());
            } else {
                TransferHistoryEntity entity = transferHistoryDao.getTransferByFileName(fileName, connectionId);
                if (entity != null && entity.status < 2) {
                    transferHistoryDao.updateStatus(entity.id, dbStatus, System.currentTimeMillis());
                    taskIdToDbId.put(taskId, entity.id);
                }
            }
        });
    }

    private void updateActiveTasksLiveData() {
        List<TransferTask> tasks = new ArrayList<>(activeTasks.values());
        activeTasksLiveData.postValue(tasks);
    }
}

package com.yulink.nas.ui.transfer;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.yulink.nas.data.db.entity.TransferHistoryEntity;
import com.yulink.nas.data.model.TransferTask;
import com.yulink.nas.data.repository.TransferRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TransferQueueViewModel extends AndroidViewModel {
    private final TransferRepository repository;
    private final MediatorLiveData<List<TransferTask>> allTransfers = new MediatorLiveData<>();

    public TransferQueueViewModel(@NonNull Application application) {
        super(application);
        repository = new TransferRepository(application);

        LiveData<List<TransferTask>> activeTasks = repository.getActiveTasks();
        LiveData<List<TransferHistoryEntity>> history = repository.getAllTransfers();

        allTransfers.addSource(activeTasks, tasks -> merge(tasks, history.getValue()));
        allTransfers.addSource(history, entities -> merge(activeTasks.getValue(), entities));
    }

    private void merge(List<TransferTask> activeTasks, List<TransferHistoryEntity> history) {
        Map<String, TransferTask> taskMap = new LinkedHashMap<>();

        // Add history first (older items lower)
        if (history != null) {
            for (TransferHistoryEntity entity : history) {
                TransferTask task = toTransferTask(entity);
                taskMap.put(task.getTaskId(), task);
            }
        }

        // Active tasks override history (in-memory has latest progress/status)
        if (activeTasks != null) {
            for (TransferTask task : activeTasks) {
                taskMap.put(task.getTaskId(), task);
            }
        }

        allTransfers.postValue(new ArrayList<>(taskMap.values()));
    }

    private TransferTask toTransferTask(TransferHistoryEntity entity) {
        TransferTask task = new TransferTask();
        task.setTaskId(String.valueOf(entity.id));
        task.setConnectionId(entity.connectionId);
        task.setFileName(entity.fileName);
        task.setSourcePath(entity.sourcePath);
        task.setDestinationPath(entity.destinationPath);
        task.setTotalBytes(entity.fileSize);
        task.setTransferredBytes(entity.transferredBytes > 0 ? entity.transferredBytes : entity.fileSize);
        task.setDirection(entity.direction == 0 ? TransferTask.Direction.UPLOAD : TransferTask.Direction.DOWNLOAD);
        task.setErrorMessage(entity.errorMessage);
        task.setDbId(entity.id);

        switch (entity.status) {
            case 0: task.setStatus(TransferTask.Status.QUEUED); break;
            case 1: task.setStatus(TransferTask.Status.RUNNING); break;
            case 2: task.setStatus(TransferTask.Status.COMPLETED); break;
            case 3: task.setStatus(TransferTask.Status.FAILED); break;
            default: task.setStatus(TransferTask.Status.FAILED); break;
        }
        return task;
    }

    public LiveData<List<TransferTask>> getAllTransfers() {
        return allTransfers;
    }

    public void cancelTask(String taskId) {
        repository.cancelTask(taskId);
    }

    public void clearCompleted() {
        repository.deleteCompletedTransfers();
    }
}

package com.yulink.nas.ui.transfer;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.yulink.nas.data.model.TransferTask;
import com.yulink.nas.data.repository.TransferRepository;

import java.util.List;

public class TransferQueueViewModel extends AndroidViewModel {
    private final TransferRepository repository;

    public TransferQueueViewModel(@NonNull Application application) {
        super(application);
        repository = new TransferRepository(application);
    }

    public LiveData<List<TransferTask>> getActiveTasks() {
        return repository.getActiveTasks();
    }

    public void cancelTask(String taskId) {
        repository.cancelTask(taskId);
    }

    public void clearCompleted() {
        repository.deleteCompletedTransfers();
    }
}

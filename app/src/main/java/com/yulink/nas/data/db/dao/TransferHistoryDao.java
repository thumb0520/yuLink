package com.yulink.nas.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.yulink.nas.data.db.entity.TransferHistoryEntity;

import java.util.List;

@Dao
public interface TransferHistoryDao {
    @Query("SELECT * FROM transfer_history ORDER BY startedAt DESC")
    LiveData<List<TransferHistoryEntity>> getAllTransfers();

    @Query("SELECT * FROM transfer_history WHERE status = 1 ORDER BY startedAt DESC")
    LiveData<List<TransferHistoryEntity>> getActiveTransfers();

    @Query("SELECT * FROM transfer_history WHERE connectionId = :connectionId ORDER BY startedAt DESC")
    LiveData<List<TransferHistoryEntity>> getTransfersByConnection(long connectionId);

    @Insert
    long insertTransfer(TransferHistoryEntity transfer);

    @Query("SELECT * FROM transfer_history WHERE fileName = :fileName AND connectionId = :connectionId ORDER BY id DESC LIMIT 1")
    TransferHistoryEntity getTransferByFileName(String fileName, long connectionId);

    @Update
    void updateTransfer(TransferHistoryEntity transfer);

    @Delete
    void deleteTransfer(TransferHistoryEntity transfer);

    @Query("DELETE FROM transfer_history WHERE status IN (2, 3, 4)")
    void deleteCompletedTransfers();

    @Query("UPDATE transfer_history SET status = :status, completedAt = :completedAt WHERE id = :id")
    void updateStatus(long id, int status, long completedAt);

    @Query("SELECT * FROM transfer_history WHERE id = :id")
    TransferHistoryEntity getTransferById(long id);

    @Query("DELETE FROM transfer_history WHERE id = :id")
    void deleteTransferById(long id);
}

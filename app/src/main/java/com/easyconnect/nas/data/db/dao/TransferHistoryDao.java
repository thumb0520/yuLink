package com.easyconnect.nas.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.easyconnect.nas.data.db.entity.TransferHistoryEntity;

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

    @Update
    void updateTransfer(TransferHistoryEntity transfer);

    @Delete
    void deleteTransfer(TransferHistoryEntity transfer);

    @Query("DELETE FROM transfer_history WHERE status IN (2, 3)")
    void deleteCompletedTransfers();
}

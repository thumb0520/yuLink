package com.easyconnect.nas.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.easyconnect.nas.data.db.entity.ConnectionEntity;

import java.util.List;

@Dao
public interface ConnectionDao {
    @Query("SELECT * FROM connections ORDER BY lastConnectedAt DESC")
    LiveData<List<ConnectionEntity>> getAllConnections();

    @Query("SELECT * FROM connections ORDER BY lastConnectedAt DESC")
    List<ConnectionEntity> getAllConnectionsSync();

    @Query("SELECT * FROM connections WHERE id = :id")
    ConnectionEntity getConnectionById(long id);

    @Insert
    long insertConnection(ConnectionEntity connection);

    @Update
    void updateConnection(ConnectionEntity connection);

    @Delete
    void deleteConnection(ConnectionEntity connection);

    @Query("DELETE FROM connections WHERE id = :id")
    void deleteConnectionById(long id);

    @Query("UPDATE connections SET lastConnectedAt = :timestamp WHERE id = :id")
    void updateLastConnected(long id, long timestamp);
}

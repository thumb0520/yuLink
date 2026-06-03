package com.easyconnect.nas.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.easyconnect.nas.data.db.converter.DateConverter;
import com.easyconnect.nas.data.db.converter.ProtocolTypeConverter;
import com.easyconnect.nas.data.db.dao.ConnectionDao;
import com.easyconnect.nas.data.db.dao.TransferHistoryDao;
import com.easyconnect.nas.data.db.entity.ConnectionEntity;
import com.easyconnect.nas.data.db.entity.TransferHistoryEntity;

@Database(
    entities = {ConnectionEntity.class, TransferHistoryEntity.class},
    version = 1,
    exportSchema = false
)
@TypeConverters({DateConverter.class, ProtocolTypeConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "easyconnect_db";
    private static volatile AppDatabase INSTANCE;

    public abstract ConnectionDao connectionDao();
    public abstract TransferHistoryDao transferHistoryDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}

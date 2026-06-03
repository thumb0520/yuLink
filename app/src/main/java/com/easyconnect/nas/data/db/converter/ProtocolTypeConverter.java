package com.easyconnect.nas.data.db.converter;

import androidx.room.TypeConverter;

import com.easyconnect.nas.data.model.ProtocolType;

public class ProtocolTypeConverter {
    @TypeConverter
    public static ProtocolType fromString(String value) {
        return value == null ? null : ProtocolType.valueOf(value);
    }

    @TypeConverter
    public static String protocolTypeToString(ProtocolType type) {
        return type == null ? null : type.name();
    }
}

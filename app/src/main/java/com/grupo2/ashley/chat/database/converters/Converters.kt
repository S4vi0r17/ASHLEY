package com.grupo2.ashley.chat.database.converters

import androidx.room.TypeConverter
import com.grupo2.ashley.chat.models.MessageStatus

// Sirve para que room pueda guardar un enum del messageStatus en la BD para la sincronizacion
class Converters {
    @TypeConverter
    fun fromMessageStatus(status: MessageStatus): String {
        return status.name
    }

    @TypeConverter
    fun toMessageStatus(value: String): MessageStatus {
        return try {
            MessageStatus.valueOf(value)
        } catch (e: IllegalArgumentException) {
            MessageStatus.SENT
        }
    }
}

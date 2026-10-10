package com.codetoday.remembrance.data.db

import android.content.Context
import androidx.room.*
import com.codetoday.remembrance.data.model.OverlayPosition
import com.codetoday.remembrance.data.model.ReminderItem
import com.codetoday.remembrance.data.model.SupportedLanguage

class Converters {
    @TypeConverter
    fun fromLanguage(language: SupportedLanguage): String = language.name

    @TypeConverter
    fun toLanguage(value: String): SupportedLanguage = try {
        SupportedLanguage.valueOf(value)
    } catch (e: Exception) {
        SupportedLanguage.BENGALI
    }

    @TypeConverter
    fun fromPosition(position: OverlayPosition): String = position.name

    @TypeConverter
    fun toPosition(value: String): OverlayPosition = try {
        OverlayPosition.valueOf(value)
    } catch (e: Exception) {
        OverlayPosition.TOP
    }
}

@Database(entities = [ReminderItem::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "remembrance_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

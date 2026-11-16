package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [
    PlaylistEntity::class,
    PlaylistTrackEntity::class,
    ImportedMediaEntity::class,
    TrackMetadataEntity::class,
  ],
  version = 1,
  exportSchema = false,
)
abstract class KlangwerkDatabase : RoomDatabase() {
  abstract fun playlistDao(): PlaylistDao

  abstract fun importedMediaDao(): ImportedMediaDao

  abstract fun trackMetadataDao(): TrackMetadataDao

  companion object {
    @Volatile private var INSTANCE: KlangwerkDatabase? = null

    fun getInstance(context: Context): KlangwerkDatabase {
      return INSTANCE
        ?: synchronized(this) {
          val instance =
            Room.databaseBuilder(
                context.applicationContext,
                KlangwerkDatabase::class.java,
                "klangwerk_db",
              )
              .build()
          INSTANCE = instance
          instance
        }
    }
  }
}

package fi.ainon.polarAppis.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DataItemType::class, PolarInfoData::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun polarInfoDataDao(): PolarInfoDataDao
}

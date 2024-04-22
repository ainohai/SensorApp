package fi.ainon.polarAppis.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PolarInfoData::class, HrData::class], version = 2)
abstract class AppDatabase : RoomDatabase() {

    abstract fun polarInfoDataDao(): PolarInfoDataDao
    abstract fun hrDataDao(): HrDataDao
}

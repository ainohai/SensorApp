package fi.ainon.polarAppis.data.local.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity
data class PolarInfoData(
    val connected: Boolean
) {
    @PrimaryKey(autoGenerate = false)
    var uid: Int = 0
}

@Dao
interface PolarInfoDataDao {
    @Query("SELECT * FROM polarinfodata LIMIT 1")
    fun getPolarInfoData(): Flow<List<PolarInfoData>>

    @Upsert
    suspend fun upsertPolarInfoData(item: PolarInfoData)
}

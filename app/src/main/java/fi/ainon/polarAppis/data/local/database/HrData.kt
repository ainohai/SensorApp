package fi.ainon.polarAppis.data.local.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity
data class HrData(
    val hr1: Double,
    val timepoint: Long
) {
    @PrimaryKey(autoGenerate = true)
    var uid: Int = 0
}

@Dao
interface HrDataDao {
    @Query("SELECT * FROM hrdata ORDER BY timepoint DESC LIMIT 100")
    fun getHrData(): Flow<List<HrData>>

    @Insert
    suspend fun addHrData(item: HrData)
}

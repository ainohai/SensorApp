package fi.ainon.polarAppis.data

import fi.ainon.polarAppis.data.local.database.PolarInfoData
import fi.ainon.polarAppis.data.local.database.PolarInfoDataDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface PolarDataRepository {
    val connection: Flow<Boolean>

    suspend fun isConnected(connected: Boolean)
}

class DefaultPolarDataRepository @Inject constructor(
    private val polarInfoDataDao: PolarInfoDataDao
) : PolarDataRepository {

    override val connection: Flow<Boolean> =
        polarInfoDataDao.getPolarInfoData().map { items -> items.map { polarInfoData -> polarInfoData.connected }[0] }

    override suspend fun isConnected(connected: Boolean) {

        polarInfoDataDao.upsertPolarInfoData(PolarInfoData(connected))

    }
}

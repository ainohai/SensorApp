package fi.ainon.polarAppis.data

import android.util.Log
import fi.ainon.polarAppis.data.local.database.PolarInfoData
import fi.ainon.polarAppis.data.local.database.PolarInfoDataDao
import fi.ainon.polarAppis.dataHandling.DataHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * For delegating data from and to database.
 */
interface PolarDataRepository {
    val connection: Flow<Boolean>

    suspend fun isConnected(connected: Boolean)

}

class DefaultPolarDataRepository @Inject constructor(
    private val polarInfoDataDao: PolarInfoDataDao,
    private val dataHandler: DataHandler
) : PolarDataRepository {

    private val TAG = "PolarDataRepository: "

    init {
        listenToConnection(dataHandler.isConnected())
    }

    override val connection: Flow<Boolean> =
        polarInfoDataDao.getPolarInfoData().map { items -> items.map { polarInfoData -> polarInfoData.connected }[0] }

    override suspend fun isConnected(connected: Boolean) {

        polarInfoDataDao.upsertPolarInfoData(PolarInfoData(connected))

    }

    // TODO: we don't necessarily want to save connection status into db
    private fun listenToConnection(connectionFlow : Flow<Boolean>) {
        CoroutineScope(Dispatchers.Main).launch {
            isConnected(false)
            Log.d(TAG, "Starting listening to connection status")
            connectionFlow.collect { connectionStatus -> isConnected(connectionStatus) }
        }
    }
}

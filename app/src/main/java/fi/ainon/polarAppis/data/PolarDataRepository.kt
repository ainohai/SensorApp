package fi.ainon.polarAppis.data

import android.util.Log
import fi.ainon.polarAppis.data.local.database.HrData
import fi.ainon.polarAppis.data.local.database.HrDataDao
import fi.ainon.polarAppis.data.local.database.PolarInfoData
import fi.ainon.polarAppis.data.local.database.PolarInfoDataDao
import fi.ainon.polarAppis.dataHandling.handler.HandleConnection
import fi.ainon.polarAppis.dataHandling.handler.HandleHr
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
    val RRMSSD: Flow<List<HrData>>

    suspend fun isConnected(connected: Boolean)
    suspend fun addRRMSSD(RRMSSD: Double)
}

class DefaultPolarDataRepository @Inject constructor(
    private val polarInfoDataDao: PolarInfoDataDao,
    private val hrDataDao: HrDataDao,
    private val handleHr: HandleHr,
    private val handleConnection: HandleConnection
) : PolarDataRepository {

    private val TAG = "PolarDataRepository: "

    init {
        listenToConnection(handleConnection.dataFlow())
        listenToRRMSSD(handleHr.RRMSSD())
    }

    override val connection: Flow<Boolean> =
        polarInfoDataDao.getPolarInfoData().map { items -> items.map { polarInfoData -> polarInfoData.connected }[0] }
    override val RRMSSD: Flow<List<HrData>> =
        hrDataDao.getHrData().map{list -> list.reversed()} //TODO

    override suspend fun isConnected(connected: Boolean) {

        polarInfoDataDao.upsertPolarInfoData(PolarInfoData(connected))

    }
    override suspend fun addRRMSSD(RRMSSD: Double) {
        hrDataDao.addHrData(HrData(RRMSSD, System.currentTimeMillis()))
    }

    // TODO: we don't necessarily want to save connection status into db
    private fun listenToConnection(connectionFlow : Flow<Boolean>) {
        CoroutineScope(Dispatchers.Main).launch {
            isConnected(false)
            Log.d(TAG, "Starting listening to connection status")
            connectionFlow.collect { connectionStatus -> isConnected(connectionStatus) }
        }
    }

    private fun listenToRRMSSD(RRMSSDFlow : Flow<Double>) {
        CoroutineScope(Dispatchers.Main).launch {

            Log.d(TAG, "Starting listening to RRMSSD status")
            RRMSSDFlow.collect { value -> addRRMSSD(value) }
        }
    }
}

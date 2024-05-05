package fi.ainon.polarAppis.data

import android.util.Log
import fi.ainon.polarAppis.communication.polar.PolarConnection
import fi.ainon.polarAppis.data.local.database.HrData
import fi.ainon.polarAppis.data.local.database.HrDataDao
import fi.ainon.polarAppis.data.local.database.PolarInfoData
import fi.ainon.polarAppis.data.local.database.PolarInfoDataDao
import fi.ainon.polarAppis.dataHandling.handler.HandleH10Connection
import fi.ainon.polarAppis.dataHandling.handler.HandleH10Hr
import fi.ainon.polarAppis.dataHandling.handler.HandleH10Rrs
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
    private val handleH10Hr: HandleH10Hr,
    private val handleH10Connection: HandleH10Connection,
    private val handleH10Rrs: HandleH10Rrs,
    private val polarConnection: PolarConnection
) : PolarDataRepository {

    private val TAG = "PolarDataRepository: "

    init {
        polarConnection.cleanupCollectors() //Todo: ensures everything is set up.
        listenToConnection(handleH10Connection.dataFlow())
        listenToRRMSSD(handleH10Rrs.dataFlow())
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

package fi.ainon.polarAppis.dataHandling.handler

import android.util.Log
import fi.ainon.polarAppis.communication.polar.PolarConnection
import fi.ainon.polarAppis.dataHandling.dataObject.ConnectionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class HandleConnection @Inject constructor(
    private val polarConnection: PolarConnection,
) : DataHandler<ConnectionStatus, Boolean> {

    private val connectionStatus: SharedFlow<ConnectionStatus> = polarConnection.connectionStatus()
    private val TAG = "HandleConnection: "

    override fun handle(data: ConnectionStatus) {
        throw NotImplementedError()
    }

    override fun dataFlow(): SharedFlow<Boolean> {
        return connectionStatus
            .filter { status -> status == ConnectionStatus.CONNECTED || status == ConnectionStatus.DISCONNECTED }
            .map { connectionStatus ->
                connectionStatus == ConnectionStatus.CONNECTED
            }
            .catch { e ->
                Log.e(TAG, "Error getting connectionStatus", e)
                emit(false)
            }
            .shareIn(CoroutineScope(Dispatchers.Main), SharingStarted.WhileSubscribed(), replay = 1)
    }
}

package fi.ainon.polarAppis.dataHandling.handler

import android.util.Log
import fi.ainon.polarAppis.dataHandling.dataObject.ConnectionStatus
import fi.ainon.polarAppis.dataHandling.di.DataHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class HandleH10Connection @Inject constructor() : DataHandler<ConnectionStatus, Boolean> {

    private val TAG = "HandleH10Connection: "
    private var _connectionStatus: SharedFlow<ConnectionStatus> = MutableSharedFlow<ConnectionStatus>(replay = 1)

    override suspend fun handle(data: Flow<ConnectionStatus>) {
        //TODO not implemented yet.
    }
    fun setConnectionFlow(data: SharedFlow<ConnectionStatus>) {
        Log.d(TAG, "Sets conneection to handler")
        _connectionStatus = data
    }

    override fun dataFlow(): SharedFlow<Boolean> {
        return _connectionStatus
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

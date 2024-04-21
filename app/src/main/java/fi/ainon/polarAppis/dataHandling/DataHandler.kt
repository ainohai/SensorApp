package fi.ainon.polarAppis.dataHandling

import android.util.Log
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fi.ainon.polarAppis.communication.dataServer.ServerDataConnection
import fi.ainon.polarAppis.communication.polar.PolarConnection
import fi.ainon.polarAppis.dataHandling.dataObject.AccData
import fi.ainon.polarAppis.dataHandling.dataObject.ConnectionStatus
import fi.ainon.polarAppis.dataHandling.dataObject.DataType
import fi.ainon.polarAppis.dataHandling.dataObject.EcgData
import fi.ainon.polarAppis.dataHandling.dataObject.HrData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
interface DataHandlerModule {

    @Binds
    fun bindDataHandler(defaultDataHandler: DefaultDataHandler): DataHandler
}

interface DataHandler {
    fun handleHr(data: HrData)
    fun handleECG(data: EcgData)
    fun handleAcc(data: AccData)
    fun isConnected(): SharedFlow<Boolean>
    fun hrFlow() : SharedFlow<Int>
    fun ecgFlow() : SharedFlow<List<Pair<Long, Int>>>

}

@Singleton
class DefaultDataHandler @Inject constructor(
    private val serverDataConnection: ServerDataConnection,
    private val polarConnection: PolarConnection,
) : DataHandler {

    private val connectionStatus: SharedFlow<ConnectionStatus> = polarConnection.connectionStatus()
    private val TAG = "DataHandler: "
    private var _hrFlow = MutableSharedFlow<Int>()
    private var hrFlow = _hrFlow.asSharedFlow()
    private var _ecgFlow = MutableSharedFlow<List<Pair<Long, Int>>>()
    private var ecgFlow = _ecgFlow.asSharedFlow()

    override fun handleHr(data: HrData) {

        Log.d(TAG, "Handle hr")

        val jsonString = Json.encodeToString(data)
        val byteData = jsonString.toByteArray(Charsets.UTF_16)
        serverDataConnection.addData(byteData, DataType.HR)

        // Hr does not contain timepoint
        val hrValues = data.samples.map{hrSample ->  hrSample.hr}
        CoroutineScope(Dispatchers.Default).launch {
            try {
                _hrFlow.emitAll(hrValues.asFlow())
            } catch (e: Exception) {
                Log.e(TAG, "Emit hr flow failed.", e)
            }
        }
    }
    override fun handleECG(data: EcgData) {

        Log.d(TAG, "Handle ecg")

        //Todo: Function for this and remove duplicates
        val jsonString = Json.encodeToString(data)
        val byteData = jsonString.toByteArray(Charsets.UTF_16)
        serverDataConnection.addData(byteData, DataType.ECG)

        val ecgValues = data.samples.map{ecgSample ->  Pair<Long, Int>(ecgSample.timeStamp, ecgSample.voltage)}
        CoroutineScope(Dispatchers.Default).launch {
            try {
                _ecgFlow.emit(ecgValues)
            } catch (e: Exception) {
                Log.e(TAG, "Emit hr flow failed.", e)
            }
        }
    }

    override fun handleAcc(data: AccData) {

        Log.d(TAG, "Handle acc")

        val jsonString = Json.encodeToString(data)
        val byteData = jsonString.toByteArray(Charsets.UTF_16)
        serverDataConnection.addData(byteData, DataType.ACC)

        //Todo: Other handling
    }

    //TODO: Do we need shared flow here.
    override fun isConnected(): SharedFlow<Boolean> {
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

    override fun hrFlow(): SharedFlow<Int> {
        return hrFlow
    }
    override fun ecgFlow(): SharedFlow<List<Pair<Long, Int>>> {
        return ecgFlow
    }

}

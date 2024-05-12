package fi.ainon.polarAppis.dataHandling.handler

import android.util.Log
import fi.ainon.polarAppis.communication.dataServer.ServerDataConnection
import fi.ainon.polarAppis.dataHandling.dataObject.DataType
import fi.ainon.polarAppis.dataHandling.dataObject.HrData
import fi.ainon.polarAppis.dataHandling.di.DataHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class HandleH10Hr @Inject constructor(
    private val serverDataConnection: ServerDataConnection,
) : DataHandler<HrData, HrData.HrSample> {


    private val TAG = "HandleH10Hr: "
    private var _hrFlow = MutableSharedFlow<HrData.HrSample>()
    private var hrFlow = _hrFlow.asSharedFlow()

    override suspend fun handle(dataFlow: Flow<HrData>) {

        Log.d(TAG, "Handle hr")

        CoroutineScope(Dispatchers.Default).launch {
            dataFlow.collect { data ->
                val jsonString = Json.encodeToString(data)
                val byteData = jsonString.toByteArray(Charsets.UTF_16)
                serverDataConnection.addData(byteData, DataType.HR)

                try {
                    _hrFlow.emitAll(data.samples.asFlow())
                } catch (e: Exception) {
                    Log.e(TAG, "Emit hr flow failed.", e)
                }
            }
        }
    }

    override fun dataFlow(): SharedFlow<HrData.HrSample> {
        return hrFlow
    }

}

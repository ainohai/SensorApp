package fi.ainon.polarAppis.dataHandling.handler

import android.util.Log
import fi.ainon.polarAppis.communication.dataServer.ServerDataConnection
import fi.ainon.polarAppis.dataHandling.dataObject.AccData
import fi.ainon.polarAppis.dataHandling.dataObject.DataType
import fi.ainon.polarAppis.dataHandling.di.DataHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class HandleAcc @Inject constructor(
    private val serverDataConnection: ServerDataConnection,
) : DataHandler<AccData, AccData> {

    private val TAG = "HandlerAcc: "
    override suspend fun handle(dataFlow: Flow<AccData>) {

        Log.d(TAG, "Handle acc")

        CoroutineScope(Dispatchers.Default).launch {
            dataFlow.collect { data ->
                val jsonString = Json.encodeToString(data)
                val byteData = jsonString.toByteArray(Charsets.UTF_16)
                serverDataConnection.addData(byteData, DataType.ACC)
            }
        }
        //Todo: Other handling
    }

    override fun dataFlow(): SharedFlow<AccData> {
        throw NotImplementedError()
    }


}

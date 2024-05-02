package fi.ainon.polarAppis.dataHandling.handler

import android.util.Log
import fi.ainon.polarAppis.communication.dataServer.ServerDataConnection
import fi.ainon.polarAppis.dataHandling.dataObject.DataType
import fi.ainon.polarAppis.dataHandling.dataObject.EcgData
import fi.ainon.polarAppis.dataHandling.di.DataHandler
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HandleEcg @Inject constructor(
    private val serverDataConnection: ServerDataConnection,
) : DataHandler<EcgData, Int> {

    private val TAG = "EcgHandler: "

    override fun handle(data: EcgData) {

        Log.d(TAG, "Handle ecg")

        //Todo: Function for this and remove duplicates
        val jsonString = Json.encodeToString(data)
        val byteData = jsonString.toByteArray(Charsets.UTF_16)
        serverDataConnection.addData(byteData, DataType.ECG)

        // No local usage
    }


    override fun dataFlow(): SharedFlow<Int> {
        throw NotImplementedError()
    }

}

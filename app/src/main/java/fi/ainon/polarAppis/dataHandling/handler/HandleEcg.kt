package fi.ainon.polarAppis.dataHandling.handler

import android.util.Log
import fi.ainon.polarAppis.communication.dataServer.ServerDataConnection
import fi.ainon.polarAppis.dataHandling.dataObject.DataType
import fi.ainon.polarAppis.dataHandling.dataObject.EcgData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HandleEcg @Inject constructor(
    private val serverDataConnection: ServerDataConnection,
) : DataHandler<EcgData, Int> {

    private val TAG = "EcgHandler: "
    private var _ecgFlow = MutableSharedFlow<Pair<Long, Int>>()
    private var ecgFlow = _ecgFlow.asSharedFlow()

    private var remnantEcgValues: MutableList<Pair<Long, Int>> = mutableListOf()
    private val ECG_DOWNSAMPLE_SIZE = 20

    override fun handle(data: EcgData) {

        Log.d(TAG, "Handle ecg")

        //Todo: Function for this and remove duplicates
        val jsonString = Json.encodeToString(data)
        val byteData = jsonString.toByteArray(Charsets.UTF_16)
        serverDataConnection.addData(byteData, DataType.ECG)

        val ecgValues = data.samples.map { ecgSample ->
            Pair<Long, Int>(
                ecgSample.timeStamp,
                ecgSample.voltage
            )
        }
        remnantEcgValues.addAll(ecgValues)

        downsampleAndEmit()

    }

    // Really crude downsampling.
    private fun downsampleAndEmit() {

        while (remnantEcgValues.size > ECG_DOWNSAMPLE_SIZE) {

            val newDownsample = remnantEcgValues.slice(0..<ECG_DOWNSAMPLE_SIZE)
            remnantEcgValues =
                remnantEcgValues.slice(ECG_DOWNSAMPLE_SIZE..remnantEcgValues.lastIndex)
                    .toMutableList()

            val voltage = newDownsample.map { sample -> sample.second }.reduce { acc, i -> i + acc }

            CoroutineScope(Dispatchers.Default).launch {
                try {
                    _ecgFlow.emit(Pair(newDownsample[0].first, voltage))
                } catch (e: Exception) {
                    Log.e(TAG, "Emit hr flow failed.", e)
                }
            }
        }


    }

    override fun dataFlow(): SharedFlow<Int> {
        throw NotImplementedError()
    }

    fun ecgFlow(): SharedFlow<Pair<Long, Int>> {
        return ecgFlow
    }
}

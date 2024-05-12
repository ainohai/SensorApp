package fi.ainon.polarAppis.dataHandling.handler

import android.util.Log
import fi.ainon.polarAppis.communication.dataServer.ServerDataConnection
import fi.ainon.polarAppis.dataHandling.dataObject.DataType
import fi.ainon.polarAppis.dataHandling.dataObject.EcgData
import fi.ainon.polarAppis.dataHandling.di.DataHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
) : DataHandler<EcgData, EcgData.EcgDataSample> {

    private val TAG = "EcgHandler: "

    private var _ecgFlow = MutableSharedFlow<EcgData.EcgDataSample>()
    private var ecgFlow = _ecgFlow.asSharedFlow()

    private var remnantEcgValues: MutableList<EcgData.EcgDataSample> = mutableListOf()
    private val ECG_DOWNSAMPLE_SIZE = 20

    override suspend fun handle(dataFlow: Flow<EcgData>) {

        CoroutineScope(Dispatchers.Default).launch {
            dataFlow.collect { data ->
                Log.d(TAG, "Handle ecg")

                //Todo: Function for this and remove duplicates
                val jsonString = Json.encodeToString(data)
                val byteData = jsonString.toByteArray(Charsets.UTF_16)
                serverDataConnection.addData(byteData, DataType.ECG)

                val ecgValues = data.samples.map { ecgSample ->
                    EcgData.EcgDataSample(
                        ecgSample.timeStamp,
                        ecgSample.voltage
                    )
                }
                remnantEcgValues.addAll(ecgValues)

                downsampleAndEmit()

            }
        }
    }


    override fun dataFlow(): SharedFlow<EcgData.EcgDataSample> {
        return ecgFlow
    }

    // Really crude downsampling.
    private suspend fun downsampleAndEmit() {

        while (remnantEcgValues.size > ECG_DOWNSAMPLE_SIZE) {

            val newDownsample = remnantEcgValues.slice(0..<ECG_DOWNSAMPLE_SIZE)
            remnantEcgValues =
                remnantEcgValues.slice(ECG_DOWNSAMPLE_SIZE..remnantEcgValues.lastIndex)
                    .toMutableList()

            val voltage =
                newDownsample.map { sample -> sample.voltage }.reduce { acc, i -> maxOf(i, acc) }

            try {
                _ecgFlow.emit(EcgData.EcgDataSample(newDownsample[0].timeStamp, voltage))
            } catch (e: Exception) {
                Log.e(TAG, "Emit hr flow failed.", e)
            }

        }
    }
}

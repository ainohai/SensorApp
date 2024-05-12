package fi.ainon.polarAppis.dataHandling.sensorDataCollector

import android.util.Log
import com.polar.sdk.api.model.PolarEcgData
import fi.ainon.polarAppis.dataHandling.dataObject.EcgData
import io.reactivex.rxjava3.core.Flowable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch


class CollectEcg(
    private val ecgStream: Flowable<PolarEcgData>,
) : CommonCollect<EcgData>() {

    private val TAG = "CollectEcg: "
    private val _ecgFlow = MutableSharedFlow<EcgData>()

    override fun streamData(): Flow<EcgData> {
        //Polar returns rx flowable.
         val ecgDisposable = ecgStream.subscribe(
            { polarEcgData: PolarEcgData ->
                val samples = mutableListOf<EcgData.EcgDataSample>()
                for (data in polarEcgData.samples) {
                    //Log.d(TAG, "    yV: ${data.voltage} timeStamp: ${data.timeStamp}")
                    samples.add(EcgData.EcgDataSample(data.timeStamp, data.voltage))
                }

                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        _ecgFlow.emit(EcgData(samples))
                    } catch (e: Exception) {
                        Log.e(TAG, "Emit hr flow failed.", e)
                    }
                }
            },
            { error: Throwable ->
                Log.e(TAG, "ECG stream failed. Reason $error")
                stopCollect()
            }
        )

        setDisposable(ecgDisposable)
        return _ecgFlow.asSharedFlow()
    }


}
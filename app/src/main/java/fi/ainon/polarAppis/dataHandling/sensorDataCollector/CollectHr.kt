package fi.ainon.polarAppis.dataHandling.sensorDataCollector

import android.util.Log
import com.polar.sdk.api.model.PolarHrData
import fi.ainon.polarAppis.dataHandling.dataObject.HrData
import io.reactivex.rxjava3.core.Flowable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch


class CollectHr(
    private val hrStream: Flowable<PolarHrData>,
) : CommonCollect<HrData>() {

    private val TAG = "CollectHr: "
    private val _hrFlow = MutableSharedFlow<HrData>()

    //TODO: Should be flowable?
    override fun streamData(): Flow<HrData> {
         val hrDisposable = hrStream.subscribe(
            { polarHrData: PolarHrData ->
                val samples = mutableListOf<HrData.HrSample>()
                for (data in polarHrData.samples) {

                    //Log.d(TAG, "HR     bpm: ${data.hr} rrs: ${data.rrsMs} rrAvailable: ${data.rrAvailable} contactStatus: ${data.contactStatus} contactStatusSupported: ${data.contactStatusSupported}")
                    samples.add(HrData.HrSample(data.hr, data.rrsMs, data.rrAvailable, data.contactStatus, data.contactStatusSupported))
                }

                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        _hrFlow.emit(HrData(samples))
                    } catch (e: Exception) {
                        Log.e(TAG, "Emit hr flow failed.", e)
                    }
                }
            },
            { error: Throwable ->
                Log.e(TAG, "HR stream failed. Reason $error")
                stopCollect()
            }
        )
        setDisposable(hrDisposable)
        return _hrFlow.asSharedFlow()
    }
}
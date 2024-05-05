package fi.ainon.polarAppis.dataHandling.sensorDataCollector

import android.util.Log
import com.polar.sdk.api.model.PolarHrData
import fi.ainon.polarAppis.dataHandling.dataObject.HrData
import fi.ainon.polarAppis.dataHandling.handler.HandleH10Hr
import io.reactivex.rxjava3.core.Flowable


class CollectHr(
    private val dataHandler: HandleH10Hr,
    private val hrStream: Flowable<PolarHrData>,
) : CommonCollect() {

    private val TAG = "CollectHr: "

    init {
        collectData()
    }

    override fun streamData() {
         val hrDisposable = hrStream.subscribe(
            { polarHrData: PolarHrData ->
                val samples = mutableListOf<HrData.HrSample>()
                for (data in polarHrData.samples) {

                    //Log.d(TAG, "HR     bpm: ${data.hr} rrs: ${data.rrsMs} rrAvailable: ${data.rrAvailable} contactStatus: ${data.contactStatus} contactStatusSupported: ${data.contactStatusSupported}")
                    samples.add(HrData.HrSample(data.hr, data.rrsMs, data.rrAvailable, data.contactStatus, data.contactStatusSupported))
                }
                dataHandler.handle(HrData(samples))
            },
            { error: Throwable ->
                Log.e(TAG, "HR stream failed. Reason $error")
                stopCollect()
            }
        )
        setDisposable(hrDisposable)
    }
}
package fi.ainon.polarAppis.dataHandling.sensorDataCollector

import android.util.Log
import com.polar.sdk.api.model.PolarEcgData
import fi.ainon.polarAppis.dataHandling.dataObject.EcgData
import fi.ainon.polarAppis.dataHandling.handler.HandleEcg
import io.reactivex.rxjava3.core.Flowable


class CollectEcg(
    private val dataHandler: HandleEcg,
    private val ecgStream: Flowable<PolarEcgData>,
) : CommonCollect() {

    private val TAG = "CollectEcg: "

    init {
        collectData()
    }
    override fun streamData() {
        //Polar returns rx flowable.
         val ecgDisposable = ecgStream.subscribe(
            { polarEcgData: PolarEcgData ->
                val samples = mutableListOf<EcgData.EcgDataSample>()
                for (data in polarEcgData.samples) {
                    //Log.d(TAG, "    yV: ${data.voltage} timeStamp: ${data.timeStamp}")
                    samples.add(EcgData.EcgDataSample(data.timeStamp, data.voltage))
                }
                dataHandler.handle(EcgData(samples))
            },
            { error: Throwable ->
                Log.e(TAG, "ECG stream failed. Reason $error")
                stopCollect()
            }
        )

        setDisposable(ecgDisposable)

    }


}
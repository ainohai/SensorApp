package fi.ainon.polarAppis.dataHandling.sensorDataCollector

import android.util.Log
import com.polar.sdk.api.model.PolarEcgData
import com.polar.sdk.api.model.PolarSensorSetting
import fi.ainon.polarAppis.communication.polar.PolarConnection
import fi.ainon.polarAppis.dataHandling.dataObject.EcgData
import fi.ainon.polarAppis.dataHandling.handler.HandleEcg


class CollectEcg(
    private val dataHandler: HandleEcg,
    private val polarConnection: PolarConnection,
    polarSettings: PolarSensorSetting
) : CommonCollect(polarSettings) {

    private val TAG = "CollectEcg: "

    init {
        collectData()
    }
    override fun streamData(polarSettings: PolarSensorSetting) {
        //Polar returns rx flowable.
         val ecgDisposable = polarConnection.getEcg(polarSettings).subscribe(
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
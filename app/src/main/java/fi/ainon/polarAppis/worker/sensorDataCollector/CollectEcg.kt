package fi.ainon.polarAppis.worker.sensorDataCollector

import android.util.Log
import com.polar.sdk.api.model.PolarEcgData
import com.polar.sdk.api.model.PolarSensorSetting
import fi.ainon.polarAppis.communication.polar.PolarConnection
import fi.ainon.polarAppis.dataHandling.DataHandler
import fi.ainon.polarAppis.worker.dataObject.EcgData


class CollectEcg(
    private val dataHandler: DataHandler,
    private val polarConnection: PolarConnection,
    polarSettings: PolarSensorSetting
) : CommonCollect(polarSettings) {

    private val TAG = "CollectEcg: "

    override fun streamData(polarSettings: PolarSensorSetting) {
         val ecgDisposable = polarConnection.getEcg(polarSettings).subscribe(
            { polarEcgData: PolarEcgData ->
                val samples = mutableListOf<EcgData.EcgDataSample>()
                for (data in polarEcgData.samples) {
                    //Log.d(TAG, "    yV: ${data.voltage} timeStamp: ${data.timeStamp}")
                    samples.add(EcgData.EcgDataSample(data.timeStamp, data.voltage))
                }
                dataHandler.handleECG(EcgData(samples))
            },
            { error: Throwable ->
                Log.e(TAG, "ECG stream failed. Reason $error")
                stopCollect()
            }
        )

        setDisposable(ecgDisposable)

    }


}
package fi.ainon.polarAppis.dataHandling.sensorDataCollector

import android.util.Log
import com.polar.sdk.api.model.PolarHrData
import com.polar.sdk.api.model.PolarSensorSetting
import fi.ainon.polarAppis.communication.polar.PolarConnection
import fi.ainon.polarAppis.dataHandling.DataHandler
import fi.ainon.polarAppis.dataHandling.dataObject.HrData


class CollectHr(
    private val dataHandler: DataHandler,
    private val polarConnection: PolarConnection,
    polarSettings: PolarSensorSetting
) : CommonCollect(polarSettings) {

    private val TAG = "CollectHr: "

    init {
        collectData()
    }

    override fun streamData(polarSettings: PolarSensorSetting) {
         val hrDisposable = polarConnection.getHr().subscribe(
            { polarHrData: PolarHrData ->
                val samples = mutableListOf<HrData.HrSample>()
                for (data in polarHrData.samples) {

                    //Log.d(TAG, "HR     bpm: ${data.hr} rrs: ${data.rrsMs} rrAvailable: ${data.rrAvailable} contactStatus: ${data.contactStatus} contactStatusSupported: ${data.contactStatusSupported}")
                    samples.add(HrData.HrSample(data.hr, data.rrsMs, data.rrAvailable, data.contactStatus, data.contactStatusSupported))
                }
                dataHandler.handleHr(HrData(samples))
            },
            { error: Throwable ->
                Log.e(TAG, "HR stream failed. Reason $error")
                stopCollect()
            }
        )
        setDisposable(hrDisposable)
    }


}
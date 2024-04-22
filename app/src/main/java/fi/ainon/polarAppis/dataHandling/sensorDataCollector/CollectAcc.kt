package fi.ainon.polarAppis.dataHandling.sensorDataCollector

import android.util.Log
import com.polar.sdk.api.model.PolarAccelerometerData
import com.polar.sdk.api.model.PolarSensorSetting
import fi.ainon.polarAppis.communication.polar.PolarConnection
import fi.ainon.polarAppis.dataHandling.dataObject.AccData
import fi.ainon.polarAppis.dataHandling.handler.HandleAcc


class CollectAcc(
    private val dataHandler: HandleAcc,
    private val polarConnection: PolarConnection,
    polarSettings: PolarSensorSetting
) : CommonCollect(polarSettings) {

    private val TAG = "CollectAcc: "

    init {
        collectData()
    }
    override fun streamData(polarSettings: PolarSensorSetting) {

        val accDisposable = polarConnection.getAcc(polarSettings)
                    .subscribe(
                        { polarAccelerometerData: PolarAccelerometerData ->
                            val samples = mutableListOf<AccData.AccDataSample>()
                            for (data in polarAccelerometerData.samples) {
                                //Log.d(TAG, "ACC    x: ${data.x} y: ${data.y} z: ${data.z} timeStamp: ${data.timeStamp}")
                                samples.add(AccData.AccDataSample(data.timeStamp, data.x, data.y, data.z))
                            }
                            dataHandler.handle(AccData(samples))
                        },
                        { error: Throwable ->
                            Log.e(TAG, "ACC stream failed. Reason $error")
                            stopCollect()
                        }
                    )

        setDisposable(accDisposable)
    }
}
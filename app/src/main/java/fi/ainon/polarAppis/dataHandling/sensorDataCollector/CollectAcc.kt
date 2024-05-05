package fi.ainon.polarAppis.dataHandling.sensorDataCollector

import android.util.Log
import com.polar.sdk.api.model.PolarAccelerometerData
import fi.ainon.polarAppis.dataHandling.dataObject.AccData
import fi.ainon.polarAppis.dataHandling.handler.HandleAcc
import io.reactivex.rxjava3.core.Flowable


class CollectAcc(
    private val dataHandler: HandleAcc,
    private val accStream: Flowable<PolarAccelerometerData>,
) : CommonCollect() {

    private val TAG = "CollectAcc: "

    init {
        collectData()
    }
    override fun streamData() {

        val accDisposable = accStream
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
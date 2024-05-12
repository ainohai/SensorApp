package fi.ainon.polarAppis.dataHandling.sensorDataCollector

import android.util.Log
import com.polar.sdk.api.model.PolarAccelerometerData
import fi.ainon.polarAppis.dataHandling.dataObject.AccData
import io.reactivex.rxjava3.core.Flowable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch


class CollectAcc(
    private val accStream: Flowable<PolarAccelerometerData>,
) : CommonCollect<AccData>() {

    private val TAG = "CollectAcc: "

    override fun streamData() : Flow<AccData> {

        val _accFlow = MutableSharedFlow<AccData>()
        val accDisposable = accStream
                    .subscribe(
                        { polarAccelerometerData: PolarAccelerometerData ->
                            val samples = mutableListOf<AccData.AccDataSample>()
                            for (data in polarAccelerometerData.samples) {
                                //Log.d(TAG, "ACC    x: ${data.x} y: ${data.y} z: ${data.z} timeStamp: ${data.timeStamp}")
                                samples.add(AccData.AccDataSample(data.timeStamp, data.x, data.y, data.z))
                            }

                            CoroutineScope(Dispatchers.Default).launch {
                                try {
                                    _accFlow.emit(AccData(samples))
                                } catch (e: Exception) {
                                    Log.e(TAG, "Emit hr flow failed.", e)
                                }
                            }
                        },
                        { error: Throwable ->
                            Log.e(TAG, "ACC stream failed. Reason $error")
                            stopCollect()
                        }
                    )

        setDisposable(accDisposable)
        Log.d(TAG, "Creating acc flow")
        return _accFlow.asSharedFlow()
    }
}
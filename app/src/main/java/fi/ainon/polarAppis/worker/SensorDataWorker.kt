package fi.ainon.polarAppis.worker

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.polar.sdk.api.model.PolarSensorSetting
import fi.ainon.polarAppis.communication.polar.PolarConnection
import fi.ainon.polarAppis.dataHandling.dataObject.DataSetting
import fi.ainon.polarAppis.dataHandling.dataObject.DataType
import fi.ainon.polarAppis.dataHandling.handler.HandleAcc
import fi.ainon.polarAppis.dataHandling.handler.HandleConnection
import fi.ainon.polarAppis.dataHandling.handler.HandleEcg
import fi.ainon.polarAppis.dataHandling.handler.HandleHr
import fi.ainon.polarAppis.dataHandling.sensorDataCollector.CollectAcc
import fi.ainon.polarAppis.dataHandling.sensorDataCollector.CollectEcg
import fi.ainon.polarAppis.dataHandling.sensorDataCollector.CollectHr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class SensorDataWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val polarConnection: PolarConnection,
    private val connectionHandler: HandleConnection,
    private val accHandler: HandleAcc,
    private val ecgHandler: HandleEcg,
    private val hrHandler: HandleHr
) : CoroutineWorker(context, workerParams) {

    private val TAG = "SensorWorker: "
    private var ecg : CollectEcg? = null;
    private var acc : CollectAcc? = null;
    private var hr : CollectHr? = null;


    @SuppressLint("CheckResult")
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {

            Log.d(TAG, "Starting doWork in sensor worker")

            val collectionTime =
                inputData.getString(DataSetting.COLLECTION_TIME_IN_S.name)?.toLongOrNull() ?: 30

            withTimeoutOrNull(collectionTime * 1000) {
                connectionHandler.dataFlow().collect { isConnected ->
                    Log.d(TAG, "Is connected: $isConnected")
                    if (isConnected) {
                        collectData()
                        Log.d(TAG, "Already connected, starting to collect data")
                    }
                }
            }

            Log.d(TAG, "Timeout")

        } catch (ex: Exception) {
            Log.e(TAG, "Error when collecting sensor data", ex)
            cleanupCollectors()
            Result.failure()
        }
        // Also when user stops the worker, finally is run.
        finally {
            cleanupCollectors()
            Log.d(TAG, "End worker")
            Result.success()
        }

        Result.success()
    }

    private fun cleanupCollectors() {
        Log.d(TAG, "Clean up collectors")
        ecg?.stopCollect()
        acc?.stopCollect()
        hr?.stopCollect()
    }

    private fun collectData() {
        Log.d(TAG, "Starting to collect data")

        if (isActivated(DataType.ECG)) {
            ecg = CollectEcg(ecgHandler, polarConnection, createEcgSettings())
        }
        if (isActivated(DataType.ACC)) {
            acc = CollectAcc(accHandler, polarConnection, createAccSettings())
        }
        if (isActivated(DataType.HR)) {
            hr = CollectHr(hrHandler, polarConnection, createEcgSettings())
        }
    }

    private fun isActivated(dataType: DataType): Boolean {
        return inputData.getString(dataType.name).toBoolean()
    }

    fun createEcgSettings(): PolarSensorSetting {
        val ecgResolution =
            inputData.getString(DataSetting.ECG_RESOLUTION.name) ?: ListenableWorker.Result.failure()
        val ecgSampleRate =
            inputData.getString(DataSetting.ECG_SAMPLE_RATE.name) ?: ListenableWorker.Result.failure()
        val ecgRange =
            inputData.getString(DataSetting.ECG_RANGE.name) ?: ListenableWorker.Result.failure()

        val polarSettings = PolarSensorSetting(
            mapOf(
                Pair(PolarSensorSetting.SettingType.SAMPLE_RATE, (ecgSampleRate as String).toInt()),
                Pair(PolarSensorSetting.SettingType.RESOLUTION, (ecgResolution as String).toInt()),
                Pair(PolarSensorSetting.SettingType.RANGE, (ecgRange as String).toInt()),
            )
        )
        return polarSettings
    }

    fun createAccSettings(): PolarSensorSetting {

        val accResolution =
            inputData.getString(DataSetting.ACC_RESOLUTION.name) ?: ListenableWorker.Result.failure()
        val accSampleRate =
            inputData.getString(DataSetting.ACC_SAMPLE_RATE.name) ?: ListenableWorker.Result.failure()
        val accRange =
            inputData.getString(DataSetting.ACC_RANGE.name) ?: ListenableWorker.Result.failure()

        val polarSettings = PolarSensorSetting(
            mapOf(
                Pair(PolarSensorSetting.SettingType.SAMPLE_RATE, (accSampleRate as String).toInt()),
                Pair(PolarSensorSetting.SettingType.RESOLUTION, (accResolution as String).toInt()),
                Pair(PolarSensorSetting.SettingType.RANGE, (accRange as String).toInt()),
            )
        )
        return polarSettings
    }


}
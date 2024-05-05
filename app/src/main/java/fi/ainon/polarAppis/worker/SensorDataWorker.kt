package fi.ainon.polarAppis.worker

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.polar.sdk.api.model.PolarSensorSetting
import fi.ainon.polarAppis.communication.polar.PolarConnection
import fi.ainon.polarAppis.dataHandling.Notifier
import fi.ainon.polarAppis.dataHandling.dataObject.CollectionSetting
import fi.ainon.polarAppis.dataHandling.dataObject.DataType
import fi.ainon.polarAppis.dataHandling.dataObject.PolarDataSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SensorDataWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val notifier: Notifier,
    private val h10Connection: PolarConnection
) : CoroutineWorker(context, workerParams) {

    private val WAITING_FOR_CONNECTION_S = 30;
    private val TAG = "SensorWorker: "

    @SuppressLint("CheckResult")
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {

            notifier.postNotification(
                (inputData.getString(CollectionSetting.WORKER_START_TIME.name)
                    ?: "") + " " +tags.joinToString(", ")
            )

            Log.d(TAG, "Starting doWork in sensor worker")

            Log.d(TAG, "Worker triggering connect")
            h10Connection.connect(true)

            val collectionTime =
                inputData.getString(CollectionSetting.COLLECTION_TIME_IN_MIN.name)?.toLongOrNull() ?: 30

            val ecg = isActivated(DataType.ECG)
            val acc = isActivated(DataType.ACC)
            val hr = isActivated(DataType.HR)

            h10Connection.collectData(collectionTime, hr, ecg, acc, createEcgSettings(), createAccSettings())

            Log.d(TAG, "Timeout")

        } catch (ex: Exception) {
            Log.e(TAG, "Error when collecting sensor data", ex)
            h10Connection.cleanupCollectors()
            Result.failure()
        }
        // Also when user stops the worker, finally is run.
        finally {
            h10Connection.cleanupCollectors()
            Log.d(TAG, "End worker")
            h10Connection.connect(false)
            Result.success()
        }

        Result.success()
    }

    private fun isActivated(dataType: DataType): Boolean {
        return inputData.getString(dataType.name).toBoolean()
    }

    private fun createEcgSettings(): PolarSensorSetting {
        val ecgResolution =
            inputData.getString(PolarDataSetting.ECG_RESOLUTION.name) ?: ListenableWorker.Result.failure()
        val ecgSampleRate =
            inputData.getString(PolarDataSetting.ECG_SAMPLE_RATE.name) ?: ListenableWorker.Result.failure()
        val ecgRange =
            inputData.getString(PolarDataSetting.ECG_RANGE.name) ?: ListenableWorker.Result.failure()

        val polarSettings = PolarSensorSetting(
            mapOf(
                Pair(PolarSensorSetting.SettingType.SAMPLE_RATE, (ecgSampleRate as String).toInt()),
                Pair(PolarSensorSetting.SettingType.RESOLUTION, (ecgResolution as String).toInt()),
                Pair(PolarSensorSetting.SettingType.RANGE, (ecgRange as String).toInt()),
            )
        )
        return polarSettings
    }

    private fun createAccSettings(): PolarSensorSetting {

        val accResolution =
            inputData.getString(PolarDataSetting.ACC_RESOLUTION.name) ?: ListenableWorker.Result.failure()
        val accSampleRate =
            inputData.getString(PolarDataSetting.ACC_SAMPLE_RATE.name) ?: ListenableWorker.Result.failure()
        val accRange =
            inputData.getString(PolarDataSetting.ACC_RANGE.name) ?: ListenableWorker.Result.failure()

        val polarSettings = PolarSensorSetting(
            mapOf(
                Pair(PolarSensorSetting.SettingType.SAMPLE_RATE, (accSampleRate as String).toInt()),
                Pair(PolarSensorSetting.SettingType.RESOLUTION, (accResolution as String).toInt()),
                Pair(PolarSensorSetting.SettingType.RANGE, (accRange as String).toInt()),
            )
        )
        return polarSettings
    }

    fun createPpgSettings(): PolarSensorSetting {

        val ppgResolution =
            inputData.getString(PolarDataSetting.PPG_RESOLUTION.name) ?: ListenableWorker.Result.failure()
        val ppgSampleRate =
            inputData.getString(PolarDataSetting.PPG_SAMPLE_RATE.name) ?: ListenableWorker.Result.failure()
        val ppgChannels =
            inputData.getString(PolarDataSetting.PPG_CHANNELS.name) ?: ListenableWorker.Result.failure()

        val polarSettings = PolarSensorSetting(
            mapOf(
                Pair(PolarSensorSetting.SettingType.SAMPLE_RATE, (ppgSampleRate as String).toInt()),
                Pair(PolarSensorSetting.SettingType.RESOLUTION, (ppgResolution as String).toInt()),
                Pair(PolarSensorSetting.SettingType.CHANNELS, (ppgChannels as String).toInt()),
            )
        )
        return polarSettings
    }


}
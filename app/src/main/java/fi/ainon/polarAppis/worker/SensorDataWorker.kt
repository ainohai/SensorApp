package fi.ainon.polarAppis.worker

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.polar.sdk.api.model.PolarSensorSetting
import fi.ainon.polarAppis.communication.polar.PolarConnection
import fi.ainon.polarAppis.data.PolarDataRepository
import fi.ainon.polarAppis.dataHandling.Notifier
import fi.ainon.polarAppis.dataHandling.dataObject.CollectionSetting
import fi.ainon.polarAppis.dataHandling.dataObject.DataType
import fi.ainon.polarAppis.dataHandling.dataObject.PolarDataSetting
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
    private val hrHandler: HandleHr,
    private val notifier: Notifier,
    private val polarDataRepository: PolarDataRepository
) : CoroutineWorker(context, workerParams) {

    private val WAITING_FOR_CONNECTION_S = 30;
    private val TAG = "SensorWorker: "
    private var ecg : CollectEcg? = null;
    private var acc : CollectAcc? = null;
    private var hr : CollectHr? = null;



    @SuppressLint("CheckResult")
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {

            notifier.postNotification(
                (inputData.getString(CollectionSetting.WORKER_START_TIME.name)
                    ?: "") + " " +tags.joinToString(", ")
            )

            Log.d(TAG, "Starting doWork in sensor worker")

            Log.d(TAG, "Worker triggering connect")
            polarConnection.connect(true)

            val collectionTime =
                inputData.getString(CollectionSetting.COLLECTION_TIME_IN_MIN.name)?.toLongOrNull() ?: 30

            if (ecg?.isDisposed() != false && acc?.isDisposed() != false && hr?.isDisposed() != false) {

                withTimeoutOrNull(collectionTime * 1000) {
                    connectionHandler.dataFlow().collect { isConnected ->
                        Log.d(TAG, "Is connected: $isConnected")
                        if (isConnected) {
                            collectData()
                            Log.d(TAG, "Already connected, starting to collect data")
                        }
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
            polarConnection.connect(false)
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

        if (isActivated(DataType.ECG) && ecg == null) {
            ecg = CollectEcg(ecgHandler, polarConnection, createEcgSettings())
        }
        if (isActivated(DataType.ACC) && acc == null) {
            acc = CollectAcc(accHandler, polarConnection, createAccSettings())
        }
        if (isActivated(DataType.HR) && hr == null) {
            hr = CollectHr(hrHandler, polarConnection, createEcgSettings())
        }
    }

    private fun isActivated(dataType: DataType): Boolean {
        return inputData.getString(dataType.name).toBoolean()
    }

    fun createEcgSettings(): PolarSensorSetting {
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

    fun createAccSettings(): PolarSensorSetting {

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
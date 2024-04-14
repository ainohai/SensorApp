package fi.ainon.polarAppis.worker

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.polar.sdk.api.model.PolarSensorSetting
import fi.ainon.polarAppis.communication.polar.PolarConnection
import fi.ainon.polarAppis.dataHandling.DataHandler
import fi.ainon.polarAppis.worker.dataObject.ConnectionStatus
import fi.ainon.polarAppis.worker.dataObject.DataType
import fi.ainon.polarAppis.worker.sensorDataCollector.CollectAcc
import fi.ainon.polarAppis.worker.sensorDataCollector.CollectEcg
import fi.ainon.polarAppis.worker.sensorDataCollector.CollectHr
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Action
import io.reactivex.rxjava3.functions.Consumer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class SensorDataWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val polarConnection: PolarConnection,
    private val dataHandler: DataHandler
) : CoroutineWorker(context, workerParams) {

    private val TAG = "PolarConnection: "
    private var ecg : CollectEcg? = null;
    private var acc : CollectAcc? = null;
    private var hr : CollectHr? = null;

    //todo:
    private var onNext: Consumer<ConnectionStatus> = Consumer { value -> println("Got status: $value") }
    private var onError: Consumer<Throwable> = Consumer { error -> println("Got err: $error") }
    private var onComplete = Action { println("Complete") }
    private var connectionStatusDisposable: Disposable? = null


    @SuppressLint("CheckResult")
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {

            connectionStatusDisposable = polarConnection.subscribeConnectionStatus(onNext, onError, onComplete)

            val settings = createSettings()
            if (isActivated(DataType.ECG)) {
                ecg = CollectEcg(dataHandler, polarConnection, settings)
            }
            if (isActivated(DataType.ACC)) {
                acc = CollectAcc(dataHandler, polarConnection, settings)
            }
            if (isActivated(DataType.HR)) {
                hr = CollectHr(dataHandler, polarConnection, settings)
            }


            //TODO: Check neater ways.
            // Worker is kept alive until we are not interested in subscription.
            delay(60*1000)

        } catch (ex: Exception) {
            Log.e(TAG, "Error when collecting sensor data", ex)
            cleanup()
            Result.failure()
        }
        // Also when user stops the worker, finally is run.
        finally {
            cleanup()
            Log.e(TAG, "End worker")
            Result.success()
        }

        Result.success()
    }

    private fun cleanup() {
        ecg?.stopCollect()
        acc?.stopCollect()
        hr?.stopCollect()
        connectionStatusDisposable?.dispose()
    }

    private fun isActivated(dataType: DataType): Boolean {
        return inputData.getString(dataType.name).toBoolean()
    }

    fun createSettings(): PolarSensorSetting {
        val resolution =
            inputData.getString(PolarSensorSetting.SettingType.RESOLUTION.name) ?: ListenableWorker.Result.failure()
        val sampleRate =
            inputData.getString(PolarSensorSetting.SettingType.SAMPLE_RATE.name) ?: ListenableWorker.Result.failure()

        val polarSettings = PolarSensorSetting(
            mapOf(
                Pair(PolarSensorSetting.SettingType.SAMPLE_RATE, (sampleRate as String).toInt()),
                Pair(PolarSensorSetting.SettingType.RESOLUTION, (resolution as String).toInt()),
            )
        )
        return polarSettings
    }

}
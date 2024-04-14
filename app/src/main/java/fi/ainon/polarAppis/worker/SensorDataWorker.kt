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
import java.lang.Thread.sleep

class SensorDataWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val polarConnection: PolarConnection,
    private val dataHandler: DataHandler
) : CoroutineWorker(context, workerParams) {

    private val TAG = "Sensor worker: "
    private var ecg : CollectEcg? = null;
    private var acc : CollectAcc? = null;
    private var hr : CollectHr? = null;


    private var onNext: Consumer<ConnectionStatus> = Consumer { value -> reactConnectionChanges(value) }
    private var onError: Consumer<Throwable> = Consumer { error -> throw IllegalStateException("Error with ConnectionStatus", error) }
    private var onComplete = Action { Log.e(TAG, "Connection status listening completed") }
    private var connectionStatusDisposable: Disposable? = null


    @SuppressLint("CheckResult")
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {

            Log.d(TAG, "Starting doWork in sensor worker")
            if (!polarConnection.isConnected()) {
                Log.d(TAG, "Worker triggering connect")
                polarConnection.connect()
                connectionStatusDisposable =
                    polarConnection.subscribeConnectionStatus(onNext, onError, onComplete)
            }
            else {
                Log.d(TAG, "Already connected, starting to collect data")
                collectData()
            }

            //TODO: Parametrize + check neater ways.
            // Worker is kept alive until we are not interested in subscription.
            delay(60*1000)
            Log.d(TAG, "Timeout")

        } catch (ex: Exception) {
            Log.e(TAG, "Error when collecting sensor data", ex)
            cleanupCollectors()
            cleanupConnection()
            Result.failure()
        }
        // Also when user stops the worker, finally is run.
        finally {
            cleanupCollectors()
            cleanupConnection()
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

    private fun cleanupConnection() {
        Log.d(TAG, "Clean up connection")
        connectionStatusDisposable?.dispose()
    }

    private fun reactConnectionChanges(connectionStatus: ConnectionStatus) {
        Log.d(TAG, "Reacting to connection status change $connectionStatus")
        if (ConnectionStatus.CONNECTED == connectionStatus) {
            sleep(1000) //Ensure everything is setup
            collectData()
        }
        else if (ConnectionStatus.DISCONNECTED == connectionStatus) {
            cleanupCollectors()
        }
    }

    private fun collectData() {
        Log.d(TAG, "Starting to collect data")
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
    }

    private fun isActivated(dataType: DataType): Boolean {
        return inputData.getString(dataType.name).toBoolean()
    }

    fun createSettings(): PolarSensorSetting {
        val resolution =
            inputData.getString(PolarSensorSetting.SettingType.RESOLUTION.name) ?: ListenableWorker.Result.failure()
        val sampleRate =
            inputData.getString(PolarSensorSetting.SettingType.SAMPLE_RATE.name) ?: ListenableWorker.Result.failure()
        val range =
            inputData.getString(PolarSensorSetting.SettingType.RANGE.name) ?: ListenableWorker.Result.failure()


        val polarSettings = PolarSensorSetting(
            mapOf(
                Pair(PolarSensorSetting.SettingType.SAMPLE_RATE, (sampleRate as String).toInt()),
                Pair(PolarSensorSetting.SettingType.RESOLUTION, (resolution as String).toInt()),
                Pair(PolarSensorSetting.SettingType.RANGE, (range as String).toInt()),
            )
        )
        return polarSettings
    }

}
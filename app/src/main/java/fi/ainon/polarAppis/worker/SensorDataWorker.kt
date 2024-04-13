package fi.ainon.polarAppis.worker

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import fi.ainon.polarAppis.communication.polar.PolarConnection
import fi.ainon.polarAppis.worker.sensorDataCollector.CollectEcg
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


    @SuppressLint("CheckResult")
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {

            ecg = CollectEcg(dataHandler, polarConnection, inputData)
            ecg?.collectData();

            //TODO: Check neater ways. What if user wants to end prematurely.
            // Worker is kept alive until we are not interested in subscription.
            delay(60*1000)

        } catch (ex: Exception) {
            Log.e(TAG, "Error when collecting sensor data", ex)
            ecg?.stopCollect()
            Result.failure()
        }
        finally {
            ecg?.stopCollect()
            Log.e(TAG, "End worker")
            Result.success()
        }

        Result.success()
    }

}
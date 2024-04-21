package fi.ainon.polarAppis.worker

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import fi.ainon.polarAppis.communication.polar.PolarConnection
import fi.ainon.polarAppis.dataHandling.DataHandler
import fi.ainon.polarAppis.dataHandling.dataObject.ConnectionSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ConnectionWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val polarConnection: PolarConnection,
    private val dataHandler: DataHandler
) : CoroutineWorker(context, workerParams) {

    private val TAG = "ConnectionWorker: "

    @SuppressLint("CheckResult")
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {

           val shouldBeConnected =
                inputData.getBoolean(ConnectionSetting.SHOULD_BE_CONNECTED.name, false)

            Log.d(TAG, "Worker triggering connect: ${shouldBeConnected}")
            polarConnection.connect(shouldBeConnected)

        } catch (ex: Exception) {
            Log.e(TAG, "Error when trying to set connection", ex)
            Result.failure()
        }
        // Also when user stops the worker, finally is run.
        finally {
            Log.d(TAG, "End worker")
            Result.success()
        }

        Result.success()
    }
}
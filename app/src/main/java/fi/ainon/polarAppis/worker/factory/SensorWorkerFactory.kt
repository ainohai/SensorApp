package fi.ainon.polarAppis.worker.factory

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import fi.ainon.polarAppis.communication.polar.PolarConnection
import fi.ainon.polarAppis.dataHandling.DataHandler
import fi.ainon.polarAppis.worker.ConnectionWorker
import fi.ainon.polarAppis.worker.SensorDataWorker

class SensorWorkerFactory (
    private val context: Context,
    private val polarConnection: PolarConnection,
    private val dataHandler: DataHandler
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {

        return when(workerClassName) {
            SensorDataWorker::class.java.name ->
                SensorDataWorker(appContext, workerParameters, polarConnection, dataHandler)
            ConnectionWorker::class.java.name ->
                ConnectionWorker(appContext, workerParameters, polarConnection, dataHandler)
            else ->
                // Return null, so that the base class can delegate to the default WorkerFactory.
                null
        }

    }
}
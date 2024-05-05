package fi.ainon.polarAppis.worker.factory

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import fi.ainon.polarAppis.BuildConfig
import fi.ainon.polarAppis.communication.polar.DefaultPolarConnectionApi
import fi.ainon.polarAppis.communication.polar.PolarConnection
import fi.ainon.polarAppis.dataHandling.Notifier
import fi.ainon.polarAppis.worker.ConnectionWorker
import fi.ainon.polarAppis.worker.SensorDataWorker

class SensorWorkerFactory (
    private val context: Context,
    private val notifier: Notifier,
    private val h10Connection: PolarConnection
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {

        return when(workerClassName) {
            SensorDataWorker::class.java.name ->
                SensorDataWorker(appContext, workerParameters, notifier, h10Connection)
            ConnectionWorker::class.java.name ->
                ConnectionWorker(appContext, workerParameters, DefaultPolarConnectionApi(context, BuildConfig.POLAR_H10))
            else ->
                // Return null, so that the base class can delegate to the default WorkerFactory.
                null
        }

    }
}
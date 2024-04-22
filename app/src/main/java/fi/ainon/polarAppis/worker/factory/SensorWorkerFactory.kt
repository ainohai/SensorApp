package fi.ainon.polarAppis.worker.factory

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import fi.ainon.polarAppis.communication.polar.PolarConnection
import fi.ainon.polarAppis.dataHandling.handler.HandleAcc
import fi.ainon.polarAppis.dataHandling.handler.HandleConnection
import fi.ainon.polarAppis.dataHandling.handler.HandleEcg
import fi.ainon.polarAppis.dataHandling.handler.HandleHr
import fi.ainon.polarAppis.worker.ConnectionWorker
import fi.ainon.polarAppis.worker.SensorDataWorker

class SensorWorkerFactory (
    private val context: Context,
    private val polarConnection: PolarConnection,
    private val connectionHandler: HandleConnection,
    private val accHandler: HandleAcc,
    private val ecgHandler: HandleEcg,
    private val hrHandler: HandleHr
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {

        return when(workerClassName) {
            SensorDataWorker::class.java.name ->
                SensorDataWorker(appContext, workerParameters, polarConnection, connectionHandler, accHandler, ecgHandler, hrHandler)
            ConnectionWorker::class.java.name ->
                ConnectionWorker(appContext, workerParameters, polarConnection)
            else ->
                // Return null, so that the base class can delegate to the default WorkerFactory.
                null
        }

    }
}
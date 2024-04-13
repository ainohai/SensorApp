package fi.ainon.polarAppis.worker

import android.content.Context
import androidx.work.DelegatingWorkerFactory
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.ainon.polarAppis.communication.polar.PolarConnection
import fi.ainon.polarAppis.worker.SensorDataWorker
import javax.inject.Inject
import javax.inject.Singleton


/*@Singleton
class AppisWorkerFactory @Inject constructor(
    polarConnection: PolarConnection
) : DelegatingWorkerFactory() {
    init {
        addFactory(SensorWorkerFactory(polarConnection))
        // Add here other factories that you may need in your application
    }
}*/

class SensorWorkerFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val polarConnection: PolarConnection,
    private val dataHandler: DataHandler) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {

        return when(workerClassName) {
            SensorDataWorker::class.java.name ->
                SensorDataWorker(appContext, workerParameters, polarConnection, dataHandler)
            else ->
                // Return null, so that the base class can delegate to the default WorkerFactory.
                null
        }

    }
}
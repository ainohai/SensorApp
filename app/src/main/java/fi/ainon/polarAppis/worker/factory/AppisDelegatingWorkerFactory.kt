package fi.ainon.polarAppis.worker.factory

import android.content.Context
import androidx.work.DelegatingWorkerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.ainon.polarAppis.communication.polar.PolarConnection
import fi.ainon.polarAppis.dataHandling.DataHandler
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppisDelegatingWorkerFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    polarConnection: PolarConnection,
    dataHandler: DataHandler
) : DelegatingWorkerFactory() {
    init {
        addFactory(SensorWorkerFactory(context, polarConnection, dataHandler))
        // Add here other factories that you may need in your application
    }
}
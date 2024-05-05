package fi.ainon.polarAppis.worker.factory

import android.content.Context
import androidx.work.DelegatingWorkerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.ainon.polarAppis.communication.polar.PolarConnection
import fi.ainon.polarAppis.dataHandling.SystemTrayNotifier
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppisDelegatingWorkerFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notifier: SystemTrayNotifier,
    h10Connection: PolarConnection
) : DelegatingWorkerFactory() {
    init {
        addFactory(SensorWorkerFactory(context,notifier, h10Connection))
        // Add here other factories that you may need in your application
    }
}
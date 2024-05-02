package fi.ainon.polarAppis.worker.factory

import android.content.Context
import androidx.work.DelegatingWorkerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.ainon.polarAppis.communication.polar.PolarConnection
import fi.ainon.polarAppis.dataHandling.SystemTrayNotifier
import fi.ainon.polarAppis.dataHandling.handler.HandleAcc
import fi.ainon.polarAppis.dataHandling.handler.HandleConnection
import fi.ainon.polarAppis.dataHandling.handler.HandleEcg
import fi.ainon.polarAppis.dataHandling.handler.HandleHr
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppisDelegatingWorkerFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    polarConnection: PolarConnection,
    connectionHandler: HandleConnection,
    accHandler: HandleAcc,
    ecgHandler: HandleEcg,
    hrHandler: HandleHr,
    private val notifier: SystemTrayNotifier
) : DelegatingWorkerFactory() {
    init {
        addFactory(SensorWorkerFactory(context, polarConnection, connectionHandler, accHandler, ecgHandler, hrHandler, notifier))
        // Add here other factories that you may need in your application
    }
}
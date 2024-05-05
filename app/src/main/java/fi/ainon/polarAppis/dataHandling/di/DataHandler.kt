package fi.ainon.polarAppis.dataHandling.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fi.ainon.polarAppis.dataHandling.DefaultScheduleAlarm
import fi.ainon.polarAppis.dataHandling.ScheduleAlarm
import fi.ainon.polarAppis.dataHandling.SensorWorkerInitializer
import fi.ainon.polarAppis.dataHandling.WorkerInitializer
import fi.ainon.polarAppis.dataHandling.dataObject.AccData
import fi.ainon.polarAppis.dataHandling.dataObject.ConnectionStatus
import fi.ainon.polarAppis.dataHandling.dataObject.EcgData
import fi.ainon.polarAppis.dataHandling.dataObject.HrData
import fi.ainon.polarAppis.dataHandling.handler.HandleAcc
import fi.ainon.polarAppis.dataHandling.handler.HandleEcg
import fi.ainon.polarAppis.dataHandling.handler.HandleH10Connection
import fi.ainon.polarAppis.dataHandling.handler.HandleH10Hr
import fi.ainon.polarAppis.dataHandling.handler.HandleH10Rrs
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
interface DataHandlerModule {
    @Binds
    fun bindAccHandler(defaultDataHandler: HandleAcc): DataHandler<AccData, AccData>
    @Binds
    fun bindEcgHandler(defaultDataHandler: HandleEcg): DataHandler<EcgData, Int>
    @Binds
    fun bindHrHandler(defaultDataHandler: HandleH10Hr): DataHandler<HrData, HrData.HrSample>
    @Binds
    fun bindConnectionHandler(defaultDataHandler: HandleH10Connection): DataHandler<ConnectionStatus, Boolean>

    @Binds
    fun bindH10RrsHandler(defaultDataHandler: HandleH10Rrs): DataHandler<HrData, Double>

    @Binds
    @Singleton
    fun bindWorkerInitializer(sensorWorkerInitializer: SensorWorkerInitializer): WorkerInitializer
    @Binds
    fun bindScheduleAlarm(scheduleAlarm: DefaultScheduleAlarm) : ScheduleAlarm
}

/**
 *
 * Data handler formats the data and passes it for server api calls and offers data flow to those who are interested in listening.
 * All collected data types have their own handlers.
 *
 */
interface DataHandler<T,S> {
    fun handle(data: T)
    fun dataFlow() :SharedFlow<S>

}

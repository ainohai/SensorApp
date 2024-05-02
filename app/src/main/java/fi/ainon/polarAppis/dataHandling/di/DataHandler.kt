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
import fi.ainon.polarAppis.dataHandling.handler.HandleConnection
import fi.ainon.polarAppis.dataHandling.handler.HandleEcg
import fi.ainon.polarAppis.dataHandling.handler.HandleHr
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
    fun bindHrHandler(defaultDataHandler: HandleHr): DataHandler<HrData, HrData.HrSample>
    @Binds
    fun bindConnectionHandler(defaultDataHandler: HandleConnection): DataHandler<ConnectionStatus, Boolean>
    @Binds
    @Singleton
    fun bindWorkerInitializer(sensorWorkerInitializer: SensorWorkerInitializer): WorkerInitializer
    @Binds
    fun bindScheduleAlarm(scheduleAlarm: DefaultScheduleAlarm) : ScheduleAlarm
}

interface DataHandler<T,S> {
    fun handle(data: T)
    fun dataFlow() :SharedFlow<S>

}

package fi.ainon.polarAppis.dataHandling.handler

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fi.ainon.polarAppis.dataHandling.dataObject.AccData
import fi.ainon.polarAppis.dataHandling.dataObject.ConnectionStatus
import fi.ainon.polarAppis.dataHandling.dataObject.EcgData
import fi.ainon.polarAppis.dataHandling.dataObject.HrData
import kotlinx.coroutines.flow.SharedFlow


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
}

interface DataHandler<T,S> {
    fun handle(data: T)
    fun dataFlow() :SharedFlow<S>

}

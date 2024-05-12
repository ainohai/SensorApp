package fi.ainon.polarAppis.data

import fi.ainon.polarAppis.communication.polar.PolarConnection
import fi.ainon.polarAppis.dataHandling.dataObject.EcgData
import fi.ainon.polarAppis.dataHandling.dataObject.HrData
import fi.ainon.polarAppis.dataHandling.handler.HandleEcg
import fi.ainon.polarAppis.dataHandling.handler.HandleH10Hr
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * For passing data which should not be saved into database.
 */
interface LiveDataRepository {

    fun getHr() : Flow<HrData.HrSample>

    fun getEcg(): Flow<EcgData.EcgDataSample>
}

class DefaultLiveDataRepository @Inject constructor(
    private val handleH10Hr: HandleH10Hr,
    private val handleEcg: HandleEcg,
    private val polarConnection: PolarConnection

    ) : LiveDataRepository {

    private val TAG = "LiveDataRepository: "
    private val hrFlow: Flow<HrData.HrSample>
    private val ecgFlow: Flow<EcgData.EcgDataSample>


    init {
        polarConnection.cleanupCollectors() //Todo: ensures everything is set up.
        hrFlow = handleH10Hr.dataFlow()
        ecgFlow = handleEcg.dataFlow()
    }

    override fun getHr(): Flow<HrData.HrSample> {
        return hrFlow;
    }

    override fun getEcg(): Flow<EcgData.EcgDataSample> {
        return ecgFlow
    }


}

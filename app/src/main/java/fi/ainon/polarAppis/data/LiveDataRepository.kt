package fi.ainon.polarAppis.data

import fi.ainon.polarAppis.dataHandling.dataObject.HrData
import fi.ainon.polarAppis.dataHandling.handler.HandleEcg
import fi.ainon.polarAppis.dataHandling.handler.HandleHr
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * For passing data which should not be saved into database.
 */
interface LiveDataRepository {

    fun getHr() : Flow<HrData.HrSample>
    fun getEcg() : Flow<Pair<Long, Int>>

}

class DefaultLiveDataRepository @Inject constructor(
    private val handleHr: HandleHr,
    private val handleEcg: HandleEcg
) : LiveDataRepository {

    private val TAG = "LiveDataRepository: "
    private val hrFlow: Flow<HrData.HrSample>
    private val ecgFlow: Flow<Pair<Long, Int>>

    init {
        hrFlow = handleHr.dataFlow()
        ecgFlow = handleEcg.ecgFlow()
    }

    override fun getHr(): Flow<HrData.HrSample> {
        return hrFlow;
    }

    override fun getEcg(): Flow<Pair<Long, Int>> {
        return ecgFlow
    }

}

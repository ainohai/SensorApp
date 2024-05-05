package fi.ainon.polarAppis.data

import fi.ainon.polarAppis.dataHandling.dataObject.HrData
import fi.ainon.polarAppis.dataHandling.handler.HandleH10Hr
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * For passing data which should not be saved into database.
 */
interface LiveDataRepository {

    fun getHr() : Flow<HrData.HrSample>

}

class DefaultLiveDataRepository @Inject constructor(
    private val handleH10Hr: HandleH10Hr,

    ) : LiveDataRepository {

    private val TAG = "LiveDataRepository: "
    private val hrFlow: Flow<HrData.HrSample>


    init {
        hrFlow = handleH10Hr.dataFlow()
    }

    override fun getHr(): Flow<HrData.HrSample> {
        return hrFlow;
    }


}

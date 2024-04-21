package fi.ainon.polarAppis.data

import fi.ainon.polarAppis.dataHandling.DataHandler
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * For passing data which should not be saved into database.
 */
interface LiveDataRepository {

    fun getHr() : Flow<Int>
    fun getEcg() : Flow<List<Pair<Long, Int>>>

}

class DefaultLiveDataRepository @Inject constructor(
    private val dataHandler: DataHandler
) : LiveDataRepository {

    private val TAG = "LiveDataRepository: "
    private val hrFlow: Flow<Int>
    private val ecgFlow: Flow<List<Pair<Long, Int>>>

    init {
        hrFlow = dataHandler.hrFlow()
        ecgFlow = dataHandler.ecgFlow()
    }

    override fun getHr(): Flow<Int> {
        return hrFlow;
    }

    override fun getEcg(): Flow<List<Pair<Long, Int>>> {
        return ecgFlow
    }

}

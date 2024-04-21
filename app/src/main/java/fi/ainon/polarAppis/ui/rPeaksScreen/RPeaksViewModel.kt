package fi.ainon.polarAppis.ui.rPeaksScreen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.ainon.polarAppis.data.LiveDataRepository
import fi.ainon.polarAppis.data.PolarDataRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RPeaksViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val liveDataRepository: LiveDataRepository,
    private val polarDataRepository: PolarDataRepository
) : ViewModel() {

    val TAG = "RPeaksViewModel: "
    private val MAX_DATAPOINTS = 50000
    private val UPDATE_BUFFER = 20

    private var ecgValues = mutableListOf<Int>()
    private val modelProducer = CartesianChartModelProducer.build()
    private var ecgUpdateBuffer = 0
    val uiState: StateFlow<RPeaksUiState> = combine(
        liveDataRepository
            .getEcg(), polarDataRepository.connection
    ) { value, connected ->

        //Todo: Cutting corners. Can be connected when is not collecting. No way here currently to know if collecting, except timing.
        if (connected) {
            val ecg = value.map{sample -> sample.second}
            ecgValues.addAll(ecg.toMutableList())
            if (ecgValues.size > MAX_DATAPOINTS) {
                ecgValues = ecgValues.slice(MAX_DATAPOINTS - ecgValues.lastIndex .. ecgValues.lastIndex).toMutableList()
            }
            ecgUpdateBuffer += 1

            if (ecgUpdateBuffer % UPDATE_BUFFER == 0) {
                modelProducer.tryRunTransaction {
                    lineSeries {
                        series(ecgValues)
                    }
                }
                ecgUpdateBuffer = 0
            }

            RPeaksUiState.CollectingData(ecgValues.size)
        } else {
            RPeaksUiState.ShowOldData
        }
    }
        .catch { emit(RPeaksUiState.Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RPeaksUiState.NoData)


    fun getModelProducer(): CartesianChartModelProducer {
        return modelProducer
    }

}

sealed interface RPeaksUiState {
    object NoData : RPeaksUiState
    object ShowOldData : RPeaksUiState
    data class Error(val throwable: Throwable) : RPeaksUiState
    data class CollectingData(val hrValue: Int) : RPeaksUiState
}

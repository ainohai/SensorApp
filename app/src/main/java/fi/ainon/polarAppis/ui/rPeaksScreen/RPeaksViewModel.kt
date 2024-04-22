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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// Needs refactoring
@HiltViewModel
class RPeaksViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val liveDataRepository: LiveDataRepository,
    private val polarDataRepository: PolarDataRepository
) : ViewModel() {

    val TAG = "RPeaksViewModel: "
    private val MAX_DATAPOINTS = 10000 // Todo: Downsampling
    private val UPDATE_BUFFER = 20

    private val showChart: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private var ecgValues = mutableListOf<Int>()
    private val modelProducer = CartesianChartModelProducer.build()
    private var ecgUpdateBuffer = 0

    val uiState: StateFlow<RPeaksUiState> = combine(
        liveDataRepository
            .getEcg(), polarDataRepository.connection,
        showChart
    ) { value, connected, expanded ->

        handleData(connected, value, expanded)

        if (!expanded) {
            RPeaksUiState.NoGraph
        } else if (connected) {
            RPeaksUiState.ShowCollectedData(ecgValues.size)
        } else {
            RPeaksUiState.ShowOldData
        }
    }
        .catch { emit(RPeaksUiState.Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RPeaksUiState.NoGraph)

    private fun handleData(
        connected: Boolean,
        value: Pair<Long, Int>,
        expanded: Boolean
    ) {
        //Todo: Cutting corners. Can be connected when is not collecting. No way here currently to know if collecting, except timing.
        if (connected) {
            viewModelScope.launch(Dispatchers.IO) {
                val ecg = value.second
                ecgValues.add(ecg)
                if (ecgValues.size > MAX_DATAPOINTS) {
                    ecgValues =
                        ecgValues.slice(MAX_DATAPOINTS - ecgValues.lastIndex..ecgValues.lastIndex)
                            .toMutableList()
                }
                ecgUpdateBuffer += 1

                if (ecgUpdateBuffer % UPDATE_BUFFER == 0 && expanded) {
                    updateGraph()
                    ecgUpdateBuffer = 0
                }
            }
        }
    }

    private fun updateGraph() {
        modelProducer.tryRunTransaction {
            lineSeries {
                series(ecgValues)
            }
        }
    }


    fun toggleShowChart(isExpanded: Boolean) {
        showChart.value = isExpanded
        if (isExpanded) {
            updateGraph()
        }
    }

    fun getModelProducer(): CartesianChartModelProducer {
        return modelProducer
    }

}

sealed interface RPeaksUiState {
    object NoGraph : RPeaksUiState
    object ShowOldData : RPeaksUiState
    data class Error(val throwable: Throwable) : RPeaksUiState
    data class ShowCollectedData(val hrValue: Int) : RPeaksUiState
}

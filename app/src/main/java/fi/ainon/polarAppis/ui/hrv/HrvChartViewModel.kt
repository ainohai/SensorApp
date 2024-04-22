package fi.ainon.polarAppis.ui.hrv

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.ainon.polarAppis.data.PolarDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

//TODO: REFACTOR UI
@HiltViewModel
class HrvChartViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val polarDataRepository: PolarDataRepository,
) : ViewModel() {

    val TAG = "HrvChartViewModel: "
    private val showChart: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private var hrvValues = mutableListOf<Double>()
    private var hrvTimepoint = mutableListOf<Long>()
    private val modelProducer = CartesianChartModelProducer.build()
    val uiState: StateFlow<HrvChartUiState> = combine(
        polarDataRepository.RRMSSD, showChart
    ) { values, showChart ->

        hrvValues = values.map { value -> value.hr1 }.toMutableList()
        hrvTimepoint = values.map { value -> value.timepoint / 1000 }.toMutableList()

        updateGraph()
        if (showChart) {
            HrvChartUiState.ShowData(hrvValues.last())
        } else {
            HrvChartUiState.NoGraph
        }
    }
        .catch { emit(HrvChartUiState.Error(it)) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            HrvChartUiState.ShowData(0.0)
        )

    private fun updateGraph() {
        modelProducer.tryRunTransaction {
            lineSeries {
                series(hrvValues)
            }
        }
    }


    fun getModelProducer(): CartesianChartModelProducer {
        return modelProducer
    }

    fun toggleShowChart(isExpanded: Boolean) {
        showChart.value = isExpanded
        if (isExpanded) {
            updateGraph()
        }
    }

}

sealed interface HrvChartUiState {
    data class Error(val throwable: Throwable) : HrvChartUiState
    data class ShowData(val hrvValue: Double) : HrvChartUiState
    object NoGraph : HrvChartUiState
}

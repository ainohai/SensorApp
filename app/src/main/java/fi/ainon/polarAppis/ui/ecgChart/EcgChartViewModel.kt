package fi.ainon.polarAppis.ui.ecgChart

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.ainon.polarAppis.communication.polar.PolarConnection
import fi.ainon.polarAppis.data.LiveDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

//TODO: REFACTOR UI
@HiltViewModel
class EcgChartViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val liveDataRepository: LiveDataRepository,
    private val polarConnection: PolarConnection
) : ViewModel() {

    val TAG = "EcgChartViewModel: "
    private val showChart: MutableStateFlow<Boolean> = MutableStateFlow(true)
    private var ecgValues = mutableListOf<Int>()
    private var ecgTimepoint = mutableListOf<Long>()
    private val modelProducer = CartesianChartModelProducer.build()
    val uiState: StateFlow<EcgChartUiState> = combine(
        liveDataRepository.getEcg(), showChart
    ) { values, showChart ->

        if (showChart) {
            ecgValues.add(values.voltage)
            ecgTimepoint.add(values.timeStamp)

            updateGraph()

            if (ecgValues.isNotEmpty()) EcgChartUiState.ShowData(ecgValues.last()) else EcgChartUiState.NoGraph
        } else {
            EcgChartUiState.NoGraph
        }
    }
        .catch { emit(EcgChartUiState.Error(it)) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            EcgChartUiState.ShowData(0)
        )

    private fun updateGraph() {
        modelProducer.tryRunTransaction {
            lineSeries {
                series(ecgValues)
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

sealed interface EcgChartUiState {
    data class Error(val throwable: Throwable) : EcgChartUiState
    data class ShowData(val ecgValue: Int) : EcgChartUiState
    object NoGraph : EcgChartUiState
}

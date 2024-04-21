package fi.ainon.polarAppis.ui.hrChart

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.ainon.polarAppis.data.LiveDataRepository
import fi.ainon.polarAppis.data.PolarDataRepository
import fi.ainon.polarAppis.ui.hrChart.HrChartUiState.CollectingData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HrChartViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val liveDataRepository: LiveDataRepository,
    private val polarDataRepository: PolarDataRepository
) : ViewModel() {

    val TAG = "HrChartViewModel: "
    private val hrValues = mutableListOf<Int>()
    private val modelProducer = CartesianChartModelProducer.build()
    private var hrUpdateBuffer = 0
    val uiState: StateFlow<HrChartUiState> = combine(
        liveDataRepository
            .getHr(), polarDataRepository.connection
    ) { value, connected ->

        //Todo: Cutting corners. Can be connected when is not collecting. No way here currently to know if collecting, except timing.
        if (connected) {
            hrValues.add(value)
            if (hrValues.size > 50) {
                hrValues.remove(0)
            }
            hrUpdateBuffer += 1

            if (hrUpdateBuffer % 10 == 0) {
                modelProducer.tryRunTransaction {
                    lineSeries {
                        series(hrValues)
                    }
                }
                hrUpdateBuffer = 0
            }

            CollectingData(value)
        } else {
            HrChartUiState.ShowOldData
        }
    }
        .catch { emit(HrChartUiState.Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HrChartUiState.NoData)


    fun getModelProducer(): CartesianChartModelProducer {
        return modelProducer
    }

}

sealed interface HrChartUiState {
    object NoData : HrChartUiState
    object ShowOldData : HrChartUiState
    data class Error(val throwable: Throwable) : HrChartUiState
    data class CollectingData(val hrValue: Int) : HrChartUiState
}

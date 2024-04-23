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
import fi.ainon.polarAppis.dataHandling.dataObject.HrData
import fi.ainon.polarAppis.ui.hrChart.HrChartUiState.CollectingData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class HrChartViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val liveDataRepository: LiveDataRepository,
    private val polarDataRepository: PolarDataRepository
) : ViewModel() {

    private val MAX_DATASET_SIZE = 1000
    private val TAG = "HrChartViewModel: "
    private var rrms = mutableListOf<Double>()
    private var hr = mutableListOf<Int>()
    private var tickX = mutableListOf<Double>()
    private var tickY = mutableListOf<Int>()
    private var rollingRRs = 0.0
    private val modelProducer = CartesianChartModelProducer.build()
    private val tickModelProducer = CartesianChartModelProducer.build()
    private var hrUpdateBuffer = 0
    private var lastrrsMs = Double.MAX_VALUE
    val uiState: StateFlow<HrChartUiState> = combine(
        liveDataRepository
            .getHr(), polarDataRepository.connection
    ) { value, connected ->

        //Todo: Cutting corners. Can be connected when is not collecting. No way here currently to know if collecting, except timing.
        if (connected) {
            viewModelScope.launch(Dispatchers.IO) {
                handlerrsMs(value)
                handleHr(value)
                hrUpdateBuffer += 1

                if (hrUpdateBuffer % 10 == 0) {
                    modelProducer.tryRunTransaction {
                        lineSeries {
                            series(hr)
                            series(rrms)
                        }
                    }
                    tickModelProducer.tryRunTransaction {
                        lineSeries {
                            series(tickX)
                        }
                    }
                    hrUpdateBuffer = 0
                }
            }

            val lastrrdms = if (rrms.isNotEmpty()) rrms.last() else -1.0
            CollectingData(Pair(value.hr, lastrrdms))
        } else {
            HrChartUiState.ShowOldData
        }
    }
        .catch { emit(HrChartUiState.Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HrChartUiState.NoData)

    //Todo: only for a test
    private fun handlerrsMs(value: HrData.HrSample) {
        if (value.rrsMs.isNotEmpty()) {
            val mean = value.rrsMs.sum() / value.rrsMs.size.toDouble()
            rollingRRs = if(tickX.size % 2 == 0) (mean / 1000) else (-1* mean / 1000)
            tickX.add(rollingRRs)
            tickY.add(value.hr)

            if (lastrrsMs != Double.MAX_VALUE) {
                rrms.add(abs(lastrrsMs - mean))
            }
            lastrrsMs = mean

            if (rrms.size > MAX_DATASET_SIZE) {
                rrms =
                    rrms.slice(rrms.lastIndex - MAX_DATASET_SIZE..rrms.lastIndex).toMutableList()

                tickX =
                    tickX.slice(tickX.lastIndex - MAX_DATASET_SIZE..tickX.lastIndex).toMutableList()
                tickY =
                    tickY.slice(tickY.lastIndex - MAX_DATASET_SIZE..tickY.lastIndex).toMutableList()
            }
        }
    }

    private fun handleHr(value: HrData.HrSample) {
        hr.add(value.hr)

        if (rrms.size > MAX_DATASET_SIZE) {
            hr = hr.slice(hr.lastIndex - MAX_DATASET_SIZE..hr.lastIndex).toMutableList()
        }

    }


    fun getModelProducer(): CartesianChartModelProducer {
        return modelProducer
    }

    fun getTickModelProducer(): CartesianChartModelProducer {
        return tickModelProducer
    }

}

sealed interface HrChartUiState {
    object NoData : HrChartUiState
    object ShowOldData : HrChartUiState
    data class Error(val throwable: Throwable) : HrChartUiState
    data class CollectingData(val hrValue: Pair<Int, Double>) : HrChartUiState
}

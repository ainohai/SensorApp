package fi.ainon.polarAppis.ui.hrChart

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.core.cartesian.AutoScrollCondition
import com.patrykandpatrick.vico.core.cartesian.Scroll
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries


@Composable
fun HrChartScreen(modifier: Modifier = Modifier, viewModel: HrChartViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    //if (state is HrChartUiState.CollectingData || state is HrChartUiState.ShowOldData) {
        HrChartScreen(
            modifier = modifier,
            state = state,
            modelProducer = viewModel::getModelProducer
        )
    //}
}

@Composable
internal fun HrChartScreen(
    modifier: Modifier = Modifier,
    state: HrChartUiState,
    modelProducer: () -> CartesianChartModelProducer
) {
    val hrValue = when (state) {
        is HrChartUiState.CollectingData -> state.hrValue
        else -> {
            0
        }
    }

    Column(modifier.verticalScroll(rememberScrollState())) {
        Row {

            LaunchedEffect(Unit) { modelProducer().tryRunTransaction { lineSeries {
                series(listOf(hrValue)) } } }
            CartesianChartHost(
                rememberCartesianChart(
                    rememberLineCartesianLayer(),
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(),
                ),
                modelProducer(),
                scrollState = rememberVicoScrollState(
                    initialScroll = Scroll.Absolute.End,
                    autoScrollCondition= AutoScrollCondition {
                            newModel,
                            oldModel ->  oldModel != null && oldModel.models.contains(newModel.models.elementAt(newModel.models.size -1))
                        }
                )
            )
        }
        if (state is HrChartUiState.CollectingData) {
            Row() {
                Text("Current hr: $hrValue")
            }
        }

    }
}


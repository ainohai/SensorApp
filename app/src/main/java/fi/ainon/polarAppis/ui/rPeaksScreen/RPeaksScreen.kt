package fi.ainon.polarAppis.ui.rPeaksScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
fun RPeaksScreen(modifier: Modifier = Modifier, viewModel: RPeaksViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    //if (state is RPeaksUiState.CollectingData || state is RPeaksUiState.ShowOldData) {
        RPeaksScreen(
            modifier = modifier,
            state = state,
            modelProducer = viewModel::getModelProducer,
            toggleShowChart = viewModel::toggleShowChart
        )
    //}
}

@Composable
internal fun RPeaksScreen(
    modifier: Modifier = Modifier,
    state: RPeaksUiState,
    modelProducer: () -> CartesianChartModelProducer,
    toggleShowChart: (Boolean) -> Unit
) {
    var expanded by remember{mutableStateOf(false)}
    val ecgValue = when (state) {
        is RPeaksUiState.ShowCollectedData -> state.hrValue
        else -> {
            0
        }
    }

    Column() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(modifier = Modifier.width(196.dp), onClick = { expanded = !expanded; toggleShowChart(expanded) }) {
                Text("Show ecg")
            }
        }
    }

    if(state is RPeaksUiState.ShowCollectedData || state is RPeaksUiState.ShowOldData) {

    Column(modifier.verticalScroll(rememberScrollState())) {
        Row {

            LaunchedEffect(Unit) {
                modelProducer().tryRunTransaction {
                    lineSeries {
                        series(listOf(ecgValue))
                    }
                }
            }
            CartesianChartHost(
                rememberCartesianChart(
                    rememberLineCartesianLayer(),
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(),
                ),
                modelProducer(),
                scrollState = rememberVicoScrollState(
                    initialScroll = Scroll.Absolute.End,
                    autoScrollCondition = AutoScrollCondition { newModel,
                                                                oldModel ->
                        oldModel != null && oldModel.models.contains(
                            newModel.models.elementAt(
                                newModel.models.size - 1
                            )
                        )
                    }
                )
            )
        }
        if (state is RPeaksUiState.ShowCollectedData) {
            Row() {
                Text("Num of ecgValues: $ecgValue")
            }
        }
    }

    }
}


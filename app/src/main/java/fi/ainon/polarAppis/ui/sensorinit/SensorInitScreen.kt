package fi.ainon.polarAppis.ui.sensorinit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle


@Composable
fun SensorInitScreen(modifier: Modifier = Modifier, viewModel: DataItemTypeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    //if (state is SensorInitUiState.Success) {
        SensorInitScreen(
            connect = viewModel::connect,
            collect = viewModel::h10Setup,
            periodic = viewModel::periodic,
            modifier = modifier,
            state = state,
            periodInterval = viewModel.intervalInMin,
            durationTime = viewModel.collectionTimeInMin,
            cancel = viewModel::cancelPeriodic
            )
    //}
}

@Composable
internal fun SensorInitScreen(
    connect: () -> Unit,
    collect: () -> Unit,
    periodic: () -> Unit,
    modifier: Modifier = Modifier,
    state: SensorInitUiState,
    periodInterval: Int,
    durationTime: Long,
    cancel: () -> Unit,
    ) {
    val connected = when (state) {
        SensorInitUiState.Loading -> 0
        is SensorInitUiState.Success -> state.connected
        else -> {false}
    }

    Column() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(modifier = Modifier.width(196.dp), onClick = { connect() }) {
                Text("Connect")
            }
            Text("Interval  $periodInterval")
            Text("Time $durationTime")
        }

        /*Row() {
            TextField(
                value = durationTime,
                onValueChange = { it: String -> durationTime = (if(it.isNotEmpty() && it.isDigitsOnly()) it.toLong() else 0) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                label = { Text("Duration: ") }
            )
            TextField(
                value = periodInterval,
                onValueChange = { it: String -> setPeriodInterval(if(it.isNotEmpty() && it.isDigitsOnly()) it.toLong() else 0) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                label = { Text("Total interval: ") }
            )
        }*/
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(modifier = Modifier.width(90.dp), onClick = { periodic() }) {
                Text("Periodic")
            }
            Button(modifier = Modifier.width(90.dp), onClick = { collect() }) {
                Text("Single")
            }
            Button(modifier = Modifier.width(90.dp), onClick = { cancel() }) {
                Text("Cancel periodic")
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Connected: ")
            Text(connected.toString())
        }
    }
}


// Previews
/*
@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    MyApplicationTheme {
        SensorInitScreen(
            pingMe =  {false},
            connect = {},
            collect = {},
            state = SensorInitUiState.Success(
                connected = true,
            ),
        )
    }
}




@Preview(showBackground = true, widthDp = 480)
@Composable
private fun PortraitPreview() {
    MyApplicationTheme {
        SensorInitScreen(
            pingMe = {false},
            connect = {},
            collect = {},
            state = SensorInitUiState.Success(
                connected = false,
            ),
        )
    }
}*/


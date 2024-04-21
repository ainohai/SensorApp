package fi.ainon.polarAppis.ui.sensorinit

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
            modifier = modifier,
            state = state
        )
    //}
}

@Composable
internal fun SensorInitScreen(
    connect: () -> Unit,
    collect: () -> Unit,
    modifier: Modifier = Modifier,
    state: SensorInitUiState,
) {
    val connected = when (state) {
        SensorInitUiState.Loading -> 0
        is SensorInitUiState.Success -> state.connected
        else -> {false}
    }

    Column(modifier.verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(modifier = Modifier.width(196.dp), onClick = { connect() }) {
                Text("Connect")
            }
        }
        /*TextField(
            value = number,
            onValueChange = { number = it },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            label = Text("Collection time: ")
        )*/
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(modifier = Modifier.width(196.dp), onClick = { collect() }) {
                Text("Collect")
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


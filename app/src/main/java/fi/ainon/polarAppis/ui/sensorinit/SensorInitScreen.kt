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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fi.ainon.polarAppis.ui.theme.MyApplicationTheme


@Composable
fun SensorInitScreen(modifier: Modifier = Modifier, viewModel: DataItemTypeViewModel = hiltViewModel()) {
    val items by viewModel.uiState.collectAsStateWithLifecycle()
    if (items is SensorInitUiState.Success) {
        SensorInitScreen(
            pingMe = viewModel::pingMe,
            connect = viewModel::connect,
            collect = viewModel::h10Setup,
            modifier = modifier
        )
    }
}

@Composable
internal fun SensorInitScreen(

    pingMe: () -> Boolean,
    connect: () -> Unit,
    collect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.verticalScroll(rememberScrollState())) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(modifier = Modifier.width(96.dp), onClick = { pingMe() }) {
                Text("Ping")
            }
        }
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
    }
}


// Previews

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    MyApplicationTheme {
        SensorInitScreen(
            pingMe =  {false},
            connect = {},
            collect = {},
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
        )
    }
}

package fi.ainon.polarAppis.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import fi.ainon.polarAppis.ui.hrChart.HrChartScreen
import fi.ainon.polarAppis.ui.sensorinit.SensorInitScreen

@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            Column() {
                SensorInitScreen(modifier = Modifier.padding(16.dp))
                HrChartScreen(modifier = Modifier.padding(16.dp))
            }
        }
    }
}

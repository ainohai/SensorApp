package fi.ainon.polarAppis.ui.sensorinit

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.ainon.polarAppis.communication.polar.PolarH10Connection
import fi.ainon.polarAppis.data.PolarDataRepository
import fi.ainon.polarAppis.dataHandling.ScheduleAlarm
import fi.ainon.polarAppis.dataHandling.WorkerInitializer
import fi.ainon.polarAppis.ui.sensorinit.SensorInitUiState.Error
import fi.ainon.polarAppis.ui.sensorinit.SensorInitUiState.Loading
import fi.ainon.polarAppis.ui.sensorinit.SensorInitUiState.Success
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DataItemTypeViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val polarDataRepository: PolarDataRepository,
    private val workerInitializer: WorkerInitializer,
    private val scheduleAlarm: ScheduleAlarm,
    private val polarH10Connection: PolarH10Connection
) : ViewModel() {

    var collectionTimeInMin = 5L
    var intervalInMin = 30
    val TAG = "SensorInitViewModel: "
    val uiState: StateFlow<SensorInitUiState> = polarDataRepository
        .connection.map<Boolean, SensorInitUiState>(::Success)
        .catch { emit(Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Loading)

    fun connect() {
        polarH10Connection.toggleConnection()
    }

    fun periodic() {
        //workerInitializer.periodic(collectionTimeInMin, intervalInMin)
        scheduleAlarm.schedule(collectionTimeInMin, intervalInMin)
    }

    fun cancelPeriodic() {
        scheduleAlarm.cancel()
    }

    fun h10Setup() {
        workerInitializer.h10Setup(collectionTimeInMin, System.currentTimeMillis())
    }

}

sealed interface SensorInitUiState {
    object Loading : SensorInitUiState
    data class Error(val throwable: Throwable) : SensorInitUiState
    data class Success(val connected: Boolean) : SensorInitUiState
}

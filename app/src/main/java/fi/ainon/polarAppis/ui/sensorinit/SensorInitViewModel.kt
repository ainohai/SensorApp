package fi.ainon.polarAppis.ui.sensorinit

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.ainon.polarAppis.communication.polar.PolarConnection
import fi.ainon.polarAppis.data.PolarDataRepository
import fi.ainon.polarAppis.dataHandling.dataObject.ConnectionSetting
import fi.ainon.polarAppis.dataHandling.dataObject.DataSetting
import fi.ainon.polarAppis.dataHandling.dataObject.DataType
import fi.ainon.polarAppis.ui.sensorinit.SensorInitUiState.Error
import fi.ainon.polarAppis.ui.sensorinit.SensorInitUiState.Loading
import fi.ainon.polarAppis.ui.sensorinit.SensorInitUiState.Success
import fi.ainon.polarAppis.worker.ConnectionWorker
import fi.ainon.polarAppis.worker.SensorDataWorker
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
    private val polarConnection: PolarConnection,
) : ViewModel() {

    val SENSORTAG = "polarSensorDataWorker"
    val CONNECTIONTAG = "polarConnectionWorker"
    val TAG = "SensorInitViewModel: "
    val uiState: StateFlow<SensorInitUiState> = polarDataRepository
        .connection.map<Boolean, SensorInitUiState>(::Success)
        .catch { emit(Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Loading)

    fun connect() {
        polarConnection.toggleConnect()
    }

    fun h10Setup() {

        val workManager = WorkManager.getInstance(appContext)

        // We need to ensure, we are not starting multiple workers doing this.
        if (isWorkerRunning(workManager)) {
            // Cancellation here, as something is wrong, if this is tried.
            workManager.cancelAllWorkByTag(SENSORTAG)
        }
        else {
            createWorkRequest(workManager)
        }
    }

    private fun isWorkerRunning(workManager: WorkManager): Boolean {
        val workInfos = workManager.getWorkInfosByTag(SENSORTAG).get()

        for (workInfo in workInfos) {
            if (workInfo.state == WorkInfo.State.RUNNING) {
                return true
            }
        }
        return false
    }

    private fun createWorkRequest(workManager: WorkManager) {

        val collectionTimeInS = 15 * 60L
        val sensorWorkRequest: OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<SensorDataWorker>()
                .setInputData(getH10SettingsWorkData(collectionTimeInS))
                .addTag(SENSORTAG)
                .build()

        // Todo: If work manager is canceled in the middle of measurement, does not disconnect. Check if there is equivalent to finally.
        workManager
            .beginUniqueWork(CONNECTIONTAG, ExistingWorkPolicy.REPLACE, connectionWorkRequest(true))
            .then(sensorWorkRequest)
            .then(connectionWorkRequest(false))
            .enqueue()
    }

    private fun connectionWorkRequest(shouldBeConnected: Boolean): OneTimeWorkRequest {
        val connectionWorkRequest: OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<ConnectionWorker>()
                .setInputData(getConnectionData(shouldBeConnected))
                .addTag(CONNECTIONTAG)
                .build()
        return connectionWorkRequest
    }

    private fun getConnectionData(shouldBeConnected: Boolean): Data {

        //TODO: parametrize
        return workDataOf(
            ConnectionSetting.SHOULD_BE_CONNECTED.name to shouldBeConnected)
    }

    private fun getH10SettingsWorkData(collectionTime: Long): Data {

        //TODO: parametrize
        return workDataOf(
            DataSetting.ECG_SAMPLE_RATE.name to "130",
            DataSetting.ECG_RESOLUTION.name to "14",
            DataSetting.ECG_RANGE.name to "2",
            DataSetting.ACC_SAMPLE_RATE.name to "25",
            DataSetting.ACC_RESOLUTION.name to "16",
            DataSetting.ACC_RANGE.name to "2",
            DataType.HR.name to "true",
            DataType.ECG.name to "true",
            DataType.ACC.name to "false",
            DataSetting.COLLECTION_TIME_IN_S.name to collectionTime.toString())

    }
}

sealed interface SensorInitUiState {
    object Loading : SensorInitUiState
    data class Error(val throwable: Throwable) : SensorInitUiState
    data class Success(val connected: Boolean) : SensorInitUiState
}

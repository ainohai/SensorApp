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
import com.polar.sdk.api.model.PolarSensorSetting
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.ainon.polarAppis.communication.polar.PolarConnection
import fi.ainon.polarAppis.data.DataItemTypeRepository
import fi.ainon.polarAppis.ui.sensorinit.SensorInitUiState.Error
import fi.ainon.polarAppis.ui.sensorinit.SensorInitUiState.Loading
import fi.ainon.polarAppis.ui.sensorinit.SensorInitUiState.Success
import fi.ainon.polarAppis.worker.SensorDataWorker
import fi.ainon.polarAppis.worker.dataObject.DataType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.EnumMap
import javax.inject.Inject

@HiltViewModel
class DataItemTypeViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val dataItemTypeRepository: DataItemTypeRepository,
    private val polarConnection: PolarConnection,
) : ViewModel() {

    val SENSORTAG = "polarSensorDataWorker"

    val uiState: StateFlow<SensorInitUiState> = dataItemTypeRepository
        .dataItemTypes.map<List<String>, SensorInitUiState>(::Success)
        .catch { emit(Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Loading)


    fun pingMe(): Boolean {
        return true;
    }


    fun connect() {
        polarConnection.connect()
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
        val sensorWorkRequest: OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<SensorDataWorker>()
                .setInputData(getH10SettingsWorkData())
                .addTag(SENSORTAG)
                .build()

        workManager.enqueueUniqueWork(SENSORTAG, ExistingWorkPolicy.REPLACE, sensorWorkRequest)
    }

    fun acc() {
        polarConnection.getAcc(getH10Settings())
    }

    private fun getH10Settings(): PolarSensorSetting {
        val selected: MutableMap<PolarSensorSetting.SettingType, Int> =
            EnumMap(PolarSensorSetting.SettingType::class.java)
        selected[PolarSensorSetting.SettingType.SAMPLE_RATE] = 130 //25 // 50, 100, 200
        //selected[PolarSensorSetting.SettingType.CHANNELS] = -
        //selected[PolarSensorSetting.SettingType.RANGE] = 2 // 4 8
        selected[PolarSensorSetting.SettingType.RESOLUTION] = 14//16
        return PolarSensorSetting(selected)
    }

    private fun getH10SettingsWorkData(): Data {

        return workDataOf(
            PolarSensorSetting.SettingType.SAMPLE_RATE.name to "130",
            PolarSensorSetting.SettingType.RESOLUTION.name to "14",
            PolarSensorSetting.SettingType.RANGE.name to "2",
            DataType.HR.name to "true",
            DataType.ECG.name to "true",
            DataType.ACC.name to "true")

    }

}

sealed interface SensorInitUiState {
    object Loading : SensorInitUiState
    data class Error(val throwable: Throwable) : SensorInitUiState
    data class Success(val data: List<String>) : SensorInitUiState
}

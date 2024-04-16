package fi.ainon.polarAppis.ui.sensorinit

import android.content.Context
import android.util.Log
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
import fi.ainon.polarAppis.data.PolarDataRepository
import fi.ainon.polarAppis.ui.sensorinit.SensorInitUiState.Error
import fi.ainon.polarAppis.ui.sensorinit.SensorInitUiState.Loading
import fi.ainon.polarAppis.ui.sensorinit.SensorInitUiState.Success
import fi.ainon.polarAppis.worker.SensorDataWorker
import fi.ainon.polarAppis.worker.dataObject.ConnectionStatus
import fi.ainon.polarAppis.worker.dataObject.DataSetting
import fi.ainon.polarAppis.worker.dataObject.DataType
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Action
import io.reactivex.rxjava3.functions.Consumer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.EnumMap
import javax.inject.Inject

@HiltViewModel
class DataItemTypeViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val polarDataRepository: PolarDataRepository,
    private val polarConnection: PolarConnection,
) : ViewModel() {

    val SENSORTAG = "polarSensorDataWorker"
    val TAG = "SensorInitViewModel: "
    val uiState: StateFlow<SensorInitUiState> = polarDataRepository
        .connection.map<Boolean, SensorInitUiState>(::Success)
        .catch { emit(Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Loading)

    //Todo: move all this.
    private var onNext: Consumer<ConnectionStatus> = Consumer { connection -> upsertConnection(connection)}
    private var onError: Consumer<Throwable> = Consumer { error -> throw IllegalStateException("Error with ConnectionStatus", error) }
    private var onComplete = Action { Log.e(TAG, "Connection status listening completed") }
    private var connectionStatusDisposable: Disposable? = null
    init {
        //Todo: This one not disposed.
        connectionStatusDisposable = polarConnection.subscribeConnectionStatus(onNext, onError, onComplete)
    }


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
            DataType.ACC.name to "true")

    }

    private fun upsertConnection (connectionStatus: ConnectionStatus) {

        if (ConnectionStatus.CONNECTED == connectionStatus || ConnectionStatus.DISCONNECTED == connectionStatus) {

            val isConnected = ConnectionStatus.CONNECTED == connectionStatus

            val job = viewModelScope.launch {
                polarDataRepository.isConnected(isConnected)
                println("Coroutine is running")
            }
        }
    }

}

sealed interface SensorInitUiState {
    object Loading : SensorInitUiState
    data class Error(val throwable: Throwable) : SensorInitUiState
    data class Success(val connected: Boolean) : SensorInitUiState
}

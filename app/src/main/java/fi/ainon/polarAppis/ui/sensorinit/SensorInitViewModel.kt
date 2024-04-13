/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fi.ainon.polarAppis.ui.sensorinit

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.polar.sdk.api.model.PolarSensorSetting
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.ainon.polarAppis.communication.polar.PolarConnection
import fi.ainon.polarAppis.data.DataItemTypeRepository
import fi.ainon.polarAppis.ui.sensorinit.DataItemTypeUiState.Error
import fi.ainon.polarAppis.ui.sensorinit.DataItemTypeUiState.Loading
import fi.ainon.polarAppis.ui.sensorinit.DataItemTypeUiState.Success
import fi.ainon.polarAppis.worker.SensorDataWorker
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

    val uiState: StateFlow<DataItemTypeUiState> = dataItemTypeRepository
        .dataItemTypes.map<List<String>, DataItemTypeUiState>(::Success)
        .catch { emit(Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Loading)


    fun pingMe(): Boolean {
        return true;
    }


    fun connect() {
        polarConnection.connect()
    }

    fun hr() {
        polarConnection.getHr()
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
        val sensorWorkRequest: WorkRequest =
            OneTimeWorkRequestBuilder<SensorDataWorker>()
                .setInputData(getH10SettingsWorkData())
                .addTag(SENSORTAG)
                .build()

        workManager.enqueue(sensorWorkRequest)
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
            DataType.HR.name to "false",
            DataType.ECG.name to "true",
            DataType.ACC.name to "false")

    }

}

sealed interface DataItemTypeUiState {
    object Loading : DataItemTypeUiState
    data class Error(val throwable: Throwable) : DataItemTypeUiState
    data class Success(val data: List<String>) : DataItemTypeUiState
}

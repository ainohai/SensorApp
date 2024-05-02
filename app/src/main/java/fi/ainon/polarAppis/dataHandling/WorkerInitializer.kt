package fi.ainon.polarAppis.dataHandling

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.ainon.polarAppis.dataHandling.dataObject.CollectionSetting
import fi.ainon.polarAppis.dataHandling.dataObject.ConnectionSetting
import fi.ainon.polarAppis.dataHandling.dataObject.DataType
import fi.ainon.polarAppis.dataHandling.dataObject.PolarDataSetting
import fi.ainon.polarAppis.worker.ConnectionWorker
import fi.ainon.polarAppis.worker.SensorDataWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton


interface WorkerInitializer {
    fun periodic(collectionTimeInMin: Long, intervalInMin: Long)
    fun h10Setup(collectionTimeInMin: Long, triggerTime: Long)
}

@Singleton
class SensorWorkerInitializer @Inject constructor(
    @ApplicationContext private val appContext: Context,
): WorkerInitializer {

    val SENSORTAG = "polarSensorDataWorker"
    val PERIODICSENSORTAG = "periodicPolarSensorDataWorker"
    val CONNECTIONTAG = "polarConnectionWorker"
    val FLEX_TIME_INTERVAL: Long = 25

    override fun periodic(collectionTimeInMin: Long, intervalInMin: Long) {

        val workManager = WorkManager.getInstance(appContext)
        if (isWorkerRunning(workManager, PERIODICSENSORTAG)) {
            // Cancellation here, as something is wrong, if this is tried.
            workManager.cancelAllWorkByTag(PERIODICSENSORTAG)
        }
        periodicCollection(collectionTimeInMin, intervalInMin)
    }


    override fun h10Setup(collectionTimeInMin: Long, triggerTime: Long) {

        val workManager = WorkManager.getInstance(appContext)

        // We need to ensure, we are not starting multiple workers doing this.
        if (isWorkerRunning(workManager, SENSORTAG)) {
            // Cancellation here, as something is wrong, if this is tried.
            workManager.cancelAllWorkByTag(SENSORTAG)
        }
        else {
            createWorkRequest(workManager, collectionTimeInMin)
        }
    }

    private fun isWorkerRunning(workManager: WorkManager, tag: String): Boolean {
        val workInfos = workManager.getWorkInfosByTag(tag).get()

        for (workInfo in workInfos) {
            if (workInfo.state == WorkInfo.State.RUNNING || workInfo.state == WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.BLOCKED ) {
                return true
            }
        }
        return false
    }

    private fun createWorkRequest(workManager: WorkManager, collectionTimeInMin: Long) {

        val sensorWorkRequest: OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<SensorDataWorker>()
                .setInputData(getH10SettingsWorkData(collectionTimeInMin * 60L))
                .addTag(SENSORTAG)
                .build()

        // Todo: If work manager is canceled in the middle of measurement, does not disconnect. Check if there is equivalent to finally.
        workManager
            .beginUniqueWork(SENSORTAG, ExistingWorkPolicy.REPLACE, sensorWorkRequest)
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

    private fun periodicCollection(collectionTimeInMin: Long, intervalInMin: Long) {

        val request =
            PeriodicWorkRequestBuilder<SensorDataWorker>(intervalInMin, TimeUnit.MINUTES,
            FLEX_TIME_INTERVAL, TimeUnit.MINUTES)
                .setInputData(getH10SettingsWorkData(collectionTimeInMin * 60L))
                .addTag(PERIODICSENSORTAG)
                .build()

        WorkManager.getInstance(appContext).enqueue(request)

    }

    private fun getConnectionData(shouldBeConnected: Boolean): Data {

        //TODO: parametrize
        return workDataOf(
            ConnectionSetting.SHOULD_BE_CONNECTED.name to shouldBeConnected)
    }

    private fun getH10SettingsWorkData(collectionTime: Long): Data {
        //TODO: parametrize
        return workDataOf(
            PolarDataSetting.ECG_SAMPLE_RATE.name to "130",
            PolarDataSetting.ECG_RESOLUTION.name to "14",
            PolarDataSetting.ECG_RANGE.name to "2",
            PolarDataSetting.ACC_SAMPLE_RATE.name to "25",
            PolarDataSetting.ACC_RESOLUTION.name to "16",
            PolarDataSetting.ACC_RANGE.name to "2",
            PolarDataSetting.PPG_RESOLUTION.name to "22",
            PolarDataSetting.PPG_SAMPLE_RATE.name to "55",
            PolarDataSetting.PPG_CHANNELS.name to "4",
            DataType.HR.name to "true",
            DataType.ECG.name to "true",
            DataType.ACC.name to "false",
            DataType.PPG.name to "true",
            DataType.PPI.name to "true",
            CollectionSetting.COLLECTION_TIME_IN_MIN.name to collectionTime.toString(),
            CollectionSetting.WORKER_START_TIME.name to System.currentTimeMillis().toString())

    }

}
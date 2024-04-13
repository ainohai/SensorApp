package fi.ainon.polarAppis.worker

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.polar.sdk.api.model.PolarEcgData
import com.polar.sdk.api.model.PolarSensorSetting
import fi.ainon.polarAppis.communication.polar.DefaultPolarConnection
import fi.ainon.polarAppis.communication.polar.PolarConnection
import fi.ainon.polarAppis.ui.dataitemtype.DataType
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

class SensorDataWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val polarConnection: PolarConnection,
    private val dataHandler: DataHandler
) : CoroutineWorker(context, workerParams) {

    private val TAG = "PolarConnection: "
    private var ecgDisposable: Disposable? = null


    @SuppressLint("CheckResult")
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {

            val polarSettings = createSettings()

            val isDisposed = ecgDisposable?.isDisposed ?: true
            if (!isDisposed) {
                streamEcg(polarSettings)
            } else {
                ecgDisposable?.dispose()
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Error when collecting sensor data", ex)
            ecgDisposable?.dispose()
            Result.failure()
        }
        finally {
            Result.success()
        }

        Log.d(TAG, "Iam in the worker now thankyou")
        Result.success()
    }

    private fun streamEcg(polarSettings: PolarSensorSetting) {
        ecgDisposable = polarConnection.getEcg(polarSettings).subscribe(
            { polarEcgData: PolarEcgData ->
                val samples = mutableListOf<EcgData.EcgDataSample>()
                for (data in polarEcgData.samples) {
                    //Log.d(TAG, "    yV: ${data.voltage} timeStamp: ${data.timeStamp}")
                    samples.add(EcgData.EcgDataSample(data.timeStamp, data.voltage))
                }
                dataHandler.handleECG(EcgData(samples))
            },
            { error: Throwable ->
                Log.e(TAG, "ECG stream failed. Reason $error")
            },
        )
    }

    private fun createSettings(): PolarSensorSetting {
        val resolution =
            inputData.getString(PolarSensorSetting.SettingType.RESOLUTION.name) ?: Result.failure()
        val sampleRate =
            inputData.getString(PolarSensorSetting.SettingType.SAMPLE_RATE.name) ?: Result.failure()

        val polarSettings = PolarSensorSetting(
            mapOf(
                Pair(PolarSensorSetting.SettingType.SAMPLE_RATE, (sampleRate as String).toInt()),
                Pair(PolarSensorSetting.SettingType.RESOLUTION, (resolution as String).toInt()),
            )
        )
        return polarSettings
    }



}
package fi.ainon.polarAppis.worker.sensorDataCollector

import android.util.Log
import androidx.work.Data
import androidx.work.ListenableWorker
import com.polar.sdk.api.model.PolarEcgData
import com.polar.sdk.api.model.PolarSensorSetting
import fi.ainon.polarAppis.communication.polar.PolarConnection
import fi.ainon.polarAppis.worker.DataHandler
import fi.ainon.polarAppis.worker.EcgData


class CollectEcg(
    private val dataHandler: DataHandler,
    private val polarConnection: PolarConnection,
    private val inputData: Data
) : CommonCollect() {

    private val TAG = "CollectEcg: "

    override fun collectData() {
        val polarSettings = createSettings()
        streamEcg(polarSettings)
    }

    override fun stopCollect() {
        dispose()
    }
    override fun streamEcg(polarSettings: PolarSensorSetting) {
         val ecgDisposable = polarConnection.getEcg(polarSettings).subscribe(
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
                stopCollect()
            }
        )

        setDisposable(ecgDisposable)

    }

    override fun createSettings(): PolarSensorSetting {
        val resolution =
            inputData.getString(PolarSensorSetting.SettingType.RESOLUTION.name) ?: ListenableWorker.Result.failure()
        val sampleRate =
            inputData.getString(PolarSensorSetting.SettingType.SAMPLE_RATE.name) ?: ListenableWorker.Result.failure()

        val polarSettings = PolarSensorSetting(
            mapOf(
                Pair(PolarSensorSetting.SettingType.SAMPLE_RATE, (sampleRate as String).toInt()),
                Pair(PolarSensorSetting.SettingType.RESOLUTION, (resolution as String).toInt()),
            )
        )
        return polarSettings
    }
}
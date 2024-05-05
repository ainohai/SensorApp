package fi.ainon.polarAppis.communication.polar

import android.content.Context
import android.util.Log
import com.polar.sdk.api.model.PolarSensorSetting
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.ainon.polarAppis.BuildConfig
import fi.ainon.polarAppis.dataHandling.handler.HandleAcc
import fi.ainon.polarAppis.dataHandling.handler.HandleEcg
import fi.ainon.polarAppis.dataHandling.handler.HandleH10Connection
import fi.ainon.polarAppis.dataHandling.handler.HandleH10Hr
import fi.ainon.polarAppis.dataHandling.sensorDataCollector.CollectAcc
import fi.ainon.polarAppis.dataHandling.sensorDataCollector.CollectEcg
import fi.ainon.polarAppis.dataHandling.sensorDataCollector.CollectHr
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Responsible for starting and stopping connection and data collection.
 */

interface PolarConnection {

    fun connect(shouldBeConnected: Boolean)
    fun cleanupCollectors()
    suspend fun collectData(
        collectionTime: Long,
        measureHr: Boolean,
        measureEcg: Boolean,
        measureAcc: Boolean,
        ecgSettings: PolarSensorSetting,
        accSetting: PolarSensorSetting
    )

}

@Singleton
class PolarH10Connection @Inject constructor(
    @ApplicationContext appContext: Context,
    private val accHandler: HandleAcc,
    private val ecgHandler: HandleEcg,
    private val hrHandler: HandleH10Hr,
    private val connectionHandler: HandleH10Connection,
) : PolarConnection {

    private val TAG = "H10Connection: "
    private val api: PolarConnectionApi
    private var ecg : CollectEcg? = null
    private var acc : CollectAcc? = null
    private var hr : CollectHr? = null

    init {
        api = DefaultPolarConnectionApi(appContext, BuildConfig.POLAR_H10)
        connectionHandler.setConnectionFlow(api.connectionStatus())
    }

    override fun connect(shouldBeConnected: Boolean) {

        api.connect(shouldBeConnected)

    }

    fun toggleConnection() {

        api.toggleConnect()
    }
    override fun cleanupCollectors() {
        Log.d(TAG, "Clean up collectors")
        ecg?.stopCollect()
        acc?.stopCollect()
        hr?.stopCollect()
    }

    override suspend fun collectData(collectionTime: Long, measureHr: Boolean, measureEcg: Boolean, measureAcc: Boolean, ecgSettings: PolarSensorSetting, accSetting: PolarSensorSetting) {
        if (ecg?.isDisposed() != false && acc?.isDisposed() != false && hr?.isDisposed() != false) {

            withTimeoutOrNull(collectionTime * 1000) {
                connectionHandler.dataFlow().collect { isConnected ->
                    Log.d(TAG, "Is connected: $isConnected")
                    if (isConnected) {
                        collectData(measureHr, measureEcg, measureAcc, ecgSettings, accSetting)
                        Log.d(TAG, "Already connected, starting to collect data")
                    }
                }
            }
        }
    }

    private fun collectData(measureHr: Boolean, measureEcg: Boolean, measureAcc: Boolean, ecgSettings: PolarSensorSetting, accSetting: PolarSensorSetting) {
        Log.d(TAG, "Starting to collect data")

        if (measureHr && hr == null) {
            hr = CollectHr(hrHandler, api.startHrStream())
        }
        if (measureEcg && ecg == null) {
            ecg = CollectEcg(ecgHandler, api.startEcgStream(ecgSettings))
        }
        if (measureAcc && acc == null) {
            acc = CollectAcc(accHandler, api.startAccStream(accSetting))
        }
    }
}
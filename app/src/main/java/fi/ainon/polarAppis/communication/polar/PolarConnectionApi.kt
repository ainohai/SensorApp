package fi.ainon.polarAppis.communication.polar

import android.content.Context
import android.util.Log
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.errors.PolarInvalidArgument
import com.polar.sdk.api.model.PolarAccelerometerData
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarEcgData
import com.polar.sdk.api.model.PolarHrData
import com.polar.sdk.api.model.PolarPpgData
import com.polar.sdk.api.model.PolarPpiData
import com.polar.sdk.api.model.PolarSensorSetting
import fi.ainon.polarAppis.dataHandling.dataObject.ConnectionStatus
import io.reactivex.rxjava3.core.Flowable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

interface PolarConnectionApi {

    fun onResume()
    fun onDestroy()
    fun connectionStatus(): SharedFlow<ConnectionStatus>
    fun isConnected(): Boolean
    fun toggleConnect()
    fun connect(shouldBeConnected: Boolean)

    fun startHrStream(): Flowable<PolarHrData>
    fun startEcgStream(settings: PolarSensorSetting): Flowable<PolarEcgData>
    fun startAccStream(settings: PolarSensorSetting): Flowable<PolarAccelerometerData>
    fun startPpgStream(settings: PolarSensorSetting): Flowable<PolarPpgData>
    fun startPpiStream(): Flowable<PolarPpiData>
}


class DefaultPolarConnectionApi (
    appContext: Context,
    deviceId: String
) : PolarConnectionApi {

    private var deviceId = deviceId
    private val API_LOGGER_TAG: String = "POLAR API kutsu: "
    private val TAG = "PolarConnection: "

    private var deviceConnectionStatus = ConnectionStatus.DISCONNECTED

    private val _connectionStatus = MutableSharedFlow<ConnectionStatus>(replay = 1)
    private val connectionStatus = _connectionStatus.asSharedFlow()

    private val api: PolarBleApi by lazy {
        // Notice all features are enabled
        PolarBleApiDefaultImpl.defaultImplementation(
            appContext,
            setOf(
                PolarBleApi.PolarBleSdkFeature.FEATURE_HR,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_SDK_MODE,
                PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_H10_EXERCISE_RECORDING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_OFFLINE_RECORDING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_DEVICE_TIME_SETUP,
                PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO
            )
        )
    }

    init {
        apiSetup();
    }

    private fun apiSetup() {
        api.setApiLogger { s: String -> Log.d(API_LOGGER_TAG, s) }

        //Todo: can you use callback flow here?
        api.setApiCallback(object : PolarBleApiCallback() {
            override fun blePowerStateChanged(powered: Boolean) {
                Log.d(TAG, "BLE power: $powered")

            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "CONNECTED: ${polarDeviceInfo.deviceId}")
                deviceId = polarDeviceInfo.deviceId
                nextStatus(ConnectionStatus.CONNECTED)

            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "CONNECTING: ${polarDeviceInfo.deviceId}")
                nextStatus(ConnectionStatus.CONNECTING)

            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "DISCONNECTED: ${polarDeviceInfo.deviceId}")
                nextStatus(ConnectionStatus.DISCONNECTED)

            }

            override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {
                Log.d(TAG, "DIS INFO uuid: $uuid value: $value")

            }

            override fun batteryLevelReceived(identifier: String, level: Int) {
                Log.d(TAG, "BATTERY LEVEL: $level")

            }
        })
    }

    override fun isConnected(): Boolean {
        Log.d(TAG, "Device is connected ${deviceConnectionStatus === ConnectionStatus.CONNECTED}.")
        return deviceConnectionStatus == ConnectionStatus.CONNECTED
    }

    override fun toggleConnect() {
        val shouldBeConnected = !isConnected()
        tryConnect(shouldBeConnected, deviceId)
    }

    override fun connect(shouldBeConnected: Boolean) {
        try {
            tryConnect(shouldBeConnected, deviceId)
        } catch (polarInvalidArgument: PolarInvalidArgument) {
            val attempt = if (deviceConnectionStatus == ConnectionStatus.CONNECTED) {
                "disconnect"
            } else {
                "connect"
            }
            Log.e(TAG, "Failed to $attempt. Reason $polarInvalidArgument ")
        }
    }

    private fun tryConnect(shouldBeConnected: Boolean, deviceId: String) {
        if (deviceConnectionStatus != ConnectionStatus.CONNECTED && shouldBeConnected) {
            Log.d(TAG, "Asking to connect device")
            api.connectToDevice(deviceId)
        } else if (deviceConnectionStatus == ConnectionStatus.CONNECTED && !shouldBeConnected) {
            Log.d(TAG, "Disconnecting device")
            api.disconnectFromDevice(deviceId)
        }
    }

    override fun onResume() {

        api.foregroundEntered()
        nextStatus(deviceConnectionStatus)
    }

    override fun onDestroy() {

        api.shutDown()
    }

    override fun connectionStatus(): SharedFlow<ConnectionStatus> {
        return connectionStatus
    }

    private fun nextStatus(connectionStatus: ConnectionStatus) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Small delay to ensure everything is set up.
                if (connectionStatus == ConnectionStatus.CONNECTED) {
                    delay(1*1000)
                }
                deviceConnectionStatus = connectionStatus
                _connectionStatus.emit(connectionStatus)
            } catch(e: Exception) {
                Log.e(TAG, "Passing callback failed.", e)
            }
        }
    }

    override fun startHrStream(): Flowable<PolarHrData> {
        return api.startHrStreaming(deviceId)
    }
    override fun startEcgStream(settings: PolarSensorSetting): Flowable<PolarEcgData> {
        return api.startEcgStreaming(deviceId, settings)
    }
    override fun startAccStream(settings: PolarSensorSetting): Flowable<PolarAccelerometerData> {
        return api.startAccStreaming(deviceId, settings)
    }
    override fun startPpgStream(settings: PolarSensorSetting): Flowable<PolarPpgData> {
        return api.startPpgStreaming(deviceId, settings)
    }

    override fun startPpiStream(): Flowable<PolarPpiData> {
        return api.startPpiStreaming(deviceId)
    }

}
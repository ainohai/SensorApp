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
import com.polar.sdk.api.model.PolarSensorSetting
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fi.ainon.polarAppis.BuildConfig
import fi.ainon.polarAppis.worker.dataObject.ConnectionStatus
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Action
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface PolarConnModule {

    @Singleton
    @Binds
    fun bindsPolarConn(
        polarConnection: DefaultPolarConnection
    ): PolarConnection
}

interface PolarConnection {

    fun connect()
    fun getHr(): Flowable<PolarHrData>
    fun getEcg(settings: PolarSensorSetting): Flowable<PolarEcgData>
    fun getAcc(settings: PolarSensorSetting): Flowable<PolarAccelerometerData>

    fun onResume()
    fun onDestroy()
    fun subscribeConnectionStatus(
        onNext: Consumer<ConnectionStatus>,
        onError: Consumer<Throwable>,
        onComplete: Action
    ): Disposable

    //TODO: What if device is turned of?
    fun isConnected(): Boolean
}


class DefaultPolarConnection @Inject constructor(
    @ApplicationContext appContext: Context
) : PolarConnection {

    private var DEVICE_ID = BuildConfig.POLAR_H10
    private val API_LOGGER_TAG: String = "POLAR API kutsu: "
    private val TAG = "PolarConnection: "

    private var deviceConnected = false

    private var connectionStatus = PublishSubject.create<ConnectionStatus>()

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

        api.setApiCallback(object : PolarBleApiCallback() {
            override fun blePowerStateChanged(powered: Boolean) {
                Log.d(TAG, "BLE power: $powered")

            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "CONNECTED: ${polarDeviceInfo.deviceId}")
                DEVICE_ID = polarDeviceInfo.deviceId
                deviceConnected = true
                connectionStatus.onNext(ConnectionStatus.CONNECTED)

            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "CONNECTING: ${polarDeviceInfo.deviceId}")
                connectionStatus.onNext(ConnectionStatus.CONNECTING)

            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "DISCONNECTED: ${polarDeviceInfo.deviceId}")
                deviceConnected = false
                connectionStatus.onNext(ConnectionStatus.DISCONNECTED)

            }

            override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {
                Log.d(TAG, "DIS INFO uuid: $uuid value: $value")
                connectionStatus.onNext(ConnectionStatus.DISCONNECTING)

            }

            override fun batteryLevelReceived(identifier: String, level: Int) {
                Log.d(TAG, "BATTERY LEVEL: $level")

            }
        })
    }

    override fun isConnected(): Boolean {
        Log.d(TAG, "Device is connected.")
        return deviceConnected
    }
    override fun connect() {
        try {
            if (deviceConnected) {
                Log.d(TAG, "Disconnecting device")
                api.disconnectFromDevice(DEVICE_ID)
            } else {
                Log.d(TAG, "Asking to connect device")
                api.connectToDevice(DEVICE_ID)
            }
        } catch (polarInvalidArgument: PolarInvalidArgument) {
            val attempt = if (deviceConnected) {
                "disconnect"
            } else {
                "connect"
            }
            Log.e(TAG, "Failed to $attempt. Reason $polarInvalidArgument ")
        }
    }

    override fun getHr(): Flowable<PolarHrData> {
        return api.startHrStreaming(DEVICE_ID)
    }

    override fun getEcg(settings: PolarSensorSetting): Flowable<PolarEcgData> {
        return api.startEcgStreaming(DEVICE_ID, settings)
    }

    override fun getAcc(settings: PolarSensorSetting): Flowable<PolarAccelerometerData> {
        return api.startAccStreaming(DEVICE_ID, settings)
    }

    override fun onResume() {

        api.foregroundEntered()
    }

    override fun onDestroy() {

        api.shutDown()
    }

    override fun subscribeConnectionStatus(onNext: Consumer<ConnectionStatus>, onError: Consumer<Throwable>, onComplete: Action): Disposable {
        return connectionStatus.subscribe(onNext, onError, onComplete)
    }
}
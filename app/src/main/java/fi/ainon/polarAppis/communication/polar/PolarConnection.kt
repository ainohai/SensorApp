package fi.ainon.polarAppis.communication.polar

import android.content.Context
import android.util.Log
import androidx.core.util.Pair
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.PolarH10OfflineExerciseApi
import com.polar.sdk.api.errors.PolarInvalidArgument
import com.polar.sdk.api.model.PolarAccelerometerData
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarEcgData
import com.polar.sdk.api.model.PolarExerciseData
import com.polar.sdk.api.model.PolarExerciseEntry
import com.polar.sdk.api.model.PolarGyroData
import com.polar.sdk.api.model.PolarHrData
import com.polar.sdk.api.model.PolarMagnetometerData
import com.polar.sdk.api.model.PolarOfflineRecordingData
import com.polar.sdk.api.model.PolarOfflineRecordingEntry
import com.polar.sdk.api.model.PolarPpgData
import com.polar.sdk.api.model.PolarPpiData
import com.polar.sdk.api.model.PolarRecordingSecret
import com.polar.sdk.api.model.PolarSensorSetting
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fi.ainon.polarAppis.BuildConfig
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import java.util.Calendar
import java.util.Date
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
    fun getHr()
    fun getEcg(settings: PolarSensorSetting): Flowable<PolarEcgData>
    fun getAcc(settings: PolarSensorSetting)

    fun onResume()
    fun onDestroy()
}


class DefaultPolarConnection  @Inject constructor(
    @ApplicationContext appContext: Context
) : PolarConnection {

    private var DEVICE_ID = BuildConfig.POLAR_H10
    private val API_LOGGER_TAG: String = "POLAR API kutsu: "
    private val TAG = "PolarConnection: "

    private var hrDisposable: Disposable? = null
    private var accDisposable: Disposable? = null

    private var deviceConnected = false

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

    // TODO: PASS THE INFO TO UI.
    private fun apiSetup () {
        api.setApiLogger { s: String -> Log.d(API_LOGGER_TAG, s) }

        api.setApiCallback(object : PolarBleApiCallback() {
            override fun blePowerStateChanged(powered: Boolean) {
                Log.d(TAG, "BLE power: $powered")

            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "CONNECTED: ${polarDeviceInfo.deviceId}")
                DEVICE_ID = polarDeviceInfo.deviceId
                deviceConnected = true

            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "CONNECTING: ${polarDeviceInfo.deviceId}")

            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "DISCONNECTED: ${polarDeviceInfo.deviceId}")
                deviceConnected = false

            }

            override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {
                Log.d(TAG, "DIS INFO uuid: $uuid value: $value")

            }

            override fun batteryLevelReceived(identifier: String, level: Int) {
                Log.d(TAG, "BATTERY LEVEL: $level")

            }
        })
    }

    override fun connect () {
        try {
            if (deviceConnected) {
                api.disconnectFromDevice(DEVICE_ID)
            } else {
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

    override fun getHr() {
        val isDisposed = hrDisposable?.isDisposed ?: true
        if (isDisposed) {
            //toggleButtonDown(hrButton, R.string.stop_hr_stream)
            hrDisposable = api.startHrStreaming(DEVICE_ID)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { hrData: PolarHrData ->
                        for (sample in hrData.samples) {
                            Log.d(TAG, "HR     bpm: ${sample.hr} rrs: ${sample.rrsMs} rrAvailable: ${sample.rrAvailable} contactStatus: ${sample.contactStatus} contactStatusSupported: ${sample.contactStatusSupported}")
                        }
                    },
                    { error: Throwable ->
                        //toggleButtonUp(hrButton, R.string.start_hr_stream)
                        Log.e(TAG, "HR stream failed. Reason $error")
                    },
                    { Log.d(TAG, "HR stream complete") }
                )
        } else {
            //toggleButtonUp(hrButton, R.string.start_hr_stream)
            // NOTE dispose will stop streaming if it is "running"
            hrDisposable?.dispose()
        }
    }

    override fun getEcg (settings: PolarSensorSetting): Flowable<PolarEcgData> {
        return api.startEcgStreaming(DEVICE_ID, settings)
    }

    override fun getAcc(settings: PolarSensorSetting) {
        val isDisposed = accDisposable?.isDisposed ?: true
        if (isDisposed) {
            //toggleButtonDown(accButton, R.string.stop_acc_stream)
            //accDisposable = requestStreamSettings(deviceId, PolarBleApi.PolarDeviceDataType.ACC)
            accDisposable = //streamSettings
            //    .flatMap { settings: PolarSensorSetting ->
                    api.startAccStreaming(DEVICE_ID, settings) //}
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { polarAccelerometerData: PolarAccelerometerData ->
                        for (data in polarAccelerometerData.samples) {
                            Log.d(TAG, "ACC    x: ${data.x} y: ${data.y} z: ${data.z} timeStamp: ${data.timeStamp}")
                        }
                    },
                    { error: Throwable ->
                        //toggleButtonUp(accButton, R.string.start_acc_stream)
                        Log.e(TAG, "ACC stream failed. Reason $error")
                    },
                    {
                        //showToast("ACC stream complete")
                        Log.d(TAG, "ACC stream complete")
                    }
                )
        } else {
            //toggleButtonUp(accButton, R.string.start_acc_stream)
            // NOTE dispose will stop streaming if it is "running"
            accDisposable?.dispose()
        }
    }

//todo
override fun onResume() {

    api.foregroundEntered()
}

//todo
override fun onDestroy() {

    api.shutDown()
}


private fun disposeAllStreams() {
    //ecgDisposable?.dispose() TODO!
    accDisposable?.dispose()

}

}
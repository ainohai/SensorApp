package fi.ainon.polarAppis.dataHandling.sensorDataCollector

import com.polar.sdk.api.model.PolarSensorSetting
import io.reactivex.rxjava3.disposables.Disposable

interface CollectSensorData {
    fun collectData()
    fun isDisposed(): Boolean
    fun stopCollect()
}

abstract class CommonCollect (private val polarSettings: PolarSensorSetting): CollectSensorData {
    private var disposable: Disposable? = null;

    override fun collectData() {
        streamData(polarSettings)
    }

    override fun stopCollect() {
        dispose()
    }
    abstract fun streamData(polarSettings: PolarSensorSetting)

    override fun isDisposed(): Boolean {
        return disposable?.isDisposed ?: true
    }

    protected fun setDisposable(disposable: Disposable) {
        this.disposable = disposable;
    }
    protected fun dispose() {
        disposable?.dispose()
    }
}

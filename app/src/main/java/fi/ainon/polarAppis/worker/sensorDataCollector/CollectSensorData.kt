package fi.ainon.polarAppis.worker.sensorDataCollector

import com.polar.sdk.api.model.PolarSensorSetting
import io.reactivex.rxjava3.disposables.Disposable

interface CollectSensorData {
    fun collectData()
    fun isDisposed(): Boolean
    fun stopCollect()
}

abstract class CommonCollect : CollectSensorData {
    private var disposable: Disposable? = null;

    abstract fun streamEcg(polarSettings: PolarSensorSetting)
    abstract fun createSettings(): PolarSensorSetting

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

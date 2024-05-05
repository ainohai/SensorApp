package fi.ainon.polarAppis.dataHandling.sensorDataCollector

import io.reactivex.rxjava3.disposables.Disposable

/**
 * Sensor data collector handles collecting data in case of RxJava flows used by polar api, as they take care of disposing data flows.
 * Other datatypes are handled directly from connection without collectors.
 */
interface CollectSensorData {
    fun collectData()
    fun isDisposed(): Boolean
    fun stopCollect()
}

abstract class CommonCollect (): CollectSensorData {
    private var disposable: Disposable? = null;

    override fun collectData() {
        streamData()
    }

    override fun stopCollect() {
        dispose()
    }
    abstract fun streamData()

    override fun isDisposed(): Boolean {
        return disposable == null || disposable?.isDisposed ?: true
    }

    protected fun setDisposable(disposable: Disposable) {
        this.disposable = disposable;
    }
    protected fun dispose() {
        disposable?.dispose()
        disposable = null
    }
}

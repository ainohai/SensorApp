package fi.ainon.polarAppis.dataHandling.sensorDataCollector

import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.flow.Flow

/**
 * Sensor data collector handles collecting data in case of RxJava flows used by polar api, as they take care of disposing data flows.
 * Other datatypes are handled directly from connection without collectors.
 */
interface CollectSensorData<T> {
    fun streamData(): Flow<T>
    fun isDisposed(): Boolean
    fun stopCollect()
}

abstract class CommonCollect<T> (): CollectSensorData<T> {
    private var disposable: Disposable? = null;

    override fun stopCollect() {
        dispose()
    }

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

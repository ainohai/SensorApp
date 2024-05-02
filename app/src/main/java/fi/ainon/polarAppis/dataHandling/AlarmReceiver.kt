package fi.ainon.polarAppis.dataHandling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import fi.ainon.polarAppis.dataHandling.dataObject.CollectionSetting

class AlarmReceiver (
    ) : BroadcastReceiver() {

    private val TAG = "AlarmReceiver"
    private val DEFAULT_COLLECTION_TIME_IN_MIN: Long = 1
    private val DEFAULT_INTERVAL: Int = 30

    override fun onReceive(context: Context, intent: Intent) {
        StringBuilder().apply {
            append("Action: ${intent.action}\n")
            append("URI: ${intent.toUri(Intent.URI_INTENT_SCHEME)}\n")
            toString().also { log ->
                Log.d(TAG, log)
            }
        }

        val collectionInterval = intent.extras?.getString(CollectionSetting.COLLECTION_INTERVAL_IN_MIN.name)?.toInt() ?: DEFAULT_INTERVAL
        val triggerTime = intent.extras?.getString(CollectionSetting.TRIGGER_TIME_MILLIS.name)?.toLong() ?: 0
        val collectionTime = (intent.extras?.getString(CollectionSetting.COLLECTION_TIME_IN_MIN.name)?.toLong()
            ?: DEFAULT_COLLECTION_TIME_IN_MIN)

        getWorkerInitializer(context).h10Setup(collectionTime, triggerTime)

        getScheduleAlarm(context).schedule(collectionTime, collectionInterval)

    }


    private fun getWorkerInitializer(appContext: Context): WorkerInitializer {
        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            appContext,
            WorkerAlarmEntryPont::class.java
        )
        return hiltEntryPoint.workerInitilizer()
    }

    private fun getScheduleAlarm(appContext: Context): ScheduleAlarm {
        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            appContext,
            WorkerAlarmEntryPont::class.java
        )
        return hiltEntryPoint.scheduleAlarm()
    }


    @InstallIn(SingletonComponent::class)
    @EntryPoint
    interface WorkerAlarmEntryPont {
        fun workerInitilizer(): WorkerInitializer
        fun scheduleAlarm(): ScheduleAlarm
    }

}

package fi.ainon.polarAppis.dataHandling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.ainon.polarAppis.dataHandling.dataObject.CollectionSetting
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

interface ScheduleAlarm {
    fun schedule(collectionTimeInMin: Long, scheduleIntervalInMin: Int)
    fun cancel()
    fun hasAlarm(): Boolean
}

@Singleton
class DefaultScheduleAlarm @Inject constructor(
    @ApplicationContext private val context: Context,
) : ScheduleAlarm{


    private val TAG = "DefaultScheduleAlarm"
    private val INTENT_NAME = "fi.ainon.polarAppis.dataHandling.ScheduleAlarm.ALARM_INTENT"
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private lateinit var alarmIntent: PendingIntent


    override fun schedule(collectionTimeInMin: Long, scheduleIntervalInMin: Int) {

        cancel()

        if (!alarmManager.canScheduleExactAlarms()) {

            val settingIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            settingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(context, settingIntent, Bundle())

        }



        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            //set(Calendar.HOUR_OF_DAY, 8)
            //set(Calendar.MINUTE, 1)
        }

        calendar.add(Calendar.MINUTE, scheduleIntervalInMin)

        val intent = Intent(context, AlarmReceiver::class.java)
        intent.putExtra(CollectionSetting.COLLECTION_TIME_IN_MIN.name, collectionTimeInMin)
        intent.putExtra(CollectionSetting.TRIGGER_TIME_MILLIS.name, calendar.timeInMillis)
        intent.putExtra(CollectionSetting.COLLECTION_INTERVAL_IN_MIN.name, scheduleIntervalInMin)
        intent.setAction(INTENT_NAME)
        alarmIntent = intent.let { alarmIntent ->
            PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_IMMUTABLE)
        }

        if (!alarmManager.canScheduleExactAlarms()) {
            Log.e(TAG, "Can't schedule alarm")
        }

        /*alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            scheduleIntervalInS * 1000,
            alarmIntent
        )*/

        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            alarmIntent
        )


        /*alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_FIFTEEN_MINUTES,
            alarmIntent
            )*/


        Log.d(TAG, "Alarm set in ${System.currentTimeMillis()}")
    }

    override fun hasAlarm(): Boolean {
        return ::alarmIntent.isInitialized
    }

    override fun cancel() {
        if(hasAlarm()) {
            Log.d(TAG, "Cancelling alarm")
            alarmManager?.cancel(alarmIntent)
        }
    }
}
package id.ilhamelmujib.prayertime.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PrayBootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PrayBootReceiver"
    }

    private val prayAlarmReceiver = PrayAlarmReceiver()

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val action = intent.action
            Log.e(TAG, "PrayBootReceiver $action")
            if (action != null) {
                when (action) {
                    "android.intent.action.BOOT_COMPLETED",
                    "android.intent.action.QUICKBOOT_POWERON",
                    "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                        prayAlarmReceiver.cancelAlarm(context)
                        prayAlarmReceiver.setAlarm(context)
                    }
                    // Our location could have changed, which means time calculations may be different
                    // now so cancel the alarm and set it again.
                    "android.intent.action.TIMEZONE_CHANGED",
                    "android.intent.action.TIME_SET",
                    "android.intent.action.MY_PACKAGE_REPLACED" -> {
                        prayAlarmReceiver.cancelAlarm(context)
                        prayAlarmReceiver.setAlarm(context)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
package id.ilhamelmujib.prayertime.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import id.ilhamelmujib.prayertime.services.PrayAlarmService

class PrayAlarmActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        PrayAlarmService.sendBroadcast(context, intent)
    }
}

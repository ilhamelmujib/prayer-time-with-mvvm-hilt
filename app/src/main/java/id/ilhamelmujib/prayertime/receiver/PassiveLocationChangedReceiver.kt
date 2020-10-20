package id.ilhamelmujib.prayertime.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import id.ilhamelmujib.prayertime.utils.praytimes.AppSettings

class PassiveLocationChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val key = LocationManager.KEY_LOCATION_CHANGED
        if (intent.hasExtra(key)) {
            // This update came from Passive provider, so we can extract the location
            // directly.
            intent.extras?.let {
                val location = it[key] as Location
                val prayAlarmReceiver = PrayAlarmReceiver()
                val settings = AppSettings.getInstance()

                settings.latFor = location.latitude
                settings.lngFor = location.longitude
                prayAlarmReceiver.cancelAlarm(context)
                prayAlarmReceiver.setAlarm(context)
            }
        }
    }
}
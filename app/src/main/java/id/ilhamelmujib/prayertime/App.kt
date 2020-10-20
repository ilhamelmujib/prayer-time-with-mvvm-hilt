package id.ilhamelmujib.prayertime

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import id.ilhamelmujib.prayertime.utils.*
import id.ilhamelmujib.prayertime.utils.praytimes.AppSettings


@HiltAndroidApp
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        val settings = AppSettings.getInstance(this)
        settings.run {
            if (!getBoolean(AppSettings.Key.IS_INIT)) {
                for (value in KEYS) {
                    set(ALARM_REMINDER_FOR + value, 0)
                    if (value != SUNSET && value != IMSAK) {
                        set(ALARM_FOR + value, getAlarmValue(value))
                    } else {
                        set(
                            ALARM_FOR + value,
                            USE_NONACTIVE
                        )
                    }
                }
                set(AppSettings.Key.IS_INIT, true)
            }
        }

    }

    private fun getAlarmValue(key: String): Int {
        return if (key == SUNRISE || key == IMSAK)
            USE_SILENT
        else
            USE_ADHAN
    }
}
package id.ilhamelmujib.prayertime.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.os.Build
import android.text.format.DateFormat
import android.util.Log
import androidx.core.app.NotificationCompat
import id.ilhamelmujib.prayertime.R
import id.ilhamelmujib.prayertime.services.PrayAlarmService
import id.ilhamelmujib.prayertime.ui.component.DashboardActivity
import id.ilhamelmujib.prayertime.utils.*
import id.ilhamelmujib.prayertime.utils.praytimes.AppSettings
import id.ilhamelmujib.prayertime.utils.praytimes.PrayTime
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

private const val ALARM_ID = 1010
private const val ALARM_REMINDER_ID = 1011
private const val PASSIVE_LOCATION_ID = 1011
private const val EXTRA_PRAYER_NAME = "prayer_name"
private const val EXTRA_PRAYER_TIME = "prayer_time"
private const val EXTRA_REMINDER = "prayer_reminder"

class PrayAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PrayAlarmReceiver"
    }

    private lateinit var alarmManager: AlarmManager

    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME)
        val prayerTime = intent.getLongExtra(EXTRA_PRAYER_TIME, -1)
        val reminder = intent.getBooleanExtra(EXTRA_REMINDER, false)

        val timePassed =
            prayerTime != -1L && abs(System.currentTimeMillis() - prayerTime) > FIVE_MINUTES

        if (!timePassed) {
            val time = convertLongToTime(prayerTime)
            prayerName?.let {
                if (reminder) {
                    sendReminder(context, it, prayerTime)
                } else {
                    PrayAlarmService.startService(context, it, time)
                }
            }
            Log.e(TAG, "name: $prayerName, time: $time, reminder :$reminder")
            if (!reminder) {
                Log.e(TAG, "set alarm")
                setAlarm(context)
            }
        }
    }

    fun setAlarm(context: Context) = Coroutines.main {
        if (permissionGranted(context)) {
            alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, PrayAlarmReceiver::class.java)

            val now = Calendar.getInstance(TimeZone.getDefault())
            now.timeInMillis = System.currentTimeMillis()

            var then = Calendar.getInstance(TimeZone.getDefault())
            then.timeInMillis = System.currentTimeMillis()


            val settings = AppSettings.getInstance()
            val lat = settings.latFor
            val long = settings.lngFor

            val prayerTimes: LinkedHashMap<String, String> =
                PrayTime.getPrayerTimes(context, lat, long)
            val prayerNames: List<String> = ArrayList(prayerTimes.keys)

            var nextAlarmFound = false
            var nameOfPrayerFound = ""

            for (prayer in prayerNames) {
                if (settings.getInt(ALARM_FOR + prayer) != USE_NONACTIVE) {
                    val time = prayerTimes[prayer]

                    if (time != null) {
                        then = getCalendarFromPrayerTime(context, then, time)

                        if (then.after(now)) {
                            // this is the alarm to set
                            nameOfPrayerFound = prayer
                            nextAlarmFound = true
                            break
                        }
                    }
                }

            }

            if (!nextAlarmFound) {
                for (prayer in prayerNames) {
                    if (settings.getInt(ALARM_FOR + prayer) != USE_NONACTIVE) {
                        val time = prayerTimes[prayer]

                        if (time != null) {
                            then = getCalendarFromPrayerTime(context, then, time)

                            if (then.before(now)) {
                                // this is the alarm to set
                                nameOfPrayerFound = prayer
                                nextAlarmFound = true
                                then.add(Calendar.DAY_OF_YEAR, 1)
                                break
                            }
                        }
                    }
                }
            }

            if (!nextAlarmFound) {
                Log.e(TAG, "Alarm not found")
                return@main
            }

            logDebug(
                TAG,
                "set alarm for $nameOfPrayerFound, time ${convertLongToTime(then.timeInMillis)}"
            )

            intent.putExtra(EXTRA_PRAYER_NAME, nameOfPrayerFound)
            intent.putExtra(EXTRA_PRAYER_TIME, then.timeInMillis)

            val alarmIntent =
                PendingIntent.getBroadcast(
                    context,
                    ALARM_ID,
                    intent,
                    PendingIntent.FLAG_CANCEL_CURRENT
                )

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                then.timeInMillis,
                alarmIntent
            )

            val passiveIntent = Intent(context, PassiveLocationChangedReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                PASSIVE_LOCATION_ID,
                passiveIntent,
                PendingIntent.FLAG_CANCEL_CURRENT
            )

            requestPassiveLocationUpdates(context, pendingIntent)

            val receiver = ComponentName(context, PrayBootReceiver::class.java)
            val pm = context.packageManager

            pm.setComponentEnabledSetting(
                receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            setReminder(context, nameOfPrayerFound, then.timeInMillis)
        }
    }

    private fun setReminder(context: Context, key: String, time: Long) {
        val settings = AppSettings.getInstance()
        val reminder = settings.getInt(ALARM_REMINDER_FOR + key)
        if (reminder > 0) {
            val reminderTimes = Calendar.getInstance()
            reminderTimes.timeInMillis = time
            reminderTimes.add(Calendar.MINUTE, -reminder)

            val times = reminderTimes.timeInMillis
            if (System.currentTimeMillis() - times < 0L) {
                logDebug(
                    TAG,
                    "set reminder for $key, time ${convertLongToTime(times)}"
                )

                alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, PrayAlarmReceiver::class.java).apply {
                    putExtra(EXTRA_PRAYER_NAME, key)
                    putExtra(EXTRA_PRAYER_TIME, times)
                    putExtra(EXTRA_REMINDER, true)
                }

                val alarmIntent =
                    PendingIntent.getBroadcast(
                        context,
                        ALARM_REMINDER_ID,
                        intent,
                        PendingIntent.FLAG_CANCEL_CURRENT
                    )

                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    times,
                    alarmIntent
                )
            }
        }
    }

    fun cancelAlarm(context: Context) = Coroutines.main {
        if (permissionGranted(context)) {
            if (!::alarmManager.isInitialized) {
                alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            }

            val intent = Intent(context, PrayAlarmReceiver::class.java)
            val alarmIntent = PendingIntent.getBroadcast(
                context,
                ALARM_ID,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT
            )
            val intentReminder = Intent(context, PrayAlarmReceiver::class.java)
            val alarmReminderIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REMINDER_ID,
                intentReminder,
                PendingIntent.FLAG_CANCEL_CURRENT
            )
            alarmManager.cancel(alarmIntent)
            alarmManager.cancel(alarmReminderIntent)

            //REMOVE PASSIVE LOCATION RECEIVER
            val passiveIntent = Intent(context, PassiveLocationChangedReceiver::class.java)
            val locationListenerPassivePendingIntent = PendingIntent.getActivity(
                context,
                PASSIVE_LOCATION_ID,
                passiveIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            removePassiveLocationUpdates(context, locationListenerPassivePendingIntent)

            Log.e(TAG, "Alarm cancel")
        }
    }

    // END_INCLUDE(cancel_alarm)
    private fun requestPassiveLocationUpdates(context: Context, pendingIntent: PendingIntent) {
        val oneHourInMillis = 1000 * 60 * 60.toLong()
        val fiftyKinMeters: Long = 50000
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            locationManager.requestLocationUpdates(
                LocationManager.PASSIVE_PROVIDER,
                oneHourInMillis, fiftyKinMeters.toFloat(), pendingIntent
            )
        } catch (se: SecurityException) {
            Log.w("SetAlarmReceiver", se.message, se)
        }
    }

    private fun removePassiveLocationUpdates(context: Context, pendingIntent: PendingIntent) {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            locationManager.removeUpdates(pendingIntent)
        } catch (se: SecurityException) {
            Log.w("CancelAlarmReceiver", se.message, se)
        }
    }

    private fun getCalendarFromPrayerTime(
        context: Context,
        cal: Calendar,
        prayerTime: String
    ): Calendar {
        var strTime = prayerTime
        if (!DateFormat.is24HourFormat(context)) {
            val display = SimpleDateFormat("HH:mm", Locale.getDefault())
            val parse = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val date = parse.parse(strTime)
            if (date != null) strTime = display.format(date)
        }
        val time = strTime.split(":").toTypedArray()
        cal[Calendar.HOUR_OF_DAY] = Integer.valueOf(time[0])
        cal[Calendar.MINUTE] = Integer.valueOf(time[1])
        cal[Calendar.SECOND] = 0
        cal[Calendar.MILLISECOND] = 0
        return cal
    }

    private fun sendReminder(context: Context, key: String, time: Long) {
        val settings = AppSettings.getInstance()
        val id = generateRandom()
        val position = KEYS.indexOf(key)
        val title = context.resources.getStringArray(R.array.title_prayer_time)[position]

        val intent = Intent(context, DashboardActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(EXTRA_FROM_NOTIFICATION_ADHAN, true)
        }

        val reminder = settings.getInt(ALARM_REMINDER_FOR + key)
        val reminderTimes = Calendar.getInstance()
        reminderTimes.timeInMillis = time
        reminderTimes.add(Calendar.MINUTE, reminder)
        val adzanTime = convertLongToTime(reminderTimes.timeInMillis)

        val resultPendingIntent = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_ONE_SHOT
        )

        var text = "Lihat informasi waktu sholat"
        val lastLoc = settings.getString(AppSettings.Key.LAST_LOC)
        if (lastLoc.isNotEmpty()) {
            text += " di $lastLoc"
        }
        val notificationBuilder =
            NotificationCompat.Builder(context, key)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setColor(Color.TRANSPARENT)
                .setAutoCancel(true)
                .setContentTitle("Memasuki $title dalam $reminder menit ($adzanTime)")
                .setContentText(text)
                .setContentIntent(resultPendingIntent)
                .setSound(null)
                .setDefaults(0)

        val mNotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mChannel = NotificationChannel(
                "$key-reminder", "$title-reminder", NotificationManager.IMPORTANCE_LOW
            ).apply {
                lightColor = Color.GREEN
                enableLights(true)
                description = "$title reminder channel"
            }
            mNotificationManager.createNotificationChannel(mChannel)
        }

        mNotificationManager.notify(id, notificationBuilder.build())
    }

    private fun permissionGranted(context: Context): Boolean {
        return PermissionUtil.hasPermissionLocation(context)
    }
}
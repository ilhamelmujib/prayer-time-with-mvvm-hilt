package id.ilhamelmujib.prayertime.data.repository

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.CountDownTimer
import android.text.format.DateFormat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import id.ilhamelmujib.prayertime.R
import id.ilhamelmujib.prayertime.data.model.Pray
import id.ilhamelmujib.prayertime.receiver.PrayAlarmReceiver
import id.ilhamelmujib.prayertime.utils.*
import id.ilhamelmujib.prayertime.utils.praytimes.AppSettings
import id.ilhamelmujib.prayertime.utils.praytimes.PrayTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class PrayerTimeRepository @Inject
constructor(
    @ApplicationContext private val context: Context,
    private val locale: Locale,
    private val geocoder: Geocoder
) {

    val listPrayerTime: LiveData<List<Pray>> = MutableLiveData()
    val prayerTime: LiveData<Map<String, String>> = MutableLiveData()
    val countDownPrayerTime: LiveData<String> = MutableLiveData()
    val locationName: LiveData<String> = MutableLiveData()

    private lateinit var countDownTimer: CountDownTimer
    private lateinit var mLocation: Location
    private lateinit var prayerTimes: LinkedHashMap<String, String>

    private val settings = AppSettings.getInstance()
    private val is24Format = DateFormat.is24HourFormat(context)
    private val dispatcher = Dispatchers.IO

    var key = ""

    suspend fun setLocation(location: Location) = withContext(dispatcher) {
        mLocation = location
        settings.latFor = mLocation.latitude
        settings.lngFor = mLocation.longitude
    }

    suspend fun getPrayerTime() = withContext(dispatcher) {
        prayerTimes = PrayTime.getPrayerTimes(context, mLocation.latitude, mLocation.longitude)

        val prayerNames: List<String> = ArrayList(prayerTimes.keys)
        val now = Calendar.getInstance()
        now.timeInMillis = System.currentTimeMillis()

        var then = Calendar.getInstance()
        then.timeInMillis = System.currentTimeMillis()

        for (prayer in prayerNames) {
            val not = arrayOf(IMSAK, SUNRISE, SUNSET)
            if (prayer.notInString(not)) {
                val time = prayerTimes[prayer]
                if (time != null) {
                    then = getCalendarFromPrayerTime(then, time)

                    if (then.after(now)) {
                        key = prayer
                        break
                    }
                }
            }
        }

        if (key == "") key = FAJR

        val position = KEYS.indexOf(key)
        val name = context.resources.getStringArray(R.array.title_prayer_time)[position]
        val time = twoDigit(prayerTimes[key].toString())

        val mPrayerTime = mutableMapOf<String, String>().also {
            it[name] = time
        }

        prayerTime.post(mPrayerTime)
        setCountDownTimer(name, time)

        launch {
            setAlarm()
        }
    }

    suspend fun getListPrayerTime() = withContext(dispatcher) {
        val list = mutableListOf<Pray>().apply {
            for (i in 0 until prayerTimes.size) {
                val key = KEYS[i]
                if (key != SUNSET && key != IMSAK) {
                    val name = context.resources.getStringArray(R.array.title_prayer_time)[i]
                    val time = twoDigit(prayerTimes[key].toString())
                    val setting = settings.getInt(ALARM_FOR + key)
                    add(Pray(key, name, time, setting))
                }
            }
        }

        listPrayerTime.post(list)
    }

    private fun twoDigit(time: String): String {
        return if (is24Format) {
            val sb = StringBuilder().apply {
                val splitTime = time.split(":")
                for (i in splitTime.indices) {
                    append(twoDigitString(splitTime[i].toInt()))
                    append(":")
                }

            }.toString()
            return sb.removeRange(sb.length - 1, sb.length)
        } else {
            time
        }
    }

    private fun setCountDownTimer(name: String, time: String) {
        val sdf = SimpleDateFormat("HH:mm:ss", locale)
        val start = sdf.parse(currentDate("HH:mm:ss"))
        val end = if (!is24Format) {
            val parse = SimpleDateFormat("hh:mm a", locale)
            val format = parse.parse(time)
            val strTime = if (format != null) {
                sdf.format(format)
            } else {
                "00:00:00"
            }
            sdf.parse(strTime)
        } else {
            sdf.parse("$time:00")
        }

        if (start != null && end != null) {
            var difference = end.time - start.time
            if (difference < 0) {
                val max = sdf.parse("24:00:00")
                val min = sdf.parse("00:00:00")
                if (max != null && min != null)
                    difference = (max.time - start.time) + (end.time - min.time)
            }

            Coroutines.main {
                if (::countDownTimer.isInitialized) countDownTimer.cancel()

                countDownTimer = object : CountDownTimer(difference, 1000) {
                    override fun onTick(times: Long) {
                        val value = formatMilliSecondsToTime(times).notNull()
                        countDownPrayerTime.post(value)
                    }

                    override fun onFinish() {
                        countDownPrayerTime.post("Adzan $name")
                    }
                }

                countDownTimer.start()
            }
        }
    }

    private fun formatMilliSecondsToTime(milliseconds: Long): String? {
        val seconds = (milliseconds / 1000).toInt() % 60
        val minutes = (milliseconds / (1000 * 60) % 60).toInt()
        val hours = (milliseconds / (1000 * 60 * 60) % 24).toInt()
        return "${twoDigitString(hours)}:${twoDigitString(minutes)}:${twoDigitString(seconds)}"
    }

    private fun twoDigitString(number: Int): String {
        if (number == 0) {
            return "00"
        }
        return if (number / 10 == 0) {
            "0$number"
        } else number.toString()
    }

    private fun getCalendarFromPrayerTime(cal: Calendar, prayerTime: String): Calendar {
        var strTime = prayerTime
        if (!is24Format) {
            val display = SimpleDateFormat("HH:mm", locale)
            val parse = SimpleDateFormat("hh:mm a", locale)
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


    fun getCity() {
        val city = try {
            val address = geocoder.getFromLocation(mLocation.latitude, mLocation.longitude, 1)
            address[0].subAdminArea ?: address[0].locality
        } catch (e: Exception) {
            e.printStackTrace()
            context.getString(R.string.text_unknown)
        }

        locationName.post(city)
    }

    private fun setAlarm() {
        val prayAlarmReceiver = PrayAlarmReceiver()
        prayAlarmReceiver.cancelAlarm(context)
        prayAlarmReceiver.setAlarm(context)
    }
}
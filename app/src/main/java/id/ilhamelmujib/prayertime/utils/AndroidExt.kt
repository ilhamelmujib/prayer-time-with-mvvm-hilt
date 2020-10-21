package id.ilhamelmujib.prayertime.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.view.View
import android.widget.Toast
import androidx.core.content.getSystemService
import com.github.msarhan.ummalqura.calendar.UmmalquraCalendar
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

fun Context.isGPSEnable() =
    getSystemService<LocationManager>()?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false

fun String.notInString(data: Array<String>): Boolean {
    for (key in data) {
        if (this == key)
            return false
    }
    return true
}

fun currentDate(format: String = "yyyyMMddHHmmss"): String {
    val df = SimpleDateFormat(format, Locale.getDefault())
    return df.format(Date())
}

fun String?.notNull(): String {
    return this ?: ""
}

fun generateRandom() = Random().nextInt(9999 - 1000) + 1100

fun convertLongToTime(time: Long): String {
    val date = Date(time)
    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
    return format.format(date)
}

fun Context.toast(message: String) =
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

fun View.toVisible() {
    this.visibility = View.VISIBLE
}

fun View.toGone() {
    this.visibility = View.GONE
}

fun getAllDayCalendar() : ArrayList<String>{
    var mCalendar = Calendar.getInstance()
    val allDays = ArrayList<String>()
    val mFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
    mCalendar.add(Calendar.YEAR, -1)
    for(day in 0 until 366) {
        allDays.add(mFormat.format(mCalendar.time))
        mCalendar.add(Calendar.DATE, 1)
    }

    mCalendar = Calendar.getInstance()
    for(day in 0 until 366) {
        allDays.add(mFormat.format(mCalendar.time))
        mCalendar.add(Calendar.DATE, 1)
    }
    return allDays
}


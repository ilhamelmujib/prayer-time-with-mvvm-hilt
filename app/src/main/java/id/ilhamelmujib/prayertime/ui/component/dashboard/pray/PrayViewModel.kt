package id.ilhamelmujib.prayertime.ui.component.dashboard.pray

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.github.msarhan.ummalqura.calendar.UmmalquraCalendar
import dagger.hilt.android.qualifiers.ApplicationContext
import id.ilhamelmujib.prayertime.R
import id.ilhamelmujib.prayertime.data.model.Week
import id.ilhamelmujib.prayertime.data.repository.PrayerTimeRepository
import id.ilhamelmujib.prayertime.ui.base.BaseViewModel
import id.ilhamelmujib.prayertime.utils.Coroutines
import id.ilhamelmujib.prayertime.utils.getAllDayCalendar
import id.ilhamelmujib.prayertime.utils.post
import java.text.SimpleDateFormat
import java.util.*

class PrayViewModel @ViewModelInject
constructor(
    @ApplicationContext private val context: Context,
    private val prayerTimeRepository: PrayerTimeRepository
) : BaseViewModel() {
    val listWeek: LiveData<List<Week>> = MutableLiveData()
    val hijriDate: LiveData<String> = MutableLiveData()
    val loading: LiveData<Boolean> = MutableLiveData()

    val listPrayerTime = prayerTimeRepository.listPrayerTime
    val prayerTime = prayerTimeRepository.prayerTime
    val countDownPrayerTime = prayerTimeRepository.countDownPrayerTime
    val locationName = prayerTimeRepository.locationName

    var key = ""

    init {
        getCalendar()
    }

    fun getPrayerTime(location: Location) = Coroutines.io {
        loading.post(true)
        prayerTimeRepository.setLocation(location)
        prayerTimeRepository.getPrayerTime()
        key = prayerTimeRepository.key
        prayerTimeRepository.getListPrayerTime()
        prayerTimeRepository.getCity()
        loading.post(false)
    }

    @SuppressLint("SimpleDateFormat")
    private fun getCalendar() {
        val monthList = arrayOf(
            "Muharam",
            "Safar",
            "Rabi'ul Awal",
            "Rabi'ul Akhir",
            "Jumadil Awal",
            "Jumadil Akhir",
            "Rajab",
            "Sya'ban",
            "Ramadhan",
            "Syawal",
            "Dzulkaidan",
            "Dzulhijjah"
        )

        val calHijri = UmmalquraCalendar()
        val monthHijri = monthList[calHijri[Calendar.MONTH]]
        val dateHijri = "${calHijri[Calendar.DAY_OF_MONTH]} $monthHijri ${calHijri[Calendar.YEAR]}"
        hijriDate.post(dateHijri)

        val weekly = mutableListOf<Week>().apply{
            val fWeek = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
            val fDay = SimpleDateFormat("EEEE", Locale.getDefault())

            var listWeek = mutableListOf<String>()
            var isStartWeekly = false

            getAllDayCalendar().forEach {
                val newDate: Date = fWeek.parse(it)
                val day = fDay.format(newDate)

                if (day == context.getString(R.string.text_monday) && !isStartWeekly) {
                    isStartWeekly = true
                    listWeek.add(it)
                }

                if (day != context.getString(R.string.text_monday) && isStartWeekly) listWeek.add(it)

                if (listWeek.size == 7) {
                    add(Week(listWeek[0], listWeek[1], listWeek[2], listWeek[3], listWeek[4], listWeek[5], listWeek[6]))
                    isStartWeekly = false
                    listWeek = mutableListOf()
                }
            }
        }

        listWeek.post(weekly.toList())


    }

}
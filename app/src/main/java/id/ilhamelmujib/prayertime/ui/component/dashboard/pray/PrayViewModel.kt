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
import id.ilhamelmujib.prayertime.data.model.Weekly
import id.ilhamelmujib.prayertime.data.repository.PrayerTimeRepository
import id.ilhamelmujib.prayertime.ui.base.BaseViewModel
import id.ilhamelmujib.prayertime.utils.Coroutines
import id.ilhamelmujib.prayertime.utils.currentDate
import id.ilhamelmujib.prayertime.utils.getAllDayCalendar
import id.ilhamelmujib.prayertime.utils.post
import java.text.SimpleDateFormat
import java.util.*

class PrayViewModel @ViewModelInject
constructor(
    @ApplicationContext private val context: Context,
    private val prayerTimeRepository: PrayerTimeRepository
) : BaseViewModel() {
    val listWeekly: LiveData<List<Weekly>> = MutableLiveData()
    val hijriMonth: LiveData<String> = MutableLiveData()
    val loading: LiveData<Boolean> = MutableLiveData()

    val listPrayerTime = prayerTimeRepository.listPrayerTime
    val prayerTime = prayerTimeRepository.prayerTime
    val countDownPrayerTime = prayerTimeRepository.countDownPrayerTime
    val locationName = prayerTimeRepository.locationName

    var key = ""

    init {
        getHijriMonth()
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
    private fun getHijriMonth() {
        val monthList = context.resources.getStringArray(R.array.month_hijri)
        val calHijri = UmmalquraCalendar()
        val monthHijri = monthList[calHijri[Calendar.MONTH]]
        val dateHijri = "$monthHijri ${calHijri[Calendar.YEAR]}"
        hijriMonth.post(dateHijri)
    }

    private fun getCalendar(){
        val weekly = mutableListOf<Weekly>().apply{
            val fWeek = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
            val fDay = SimpleDateFormat("EEEE", Locale.getDefault())

            var listWeek = mutableListOf<String>()
            var isStartWeekly = false

            getAllDayCalendar().forEach {
                val date: Date = fWeek.parse(it)
                val day = fDay.format(date)

                if (day == context.getString(R.string.text_monday) && !isStartWeekly) {
                    isStartWeekly = true
                    listWeek.add(it)
                }

                if (day != context.getString(R.string.text_monday) && isStartWeekly) listWeek.add(it)

                if (listWeek.size == 7) {

                    add(Weekly(listWeek[0], listWeek[1], listWeek[2], listWeek[3], listWeek[4], listWeek[5], listWeek[6], toHijri(listWeek[0]), toHijri(listWeek[1]), toHijri(listWeek[2]), toHijri(listWeek[3]), toHijri(listWeek[4]), toHijri(listWeek[5]), toHijri(listWeek[6]) ,isSelected = listWeek.contains(
                        currentDate("EEEE, dd MMMM yyyy")
                    )))
                    isStartWeekly = false
                    listWeek = mutableListOf()
                }


            }
        }

        listWeekly.post(weekly.toList())

    }

    private fun toHijri(date: String) : String{
        val format = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        val date: Date = format.parse(date)
        val cal = Calendar.getInstance()
        cal.time = date

        val calHijri = UmmalquraCalendar()
        calHijri.time = cal.time
        val dayHijri = "${calHijri[Calendar.DAY_OF_MONTH]}"
        return dayHijri
    }

}
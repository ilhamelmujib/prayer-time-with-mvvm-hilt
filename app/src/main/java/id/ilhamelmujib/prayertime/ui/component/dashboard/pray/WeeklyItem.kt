package id.ilhamelmujib.prayertime.ui.component.dashboard.pray

import android.widget.LinearLayout
import com.xwray.groupie.databinding.BindableItem
import id.ilhamelmujib.prayertime.R
import id.ilhamelmujib.prayertime.data.model.Weekly
import id.ilhamelmujib.prayertime.databinding.ItemWeeklyBinding
import id.ilhamelmujib.prayertime.utils.currentDate
import java.text.SimpleDateFormat
import java.util.*

class WeeklyItem(
    private var mWeekly: Weekly
) : BindableItem<ItemWeeklyBinding>() {

    override fun getLayout() = R.layout.item_weekly

    override fun bind(viewBinding: ItemWeeklyBinding, position: Int) {
        viewBinding.apply {
            weekly = mWeekly

            tvMon.text = toDate(mWeekly.greMon)
            tvTue.text = toDate(mWeekly.greTue)
            tvWed.text = toDate(mWeekly.greWed)
            tvThu.text = toDate(mWeekly.greThu)
            tvFri.text = toDate(mWeekly.greFri)
            tvSat.text = toDate(mWeekly.greSat)
            tvSun.text = toDate(mWeekly.greSun)

            tvMonHij.text = mWeekly.hijMon
            tvTueHij.text = mWeekly.hijTue
            tvWedHij.text = mWeekly.hijWed
            tvThuHij.text = mWeekly.hijThu
            tvFriHij.text = mWeekly.hijFri
            tvSatHij.text = mWeekly.hijSat
            tvSunHij.text = mWeekly.hijSun

            linMon.tag = mWeekly.greMon
            linTue.tag = mWeekly.greTue
            linWed.tag = mWeekly.greWed
            linThu.tag = mWeekly.greThu
            linFri.tag = mWeekly.greFri
            linSat.tag = mWeekly.greSat
            linSun.tag = mWeekly.greSun
            selectionDateNow(linMon)
            selectionDateNow(linTue)
            selectionDateNow(linWed)
            selectionDateNow(linThu)
            selectionDateNow(linFri)
            selectionDateNow(linSat)
            selectionDateNow(linSun)
        }

    }

    private fun selectionDateNow(lin: LinearLayout){
        val opacity = if (lin.tag.toString() == currentDate("EEEE, dd MMMM yyyy")) 1.0 else 0.36
        lin.alpha = opacity.toFloat()
    }

    private fun toDate(date: String) : String{
        val format = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        val dateFormat: Date = format.parse(date)
        val fDate = SimpleDateFormat("dd", Locale.getDefault())
        val date = fDate.format(dateFormat)
        return date
    }

}
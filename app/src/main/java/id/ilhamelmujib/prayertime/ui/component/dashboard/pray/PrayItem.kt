package id.ilhamelmujib.prayertime.ui.component.dashboard.pray

import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.xwray.groupie.databinding.BindableItem
import id.ilhamelmujib.prayertime.R
import id.ilhamelmujib.prayertime.data.model.Pray
import id.ilhamelmujib.prayertime.databinding.ItemPrayBinding

class PrayItem(
    private var mPray: Pray,
    private val key: String
) : BindableItem<ItemPrayBinding>() {

    override fun getLayout() = R.layout.item_pray
    private val icon = arrayOf(
        R.drawable.png_time_fajr,
        R.drawable.png_time_sunrise,
        R.drawable.png_time_dhuhr,
        R.drawable.png_time_asr,
        R.drawable.png_time_maghrib,
        R.drawable.png_time_isha
    )

    override fun bind(viewBinding: ItemPrayBinding, position: Int) {
        viewBinding.apply {
            pray = mPray
            root.apply {
                val opacity = if (key == mPray.key) 1.0 else 0.36
                ivIcon.apply {
                    setImageResource(icon[position])
                    alpha = opacity.toFloat()
                }
                ivSetting.alpha = opacity.toFloat()
                tvName.alpha = opacity.toFloat()
                tvTime.alpha = opacity.toFloat()

            }
        }
    }


}
package id.ilhamelmujib.prayertime.ui.component.dashboard.pray

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import dagger.hilt.android.AndroidEntryPoint
import id.ilhamelmujib.prayertime.R
import id.ilhamelmujib.prayertime.data.model.Pray
import id.ilhamelmujib.prayertime.ui.base.BaseFragment
import id.ilhamelmujib.prayertime.ui.component.DashboardActivity
import id.ilhamelmujib.prayertime.utils.*
import id.ilhamelmujib.prayertime.utils.praytimes.LocationProvider
import kotlinx.android.synthetic.main.fragment_pray.*

@AndroidEntryPoint
class PrayFragment : BaseFragment(), LocationProvider.LocationProviderCallback {

    private val viewModel: PrayViewModel by viewModels()
    private lateinit var mLocationProvider: LocationProvider
    private lateinit var location: Location
    private val mAdapter by lazy { GroupAdapter<ViewHolder>() }

    override val layoutId: Int get() = R.layout.fragment_pray

    private val activity by lazy {
        requireActivity() as DashboardActivity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initLocation()
    }

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)
        if (!::mLocationProvider.isInitialized) {
            initLocation()
        }
        if (childFragment.tag == LOCATION_FRAGMENT) {
            mLocationProvider.getLocation(this)
        }
    }

    override fun onLocationChanged(location: Location) {
        this.location = location
        getPrayerTime()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        observePrayerTime()
    }

    private fun initLocation() {
        mLocationProvider = LocationProvider()
        childFragmentManager.findFragmentByTag(LOCATION_FRAGMENT)
            .let { locationFragment ->
                if (locationFragment == null) {
                    childFragmentManager
                        .beginTransaction()
                        .add(mLocationProvider, LOCATION_FRAGMENT)
                        .commit()
                }
            }
    }

    @SuppressLint("SetTextI18n")
    private fun observePrayerTime() {
        viewLifecycleOwner.run {
            var countdown = ""
            var prayerName = ""

            observe(viewModel.loading, ::showLoading)
            observe(viewModel.prayerTime) {
                var prayerTime: String? = null
                for (key in it.keys) {
                    prayerName = key
                    prayerTime = it[key]
                }

                setCountDownAndPrayerName(countdown)
                tvPrayerTime.text = "$prayerName $prayerTime"
            }

            observe(viewModel.countDownPrayerTime) {
                countdown = it
                setCountDownAndPrayerName(countdown)
            }

            observe(viewModel.locationName) {
                tvCurrentLocation.text = it
            }

            observe(viewModel.listPrayerTime, ::initPray)
            observe(viewModel.hijriDate) {
                tvDateHijri.text = it
            }
        }
    }

    private fun setCountDownAndPrayerName(value: String) {
        tvCountDownPrayer.text = value
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            progressBar.toVisible()
            contentLayout.toGone()
        } else {
            progressBar.toGone()
            contentLayout.toVisible()
        }
    }

    private fun initView() {
        progressBar.toVisible()
        tvDateGregorian.text = currentDate("EEEE, dd MMMM yyyy")

        rvPrayerTimes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = mAdapter
        }
    }

    override fun onLocationError(msg: String) {
        activity.run {
            toast(msg)
            removeFragment()
        }
    }

    override fun onGPSisDisable() {
        activity.toast(getString(R.string.msg_error_gps_disable))
        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }

    private fun DashboardActivity.removeFragment() {
        removeFragment(this@PrayFragment.javaClass.name)
        prayFragment = null
    }

    private fun initPray(list: List<Pray>) {
        val listItem = list.map {
            PrayItem(it, viewModel.key)
        }
        mAdapter.apply {
            clear()
            addAll(listItem)
        }
    }

    private fun getPrayerTime() {
        viewModel.getPrayerTime(location)
    }

    override fun onResume() {
        super.onResume()
        if (PermissionUtil.hasPermissionLocation(requireContext())) {
            mLocationProvider.getLocation(this)
        }
    }
}
package id.ilhamelmujib.prayertime.ui.component

import android.os.Bundle
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import id.ilhamelmujib.prayertime.R
import id.ilhamelmujib.prayertime.ui.base.BaseActivity
import id.ilhamelmujib.prayertime.ui.component.dashboard.home.HomeFragment
import id.ilhamelmujib.prayertime.ui.component.dashboard.pray.PrayFragment
import id.ilhamelmujib.prayertime.ui.component.dashboard.profile.ProfileFragment
import id.ilhamelmujib.prayertime.ui.component.dashboard.quran.QuranFragment
import id.ilhamelmujib.prayertime.utils.EXTRA_FROM_NOTIFICATION_ADHAN
import kotlinx.android.synthetic.main.activity_dashboard.*

@AndroidEntryPoint
class DashboardActivity : BaseActivity() {

    private val homeFragment = HomeFragment()
    var prayFragment: PrayFragment? = PrayFragment()
    private val quranFragment = QuranFragment()
    private val profileFragment = ProfileFragment()
    private val fragmentManager = supportFragmentManager
    private var activeFragment: Fragment = homeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        if (intent.getBooleanExtra(EXTRA_FROM_NOTIFICATION_ADHAN, false)) {
            addFragment(prayFragment)
            toPrayerFragment()
        } else {
            addFragment(homeFragment)
        }

        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            addFragment(
                when (item.itemId) {
                    R.id.navHome -> homeFragment
                    R.id.navPray -> prayFragment
                    R.id.navQuran -> quranFragment
                    R.id.navProfile -> profileFragment
                    else -> activeFragment
                }
            )
            true
        }
    }

    private fun addFragment(fragment: Fragment?) {
        fragment?.let {
            if (fragmentManager.findFragmentByTag(it.javaClass.name) == null) {
                fragmentManager
                    .beginTransaction()
                    .add(R.id.container, it, it.javaClass.name)
                    .show(it)
                    .commit()
            }

            fragmentManager
                .beginTransaction()
                .hide(activeFragment)
                .show(it)
                .commit()

            activeFragment = it
        }
    }

    private fun toHomeFragment() {
        bottomNavigation.selectedItemId = R.id.navHome
    }

    private fun toPrayerFragment() {
        bottomNavigation.selectedItemId = R.id.navPray
    }

    fun removeFragment(tag: String) {
        fragmentManager.findFragmentByTag(tag)?.let {
            fragmentManager.beginTransaction().remove(it).commit()
        }
        toHomeFragment()
    }

}
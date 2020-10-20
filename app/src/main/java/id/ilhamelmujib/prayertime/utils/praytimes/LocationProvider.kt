package id.ilhamelmujib.prayertime.utils.praytimes

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*
import id.ilhamelmujib.prayertime.R
import id.ilhamelmujib.prayertime.utils.PermissionUtil
import id.ilhamelmujib.prayertime.utils.REQUEST_LOCATION
import id.ilhamelmujib.prayertime.utils.isGPSEnable
import id.ilhamelmujib.prayertime.utils.requestPermissionLocation

class LocationProvider : Fragment() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private lateinit var mLocationProviderCallback: LocationProviderCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    fun getLocation(callback: LocationProviderCallback) {
        try {
            mLocationProviderCallback = callback
            if (!requireContext().isGPSEnable()) {
                mLocationProviderCallback.onGPSisDisable()
                return
            }

            if (PermissionUtil.hasPermissionLocation(
                    requireContext()
                )
            ) {
                getLocationUpdates()
            } else {
                requestPermissionLocation()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        locationRequest = LocationRequest()
        locationRequest.interval = 50000
        locationRequest.fastestInterval = 50000
        locationRequest.smallestDisplacement = 170f //170 m = 0.1 mile
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY //according to your app

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult?) {
                if (result != null) {
                    mLocationProviderCallback.onLocationChanged(result.lastLocation)
                } else {
                    mLocationProviderCallback.onLocationError("Location result null")
                }
            }
        }
    }

    // Start location updates
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (::fusedLocationClient.isInitialized)
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
    }

    // Stop location updates
    private fun stopLocationUpdates() {
        if (::fusedLocationClient.isInitialized)
            fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // Stop receiving location update when activity not visible/foreground
    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    // Start receiving location update when activity  visible/foreground
    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_LOCATION) {
            if (PermissionUtil.verifyPermissions(
                    grantResults
                )
            ) {
                getLocationUpdates()
            } else {
                mLocationProviderCallback.onLocationError(getString(R.string.msg_permission_denied))
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    interface LocationProviderCallback {
        fun onLocationChanged(location: Location)
        fun onLocationError(msg: String)
        fun onGPSisDisable()
    }
}
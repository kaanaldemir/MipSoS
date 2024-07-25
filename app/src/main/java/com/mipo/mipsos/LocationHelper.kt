package com.mipo.mipsos

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.core.content.ContextCompat

class LocationHelper(private val context: Context, private val locationTextView: TextView) {

    private var lastLocation: Location? = null
    private val handler = Handler(Looper.getMainLooper())

    fun getLocation(callback: (Location?) -> Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permissions are not granted, handle it accordingly
            locationTextView.text = "Location permissions are not granted."
            callback(null)
            return
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val latitude = location.latitude
                val longitude = location.longitude

                locationTextView.text = "Lat: $latitude Long: $longitude"

                lastLocation = location
                locationManager.removeUpdates(this)
                callback(location)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {
                locationTextView.text = "Searching location..."
            }

            override fun onProviderDisabled(provider: String) {
                locationTextView.text = "Please Enable Location!"
            }
        }

        val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (lastKnownLocation != null) {
            locationListener.onLocationChanged(lastKnownLocation)
            callback(lastKnownLocation)
        } else {
            locationTextView.text = "Searching location..."
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                60000, // 1 minute
                10f,
                locationListener
            )
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                60000, // 1 minute
                10f,
                locationListener
            )
        }
    }

    fun startLocationUpdates() {
        handler.post(object : Runnable {
            override fun run() {
                getLocation { location ->
                    val latitude = location?.latitude ?: 0.0
                    val longitude = location?.longitude ?: 0.0
                    locationTextView.text = "Lat: $latitude Long: $longitude"
                }
                handler.postDelayed(this, 5 * 60 * 1000) // 5 minutes
            }
        })
    }

    fun stopLocationUpdates() {
        handler.removeCallbacksAndMessages(null)
    }

    fun getLastLocation(): Location? {
        return lastLocation
    }

    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}

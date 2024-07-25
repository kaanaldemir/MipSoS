package com.mipo.mipsos

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*

class LocationHelper(private val context: Context, private val locationTextView: TextView) {

    private var lastLocation: Location? = null
    private val handler = Handler(Looper.getMainLooper())
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private lateinit var locationCallback: LocationCallback

    fun getLocation(callback: (Location?) -> Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permissions are not granted, handle it accordingly
            locationTextView.text = "Location permissions are not granted."
            callback(null)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                lastLocation = location
                val latitude = location.latitude
                val longitude = location.longitude
                locationTextView.text = "Lat: $latitude Long: $longitude"
                callback(location)
            } else {
                // If the last location is null, request a location update
                requestLocationUpdate(callback)
            }
        }.addOnFailureListener {
            // If there's an error getting the last location, request a location update
            requestLocationUpdate(callback)
        }
    }

    private fun requestLocationUpdate(callback: (Location?) -> Unit) {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    lastLocation = location
                    val latitude = location.latitude
                    val longitude = location.longitude
                    locationTextView.text = "Lat: $latitude Long: $longitude"
                    callback(location)
                }
                fusedLocationClient.removeLocationUpdates(this)
            }
        }

        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
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
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    fun getLastLocation(): Location? {
        return lastLocation
    }

    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    }
}

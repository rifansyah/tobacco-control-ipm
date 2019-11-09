package com.tribute.app.pelajarmemantau

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var marker: Marker
    private lateinit var latLng: LatLng

    private var mLocationManager: LocationManager? = null
    private var mLocationManager2: LocationManager? = null
    private val mLocationListener = object : LocationListener {
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

        }

        override fun onProviderEnabled(provider: String?) {

        }

        override fun onProviderDisabled(provider: String?) {
            showDialogIfLocationIsDisabled()
        }


        override fun onLocationChanged(location: Location) {
            setLocationAndMarker(LatLng(location?.latitude, location?.longitude))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap


        latLng = LatLng(-7.782834, 110.368279)
        marker = mMap.addMarker(MarkerOptions().position(latLng).title("Klik untuk pilih lokasi"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
        marker.showInfoWindow()

        mMap.setOnInfoWindowClickListener {
            val returnIntent = Intent()
            returnIntent.putExtra("result", latLng)
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
        }

        mMap.setOnMarkerClickListener {
            val returnIntent = Intent()
            returnIntent.putExtra("result", latLng)
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
            return@setOnMarkerClickListener true
        }

        mMap.setOnMapClickListener {
            setLocationAndMarker(it)
        }
    }

    private fun setLocationAndMarker(latLng: LatLng) {
        this.latLng = latLng
        marker.remove()
        marker = mMap.addMarker(MarkerOptions().position(latLng).title("Klik untuk pilih lokasi"))
        marker.showInfoWindow()
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    private fun getLocation() {
        checkPermission()

        mLocationManager = getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager

        mLocationManager?.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER, 3000,
            5000.0.toFloat(), mLocationListener)

        mLocationManager2 = getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager

        mLocationManager2?.requestLocationUpdates(
            LocationManager.GPS_PROVIDER, 3000,
            5000.0.toFloat(), mLocationListener)

    }

    fun checkPermission() {
        if ( ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {

            ActivityCompat.requestPermissions( this, arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION),
                0 )
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onStart() {
        super.onStart()
        getLocation()
    }

    private fun stopLocationUpdates() {
        mLocationManager?.removeUpdates(mLocationListener)
        mLocationManager2?.removeUpdates(mLocationListener)
    }
}

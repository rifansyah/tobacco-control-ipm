package com.tribute.app.tobaccocontrol_ipm

import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.android.synthetic.main.activity_main.*
import android.widget.Toast
import com.google.firebase.firestore.FieldValue
import com.google.firestore.v1.DocumentTransform
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    val homeFragment = HomeFragment()
    val profileFragment = ProfileFragment()

    private var latitude : Double? = null
    private var longitude : Double? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var doubleBackToExitPressedOnce = false
    private var mLocationManager : LocationManager? = null

    private val mLocationListener = object : LocationListener {
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

        }

        override fun onProviderEnabled(provider: String?) {
        }

        override fun onProviderDisabled(provider: String?) {
            showDialogIfLocationIsDisabled()
        }


        override fun onLocationChanged(location: Location) {

        }
    }

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                replaceFragmentWithoutBackStack(homeFragment, R.id.fragment_container)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_report -> {
                startActivity(Intent(this, ReportActivity::class.java))
//                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_profile -> {
                replaceFragmentWithoutBackStack(profileFragment, R.id.fragment_container)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        checkPermission()

        addFragment(HomeFragment(), R.id.fragment_container)
    }

    fun getLocation() {
        //check permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission(this)
        }
        //get location
        mLocationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        mLocationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000,
            5000.0.toFloat(), mLocationListener);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    fun checkPermission() {
        if ( ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {

            ActivityCompat.requestPermissions( this, arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION),
                0 )
        }
    }

    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()

            return
        }

        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Klik kembali satu kali lagi untuk keluar", Toast.LENGTH_SHORT).show()

        Handler().postDelayed(Runnable { doubleBackToExitPressedOnce = false }, 2000)
    }

    companion object {
        const val MYPREF = "SharedPreference"
        const val ROLEUSER = "UserRole"
        const val USER = "USER"
        const val ADMIN = "ADMIN"
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
    }

}

package com.tribute.app.tobaccocontrol_ipm


import android.app.Dialog
import android.app.PendingIntent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.fragment_report_description.*
import java.io.File
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.widget.ImageView
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.location.*
import android.os.Build
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.lang.Exception
import java.util.*


class ReportDescriptionFragment : Fragment(), View.OnClickListener {

    private lateinit var postImage : ImageView
    private lateinit var dest : File

    private val MAP_BUTTON_REQUEST_CODE = 1

    private var latitude : Double? = null
    private var longitude : Double? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var progressDialog : Dialog? = null

    private var mLocationManager: LocationManager? = null
    private var mLocationManager2: LocationManager? = null
    private val mLocationListener = object : LocationListener {
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

        }

        override fun onProviderEnabled(provider: String?) {

        }

        override fun onProviderDisabled(provider: String?) {
            progressDialog?.dismiss()
            context?.showDialogIfLocationIsDisabled()
        }


        override fun onLocationChanged(location: Location) {
            latitude = location?.latitude
            longitude = location?.longitude
            try {
                val geocoder = Geocoder(context!!, Locale.getDefault());
                val addresses = geocoder.getFromLocation(latitude!!, longitude!!, 1);
                cityName = addresses.get(0).locality
                provinceName = addresses.get(0).adminArea
            } catch (e: Exception) {
                activity?.onBackPressed()
                progressDialog?.dismiss()
                context?.onSnackNotif(postImage, "Gagal mengambil lokasi, periksa gps kamu")
            }

            tv_location.text = ("$cityName, $provinceName")
            progressDialog?.dismiss()
        }
    }

    var cityName = ""
    var provinceName = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_report_description, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressDialog = context?.showProgressDialog()
        progressDialog?.findViewById<TextView>(R.id.tv_upload_progress)?.text = "Sedang mengambil data lokasi kamu, mohon menunggu"
        progressDialog?.show()
        progressDialog?.findViewById<TextView>(R.id.tv_cancel)?.setOnClickListener {
            progressDialog?.dismiss()
            activity?.onBackPressed()
        }

        postImage = view.findViewById(R.id.iv_post_image) as ImageView

        setupOnClick()
        setImage(view)

        getLocation()
    }

    fun getLocation() {
        //check permission
        checkPermission()

        mLocationManager = context?.getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager

        mLocationManager?.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER, 3000,
            5000.0.toFloat(), mLocationListener)

        mLocationManager2 = context?.getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager

        mLocationManager2?.requestLocationUpdates(
            LocationManager.GPS_PROVIDER, 3000,
            5000.0.toFloat(), mLocationListener)

    }
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context!!)
//
//        checkPermission()
//
//        fusedLocationClient.lastLocation
//            .addOnSuccessListener { location : Location? ->
//                latitude = location?.latitude
//                longitude = location?.longitude
//                try {
//                    val geocoder = Geocoder(context!!, Locale.getDefault());
//                    val addresses = geocoder.getFromLocation(latitude!!, longitude!!, 1);
//                    cityName = addresses.get(0).locality
//                    provinceName = addresses.get(0).adminArea
//                } catch (e: Exception) {
//                    activity?.onBackPressed()
//                    progressDialog?.dismiss()
//                    context?.onSnackNotif(postImage, "Gagal mengambil lokasi, periksa gps kamu")
//                }
//
//                tv_location.text = ("$cityName, $provinceName")
//                progressDialog?.dismiss()
//            }.addOnFailureListener {
//                activity?.onBackPressed()
//                progressDialog?.dismiss()
//                context?.onSnackNotif(postImage, "Gagal mengambil lokasi, periksa gps kamu")
//            }
    fun checkPermission() {
        if ( ContextCompat.checkSelfPermission( context!!, android.Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {

            ActivityCompat.requestPermissions( activity!!, arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION),
                0 )
        }
    }

    fun setupOnClick() {
        btn_continue.setOnClickListener(this)
        ll_location.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.btn_continue -> {
                if (isValid()) gotoNextFragment()
            }
        }
    }

    fun setImage(v: View) {
        if (dest.exists()) {

            val bitmap = BitmapFactory.decodeFile(dest.getAbsolutePath())

            val ei = ExifInterface(dest.path)
            val orientation = ei.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )

            var rotatedBitmap: Bitmap? = null
            when (orientation) {

                ExifInterface.ORIENTATION_ROTATE_90 -> rotatedBitmap = rotateImage(bitmap, 90.toFloat())

                ExifInterface.ORIENTATION_ROTATE_180 -> rotatedBitmap = rotateImage(bitmap, 180.toFloat())

                ExifInterface.ORIENTATION_ROTATE_270 -> rotatedBitmap = rotateImage(bitmap, 270.toFloat())

                ExifInterface.ORIENTATION_NORMAL -> rotatedBitmap = bitmap
                else -> rotatedBitmap = bitmap
            }

            postImage.setImageBitmap(rotatedBitmap)

        }
    }

    fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }

    fun isValid() : Boolean {
        var valid = true

        if (tv_location.text.toString().isEmpty()) {
            valid = false
            tv_location.error = "Masukkan lokasi"
        }

        if (et_description.text.toString().isEmpty()) {
            valid = false
            et_description.error = "Deskripsi tidak boleh kosong"
        }

        return valid
    }

    fun gotoNextFragment() {
        val reportReviewFragment = ReportReviewFragment()

        val description = et_description.text.toString()

        val bundle = Bundle()
        bundle.putDouble("latitude", latitude!!) // Put anything what you want
        bundle.putDouble("longitude", longitude!!) // Put anything what you want
        bundle.putString("dest", dest.path) // Put anything what you want
        bundle.putString("city", cityName) // Put anything what you want
        bundle.putString("province", provinceName) // Put anything what you want
        bundle.putString("description", description) // Put anything what you want

        reportReviewFragment.setArguments(bundle)

        (activity as AppCompatActivity).replaceFragment(reportReviewFragment, R.id.fl_container)
    }

    fun setDestFile(dest: File) {
        this.dest = dest
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

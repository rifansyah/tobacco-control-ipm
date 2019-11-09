package com.tribute.app.pelajarmemantau


import android.app.*
import android.content.Intent
import android.os.Bundle
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
import android.graphics.Matrix
import android.location.*
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.maps.model.LatLng
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*


class ReportDescriptionFragment : Fragment(), View.OnClickListener {

    private lateinit var postImage : ImageView
    private lateinit var dest : File

    private val MAP_BUTTON_REQUEST_CODE = 1

    private var latLng : LatLng? = null

    private var cityName = ""
    private var provinceName = ""
    private var date = ""
    private var time = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_report_description, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postImage = view.findViewById(R.id.iv_post_image) as ImageView

        setupOnClick()
        setImage(view)
    }

    private fun setupOnClick() {
        btn_continue.setOnClickListener(this)
        ll_location.setOnClickListener(this)
        ll_date_time.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.btn_continue -> {
                if (isValid()) gotoNextFragment()
            }
            R.id.ll_location -> {
                startActivityForResult(Intent(context, MapsActivity::class.java), MAP_BUTTON_REQUEST_CODE)
            }
            R.id.ll_date_time -> {
                showDatePicker()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        progressbar.visibility = View.VISIBLE
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == MAP_BUTTON_REQUEST_CODE) {
                try {
                    latLng = data?.extras?.get("result") as LatLng

                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(latLng!!.latitude, latLng!!.longitude, 1)
                    cityName = addresses[0].locality
                    provinceName = addresses[0].adminArea

                    tv_location.text = ("$cityName, $provinceName")
                } catch (e: Exception) {
                    context?.onSnackNotif(postImage, "Gagal mengambil lokasi, periksa gps kamu")
                }
            }
        }
        progressbar.visibility = View.GONE
    }

    private fun setImage(v: View) {
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

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }

    private fun isValid() : Boolean {
        var valid = true

        if (tv_location.text.toString().isEmpty()) {
            valid = false
            tv_location.error = "Masukkan lokasi"
        }

        if (et_description.text.toString().isEmpty()) {
            valid = false
            et_description.error = "Deskripsi tidak boleh kosong"
        }

        if (date.isEmpty() || time.isEmpty()) {
            valid = false
            context?.onSnackNotif(tv_date_time, "Masukkan waktu pelanggaran")
            return valid
        }

        if (latLng == null) {
            valid = false
            context?.onSnackNotif(tv_date_time, "Pilih lokasi terlebih dahulu")
            return valid
        }

        return valid
    }

    private fun gotoNextFragment() {
        val reportReviewFragment = ReportReviewFragment()

        val description = et_description.text.toString()

        val bundle = Bundle()
        bundle.putString("time", time)
        bundle.putString("date", date)
        bundle.putString("dest", dest.path)
        bundle.putString("city", cityName)
        bundle.putString("province", provinceName)
        bundle.putString("description", description)
        bundle.putDouble("latitude", latLng!!.latitude)
        bundle.putDouble("longitude", latLng!!.longitude)
        bundle.putString("additionalLocationInfo", et_location_name.text.toString())
        bundle.putString("violationPlace", spinner_violation_place.selectedItem.toString())
        bundle.putString("violationKind", spinner_violation_kind.selectedItem.toString())

        reportReviewFragment.setArguments(bundle)

        (activity as AppCompatActivity).replaceFragment(reportReviewFragment, R.id.fl_container)
    }

    fun setDestFile(dest: File) {
        this.dest = dest
    }

    private fun showDatePicker() {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(context,
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                showTimePicker(year, monthOfYear, dayOfMonth) }, year, month, day)
        datePickerDialog.show()
    }

    private fun showTimePicker(year: Int, monthOfYear: Int, dayOfMonth: Int) {
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR)
        val minute = c.get(Calendar.MINUTE)
        val timePickerDialog = TimePickerDialog(context,
            TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
                String
                tv_date_time.text = getDateTimeFormatted(year, monthOfYear, dayOfMonth, hourOfDay, minute) }, hour, minute, true)
        timePickerDialog.show()
    }

    private fun getDateTimeFormatted(year: Int, month: Int, day: Int, hour: Int, minute: Int) : String {
        val c = Calendar.getInstance()
        c.set(Calendar.YEAR, year)
        c.set(Calendar.MONTH, month)
        c.set(Calendar.DAY_OF_MONTH, day)
        c.set(Calendar.HOUR, hour)
        c.set(Calendar.MINUTE, minute)

        val pattern = "dd MMM yyyy - HH mm"
        val formatter = SimpleDateFormat(pattern)

        val patternDate = "dd MMM yyyy"
        val formatterDate = SimpleDateFormat(patternDate)
        date = formatterDate.format(c.time)

        val patternTime = "HH mm"
        val formatterTime = SimpleDateFormat(patternTime)
        time = formatterTime.format(c.time)

        return formatter.format(c.time)
    }
}

package com.tribute.app.tobaccocontrol_ipm


import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.log.logcat
import io.fotoapparat.log.loggers
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.selector.*
import io.fotoapparat.view.CameraView
import kotlinx.android.synthetic.main.fragment_report_take_photo.*
import android.widget.SeekBar
import io.fotoapparat.configuration.UpdateConfiguration
import android.widget.CompoundButton
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat.setRotation
import android.R.attr.bitmap
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import com.tribute.app.tobaccocontrol_ipm.RealPathUtil.*
import io.fotoapparat.result.BitmapPhoto
import io.fotoapparat.result.WhenDoneListener
import io.fotoapparat.result.PhotoResult
import io.fotoapparat.result.transformer.scaled
import kotlinx.android.synthetic.main.activity_report.*
import java.io.File


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 *
 */
class ReportTakePhotoFragment : Fragment(), View.OnClickListener {

    private val LOGGING_TAG = "Fotoapparat"
    private val REQUEST_GALLERY = 1

    var fotoapparat: Fotoapparat? = null

    val permissions = arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE)

    var flash = false

    private var photoPath = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_report_take_photo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        createFotoapparat()
        zoomSeekBar()
        setOnClick()
    }

    fun setOnClick() {
        iv_flash.setOnClickListener(this)
        iv_takephoto.setOnClickListener(this)
        iv_gallery.setOnClickListener(this)
    }

    private fun createFotoapparat(){

        val cameraConfiguration = CameraConfiguration(
            focusMode = firstAvailable(              // (optional) use the first focus mode which is supported by device
                continuousFocusPicture(),
                autoFocus(),                       // if continuous focus is not available on device, auto focus will be used
                fixed()                            // if even auto focus is not available - fixed focus mode will be used
            )
        )

        fotoapparat = Fotoapparat(
            context = context!!,
            view = camera_view,
            scaleType = ScaleType.CenterCrop,
            lensPosition = back(),
            focusView = focusView,
            cameraConfiguration = cameraConfiguration,
            logger = loggers(
                logcat()
            ),
            cameraErrorCallback = { error ->
                println("Recorder errors: $error")
            }
        )

        fotoapparat!!.start()

    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.iv_flash -> {
                flash = !flash
                fotoapparat?.updateConfiguration(
                    UpdateConfiguration.builder()
                        .flash(
                            if (flash) torch() else off()
                        )
                        .build()
                )

                if(flash) iv_flash.setImageResource(R.drawable.ic_flash_on)
                else iv_flash.setImageResource(R.drawable.ic_flash)
            }

            R.id.iv_takephoto -> {
                takePhoto()
//                (activity as AppCompatActivity).replaceFragment(ReportDescriptionFragment(), R.id.fl_container)
            }

            R.id.iv_gallery -> {
                selectImageInAlbum(REQUEST_GALLERY, activity!!)
            }
        }
    }

    private fun zoomSeekBar() {

        sb_zoom.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                fotoapparat!!.setZoom(progress / seekBar?.max?.toFloat()!!)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
//
//            fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
//
//            }
        })
    }

    private fun takePhoto() {
        val filename = "Pictures/tb-ipm/"
        val sd = Environment.getExternalStorageDirectory()
        var dest = File(sd, filename)
        var success = true

        if (hasNoPermissions()) {
            requestPermission()
        }else{
            if (!dest.exists()) {
                success = dest.mkdirs()
            }
            if (success) {
                dest = File(dest, "foto.png")
                fotoapparat
                    ?.takePicture()
                    ?.saveToFile(dest)
                    ?.whenAvailable {
                        gotoNextFragment(dest)
                    }
            }
        }
    }

    fun selectImageInAlbum(requestGallery: Int, activity: Activity) {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        if (intent.resolveActivity(activity.packageManager) != null) {
            startActivityForResult(
                Intent.createChooser(intent, "Select Image"),
                requestGallery
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == AppCompatActivity.RESULT_OK) {

            if(requestCode == REQUEST_GALLERY) {
                try {
                    val uri = data!!.data
                    if (Build.VERSION.SDK_INT >= 19) {
                        photoPath = getRealPathFromURI_API19(context, uri)
                    }
                    else if (Build.VERSION.SDK_INT >= 11 && Build.VERSION.SDK_INT <= 18) {
                        photoPath = getRealPathFromURI_API11to18(context, uri)
                    }
                    else if (Build.VERSION.SDK_INT < 11) {
                        photoPath = getRealPathFromURI_BelowAPI11(context, uri)
                    }

                    val dest = File(photoPath)
                    gotoNextFragment(dest)
                } catch (e: Exception) {
                    context?.onSnackNotif(iv_gallery, "Gambar tidak ditemukan")
                }
            }
        }
    }

    fun gotoNextFragment(dest: File) {
        val reportDescriptionFragment = ReportDescriptionFragment()
        (activity as AppCompatActivity).replaceFragment(reportDescriptionFragment, R.id.fl_container)

        reportDescriptionFragment.setDestFile(dest)
    }

    override fun onStop() {
        super.onStop()
        fotoapparat?.stop()
    }

    override fun onStart() {
        super.onStart()
        if (hasNoPermissions()) {
            requestPermission()
        }else{
            fotoapparat?.start()
        }
    }

    private fun hasNoPermissions(): Boolean{
        return ContextCompat.checkSelfPermission(context!!,
            Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context!!,
            Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context!!,
            Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission(){
        ActivityCompat.requestPermissions(activity!!, permissions,0)
    }

}

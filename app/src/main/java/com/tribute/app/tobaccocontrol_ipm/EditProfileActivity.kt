package com.tribute.app.tobaccocontrol_ipm

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.tribute.app.tobaccocontrol_ipm.RealPathUtil.*
import kotlinx.android.synthetic.main.activity_edit_profile.*
import kotlinx.android.synthetic.main.progress_bar_dialog.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class EditProfileActivity : AppCompatActivity(), View.OnClickListener {

    private val myDB = FirebaseFirestore.getInstance()
    private val mAuth = FirebaseAuth.getInstance()
    private val storageRef = FirebaseStorage.getInstance().getReference();

    private var progressDialog : Dialog? = null

    private val REQUEST_GALLERY_PHOTO = 1
    private val REQUEST_TAKE_PHOTO = 2

    private var photoPath = ""

    val functionality = Functionality()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // check location
        showDialogIfLocationIsDisabled()

        progressDialog = showProgressDialog()

        setupToolbar()
        setCurrentData()
        setupOnClick()
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.ll_image -> {
                functionality.showPickImageDialog(REQUEST_GALLERY_PHOTO, REQUEST_TAKE_PHOTO, this)
            }
            R.id.btn_save -> {
                if (isValid()) sendDataToServer()
            }
        }
    }

    fun setupToolbar() {
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        //set title toolbar
        val title = toolbar.findViewById(R.id.title) as TextView
        val icon = toolbar.findViewById(R.id.icon_logo) as ImageView
        title.text = getString(R.string.edit)
        icon.visibility = View.GONE
    }

    fun setCurrentData() {
        myDB.collection("users").document(mAuth.currentUser?.uid.toString()).get().addOnSuccessListener {
            val name = it.get("name").toString()
            val address = it.get("address").toString()
            val urlPhoto = it.get("url_photo").toString()

            et_name.setText(name)
            et_address.setText(address)

            try {
                Glide.with(this)
                    .load(urlPhoto)
                    .apply(requestOptions)
                    .into(iv_image)
            } catch (e: Exception) {
                Log.e("Edit Profile", e.message)
            }
        }
    }

    fun setupOnClick() {
        ll_image.setOnClickListener(this)
        btn_save.setOnClickListener(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == AppCompatActivity.RESULT_OK) {
            progressDialog?.show()

            if(requestCode == REQUEST_TAKE_PHOTO) {
                val path = functionality.currentPhotoPath

                functionality.scanFile(path, this) { uriFile, pathString, activity ->
                    var bitmap = functionality.getBitmap(pathString)
                    bitmap = functionality.fixRotatedImage(bitmap)
                    uploadBitmap(bitmap)
                }
            }
            else if(requestCode == REQUEST_GALLERY_PHOTO) {
                val uri = data!!.data
                if (Build.VERSION.SDK_INT >= 19) {
                    photoPath = getRealPathFromURI_API19(this, uri)
                }
                else if (Build.VERSION.SDK_INT >= 11 && Build.VERSION.SDK_INT <= 18) {
                    photoPath = getRealPathFromURI_API11to18(this, uri)
                }
                else if (Build.VERSION.SDK_INT < 11) {
                    photoPath = getRealPathFromURI_BelowAPI11(this, uri)
                }

                val bitmap = functionality.getBitmap(photoPath)
                uploadBitmap(bitmap)
            }
        }
    }

    private fun uploadBitmap(bitmap: Bitmap) {

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
        val data = baos.toByteArray()

        val imageRef = storageRef.child("users").child("profile_" + mAuth.currentUser?.uid.toString() + ".jpg")

        var uploadTask = imageRef.putBytes(data)

        progressDialog?.tv_cancel?.setOnClickListener {
            uploadTask.cancel()
        }

        uploadTask.addOnProgressListener {
            val progress = 100.0 * it.bytesTransferred / it.totalByteCount
            progressDialog?.tv_upload_progress?.text = "Sedang mengunggah : %.2f".format(progress) + "%"
        }.addOnFailureListener {
            progressDialog?.dismiss()
        }.addOnSuccessListener {
            progressDialog?.dismiss()
        }.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            return@Continuation imageRef.downloadUrl
        }).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result.toString()
                iv_image.setImageBitmap(bitmap)
                sendDataPhotoProfileToServer(downloadUri)
            } else {
                // Handle failures
                // ...
            }
            progressDialog?.dismiss()
        }
    }

    fun sendDataPhotoProfileToServer(url: String) {
        val data = HashMap<String, Any>()
        data["url_photo"] = url

        myDB.collection("users").document(mAuth.currentUser?.uid.toString()).update(data)
    }

    fun sendDataToServer() {
        progressbar_edit.visibility = View.VISIBLE
        val name = et_name.text.toString()
        val address = et_address.text.toString()

        val data = HashMap<String, Any>()
        data["name"] = name
        data["address"] = address
        data["photo_signature"] = getTimestamp()

        myDB.collection("users").document(mAuth.currentUser?.uid.toString()).update(data).addOnSuccessListener {
            finish()
            progressbar_edit.visibility = View.GONE
        }.addOnFailureListener {
            progressbar_edit.visibility = View.GONE
        }
    }

    fun isValid() : Boolean {
        var err = true
        if(et_name.text.toString().isEmpty() || et_address.text.toString().isEmpty()) {
            err = false
            onSnackNotif(cl_container, "Isi semua data terlebih dahulu")
        }

        return err
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}

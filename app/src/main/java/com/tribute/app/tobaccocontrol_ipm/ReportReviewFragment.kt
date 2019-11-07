package com.tribute.app.tobaccocontrol_ipm


import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.tasks.Continuation
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.fragment_report_review.*
import kotlinx.android.synthetic.main.fragment_report_review.view.*
import java.io.File
import com.google.firebase.storage.StorageReference
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.storage.UploadTask
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import kotlinx.android.synthetic.main.progress_bar_dialog.*
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*


class ReportReviewFragment : Fragment(), View.OnClickListener {

    private lateinit var dest : File
    private lateinit var cityName : String
    private lateinit var provinceName : String
    private lateinit var additionalLocationInfo : String
    private lateinit var date : String
    private lateinit var time : String
    private lateinit var violationPlace : String
    private lateinit var violationKind : String
    private lateinit var description: String
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    private lateinit var postImage : ImageView

    private val myDB = FirebaseFirestore.getInstance()
    private val mStorageRef = FirebaseStorage.getInstance().getReference();
    private val mAuth  = FirebaseAuth.getInstance()

    private val functionality = Functionality()

    private var progressDialog : Dialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val bundle = this.arguments

        progressDialog = context?.showProgressDialog()

        if (bundle != null) {
            date = bundle.getString("date")
            time = bundle.getString("time")
            cityName = bundle.getString("city")
            dest = File(bundle.getString("dest"))
            latitude = bundle.getDouble("latitude")
            longitude = bundle.getDouble("longitude")
            provinceName = bundle.getString("province")
            additionalLocationInfo = bundle.getString("additionalLocationInfo")
            description = bundle.getString("description")
            violationPlace = bundle.getString("violationPlace")
            violationKind = bundle.getString("violationKind")
        }

        return inflater.inflate(R.layout.fragment_report_review, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupOnClick()

        postImage = view.findViewById(R.id.iv_post_image)

        setImage(view)
        setData(view)
    }

    private fun setupOnClick() {
        btn_done.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.btn_done -> {
                context?.showPickImageDialog {
                    if(it) {
                        functionality.currentPhotoPath = dest.path
                        uploadImage(functionality.fixRotatedImage(functionality.getBitmap(dest.path)))
                    }
                }

            }
        }
    }

    private fun setData(v: View) {
        v.tv_location.text = "$cityName, $provinceName"
        v.tv_description.text = description
        v.tv_date_time.text = "$date - $time"
        v.tv_location_name.text = additionalLocationInfo
        v.tv_violation_place.text = violationPlace
        v.tv_violation_kind.text = violationKind
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

    private fun uploadImage(bitmap: Bitmap) {
        progressDialog?.show()

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
        val data = baos.toByteArray()

        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageRef = mStorageRef.child("images/${timeStamp}.jpg")

        var uploadTask = imageRef.putBytes(data)

        progressDialog?.tv_cancel?.setOnClickListener {
            uploadTask.cancel()
        }

        uploadTask.addOnProgressListener {
            val progress = 100.0 * it.bytesTransferred / it.totalByteCount
            progressDialog?.tv_upload_progress?.text = "Sedang mengunggah : %.2f".format(progress) + "%"
        }.addOnFailureListener {
            // Handle unsuccessful uploads
            progressDialog?.dismiss()
        }.addOnSuccessListener {
            progressDialog?.dismiss()
        }.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    progressDialog?.dismiss()
                    throw it
                }
            }
            return@Continuation imageRef.downloadUrl
        }).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result.toString()

                sendDataToDb(downloadUri, timeStamp)
            } else {
                progressDialog?.dismiss()
            }

        }
    }

    private fun sendDataToDb(downloadUri: String, photoName: String) {
        val timestamp = context?.getTimeForKeyDatabase().toString()

        val post = HashMap<String, Any>()
        post["date"] = date
        post["time"] = time
        post["city"] = cityName
        post["latitude"] = latitude
        post["longitude"] = longitude
        post["timestamp"] = timestamp
        post["name_photo"] = photoName
        post["province"] = provinceName
        post["url_photo"] = downloadUri
        post["description"] = description
        post["violation_kind"] = violationKind
        post["violation_place"] = violationPlace
        post["id_user"] = mAuth.currentUser?.uid.toString()
        post["additional_location_info"] = additionalLocationInfo

        myDB.collection("post").add(post)
            .addOnSuccessListener {
                activity?.finishAffinity()
                activity?.startActivity(Intent(context, MainActivity::class.java))
            }
            .addOnFailureListener {
                progressDialog?.dismiss()
            }
    }
}

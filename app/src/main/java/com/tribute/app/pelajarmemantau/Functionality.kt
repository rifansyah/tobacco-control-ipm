package com.tribute.app.pelajarmemantau

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.IOException

@SuppressLint("Registered")
class Functionality : Application() {

    var currentPhotoPath = ""

    val permissions = arrayOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )

    fun getBitmapFromView(view: View): Bitmap {
        //Define a bitmap with the same size as the view
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        //Bind a canvas to it
        val canvas = Canvas(returnedBitmap)
        //Get the view's background
        val bgDrawable = view.background
        if (bgDrawable != null) {
            //has background drawable, then draw it on the canvas
            bgDrawable.draw(canvas)
        } else {
            //does not have background drawable, then draw white background on the canvas
            canvas.drawColor(Color.WHITE)
        }
        // draw the view on the canvas
        view.draw(canvas)
        //return the bitmap
        return returnedBitmap
    }

    fun showPickImageDialog(requestGallery: Int, requestCamera: Int, activity: Activity) {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Pilih gambar")
        builder.setMessage("Ambil gambar lewat : ")
        builder.setPositiveButton("Kamera") { _, _ ->
            takePhoto(requestCamera, activity)
        }
        builder.setNegativeButton("Galeri") { _, _ ->
            selectImageInAlbum(requestGallery, activity)
        }

        builder.setNeutralButton("Batal") { _, _ ->
        }
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    fun selectImageInAlbum(requestGallery: Int, activity: Activity) {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        if (intent.resolveActivity(activity.packageManager) != null) {
            activity.startActivityForResult(
                Intent.createChooser(intent, "Select Image"),
                requestGallery
            )
        }
    }

    @Throws(IOException::class)
    fun createImageFile(activity: Activity): File {
        // Create an image file name
        val timeStamp = System.currentTimeMillis()
        val storageDir: File = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "$timeStamp", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    fun takePhoto(requestCamera: Int, activity: Activity) {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(activity.packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile(activity)
                } catch (ex: IOException) {

                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        activity,
                        "com.tribute.android.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    activity.startActivityForResult(takePictureIntent, requestCamera)
                }
            }
        }
    }

    fun fixRotatedImage(bitmap: Bitmap) : Bitmap {
        val ei = ExifInterface(currentPhotoPath)
        val orientation = ei.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED)

        var rotatedBitmap = bitmap
        when(orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotatedBitmap = rotateImage(bitmap, 90.toFloat());

            ExifInterface.ORIENTATION_ROTATE_180 -> rotatedBitmap = rotateImage(bitmap, 180.toFloat());

            ExifInterface.ORIENTATION_ROTATE_270 -> rotatedBitmap = rotateImage(bitmap, 270.toFloat());

            ExifInterface.ORIENTATION_NORMAL -> rotatedBitmap = bitmap;
        }
        return rotatedBitmap
    }

    fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }

    fun scanFile(path: String, activity: Activity, onSuccessful: (Uri, String, Activity) -> Unit) {
        MediaScannerConnection.scanFile(
            activity,
            arrayOf(path), null
        ) { _, uri ->
            onSuccessful(uri, path, activity)
        }
    }

    fun getBitmap(path: String) : Bitmap {
        val bmOptions = BitmapFactory.Options();
        val bitmap = BitmapFactory.decodeFile(path, bmOptions);
        return bitmap
    }

    fun hasNoPermissions(activity: Activity): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.CAMERA
        ) != PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission(activity: Activity) {
        ActivityCompat.requestPermissions(activity, permissions, 0)
    }

}
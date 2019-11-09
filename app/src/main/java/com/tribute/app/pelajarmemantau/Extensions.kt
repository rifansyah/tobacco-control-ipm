package com.tribute.app.pelajarmemantau

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.snackbar.Snackbar
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity


fun Context.showDialog(title: String, message: String, onResult: (Boolean) -> Unit) {
    val builder = AlertDialog.Builder(this)
    builder.setTitle(title)
    builder.setMessage(message)
    builder.setPositiveButton("Ya") { dialog, which ->
        onResult(true)
    }

    builder.setNegativeButton("Tidak") { dialog, which ->
        onResult(false)
    }

    val dialog: AlertDialog = builder.create()
    dialog.show()
}

fun Context.toast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

fun Context.showPickImageDialog(onSuccessful: (Boolean) -> Unit) {
    val builder = AlertDialog.Builder(this)
    builder.setTitle("Kirim laporan")
    builder.setMessage("Yakin ingin mengirim laporan ?")
    builder.setPositiveButton("Kirim") { _, _ ->
        onSuccessful(true)
    }
    builder.setNegativeButton("Batal") { _, _ ->
        onSuccessful(false)
    }

    val dialog: AlertDialog = builder.create()
    dialog.show()
}

fun Context.getCurrentDate(): String {
    val dateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale("id"))
    val today = Date()
    return dateFormatter.format(today).toString()
}

fun Context.getTimeForKeyDatabase(): String {
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale("id"))
    val today = Date()
    return dateFormatter.format(today).toString()
}


fun Context.onSnackNotif(v: View, msg: String) {
    val snackbar = Snackbar.make(v, msg, Snackbar.LENGTH_LONG)
    snackbar.show()
}

fun Context.getTimestamp(): String {
    return Timestamp(System.currentTimeMillis()).toString()
}

fun Context.showProgressDialog(): Dialog {
    var dialogs = Dialog(this)
    dialogs.requestWindowFeature(Window.FEATURE_NO_TITLE)
    dialogs.setCancelable(false)
    dialogs.setContentView(R.layout.progress_bar_dialog)

    return dialogs
}

val requestOptions: RequestOptions by lazy {
    RequestOptions()
        .placeholder(R.drawable.placeholder)
        .transforms(CenterCrop())
}

fun Context.showDialogIfLocationIsDisabled() {
    val lm = getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager
    var gps_enabled = false
    try {
        gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    } catch (ex: Exception) {
    }

    if (!gps_enabled) {
        val dialog = AlertDialog.Builder(this)
        dialog.setMessage("Lokasi harus diaktifkan")
        dialog.setPositiveButton("Pengaturan") { _, _ ->
            val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            this.startActivity(myIntent)
        }
        dialog.setCancelable(false)
        dialog.show()
    }
}


fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            afterTextChanged.invoke(s.toString())
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
}

fun EditText.validate(validator: (String) -> Boolean, message: String) {
    this.afterTextChanged {
        this.error = if (validator(it)) null else message
    }
//    this.error = if (validator(this.text.toString())) null else message
}

fun String.isValidEmail(): Boolean = Patterns.EMAIL_ADDRESS.matcher(this).matches()

fun Context.getUserRole() : String {
    val pref = getSharedPreferences(MainActivity.MYPREF,Context.MODE_PRIVATE)
    val role = pref.getString(MainActivity.ROLEUSER,MainActivity.USER)
    return role
}
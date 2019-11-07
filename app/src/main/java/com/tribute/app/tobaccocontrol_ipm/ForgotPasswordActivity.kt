package com.tribute.app.tobaccocontrol_ipm

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import android.util.Log
import kotlinx.android.synthetic.main.activity_forgot_password.*


class ForgotPasswordActivity : AppCompatActivity(), View.OnClickListener {

    private val TAG = "REGISTER ACTIVITY"
    private val mAuth  = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        setupClick()
        setupValidation()
    }

    fun setupClick() {
        btn_submit.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.btn_submit -> {
                if (!isValid()) return

                progressbar.visibility = View.VISIBLE

                val email = emailEditText.text.toString()
                sendEmail(v, email)
            }
        }
    }

    private fun sendEmail(v: View, email: String) {
        mAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSnackNotif(v, "Silahkan cek email kamu untuk atur ulang password")
                    Log.d(TAG, "Email sent.")
                } else {
                    onSnackNotif(v, "Gagal, pastikan email kamu benar dan terdaftar")
                    Log.d(TAG, "Email failed.")
                }
                progressbar.visibility = View.GONE
            }
    }

    fun setupValidation() {
        emailEditText.validate(
            { s -> s.isValidEmail() },
            "Masukkan alamat email yang valid"
        )
    }

    fun isValid() : Boolean {
        var valid = true

        if(emailEditText.text.toString().isEmpty()) {
            valid = false
            emailEditText.error = "Tidak boleh kosong"
        }

        return valid
    }
}

package com.tribute.app.tobaccocontrol_ipm

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_register.*
import com.google.firebase.auth.FirebaseAuth
import android.widget.Toast
import com.google.firebase.auth.AuthResult
import com.google.android.gms.tasks.Task
import androidx.annotation.NonNull
import com.google.android.gms.tasks.OnCompleteListener
import android.R.attr.password
import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*


class RegisterActivity : AppCompatActivity(), View.OnClickListener {

    private val mAuth  = FirebaseAuth.getInstance()

    private val TAG = "REGISTER ACTIVITY"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // check location
        showDialogIfLocationIsDisabled()

        setOnClick()
        setupValidation()
    }

    fun setOnClick() {
        btn_signup.setOnClickListener(this)
        tv_login.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.tv_login -> {
                startActivity(Intent(this, LoginActivity::class.java))
                this.finishAffinity()
            }
            R.id.btn_signup -> {
                if (isValid()) register()
            }
        }
    }

    fun setupValidation() {
        emailEditText.validate(
            { s -> s.isValidEmail() },
            "Valid email address required"
        )
    }

    fun isValid() : Boolean {
        var valid = true

        if(emailEditText.text.toString().isEmpty()) {
            valid = false
            emailEditText.error = "Tidak boleh kosong"
        }

        if(passwordEditText.text.toString().isEmpty()) {
            valid = false
            til_password.error = "Tidak boleh kosong"
        }

        if(nameEditText.text.toString().isEmpty()) {
            valid = false
            nameEditText.error = "Tidak boleh kosong"
        }

        return valid
    }

    fun register() {
        progressbar.visibility = View.VISIBLE

        val email = emailEditText.text.toString()
        val password = passwordEditText.text.toString()

        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val editor = getSharedPreferences(MainActivity.MYPREF, Context.MODE_PRIVATE).edit()

                    editor.putString(MainActivity.ROLEUSER, MainActivity.USER)
                    editor.apply()

                    Log.d(TAG, "createUserWithEmail:success")
                    val user = mAuth.currentUser
                    createDbUser(email, user!!)
                    startActivity(Intent(this, MainActivity::class.java))
                    finishAffinity()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(
                        this@RegisterActivity, "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                progressbar.visibility = View.GONE
            }
    }

    fun createDbUser(email: String, user: FirebaseUser) {
        val db = FirebaseFirestore.getInstance()


        val userValue = mapOf(
            "id" to user.uid,
            "name" to nameEditText.text.toString().trim(),
            "email" to email,
            "address" to "Alamat Kamu",
            "role" to MainActivity.USER
        )

        db.collection("users").document(user.uid).set(userValue)
    }
}

package com.tribute.app.tobaccocontrol_ipm

import android.Manifest
import android.animation.Animator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity(), View.OnClickListener {

    private val TAG = "LOGIN_ACTIVITY"
    private val mAuth  = FirebaseAuth.getInstance()
    private val myDb = FirebaseFirestore.getInstance()
    private val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        showDialogIfLocationIsDisabled()

        object : CountDownTimer(100, 100) {
            override fun onFinish() {
                bookITextView.visibility = View.GONE
                loadingProgressBar.visibility = View.GONE
                rootView.setBackgroundColor(ContextCompat.getColor(this@LoginActivity, R.color.colorPrimary))
                bookIconImageView.setImageResource(R.drawable.ic_logo_white)
                startAnimation()
            }
            override fun onTick(p0: Long) {}
        }.start()

        setOnClick()
        setupValidation()
    }

    private fun setOnClick() {
        loginButton.setOnClickListener(this)
        tv_signup.setOnClickListener(this)
        tv_forget_password.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.loginButton -> {
                if (isValid()) login()
            }

            R.id.tv_signup -> {
                startActivity(Intent(this, RegisterActivity::class.java))
                this.finishAffinity()
            }

            R.id.tv_forget_password -> {
                startActivity(Intent(this, ForgotPasswordActivity::class.java))
            }
        }
    }

    private fun setupValidation() {
        emailEditText.validate(
            { s -> s.isValidEmail() },
            "Valid email address required"
        )
    }

    private fun isValid() : Boolean {
        var valid = true

        if(emailEditText.text.toString().isEmpty()) {
            valid = false
            emailEditText.error = "Tidak boleh kosong"
        }

        if(passwordEditText.text.toString().isEmpty()) {
            valid = false
            passwordEditText.error = "Tidak boleh kosong"
        }

        return valid
    }

    private fun login() {
        progressbar.visibility = VISIBLE

        val email = emailEditText.text.toString()
        val password = passwordEditText.text.toString()

        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    myDb.collection("users").document(mAuth.currentUser?.uid.toString()).get().addOnSuccessListener {
                        val editor = getSharedPreferences(MainActivity.MYPREF, Context.MODE_PRIVATE).edit()

                        if(it.getString("role").equals(MainActivity.ADMIN, true)) editor.putString(MainActivity.ROLEUSER, MainActivity.ADMIN)
                        else editor.putString(MainActivity.ROLEUSER, MainActivity.USER)

                        editor.apply()

                        Log.d(TAG, "signInWithEmail:success")
                        startActivity(Intent(this, MainActivity::class.java))
                        this.finishAffinity()
                    }
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Login gagal. silahkan coba lagi",
                        Toast.LENGTH_SHORT).show()
                }
                progressbar.visibility = View.GONE
            }
    }

    private fun startAnimation() {
        bookIconImageView.animate().apply {
            x(50f)
            y(100f)
            duration = 1000
        }.setListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(p0: Animator?) {
            }
            override fun onAnimationEnd(p0: Animator?) {
                afterAnimationView.visibility = VISIBLE
            }
            override fun onAnimationCancel(p0: Animator?) {
            }
            override fun onAnimationStart(p0: Animator?) {
            }
        })
    }

    private fun isLogin() : Boolean {
        val user = mAuth.currentUser
        return user != null
    }

    override fun onStart() {
        super.onStart()
        if(isLogin()) {
            this.finishAffinity()
            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
        }
        if (hasNoPermissions()) {
            requestPermission()
        }
    }

    private fun hasNoPermissions(): Boolean{
        return ContextCompat.checkSelfPermission(this,
            Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,
            Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission(){
        ActivityCompat.requestPermissions(this, permissions,0)
    }
}

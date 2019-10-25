package com.tribute.app.tobaccocontrol_ipm

import android.graphics.Color
import android.media.Image
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar

class ReportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        // check location
        showDialogIfLocationIsDisabled()

        setupToolbar()

        addFragment(ReportTakePhotoFragment(), R.id.fl_container)
    }

    fun setupToolbar() {
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        //set title toolbar
        val title = toolbar.findViewById(R.id.title) as TextView
        title.setText("Laporkan")
        toolbar.findViewById<ImageView>(R.id.icon_logo).visibility = View.GONE
    }

    override fun onBackPressed() {
        val fm = supportFragmentManager
        if (fm!!.backStackEntryCount > 0) {
            Log.i("ReportDescription", "popping backstack")
            fm.popBackStack()
        } else {
            Log.i("ReportDescription", "nothing on backstack, calling super")
            super.onBackPressed()
        }
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

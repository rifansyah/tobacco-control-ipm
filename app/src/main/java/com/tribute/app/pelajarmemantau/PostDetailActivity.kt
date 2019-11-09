package com.tribute.app.pelajarmemantau

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.tribute.app.pelajarmemantau.post.Comment
import com.tribute.app.pelajarmemantau.post.CommentAdapter
import kotlinx.android.synthetic.main.activity_post_detail.*
import java.lang.Exception
import android.graphics.drawable.BitmapDrawable
import android.app.Dialog
import android.graphics.Bitmap
import android.net.Uri
import android.view.MotionEvent
import android.view.Window
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.EventListener
import kotlinx.android.synthetic.main.activity_show_image.*
import java.util.*


class PostDetailActivity : AppCompatActivity(), View.OnClickListener, OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val comments: ArrayList<Comment> = ArrayList()
    private val myDB = FirebaseFirestore.getInstance()
    private lateinit var idPost : String

    private var isSupported = false

    private var supportNumb = 0
    private var latitude = 0.0
    private var longitude = 0.0

    private val requestOptions: RequestOptions by lazy {
        RequestOptions()
            .placeholder(R.drawable.ic_placeholder)
            .transforms(CenterCrop())
    }

    private val mAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_detail)

        showDialogIfLocationIsDisabled()
        idPost = intent.getStringExtra(IDPOST)

        getPostData()
        setUpToolbar()
        setRecyclerView()
        setupOnClick()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
    }

    private fun setupRemoveView(idUser: String) {
        if (getUserRole() == MainActivity.ADMIN || mAuth.currentUser?.uid.toString() == idUser) iv_remove.visibility = View.VISIBLE
    }

    private fun setupOnClick() {
        tv_send.setOnClickListener(this)
        iv_support.setOnClickListener(this)
        iv_post_image.setOnClickListener(this)
        iv_remove.setOnClickListener(this)
        tv_map.setOnClickListener(this)

        transparent_touch_panel.setOnTouchListener { _, event ->
            when(event.action) {
                MotionEvent.ACTION_DOWN -> {
                    nestedScrollView.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP -> {
                    nestedScrollView.requestDisallowInterceptTouchEvent(false)
                }
            }
            return@setOnTouchListener false
        }
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.tv_send -> {
                if (isValid()) sendCommentToDb()
            }

            R.id.iv_support -> {
                if(isSupported) {
                    myDB.collection("post").document(idPost).collection("supports").document(mAuth.currentUser?.uid.toString()).delete()
                    iv_support.setImageResource(R.drawable.ic_support)
                    tv_support.text = "${supportNumb - 1} Dukungan"
                    supportNumb -= 1
                }
                else {
                    myDB.collection("post").document(idPost).collection("supports").document(mAuth.currentUser?.uid.toString()).set(mapOf("status" to true))
                    iv_support.setImageResource(R.drawable.ic_support_activated)
                    tv_support.text = "${supportNumb + 1} Dukungan"
                    supportNumb += 1
                }
                isSupported = !isSupported
            }

            R.id.iv_post_image -> {
                var bitmap = (iv_post_image.getDrawable() as BitmapDrawable).bitmap
                viewImage(bitmap)
            }

            R.id.iv_remove -> {
                showDialog("Hapus Laporan", "Yakin ingin menghapus Laporan ini ?") {
                    if(it) {
                        myDB.collection("post").document(idPost).delete().addOnSuccessListener {
                            finish()
                            onSnackNotif(iv_remove, "Laporan berhasil di hapus")
                        }
                    }
                }
            }

            R.id.tv_map -> {
                val uri = "geo:0,0?q=$latitude,$longitude (Maninagar)"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                startActivity(intent);
            }
        }
    }

    private fun setUpToolbar() {
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun getPostData() {

        val postRef = myDB.collection("post").document(idPost)
        postRef.get().addOnSuccessListener {
            val idPost = it.id
            val idUser = if (it.get("id_user") != null) it.get("id_user").toString() else ""
            val urlPhoto = if (it.get("url_photo") != null) it.get("url_photo").toString() else ""
            val city = if (it.get("city") != null) it.get("city").toString() else "Keterangan kota tidak dimasukkan"
            val province = if (it.get("province") != null) it.get("province").toString() else ""
            val additionalLocationInfo = if (it.get("additional_location_info") != null) it.get("additional_location_info").toString() else ""
            val description = if (it.get("description").toString() != null) it.get("description").toString() else "Deskripsi tidak dimasukkan"
            val date = if (it.get("date") != null) it.get("date").toString() else ""
            val time = if (it.get("time") != null) it.get("time").toString() else ""
            val latitude = if (it.get("latitude") != null) it.get("latitude") as Double else -7.782834
            val longitude = if (it.get("longitude") != null) it.get("longitude") as Double else 110.368279
            val violationPlace = if (it.get("violation_place") != null) it.get("violation_place").toString() else "Tempat pelanggaran tidak diatur"
            val violationKind = if (it.get("violation_kind") != null) it.get("violation_kind").toString() else "Jenis pelanggaran tidak diatur"

            //set data to view
            tv_description.text = description
            tv_location.text = if (additionalLocationInfo.isEmpty()) "${city}, ${province}" else "${additionalLocationInfo}\n${city}, ${province}"
            tv_date_time.text = "$date - $time"
            tv_violation_place.text = violationPlace
            tv_violation_kind.text = violationKind

            try {
                Glide.with(this)
                    .load(urlPhoto)
                    .apply(requestOptions)
                    .into(iv_post_image)
            } catch (e: Exception) {

            }

            setCommentListener()
            getUserData(idUser)
            setupMap(latitude, longitude)


            //get data support
            myDB.collection("post").document(idPost).collection("supports").get().addOnSuccessListener {
                tv_support.text = "${it.size()} Dukungan"
                supportNumb = it.size()
            }

            myDB.collection("post").document(idPost).collection("supports").document(mAuth.currentUser?.uid.toString()).get().addOnSuccessListener {
                if(it.exists()) {
                    iv_support.setImageResource(R.drawable.ic_support_activated)
                    isSupported = true
                }
                else {
                    iv_support.setImageResource(R.drawable.ic_support)
                    isSupported = false
                }
            }
        }
    }

    private fun setupMap(latitude: Double, longitude: Double) {
        val latLng = LatLng(latitude, longitude)
        mMap.addMarker(MarkerOptions().position(latLng).title("Klik untuk pilih lokasi"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    private fun getUserData(idUser: String) {
        myDB.collection("users").document(idUser).get().addOnSuccessListener {
            val name = it.get("name").toString()
            val urlPhoto = it.get("url_photo").toString()

            try {
                Glide.with(this)
                    .load(urlPhoto)
                    .apply(requestOptions)
                    .into(profile_image)
            } catch (e: Exception) {

            }

            tv_name_sender.text = name
            setupRemoveView(it.id)

            cl_profile.setOnClickListener {
                val intent = Intent(this, ProfileActivity::class.java)
                intent.putExtra("userId", idUser)
                startActivity(intent)
            }
        }
    }

    private fun setCommentListener() {
        myDB.collection("post").document(idPost).collection("comments").orderBy("timestamp")
            .addSnapshotListener(EventListener<QuerySnapshot> { snapshots, e ->
                if (e != null) {
                    return@EventListener
                }

                for (dc in snapshots!!.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> {
                            val idComment = dc.document.id
                            val idUser = dc.document.get("id_user").toString()
                            val text = dc.document.get("text").toString()

                            val comment = Comment(idComment, idUser, text)
                            comments.add(comment)
                            rv_comments.adapter?.notifyDataSetChanged()
                        }
                        DocumentChange.Type.MODIFIED -> {

                        }
                        DocumentChange.Type.REMOVED -> {
                            val position = dc.oldIndex
                            comments.removeAt(position)
                            rv_comments.adapter?.notifyItemRemoved(position)

                            if(comments.size == 0) tv_empty_comment.visibility = View.VISIBLE
                        }
                    }
                }
                if(comments.size == 0) tv_empty_comment.visibility = View.VISIBLE
                else tv_empty_comment.visibility = View.GONE
            })
    }

    private fun setRecyclerView() {
        rv_comments.layoutManager = LinearLayoutManager(this)
        rv_comments.adapter = CommentAdapter(comments, this, idPost) { idPost, comment, position ->
            if (comment.idUser == mAuth.currentUser?.uid.toString() || getUserRole() == MainActivity.ADMIN) {
                showDialog("Hapus Komentar", "Yakin ingin menghapus komentar ini ?") {
                    if(it) {
                        myDB.collection("post").document(idPost).collection("comments").document(comment.idComment).delete().addOnSuccessListener {
                            toast("Komentar berhasil di hapus")
                        }
                    }
                }
            }
        }

        val dividerItemDecoration = DividerItemDecoration(
            rv_comments.getContext(),
            LinearLayoutManager(this).getOrientation()
        )
        rv_comments.addItemDecoration(dividerItemDecoration)
    }

    private fun isValid() : Boolean {
        if(et_comment_text.text.trim().isEmpty()) {
            onSnackNotif(et_comment_text, "Isi komentar tidak boleh kosong")
            return false
        }
        return true
    }

    private fun sendCommentToDb() {
        val textComment = et_comment_text.text.toString()
        val timestamp = FieldValue.serverTimestamp()

        val comment = HashMap<String, Any>()
        comment["id_user"] = mAuth.currentUser?.uid.toString()
        comment["text"] = textComment
        comment["timestamp"] = timestamp

        val commentRef = myDB.collection("post").document(idPost).collection("comments")

        commentRef.add(comment)
            .addOnSuccessListener {
            }
            .addOnFailureListener {
                onSnackNotif(et_comment_text, "Gagal mengirim komentar")
            }

        et_comment_text.setText("")
        val view = this.currentFocus
        view?.let { v ->
            val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.let { it.hideSoftInputFromWindow(v.windowToken, 0) }
        }
    }

    private fun viewImage(bitmap: Bitmap) {
        val nagDialog = Dialog(this,android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        nagDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        nagDialog?.setCancelable(false)
        nagDialog?.setContentView(R.layout.activity_show_image)
        nagDialog?.iv_back?.setOnClickListener{
            nagDialog?.dismiss()
        }
        nagDialog?.iv_image?.setImageBitmap(bitmap);
        nagDialog?.show();
    }


    companion object {
        const val IDPOST = "id post"
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

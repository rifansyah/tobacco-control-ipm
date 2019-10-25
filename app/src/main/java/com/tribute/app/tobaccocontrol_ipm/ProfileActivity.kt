package com.tribute.app.tobaccocontrol_ipm

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import com.tribute.app.tobaccocontrol_ipm.post.Comment
import com.tribute.app.tobaccocontrol_ipm.post.Post
import com.tribute.app.tobaccocontrol_ipm.post.PostAdapter
import kotlinx.android.synthetic.main.activity_profile.*
import java.lang.Exception

class ProfileActivity : AppCompatActivity() {

    val posts: ArrayList<Post> = ArrayList()
    val postsIndex: ArrayList<String> = ArrayList()

    val myDB = FirebaseFirestore.getInstance()
    val storageRef = FirebaseStorage.getInstance().reference

    val mAuth = FirebaseAuth.getInstance()

    private val TAG = "PROFILE"

    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        try {
            userId = intent.getStringExtra("userId")
        } catch (e: Exception) {
            onSnackNotif(rv_profile, "Profil tidak ditemukan")
            return
        }

        setupToolbar()
        setRecyclerView()
        setProfileData()
        setDataListener()
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
        title.text = getString(R.string.profile)
        icon.visibility = View.GONE
    }

    fun setProfileData() {
        myDB.collection("users").document(userId).addSnapshotListener(EventListener<DocumentSnapshot> { snapshot, e ->
            if (e != null) {
                Log.w("Profile", "Listen failed.", e)
                return@EventListener
            }

            if (snapshot != null && snapshot.exists()) {
                val it = snapshot

                val address = it.get("address").toString()
                val urlPhoto = it.get("url_photo").toString()
                val name = it.get("name").toString()
                val signaturePhoto = it.get("photo_signature").toString()

                tv_location_profile.text = address
                tv_name_profile.text = name

                val newOption : RequestOptions by lazy {
                    RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .signature(ObjectKey(signaturePhoto))
                }

                try {
                    Glide.with(this)
                        .load(urlPhoto)
                        .apply(requestOptions)
                        .apply(newOption)
                        .into(iv_image_profile)
                } catch (e: Exception) {

                }
            } else {
                Log.d(TAG, "Current data: null")
            }
        })

    }

    fun setDataListener() {
        progressbar.visibility = View.VISIBLE
        myDB.collection("post").whereEqualTo("id_user", userId).orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener(EventListener<QuerySnapshot> { snapshots, e ->
                if (e != null) {
                    if(posts.size == 0) tv_empty_report.visibility = View.VISIBLE
                    else tv_empty_report.visibility = View.GONE
                    progressbar.visibility = View.GONE
                    return@EventListener
                }

                for (dc in snapshots!!.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> {
                            val it = dc.document

                            val idPost = it.id
                            val idUser = it.get("id_user").toString()
                            val urlPhoto = it.get("url_photo").toString()
                            val city = it.get("city").toString()
                            val province = it.get("province").toString()
                            val description = it.get("description").toString()
                            val date = it.get("date").toString()
                            val latitude = it.get("latitude") as Double
                            val longitude = it.get("longitude") as Double

                            posts.add(Post(idPost, idUser, urlPhoto, city, province, description, date, latitude, longitude, ArrayList<Comment>()))
                            postsIndex.add(idPost)
                            rv_profile.adapter?.notifyDataSetChanged()
                        }
                        DocumentChange.Type.MODIFIED -> {
                            toast(dc.document.id)
                            val index = postsIndex.indexOf(dc.document.id)
                            toast(index.toString())
                            rv_profile.adapter?.notifyItemChanged(index)
                        }
                        DocumentChange.Type.REMOVED -> {
                            val position = dc.oldIndex
                            posts.removeAt(position)
                            postsIndex.removeAt(position)
                            rv_profile.adapter?.notifyItemRangeRemoved(position, posts.size)
                            if(position == 0) rv_profile.adapter?.notifyDataSetChanged()

                            // remove image from storage
                            val namePhoto = dc.document.get("name_photo").toString()
                            val photoRef = storageRef.child("images/${namePhoto}.jpg")
                            photoRef.delete().addOnSuccessListener {

                            }.addOnFailureListener {
                                Log.e(TAG, it.message)
                            }
                            if(posts.size == 0) tv_empty_report.visibility = View.VISIBLE
                        }
                    }
                }
                if(posts.size == 0) tv_empty_report.visibility = View.VISIBLE
                else tv_empty_report.visibility = View.GONE
                progressbar.visibility = View.GONE
                tv_report_number_profile.text = "${posts.size} Laporan"
            })
    }

    fun setRecyclerView() {
        rv_profile.layoutManager = LinearLayoutManager(this)
        rv_profile.adapter = PostAdapter(posts, this, { idPost, position, post, menu ->
            if (menu == HomeFragment.REMOVE) {
                showDialog("Hapus Laporan", "Yakin ingin menghapus laporan kamu ?") {
                    if(it) {
                        myDB.collection("post").document(idPost).delete().addOnSuccessListener {
                            onSnackNotif(fl_container_profile, "Laporan berhasil di hapus")
                        }
                    }
                }
            }
        } , { support, idPost ->
            if(support) myDB.collection("post").document(idPost).collection("supports").document(mAuth.currentUser?.uid.toString()).set(mapOf("status" to true))
            else myDB.collection("post").document(idPost).collection("supports").document(mAuth.currentUser?.uid.toString()).delete()
        })

        val dividerItemDecoration = DividerItemDecoration(
            rv_profile.getContext(),
            LinearLayoutManager(this).getOrientation()
        )
        rv_profile.addItemDecoration(dividerItemDecoration)
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
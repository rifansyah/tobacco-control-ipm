package com.tribute.app.tobaccocontrol_ipm


import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.denzcoskun.imageslider.ImageSlider
import com.denzcoskun.imageslider.interfaces.ItemClickListener
import com.denzcoskun.imageslider.models.SlideModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import com.tribute.app.tobaccocontrol_ipm.post.Comment
import com.tribute.app.tobaccocontrol_ipm.post.Post
import com.tribute.app.tobaccocontrol_ipm.post.PostAdapter
import kotlinx.android.synthetic.main.activity_post_detail.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import android.content.Intent
import android.net.Uri
import java.lang.Exception


class HomeFragment : Fragment() {

    val posts: ArrayList<Post> = ArrayList()

    val myDB = FirebaseFirestore.getInstance()
    val storageRef = FirebaseStorage.getInstance().reference
    val mAuth = FirebaseAuth.getInstance()

    var firstLoad = true

    val urlCarouselItem = ArrayList<String>()

    private val TAG = "HOME FRAGMENT"

    private var roleUser = MainActivity.ROLEUSER
    val imageList = ArrayList<SlideModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_home, container, false)

        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        posts.clear()
//        getPostData(view)
        setDataListener(view)
        setCarousel(view)
    }

    fun setCarousel(v: View) {
        myDB.collection("carousel").addSnapshotListener(EventListener<QuerySnapshot> { snapshots, e ->
            if (e != null) {
                return@EventListener
            }

            for (dc in snapshots!!.documentChanges) {
                when (dc.type) {
                    DocumentChange.Type.ADDED -> {
                        val it = dc.document

                        val photoUrl = it.get("url_photo").toString()
                        val link = it.get("link").toString()
                        imageList.add(SlideModel(photoUrl))
                        urlCarouselItem.add(link)
                    }
                    DocumentChange.Type.MODIFIED -> {
                        val it = dc.document
                        imageList[dc.oldIndex] = SlideModel(it.get("url_photo").toString())
                        urlCarouselItem[dc.oldIndex] = it.get("link").toString()
                    }
                    DocumentChange.Type.REMOVED -> {
                        imageList.removeAt(dc.oldIndex)
                    }
                }
            }
            val imageSlider = v.findViewById<ImageSlider>(R.id.image_slider)
            imageSlider.setImageList(imageList)

            imageSlider.setItemClickListener(object : ItemClickListener {
                override fun onItemSelected(position: Int) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse(urlCarouselItem.get(position))
                        startActivity(intent)
                    } catch (e: Exception) {
                        if(context?.getUserRole() == MainActivity.ADMIN) {
                            context?.onSnackNotif(imageSlider, "Link tidak di dukung, perbarui link di dashboard admin")
                        }
                    }
                }
            })
        })
    }

    fun setDataListener(v: View) {
        progressbar.visibility = View.VISIBLE
        myDB.collection("post").orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener(EventListener<QuerySnapshot> { snapshots, e ->
                if (e != null) {
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
                            v.rv_home.adapter?.notifyDataSetChanged()
                        }
                        DocumentChange.Type.MODIFIED -> {
                            v.rv_home.adapter?.notifyItemChanged(dc.newIndex)
                        }
                        DocumentChange.Type.REMOVED -> {
                            val position = dc.oldIndex
                            posts.removeAt(position)
                            v.rv_home.adapter?.notifyItemRangeRemoved(position, posts.size)
                            if(position == 0) v.rv_home.adapter?.notifyDataSetChanged()

                            // remove image from storage
                            val namePhoto = dc.document.get("name_photo").toString()
                            val photoRef = storageRef.child("images/${namePhoto}.jpg")
                            photoRef.delete().addOnSuccessListener {

                            }.addOnFailureListener {
                                Log.e(TAG, it.message)
                            }

                            if(posts.size == 0) v.tv_empty_report.visibility = View.VISIBLE
                        }
                    }
                }
                if(posts.size == 0) v.tv_empty_report.visibility = View.VISIBLE
                else v.tv_empty_report.visibility = View.GONE
                v.progressbar?.visibility = View.GONE
            })
    }

    override fun onStart() {
        super.onStart()
        setRecyclerView()
    }

    fun setRecyclerView() {
        rv_home.layoutManager = LinearLayoutManager(context)
        rv_home.adapter = PostAdapter(posts, context, { idPost, position, post, menu ->
            if (menu == REMOVE) {
                context?.showDialog("Hapus Laporan", "Yakin ingin menghapus laporan kamu ?") {
                    if(it) {
                        myDB.collection("post").document(idPost).delete().addOnSuccessListener {
                            context?.onSnackNotif(fl_container_home, "Laporan berhasil di hapus")
                        }
                    }
                }
            } else if (menu == SUPPORT) {
                myDB.collection("post").document(idPost).collection("supports").document(mAuth.currentUser?.uid.toString()).set(mapOf("status" to true))
            }
        } , { support, idPost ->
            if(support) myDB.collection("post").document(idPost).collection("supports").document(mAuth.currentUser?.uid.toString()).set(mapOf("status" to true))
            else myDB.collection("post").document(idPost).collection("supports").document(mAuth.currentUser?.uid.toString()).delete()
        })

        val dividerItemDecoration = DividerItemDecoration(
            rv_home.getContext(),
            LinearLayoutManager(context).getOrientation()
        )
        rv_home.addItemDecoration(dividerItemDecoration)
    }

    companion object {
        const val REMOVE = "hapus"
        const val SUPPORT = "dukung"
    }
}

package com.tribute.app.tobaccocontrol_ipm


import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.tribute.app.tobaccocontrol_ipm.post.Comment
import com.tribute.app.tobaccocontrol_ipm.post.Post
import com.tribute.app.tobaccocontrol_ipm.post.PostAdapter
import kotlinx.android.synthetic.main.fragment_profile.*
import androidx.recyclerview.widget.DividerItemDecoration
import android.view.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.fragment_profile.view.*
import java.lang.Exception


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ProfileFragment : Fragment(), View.OnClickListener {

    val posts: ArrayList<Post> = ArrayList()
    val postsIndex: ArrayList<String> = ArrayList()

    val myDB = FirebaseFirestore.getInstance()
    val storageRef = FirebaseStorage.getInstance().reference

    val mAuth = FirebaseAuth.getInstance()

    private val TAG = "PROFILE"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_profile, container, false)

        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setRecyclerView()

        posts.clear()
        setDataListener(view)
//        getPostData(view)
        setProfileData(view)

        setupOnClick()
    }

    fun setupOnClick() {
        tv_exit.setOnClickListener(this)
        tv_edit_profile.setOnClickListener(this)
    }

    fun setProfileData(v: View) {
         myDB.collection("users").document(mAuth.currentUser?.uid.toString()).addSnapshotListener(EventListener<DocumentSnapshot> { snapshot, e ->
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

                v.tv_location_profile.text = address
                v.tv_name_profile.text = name

                val newOption : RequestOptions by lazy {
                    RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .signature(ObjectKey(signaturePhoto))
                }

                try {
                    Glide.with(context!!)
                        .load(urlPhoto)
                        .apply(requestOptions)
                        .apply(newOption)
                        .into(v.iv_image_profile)
                } catch (e: Exception) {

                }
            } else {
                Log.d(TAG, "Current data: null")
            }
        })

    }

    fun setDataListener(v: View) {
        progressbar.visibility = View.VISIBLE
        myDB.collection("post").whereEqualTo("id_user", mAuth.currentUser?.uid.toString()).orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener(EventListener<QuerySnapshot> { snapshots, e ->
                if (e != null) {
                    if(posts.size == 0) v.tv_empty_report.visibility = View.VISIBLE
                    else v.tv_empty_report.visibility = View.GONE
                    v.progressbar.visibility = View.GONE
                    return@EventListener
                }

                for (dc in snapshots!!.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> {
                            val it = dc.document

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

                            posts.add(Post(idPost, idUser, urlPhoto, city, province, additionalLocationInfo, description, date, time, latitude, longitude, violationPlace, violationKind, ArrayList<Comment>()))
                            postsIndex.add(idPost)
                            v.rv_profile.adapter?.notifyDataSetChanged()
                        }
                        DocumentChange.Type.MODIFIED -> {
                            context?.toast(dc.document.id)
                            val index = postsIndex.indexOf(dc.document.id)
                            context?.toast(index.toString())
                            v.rv_profile.adapter?.notifyItemChanged(index)
                        }
                        DocumentChange.Type.REMOVED -> {
                            val position = dc.oldIndex
                            posts.removeAt(position)
                            postsIndex.removeAt(position)
                            v.rv_profile.adapter?.notifyItemRangeRemoved(position, posts.size)
                            if(position == 0) v.rv_profile.adapter?.notifyDataSetChanged()

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
                v.progressbar.visibility = View.GONE
                v.tv_report_number_profile.text = "${posts.size} Laporan"
            })
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.tv_exit -> {
                activity?.showDialog("Keluar", "Yakin mau keluar?") {
                    if(it) {
                        mAuth.signOut()
                        activity?.finishAffinity()
                        startActivity(Intent(context, LoginActivity::class.java))
                    }
                }
            }
            R.id.tv_edit_profile -> {
                startActivity(Intent(context, EditProfileActivity::class.java))
            }
        }
    }

    fun setRecyclerView() {
        rv_profile.layoutManager = LinearLayoutManager(context)
        rv_profile.adapter = PostAdapter(posts, context, { idPost, position, post, menu ->
            if (menu == HomeFragment.REMOVE) {
                context?.showDialog("Hapus Laporan", "Yakin ingin menghapus laporan kamu ?") {
                    if(it) {
                        myDB.collection("post").document(idPost).delete().addOnSuccessListener {
                            context?.onSnackNotif(fl_container_profile, "Laporan berhasil di hapus")
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
            LinearLayoutManager(context).getOrientation()
        )
        rv_profile.addItemDecoration(dividerItemDecoration)
    }
}

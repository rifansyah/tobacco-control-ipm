package com.tribute.app.pelajarmemantau.post

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.item_post.view.*
import java.lang.Exception
import android.view.MenuItem
import android.widget.PopupMenu
import androidx.core.view.get
import com.google.firebase.auth.FirebaseAuth
import com.tribute.app.pelajarmemantau.*


class PostAdapter(val items : ArrayList<Post>, val context: Context?, val menuListener: (idPost: String, position: Int, post: Post, menu: String) -> Unit, val supportListener: (support: Boolean, idPost: String) -> Unit) : RecyclerView.Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_post, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items.get(position), context)
        holder.setOnClick(items.get(position), context, position, menuListener, supportListener)
    }

    override fun getItemCount(): Int {
        return items.size
    }
}

open class ViewHolder (view: View) : RecyclerView.ViewHolder(view) {
    private val mAuth = FirebaseAuth.getInstance()

    val imagePost = view.iv_image_post
    val descriptionPost = view.tv_description
    val locationPost = view.tv_location
    val photoSender = view.iv_profile_image
    val nameSender = view.tv_name
    val llPostItem = view.ll_post_item
    val comment = view.tv_comment
    val date = view.tv_date_time
    val menu = view.tv_post_menu
    val support = view.iv_support
    val supportText = view.tv_support
    val violationPlace = view.tv_violation_place
    val violationKind = view.tv_violation_kind
    var isSupported = false

    fun setOnClick(post: Post, context: Context?, position: Int, menuListener: (idPost: String, position: Int, post: Post, menu: String) -> Unit, supportListener: (support: Boolean, idPost: String) -> Unit) {
        llPostItem.setOnClickListener{
            val intent = Intent(context, PostDetailActivity::class.java)
            intent.putExtra(PostDetailActivity.IDPOST, post.idPost)
            context?.startActivity(intent)
        }

        support.setOnClickListener {
            if(isSupported) {
                supportListener(false, post.idPost)
                support.setImageResource(R.drawable.ic_support)
                supportText.text = "${supportText.text.toString().toInt() - 1}"
            }
            else {
                supportListener(true, post.idPost)
                support.setImageResource(R.drawable.ic_support_activated)
                supportText.text = "${supportText.text.toString().toInt() + 1}"
            }
            isSupported = !isSupported
        }

        menu.setOnClickListener {
            //creating a popup menu
            val popup = PopupMenu(context, menu)
            //inflating menu from xml resource
            popup.inflate(R.menu.post_menu)
            if (post.idUser != mAuth.currentUser?.uid.toString() && context?.getUserRole() != MainActivity.ADMIN) popup.menu.get(1).setVisible(false)
            //adding click listener
            popup.setOnMenuItemClickListener(object : PopupMenu.OnMenuItemClickListener {
                override fun onMenuItemClick(item: MenuItem): Boolean {
                    when (item.getItemId()) {
                        R.id.post_support-> {
                            if(!isSupported) {
                                isSupported = true
                                support.setImageResource(R.drawable.ic_support_activated)
                                supportText.text = "${supportText.text.toString().toInt() + 1}"
                                menuListener(post.idPost, position, post, HomeFragment.SUPPORT)
                            }
                            return true
                        }
                        R.id.post_remove -> {
                            menuListener(post.idPost, position, post, HomeFragment.REMOVE)
                            return true
                        }
                        else -> return false
                    }
                }
            })
            //displaying the popup
            popup.show()
        }
    }

    fun bind(post : Post, context: Context?) {
        val myDb = FirebaseFirestore.getInstance()

        val requestOptions: RequestOptions by lazy {
            RequestOptions()
                .placeholder(R.drawable.ic_placeholder)
                .transforms(CenterCrop())
        }

        val requestProfileOptions: RequestOptions by lazy {
            RequestOptions()
                .placeholder(R.drawable.placeholder)
                .transforms(CenterCrop())
        }

        myDb.collection("users").document(post.idUser).get().addOnSuccessListener {
            try {
                Glide.with(context!!)
                    .load(it.get("url_photo").toString())
                    .apply(requestProfileOptions)
                    .into(photoSender)
            } catch (e: Exception) {
                Log.e("Post Adapter", e.message)
            }

            nameSender.text = it.get("name").toString()
        }

        try {
            Glide.with(context!!)
                .load(post.urlPhoto)
                .apply(requestOptions)
                .into(imagePost)
        } catch (e: Exception) {
            Log.e("Post Adapter", e.message)
        }

        myDb.collection("post").document(post.idPost).collection("supports").document(mAuth.currentUser?.uid.toString()).get().addOnSuccessListener {
            if(it.exists()) {
                support.setImageResource(R.drawable.ic_support_activated)
                isSupported = true
            }
            else {
                support.setImageResource(R.drawable.ic_support)
                isSupported = false
            }
        }

        myDb.collection("post").document(post.idPost).collection("supports").get().addOnSuccessListener {
            supportText.text = "${it.size()}"
        }

        myDb.collection("post").document(post.idPost).collection("comments").get().addOnSuccessListener { itCommentList ->
            val comments = ArrayList<Comment>()
            itCommentList.forEach { itComment ->
                val idComment = itComment.id
                val idCommenter = itComment.get("id_user").toString()
                val textComment = itComment.get("text").toString()

                val comment = Comment(idComment, idCommenter, textComment)
                comments.add(comment)
            }
            comment.text = "${comments.size}"
        }

        descriptionPost.text = post.descriptionPost
        locationPost.text = if (post.additionalLocationInfo.isEmpty()) "${post.city}, ${post.province}" else "${post.additionalLocationInfo}\n${post.city}, ${post.province}"
        date.text = "${post.date} - ${post.time}"
        violationPlace.text = post.violationPlace
        violationKind.text = post.violationKind
    }
}
package com.tribute.app.tobaccocontrol_ipm.post

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tribute.app.tobaccocontrol_ipm.*
import kotlinx.android.synthetic.main.item_comment.view.*
import java.lang.Exception

class CommentAdapter(val items : ArrayList<Comment>, val context: Context?, val idPost: String, val listener: (idPost: String, comment : Comment, position : Int) -> Unit) : RecyclerView.Adapter<CommentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        return CommentViewHolder(LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(items.get(position), context)
        holder.setOnClick(idPost, items.get(position), position, listener)
    }

}

class CommentViewHolder (view: View) : RecyclerView.ViewHolder(view) {
    val myDB = FirebaseFirestore.getInstance()
    val mAuth = FirebaseAuth.getInstance()

    val imageSender = view.iv_image_comment
    val nameSender = view.tv_name_comment
    val text = view.tv_text_comment

    val layout = view.cl_comment

    fun setOnClick(idPost: String, comment: Comment, position: Int, listener: (idPost: String, comment : Comment, position: Int) -> Unit) {
        layout.setOnClickListener{
            listener(idPost, comment, position)
        }
    }

    fun bind(comment: Comment, context: Context?) {
        text.text = comment.textComment

        myDB.collection("users").document(comment.idUser).get().addOnSuccessListener {
            val name = it.get("name").toString()
            val urlPhoto = it.get("url_photo").toString()

            nameSender.text = name

            try {
                Glide.with(context!!)
                    .load(urlPhoto)
                    .apply(requestOptions)
                    .into(imageSender)
            } catch (e: Exception) {

            }
        }
    }
}
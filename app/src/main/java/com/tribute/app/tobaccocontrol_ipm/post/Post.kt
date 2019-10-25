package com.tribute.app.tobaccocontrol_ipm.post

class Post(val idPost: String, val idUser : String, val urlPhoto : String, val city : String, val province : String, val descriptionPost : String, val date : String, val latitude: Double, val longitude : Double, val comments : ArrayList<Comment>) {

}
package com.tribute.app.tobaccocontrol_ipm.post

class Post(val idPost: String, val idUser : String, val urlPhoto : String, val city : String, val province : String, additionalLocationInfo : String, val descriptionPost : String, val date : String, val time : String, val latitude: Double, val longitude : Double, violationPlace : String, violationKind : String, val comments : ArrayList<Comment>)
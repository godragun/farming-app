package com.project.farmingapp.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
class SocialMediaViewModel: ViewModel() {

    private var firebaseAuth: FirebaseAuth? = null
    private var firebaseFireStore: FirebaseFirestore? = null

    var postLiveData = MutableLiveData<List<DocumentSnapshot>>()
    fun loadPosts(){

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseFireStore = FirebaseFirestore.getInstance()

        firebaseFireStore!!.collection("posts").get()
            .addOnSuccessListener {
//                postLiveData.value = it.documents[0].data.get("")
            }
            .addOnFailureListener {

            }

    }

}
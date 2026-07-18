package com.project.farmingapp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot


class ArticleViewModel : ViewModel() {

    var message1 = MutableLiveData<HashMap<String, Any>>()
    var message2 = MutableLiveData<String>()
    var message3 = MutableLiveData<List<DocumentSnapshot>>()
    var articleListener: ArticleListener? = null
    private var todoLiveData: LiveData<HashMap<String, Any>>? = null
    lateinit var firebaseDb: FirebaseFirestore

    fun getArticle(): MutableLiveData<HashMap<String, Any>> {
        Log.d("ArticleViewModelGet", message1.value.toString())
        return message1
    }

    fun getMyArticle(name: String) {

        firebaseDb = FirebaseFirestore.getInstance()

        Log.d("ArticleRepo1", "Ss")
        firebaseDb.collection("article_fruits").document("${name}")
            .get()
            .addOnSuccessListener {
                message1.value = it.data as HashMap<String, Any>?
                Log.d("ArticleViewModelDirect", message1.value.toString())
            }
            .addOnFailureListener {
                Log.d("ArticleRepo3", "ss")
            }
    }

    fun getAllArticles(name: String){

        if (message3.value.isNullOrEmpty()){
            firebaseDb = FirebaseFirestore.getInstance()
            firebaseDb.collection(name).get().addOnSuccessListener {
                message3.value = it.documents
                Log.d("I'm called4", "Yes")
            }.addOnFailureListener {
                message3.value = emptyList()
            }
        }

    }

    fun updateArticle(data: HashMap<String, Any>) {
        Log.d("ArticleViewModel", data.toString())
        message1.value = data
    }

}
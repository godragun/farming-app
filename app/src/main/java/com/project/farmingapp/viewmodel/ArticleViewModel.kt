package com.project.farmingapp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.project.farmingapp.supabase.RemoteDocument
import com.project.farmingapp.supabase.SupabaseDb
import java.util.concurrent.Executors

class ArticleViewModel : ViewModel() {

    var message1 = MutableLiveData<HashMap<String, Any>>()
    var message2 = MutableLiveData<String>()
    var message3 = MutableLiveData<List<RemoteDocument>>()
    var articleListener: ArticleListener? = null
    private var todoLiveData: LiveData<HashMap<String, Any>>? = null
    private val executor = Executors.newSingleThreadExecutor()

    fun getArticle(): MutableLiveData<HashMap<String, Any>> {
        Log.d("ArticleViewModelGet", message1.value.toString())
        return message1
    }

    fun getMyArticle(name: String) {
        executor.execute {
            try {
                Log.d("ArticleRepo1", "Ss")
                val doc = SupabaseDb.selectOne("article_fruits", SupabaseDb.eq("id", name))
                    ?: SupabaseDb.selectOne("article_fruits", SupabaseDb.eq("title", name))
                if (doc != null) {
                    @Suppress("UNCHECKED_CAST")
                    message1.postValue(HashMap(doc.data as Map<String, Any>))
                    Log.d("ArticleViewModelDirect", message1.value.toString())
                } else {
                    Log.d("ArticleRepo3", "ss")
                }
            } catch (e: Exception) {
                Log.d("ArticleRepo3", e.message ?: "ss")
            }
        }
    }

    fun getAllArticles(name: String) {
        if (message3.value.isNullOrEmpty()) {
            executor.execute {
                try {
                    val docs = SupabaseDb.select(name)
                    message3.postValue(docs)
                    Log.d("I'm called4", "Yes")
                } catch (_: Exception) {
                    message3.postValue(emptyList())
                }
            }
        }
    }

    fun updateArticle(data: HashMap<String, Any>) {
        Log.d("ArticleViewModel", data.toString())
        message1.value = data
    }
}

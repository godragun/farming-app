package com.project.farmingapp.model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.project.farmingapp.supabase.SupabaseDb
import com.project.farmingapp.viewmodel.ArticleViewModel
import java.util.concurrent.Executors

class ArticleRepository {
    var data = MutableLiveData<HashMap<String, Any>>()
    val data2 = MutableLiveData<String>()
    private lateinit var viewModel: ArticleViewModel
    private val executor = Executors.newSingleThreadExecutor()

    fun getSpecificFruitArticle(name: String): LiveData<String> {
        executor.execute {
            try {
                Log.d("ArticleRepo1", "Ss")
                val doc = SupabaseDb.selectOne("article_fruits", SupabaseDb.eq("id", name))
                    ?: SupabaseDb.selectOne("article_fruits", SupabaseDb.eq("title", name))
                if (doc != null) {
                    viewModel = ArticleViewModel()
                    @Suppress("UNCHECKED_CAST")
                    viewModel.updateArticle(HashMap(doc.data as Map<String, Any>))
                    data2.postValue("Success")
                    Log.d("ArticleRepo2", data2.value.toString())
                } else {
                    Log.d("ArticleRepo3", "ss")
                }
            } catch (e: Exception) {
                Log.d("ArticleRepo3", e.message ?: "ss")
            }
        }
        return data2
    }
}

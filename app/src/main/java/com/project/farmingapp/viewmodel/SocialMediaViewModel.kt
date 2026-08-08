package com.project.farmingapp.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.project.farmingapp.supabase.RemoteDocument
import com.project.farmingapp.supabase.SupabaseDb
import java.util.concurrent.Executors

class SocialMediaViewModel : ViewModel() {

    private val executor = Executors.newSingleThreadExecutor()
    var postLiveData = MutableLiveData<List<RemoteDocument>>()

    fun loadPosts() {
        executor.execute {
            try {
                val docs = SupabaseDb.select(
                    "posts",
                    SupabaseDb.order("time_stamp", ascending = false)
                )
                postLiveData.postValue(docs)
            } catch (_: Exception) {
                postLiveData.postValue(emptyList())
            }
        }
    }
}

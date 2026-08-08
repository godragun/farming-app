package com.project.farmingapp.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.project.farmingapp.supabase.RemoteDocument
import com.project.farmingapp.supabase.SupabaseDb
import java.util.concurrent.Executors

class UserProfilePostsViewModel : ViewModel() {

    private val executor = Executors.newSingleThreadExecutor()

    val userProfilePostsLiveData = MutableLiveData<ArrayList<HashMap<String, Any>>>()
    val userProfilePostsLiveData2 = MutableLiveData<List<String>>()

    val liveData1 = MutableLiveData<List<String>>()
    val liveData2 = MutableLiveData<ArrayList<RemoteDocument>>()
    val liveData3 = MutableLiveData<ArrayList<RemoteDocument>>()

    fun getUserPosts(userID: String?) {
        if (userID.isNullOrBlank()) return
        executor.execute {
            try {
                val user = SupabaseDb.selectOne("users", SupabaseDb.eq("email", userID), idKey = "email")
                @Suppress("UNCHECKED_CAST")
                val allPostsIDs = (user?.get("posts") as? List<*>)?.mapNotNull { it?.toString() }.orEmpty()
                userProfilePostsLiveData2.postValue(allPostsIDs)
                val localPostList = ArrayList<HashMap<String, Any>>()
                for (id in allPostsIDs) {
                    val post = SupabaseDb.selectOne("posts", SupabaseDb.eq("id", id))
                    if (post != null) {
                        @Suppress("UNCHECKED_CAST")
                        localPostList.add(HashMap(post.data as Map<String, Any>))
                    }
                }
                userProfilePostsLiveData.postValue(localPostList)
            } catch (e: Exception) {
                Log.d("User All Posts 2", "Some Failure Occured while fetching user posts")
            }
        }
    }

    fun getUserPostsIDs(userID: String?) {
        if (userID.isNullOrBlank()) return
        executor.execute {
            try {
                val user = SupabaseDb.selectOne("users", SupabaseDb.eq("email", userID), idKey = "email")
                @Suppress("UNCHECKED_CAST")
                liveData1.postValue((user?.get("posts") as? List<*>)?.mapNotNull { it?.toString() }.orEmpty())
            } catch (_: Exception) {
            }
        }
    }

    fun getAllPostsOfUser(listOfIDs: List<String>) {
        executor.execute {
            val someList = ArrayList<RemoteDocument>()
            for (id in listOfIDs) {
                try {
                    val doc = SupabaseDb.selectOne("posts", SupabaseDb.eq("id", id))
                    if (doc != null) someList.add(doc)
                } catch (_: Exception) {
                }
            }
            liveData2.postValue(someList)
        }
    }

    fun getAllPosts(userId: String?) {
        if (userId.isNullOrBlank()) return
        executor.execute {
            try {
                val docs = SupabaseDb.select("posts", SupabaseDb.eq("user_id", userId))
                liveData3.postValue(ArrayList(docs))
                Log.d("UserPrlPostsViewModel", "Updated data")
            } catch (_: Exception) {
                Log.d("Error", "Error in all docs")
            }
        }
    }
}

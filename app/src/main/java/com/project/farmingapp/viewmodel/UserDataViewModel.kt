package com.project.farmingapp.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.project.farmingapp.supabase.RemoteDocument
import com.project.farmingapp.supabase.SupabaseDb
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

class UserDataViewModel : ViewModel() {

    private val executor = Executors.newSingleThreadExecutor()
    var userliveData = MutableLiveData<RemoteDocument>()

    fun getUserData(userId: String) {
        executor.execute {
            try {
                // Profiles are keyed by email in the Android app.
                val doc = SupabaseDb.selectOne("users", SupabaseDb.eq("email", userId), idKey = "email")
                    ?: SupabaseDb.selectOne("users", SupabaseDb.eq("id", userId))
                if (doc != null) {
                    userliveData.postValue(doc)
                }
            } catch (e: Exception) {
                Log.e("UserDataViewModel", "Failed to load user", e)
            }
        }
    }

    fun updateUserField(context: Context, userID: String, about: String?, city: String?) {
        executor.execute {
            try {
                val patch = JSONObject()
                if (about != null) patch.put("about", about)
                if (city != null) patch.put("city", city)
                if (patch.length() > 0) {
                    SupabaseDb.update("users", SupabaseDb.eq("email", userID), patch)
                    getUserData(userID)
                }
            } catch (e: Exception) {
                Log.d("UserDataViewModel", "Failed to Update User Data")
            }
        }
        Toast.makeText(context, "Profile Updated", Toast.LENGTH_SHORT).show()
    }

    fun deleteUserPost(userId: String, postId: String) {
        executor.execute {
            try {
                SupabaseDb.delete("posts", SupabaseDb.eq("id", postId))
                val user = SupabaseDb.selectOne("users", SupabaseDb.eq("email", userId), idKey = "email")
                val posts = (user?.getList<String>("posts") ?: emptyList()).toMutableList()
                posts.remove(postId)
                val patch = JSONObject().put("posts", JSONArray(posts))
                SupabaseDb.update("users", SupabaseDb.eq("email", userId), patch)
                getUserData(userId)
                Log.d("UserDataViewModel", "Post Deleted")
            } catch (e: Exception) {
                Log.d("User Data View Model", "Failed to delete post")
            }
        }
    }
}

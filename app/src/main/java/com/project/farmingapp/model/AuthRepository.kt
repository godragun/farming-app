package com.project.farmingapp.model

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.project.farmingapp.supabase.SupabaseAuth
import java.io.Serializable
import java.util.concurrent.Executors

/**
 * Authentication via Supabase Auth (replaces Firebase Auth + Firestore user docs).
 */
class AuthRepository {

    private val executor = Executors.newSingleThreadExecutor()

    fun signInWithEmail(
        context: Context,
        email: String,
        password: String,
        otherData: HashMap<String, Serializable?>
    ): LiveData<String> {
        val data = MutableLiveData<String>()
        executor.execute {
            val result = SupabaseAuth.signUpWithEmail(
                context = context.applicationContext,
                email = email,
                password = password,
                profile = otherData.mapValues { it.value }
            )
            data.postValue(if (result.success) "Success" else result.message)
        }
        return data
    }

    fun signInToGoogle(
        context: Context,
        idToken: String,
        email: String,
        otherData: HashMap<String, Serializable?>
    ): LiveData<String> {
        val data = MutableLiveData<String>()
        executor.execute {
            val result = SupabaseAuth.signInWithGoogleIdToken(
                context = context.applicationContext,
                idToken = idToken,
                email = email,
                profile = otherData.mapValues { it.value }
            )
            data.postValue(if (result.success) "Success" else result.message)
        }
        return data
    }

    fun logInWithEmail(
        context: Context,
        email: String,
        password: String
    ): LiveData<String> {
        val data = MutableLiveData<String>()
        executor.execute {
            val result = SupabaseAuth.loginWithEmail(
                context = context.applicationContext,
                email = email,
                password = password
            )
            data.postValue(if (result.success) "Success" else result.message)
        }
        return data
    }
}

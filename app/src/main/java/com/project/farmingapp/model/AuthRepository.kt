package com.project.farmingapp.model

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.project.farmingapp.supabase.SupabaseAuth
import com.project.farmingapp.supabase.SupabaseAuthResult
import com.project.farmingapp.supabase.SupabaseConfig
import java.io.Serializable
import java.util.concurrent.Executors

/**
 * Authentication via Supabase Auth (replaces Firebase Auth + Firestore user docs).
 *
 * Every call runs on a worker thread. An exception escaping that thread would crash the whole
 * app, so each call is wrapped: failures are reported through the returned LiveData as a
 * user-readable message instead.
 */
class AuthRepository {

    private val executor = Executors.newSingleThreadExecutor()

    private fun run(data: MutableLiveData<String>, call: () -> SupabaseAuthResult) {
        executor.execute {
            val message = try {
                if (!SupabaseConfig.isConfigured()) {
                    "Supabase is not configured. Add SUPABASE_URL and SUPABASE_ANON_KEY to local.properties."
                } else {
                    val result = call()
                    if (result.success) "Success" else result.message.ifBlank { "Request failed" }
                }
            } catch (e: Exception) {
                Log.e("AuthRepository", "Auth request failed", e)
                e.message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again."
            } finally {
                executor.shutdown()
            }
            data.postValue(message)
        }
    }

    fun signInWithEmail(
        context: Context,
        email: String,
        password: String,
        otherData: HashMap<String, Serializable?>
    ): LiveData<String> {
        val data = MutableLiveData<String>()
        val appContext = context.applicationContext
        run(data) {
            SupabaseAuth.signUpWithEmail(
                context = appContext,
                email = email,
                password = password,
                profile = otherData.mapValues { it.value }
            )
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
        val appContext = context.applicationContext
        run(data) {
            SupabaseAuth.signInWithGoogleIdToken(
                context = appContext,
                idToken = idToken,
                email = email,
                profile = otherData.mapValues { it.value }
            )
        }
        return data
    }

    fun logInWithEmail(
        context: Context,
        email: String,
        password: String
    ): LiveData<String> {
        val data = MutableLiveData<String>()
        val appContext = context.applicationContext
        run(data) {
            SupabaseAuth.loginWithEmail(
                context = appContext,
                email = email,
                password = password
            )
        }
        return data
    }
}

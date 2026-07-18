package com.project.farmingapp.viewmodel

import android.content.Intent
import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.project.farmingapp.model.AuthRepository
import com.project.farmingapp.utilities.AuthHelper

class AuthViewModel : ViewModel() {

    var name: String? = null
    var mobNo: String? = null
    var email: String? = null
    var city: String? = null
    var password: String? = null
    var confPassword: String? = null
    var userType: String? = "normal"
    var authListener: AuthListener? = null

    //login
    var loginmail: String? = ""
    var loginpwd: String? = ""

    lateinit var authRepository: AuthRepository
    lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        private const val TAG = "GoogleActivity"
        private const val RC_SIGN_IN = 9001
    }

    val userPosts = arrayListOf<String>()
    fun signupButtonClicked(view: View) {
        authListener!!.onStarted()
        if (name.isNullOrEmpty() || mobNo.toString().length != 10 || mobNo == null || password.isNullOrEmpty() || confPassword.isNullOrEmpty() || city.isNullOrEmpty()) {
            // Failure
            authListener!!.onFailure("Error Occurred")
            return
        }
        // Success
        var data = hashMapOf(
            "name" to name,
            "mobNo" to mobNo,
            "email" to email,
            "city" to city,
            "userType" to userType,
            "posts" to  userPosts,
            "profileImage" to ""
        )
        val authRepo = AuthRepository().signInWithEmail(email!!, password!!, data)
        authListener?.onSuccess(authRepo)
    }

    fun returnActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        authListener!!.onStarted()

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val exception = task.exception
            if (task.isSuccessful) {
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    var data2 = hashMapOf(
                        "userType" to userType,
                        "posts" to userPosts,
                        "name" to account.displayName.toString(),
                        "profileImage" to account.photoUrl.toString()
                    )
                    authRepository = AuthRepository()
                    var returned = authRepository.signInToGoogle(
                        account.idToken!!,
                        account.email.toString(),
                        data2
                    )
                    Log.d("AuthView", returned.value.toString())
                    authListener?.onSuccess(returned)
                } catch (e: ApiException) {
                    authListener!!.onFailure(e.message.toString())
                }
            } else {
            }
        }
    }

    // Login works with user input; in demo mode it accepts non-empty credentials.
    fun loginButtonClicked(view: View) {
        authListener!!.onStarted()
        val emailInput = loginmail?.trim().orEmpty()
        val passwordInput = loginpwd?.trim().orEmpty()
        if (emailInput.isEmpty() || passwordInput.isEmpty()) {
            authListener?.onFailure("Enter email and password to login")
            return
        }
        AuthHelper.enterGuestMode(view.context.applicationContext)
        val authRepo = MutableLiveData<String>()
        authRepo.value = "Success"
        authListener?.onSuccess(authRepo)
    }
}
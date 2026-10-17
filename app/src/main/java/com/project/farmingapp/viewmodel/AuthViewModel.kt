package com.project.farmingapp.viewmodel

import android.content.Intent
import android.util.Log
import android.view.View
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
        val listener = authListener ?: return
        listener.onStarted()
        val emailInput = email?.trim().orEmpty()
        val passwordInput = password.orEmpty()
        if (name.isNullOrBlank() || mobNo?.trim()?.length != 10 || emailInput.isEmpty() ||
            passwordInput.isEmpty() || confPassword.isNullOrEmpty() || city.isNullOrBlank()
        ) {
            listener.onFailure("Please fill in all fields (mobile number must be 10 digits).")
            return
        }
        if (passwordInput != confPassword) {
            listener.onFailure("Passwords do not match.")
            return
        }
        val data = hashMapOf<String, java.io.Serializable?>(
            "name" to name,
            "mobNo" to mobNo,
            "email" to emailInput,
            "city" to city,
            "userType" to userType,
            "posts" to userPosts,
            "profileImage" to ""
        )
        val authRepo = AuthRepository().signInWithEmail(
            view.context.applicationContext,
            emailInput,
            passwordInput,
            data
        )
        listener.onSuccess(authRepo)
    }

    fun returnActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != RC_SIGN_IN) return
        val listener = authListener ?: return
        listener.onStarted()

        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                .getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (account == null || idToken.isNullOrBlank()) {
                listener.onFailure("Google sign-in did not return a valid account. Please try again.")
                return
            }
            val data2 = hashMapOf<String, java.io.Serializable?>(
                "userType" to userType,
                "posts" to userPosts,
                "name" to (account.displayName ?: ""),
                "profileImage" to (account.photoUrl?.toString() ?: "")
            )
            authRepository = AuthRepository()
            val returned = authRepository.signInToGoogle(
                googleSignInClient.applicationContext,
                idToken,
                account.email.orEmpty(),
                data2
            )
            listener.onSuccess(returned)
        } catch (e: ApiException) {
            // Includes the user cancelling the account picker (status code 12501).
            listener.onFailure(
                if (e.statusCode == 12501) "Google sign-in was cancelled."
                else "Google sign-in failed (code ${e.statusCode}). Please try again."
            )
        } catch (e: UninitializedPropertyAccessException) {
            listener.onFailure("Google sign-in client not ready")
        } catch (e: Exception) {
            listener.onFailure(e.message ?: "Google sign-in failed")
        }
    }

    fun loginButtonClicked(view: View) {
        val listener = authListener ?: return
        listener.onStarted()
        val emailInput = loginmail?.trim().orEmpty()
        val passwordInput = loginpwd?.trim().orEmpty()
        if (emailInput.isEmpty() || passwordInput.isEmpty()) {
            authListener?.onFailure("Enter email and password to login")
            return
        }
        // Demo offline shortcut: password "guest" enters offline guest mode.
        if (passwordInput.equals("guest", ignoreCase = true)) {
            AuthHelper.enterGuestMode(view.context.applicationContext)
            val authRepo = androidx.lifecycle.MutableLiveData<String>()
            authRepo.value = "Success"
            authListener?.onSuccess(authRepo)
            return
        }
        val authRepo = AuthRepository().logInWithEmail(
            view.context.applicationContext,
            emailInput,
            passwordInput
        )
        authListener?.onSuccess(authRepo)
    }
}

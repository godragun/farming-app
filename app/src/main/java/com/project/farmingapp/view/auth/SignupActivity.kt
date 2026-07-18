package com.project.farmingapp.view.auth

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.bumptech.glide.Glide
import com.project.farmingapp.R
import com.project.farmingapp.utilities.AgriAssetPaths
import com.project.farmingapp.databinding.ActivitySignupBinding
import com.project.farmingapp.utilities.AuthHelper
import com.project.farmingapp.utilities.hide
import com.project.farmingapp.utilities.show
import com.project.farmingapp.utilities.toast
import com.project.farmingapp.view.dashboard.DashboardActivity
import com.project.farmingapp.viewmodel.AuthListener
import com.project.farmingapp.viewmodel.AuthViewModel

class SignupActivity : AppCompatActivity(), AuthListener {

    lateinit var googleSignInClient: GoogleSignInClient
    val firebaseAuth = FirebaseAuth.getInstance()
    lateinit var viewModel: AuthViewModel
    private lateinit var binding: ActivitySignupBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AuthHelper.syncFromPrefs(this)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_signup)
        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        binding.authViewModel = viewModel
        viewModel.authListener = this

        Glide.with(this)
            .load(AgriAssetPaths.fileUri(AgriAssetPaths.LOGIN_SIGN_HERO))
            .placeholder(R.drawable.hero_banner_bg)
            .error(R.drawable.hero_banner_bg)
            .centerCrop()
            .into(binding.signupHeroBg)

        binding.loginRedirectTextSignup.setOnClickListener {
            Intent(this, LoginActivity::class.java).also {
                startActivity(it)
            }
        }

        binding.signGoogleBtnSignup.setOnClickListener {
            signIn()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.returnActivityResult(requestCode, resultCode, data)
    }

    fun signIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    companion object {
        private const val TAG = "GoogleActivity"
        private const val RC_SIGN_IN = 9001
    }

    override fun onStarted() {
        binding.progressSignup.show()
    }

    override fun onSuccess(authRepo: LiveData<String>) {
        authRepo.observe(this, Observer {
            binding.progressSignup.hide()
            if (it.toString() == "Success") {
                toast("Account Created")
                if (firebaseAuth.currentUser != null) {
                    AuthHelper.exitGuestMode(this)
                }
                Intent(this, DashboardActivity::class.java).also {
                    startActivity(it)
                }
            }
        })
    }

    override fun onFailure(message: String) {
        binding.progressSignup.hide()
        toast("Failure")
    }
}
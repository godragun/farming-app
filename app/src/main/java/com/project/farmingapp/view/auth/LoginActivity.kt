package com.project.farmingapp.view.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.project.farmingapp.R
import com.project.farmingapp.databinding.ActivityLoginBinding
import com.project.farmingapp.supabase.SupabaseAuth
import com.project.farmingapp.supabase.SupabaseSession
import com.project.farmingapp.utilities.AgriAssetPaths
import com.project.farmingapp.utilities.AuthHelper
import com.project.farmingapp.utilities.hide
import com.project.farmingapp.utilities.show
import com.project.farmingapp.utilities.showAgriMessage
import com.project.farmingapp.utilities.toast
import com.project.farmingapp.view.dashboard.DashboardActivity
import com.project.farmingapp.view.introscreen.IntroActivity
import com.project.farmingapp.viewmodel.AuthListener
import com.project.farmingapp.viewmodel.AuthViewModel
import java.util.concurrent.Executors

class LoginActivity : AppCompatActivity(), AuthListener {
    lateinit var googleSignInClient: GoogleSignInClient
    lateinit var viewModel: AuthViewModel
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AuthHelper.syncFromPrefs(this)
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val introSchemaDone = sharedPreferences.getInt(IntroActivity.PREF_INTRO_SCHEMA_VERSION, 0)
        if (introSchemaDone < IntroActivity.INTRO_SCHEMA_VERSION) {
            startActivity(Intent(this, IntroActivity::class.java))
            finish()
            return
        }
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        binding.authViewModel = viewModel
        viewModel.authListener = this

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        viewModel.googleSignInClient = googleSignInClient

        Glide.with(this)
            .load(AgriAssetPaths.fileUri(AgriAssetPaths.LOGIN_SIGN_HERO))
            .placeholder(R.drawable.hero_banner_bg)
            .error(R.drawable.hero_banner_bg)
            .centerCrop()
            .into(binding.loginHeroBg)

        if (SupabaseSession.isLoggedIn()) {
            AuthHelper.exitGuestMode(this)
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return
        }

        binding.createaccountText.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
        binding.signGoogleBtnLogin.setOnClickListener {
            signIn()
        }

        binding.forgotPasswdTextLogin.setOnClickListener {
            val userEmail = binding.emailEditLogin.text.toString()
            if (userEmail.isNullOrEmpty()) {
                showAgriMessage("Please enter your Email")
            } else {
                Executors.newSingleThreadExecutor().execute {
                    val result = SupabaseAuth.sendPasswordReset(userEmail)
                    runOnUiThread {
                        showAgriMessage(result.message)
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.returnActivityResult(requestCode, resultCode, data)
    }

    fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val a = Intent(Intent.ACTION_MAIN)
        a.addCategory(Intent.CATEGORY_HOME)
        a.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(a)
    }

    companion object {
        private const val RC_SIGN_IN = 9001
    }

    override fun onStarted() {
        binding.progressLogin.show()
    }

    override fun onSuccess(authRepo: LiveData<String>) {
        authRepo.observe(this, Observer {
            binding.progressLogin.hide()
            val result = it?.toString()?.trim().orEmpty()
            if (result.equals("Success", ignoreCase = true)) {
                toast("Logged In")
                if (SupabaseSession.isLoggedIn()) {
                    AuthHelper.exitGuestMode(this)
                }
                startActivity(Intent(this, DashboardActivity::class.java))
            } else {
                showAgriMessage(if (result.isNotEmpty()) result else "Login failed")
            }
        })
    }

    override fun onFailure(message: String) {
        binding.progressLogin.hide()
        showAgriMessage(message)
    }
}

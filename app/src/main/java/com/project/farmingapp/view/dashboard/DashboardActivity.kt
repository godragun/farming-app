package com.project.farmingapp.view.dashboard


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.os.SystemClock
import android.provider.Settings
import android.service.autofill.UserData
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.project.farmingapp.R
import com.project.farmingapp.adapter.CurrentWeatherAdapter
import com.project.farmingapp.adapter.WeatherAdapter
import com.project.farmingapp.databinding.ActivityDashboardBinding
import com.project.farmingapp.utilities.AgriAssetPaths
import com.project.farmingapp.utilities.AuthHelper
import com.project.farmingapp.supabase.SupabaseAuth
import com.project.farmingapp.supabase.SupabaseSession
import com.project.farmingapp.model.WeatherApi
import com.project.farmingapp.model.data.Weather
import com.project.farmingapp.model.data.WeatherList
import com.project.farmingapp.model.data.WeatherRootList
import com.project.farmingapp.utilities.pulseTapFeedback
import com.project.farmingapp.view.apmc.ApmcFragment
import com.project.farmingapp.view.articles.ArticleListFragment
import com.project.farmingapp.view.articles.FruitsFragment
import com.project.farmingapp.view.auth.LoginActivity
import com.project.farmingapp.view.ecommerce.*
import com.project.farmingapp.view.introscreen.IntroActivity
import com.project.farmingapp.view.socialmedia.SMCreatePostFragment
import com.project.farmingapp.view.socialmedia.SocialMediaPostsFragment
import com.project.farmingapp.view.chatbot.ChatbotFragment
import com.project.farmingapp.view.survey.SavedSurveysFragment
import com.project.farmingapp.view.survey.SurveyDetailFragment
import com.project.farmingapp.view.survey.SurveyFormFragment
import com.project.farmingapp.view.user.UserFragment
import com.project.farmingapp.view.weather.WeatherFragment
import com.project.farmingapp.viewmodel.UserDataViewModel
import com.project.farmingapp.viewmodel.UserProfilePostsViewModel
import com.project.farmingapp.viewmodel.WeatherViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import com.squareup.picasso.Picasso
import com.squareup.picasso.PicassoProvider
import org.w3c.dom.Document
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.util.*
import kotlin.collections.ArrayList

class DashboardActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, View.OnClickListener, com.google.android.gms.location.LocationListener  {
    lateinit var cartFragment: CartFragment
    lateinit var ecommerceItemFragment: EcommerceItemFragment
    lateinit var paymentFragment: PaymentFragment
    lateinit var dashboardFragment: dashboardFragment
    lateinit var ecommerceFragment: EcommerceFragment
    lateinit var weatherFragment: WeatherFragment
    lateinit var navController: NavController
    lateinit var toggle: ActionBarDrawerToggle
    lateinit var blankFragment1: WeatherFragment
    lateinit var apmcFragment: ApmcFragment
    lateinit var articleListFragment: ArticleListFragment
    lateinit var myOrdersFragment: MyOrdersFragment
    lateinit var userFragment: UserFragment
    lateinit var socialMediaPostFragment: SocialMediaPostsFragment
    lateinit var smCreatePostFragment: SMCreatePostFragment
    lateinit var chatbotFragment: ChatbotFragment
    private lateinit var viewModel: UserDataViewModel
    private lateinit var viewModel2: UserProfilePostsViewModel
    private lateinit var weatherViewModel: WeatherViewModel
    lateinit var sharedPreferences: SharedPreferences
    private lateinit var binding: ActivityDashboardBinding
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var chatbotFab: FloatingActionButton


    var userName = ""
    var data: WeatherRootList? = null
    var firstTime: Boolean? = null
    private var darkModeEnabled: Boolean = false

    private var REQUEST_LOCATION_CODE = 101
    private var mGoogleApiClient: GoogleApiClient? = null
    private var mLocation: Location? = null
    private var mLocationRequest: LocationRequest? = null
    private val UPDATE_INTERVAL = (30 * 1000).toLong()
    private val FASTEST_INTERVAL: Long = 10000
    private val defaultSriLankaLat = "6.9271"
    private val defaultSriLankaLon = "79.8612"
    private val defaultSriLankaCity = "Colombo"

    private val locationHandler = Handler(Looper.getMainLooper())
    private val automatedClickRunnable = Runnable { automatedClick() }
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private var lastWeatherLocationRequestMs: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dark mode is initialised in FarmingApp (Application class) so it is guaranteed
        // to be active before super.onCreate() locks in the theme configuration.
        // We only read the pref here to keep darkModeEnabled in sync for the toggle label.
        darkModeEnabled = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            .getBoolean("darkModeEnabled", false)
        AuthHelper.syncFromPrefs(this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_dashboard)
        viewModel = ViewModelProvider(this)[UserDataViewModel::class.java]
        binding.userDataViewModel = viewModel
        bottomNav = binding.root.findViewById(R.id.bottomNav)
        chatbotFab = binding.root.findViewById(R.id.chatbotFab)
        chatbotFab.imageTintList = null
        Glide.with(this)
            .load(AgriAssetPaths.fileUri(AgriAssetPaths.LOGO_TRANSPARENT))
            .fitCenter()
            .override(112, 112)
            .error(R.drawable.ic_baseline_edit_24)
            .into(chatbotFab)


        toggle = ActionBarDrawerToggle(this, binding.drawerLayout, R.string.open, R.string.close)
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

         weatherViewModel = ViewModelProvider(this)[WeatherViewModel::class.java]

        viewModel2 = ViewModelProvider(this)[UserProfilePostsViewModel::class.java]
//        viewModel2.getAllPosts(AuthHelper.requireEmail())

        // Avoid deprecated GoogleApiClient location APIs on main thread (ANR risk).
        // Dashboard uses FusedLocationProviderClient asynchronously instead.

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        firstTime = sharedPreferences.getBoolean("firstTime", false)

        ensureSignedInOrRedirect()
        if (!AuthHelper.isSignedIn()) return

        if (AuthHelper.guestMode) {
            val header = binding.navView.getHeaderView(0)
            header.findViewById<TextView>(R.id.navbarUserName).text = AuthHelper.GUEST_NAME
            header.findViewById<TextView>(R.id.navbarUserEmail).text = AuthHelper.GUEST_EMAIL
            header.findViewById<TextView>(R.id.cityTextNavHeader).text = "City: "
            header.findViewById<TextView>(R.id.navBarUserPostCount).text = "Posts Count: 0"
        } else {
            viewModel.getUserData(SupabaseSession.email ?: AuthHelper.requireEmail())
        }

        binding.navView.setNavigationItemSelectedListener(this)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Agri Lanka"

        val navHeader = binding.navView.getHeaderView(0)
        navHeader.findViewById<ImageView>(R.id.navHeaderBrandLogo)?.let { logoIv ->
            Glide.with(this)
                .load(AgriAssetPaths.fileUri(AgriAssetPaths.LOGO_TRANSPARENT))
                .fitCenter()
                .override(120, 120)
                .into(logoIv)
        }


        ecommerceItemFragment=EcommerceItemFragment()
        dashboardFragment = dashboardFragment()
        weatherFragment = WeatherFragment()

        if (savedInstanceState == null) {
            openScreen(dashboardFragment, "dashFrag", addToBackStack = false)
        }
        binding.root.post { updateChatbotFabVisibility() }
        supportFragmentManager.addOnBackStackChangedListener {
            updateChatbotFabVisibility()
        }

        bottomNav.selectedItemId = R.id.bottomNavHome

        val something = binding.navView.getHeaderView(0);

        if (dashboardFragment.isVisible) {
            bottomNav.selectedItemId = R.id.bottomNavHome
        }

        something.setOnClickListener {
            Toast.makeText(this, "You Clicked Slider", Toast.LENGTH_LONG).show()

            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                super.onBackPressed()
            }
            userFragment = UserFragment()
            openScreen(userFragment, "userFrag")
        }

        apmcFragment = ApmcFragment()
        socialMediaPostFragment = SocialMediaPostsFragment()
        ecommerceFragment=EcommerceFragment()
        paymentFragment = PaymentFragment()
        cartFragment= CartFragment()
        myOrdersFragment=MyOrdersFragment()
        chatbotFragment = ChatbotFragment()

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.bottomNavAPMC -> {
                    openScreen(apmcFragment, "apmcFrag")
                }
                R.id.bottomNavHome -> {
                    openScreen(dashboardFragment, "dashFrag")
                }
                R.id.bottomNavEcomm -> {
                    openScreen(ecommerceFragment, "ecommItemFrag")
                }
                R.id.bottomNavPost -> {
                    openScreen(socialMediaPostFragment, "socialFrag")
                }
            }
            true
        }

        chatbotFab.setOnClickListener {
            chatbotFab.pulseTapFeedback()
            chatbotFragment = ChatbotFragment()
            openScreen(chatbotFragment, "chatbotFabFrag")
        }

        viewModel.userliveData.observe(this, Observer {

            val something = binding.navView.getHeaderView(0)
            @Suppress("UNCHECKED_CAST")
            val posts = (it.get("posts") as? List<String>) ?: emptyList()
            val city = it.get("city")
            userName = it.get("name")?.toString() ?: ""

//            val allPosts = viewModel2.liveData3.value as ArrayList<RemoteDocument>

            if(city == null){
                something.findViewById<TextView>(R.id.cityTextNavHeader).text ="City: "
            } else{
                something.findViewById<TextView>(R.id.cityTextNavHeader).text ="City: " +  it.get("city").toString()
            }

            something.findViewById<TextView>(R.id.navbarUserName).text = userName
            something.findViewById<TextView>(R.id.navbarUserEmail).text = AuthHelper.requireEmail()
            Glide.with(this).load(it.get("profileImage")).into(something.findViewById(R.id.navbarUserImage))

            Log.d("User Data from VM", it.getString("name") ?: "")

            something.findViewById<TextView>(R.id.navBarUserPostCount).text = "Posts Count: " + posts.size.toString()
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setCurrentFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.frame_layout, fragment)

            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            setReorderingAllowed(true)
            addToBackStack("name")
            commit()
        }
    }

    private fun openScreen(fragment: Fragment, tag: String, addToBackStack: Boolean = true) {
        val tx = supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(R.id.frame_layout, fragment, tag)
            .setReorderingAllowed(true)
        if (addToBackStack) {
            tx.addToBackStack(tag)
        }
        tx.commit()
    }


    override fun onPostCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onPostCreate(savedInstanceState, persistentState)
        toggle.syncState()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        bottomNav.selectedItemId = R.id.bottomNavHome
        when (item.itemId) {

            R.id.miItem1 -> {
                    ecommerceFragment = EcommerceFragment()
                    openScreen(ecommerceFragment, "ecommListFrag")
            }
            R.id.miItem2 -> {
                apmcFragment = ApmcFragment()
                openScreen(apmcFragment, "apmcFrag")
            }
            R.id.miItem3 ->{
                smCreatePostFragment = SMCreatePostFragment()
                openScreen(smCreatePostFragment, "createPostFrag")
            }
            R.id.miItem4 -> {
                socialMediaPostFragment = SocialMediaPostsFragment()
                openScreen(socialMediaPostFragment, "socialFrag")
            }
            R.id.miItem5 -> {
                weatherFragment = WeatherFragment()
                openScreen(weatherFragment, "weatherFrag")
            }
            R.id.miItem6 -> {
                articleListFragment = ArticleListFragment()
                openScreen(articleListFragment, "articleListFrag")
            }
            R.id.miItem7 -> {
                openScreen(myOrdersFragment, "myOrdersFrag")
            }
            R.id.miItem8 -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Log Out")
                    .setMessage("Do you want to log out?")
                    .setPositiveButton("Yes") { dialogInterface, i ->
                        SupabaseAuth.signOut(this@DashboardActivity)
                        AuthHelper.exitGuestMode(this@DashboardActivity)
                        Toast.makeText(this, "Logged Out", Toast.LENGTH_LONG).show()
                        Intent(this, LoginActivity::class.java).also {
                            startActivity(it)
                        }
                    }
                    .setNegativeButton("No") { dialogInterface, i ->
                    }
                    .show()
            }
            R.id.miItem9 -> {
                chatbotFragment = ChatbotFragment()
                openScreen(chatbotFragment, "chatbotFrag")
            }
            R.id.miItemSurvey -> {
                supportFragmentManager.popBackStack(
                    "surveyFormFrag",
                    androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
                )
                openScreen(SurveyFormFragment.newInstance(), "surveyFormFrag")
            }
            R.id.miItemSavedSurveys -> {
                openScreen(SavedSurveysFragment(), "savedSurveysFrag")
            }
            R.id.miItemSettings -> {
                openScreen(com.project.farmingapp.view.settings.SettingsFragment(), "settingsFrag")
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (dashboardFragment.isVisible) {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                
            }
        }
    }


    fun automatedClick(){

        if (!checkGPSEnabled()) {
            publishDefaultSriLankaLocation()
            return
        }

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                getLocation();

            } else {
                //Request Location Permission
                checkLocationPermission()
            }
        } else {
            getLocation();
//            buildGoogleApiClient()
        }
    }

    override fun onClick(v: View?) {
        if (!checkGPSEnabled()) {
            publishDefaultSriLankaLocation()
            return
        }

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                getLocation();
            } else {
                //Request Location Permission
                checkLocationPermission()
            }
        } else {
            getLocation();
        }
    }
    @SuppressLint("MissingPermission")
    private fun getLocation() {
        val googleApiClient = mGoogleApiClient
        if (googleApiClient == null) {
            publishDefaultSriLankaLocation()
            return
        }
        mLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient)

        if (mLocation == null) {
            startLocationUpdates()
            publishDefaultSriLankaLocation()
        }
        if (mLocation != null) {
            publishWeatherCoordinates(mLocation)
        } else {
            Toast.makeText(this, "Location not detected. Using Colombo.", Toast.LENGTH_SHORT).show()
        }
    }
    private fun startLocationUpdates() {
        // Create the location request
        mLocationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(UPDATE_INTERVAL)
            .setFastestInterval(FASTEST_INTERVAL)
        // Request location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient!!, mLocationRequest!!, this)
    }

    private fun stopLocationUpdates() {
        val googleApiClient = mGoogleApiClient ?: return
        if (!googleApiClient.isConnected) return
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this)
    }

    override fun onLocationChanged(p0: Location) {
        mLocation = p0
        publishWeatherCoordinates(p0)
        stopLocationUpdates()
    }

    @Synchronized
    private fun buildGoogleApiClient() {
        if (mGoogleApiClient != null) return
        mGoogleApiClient = GoogleApiClient.Builder(this)
            .addApi(LocationServices.API)
            .build()
        mGoogleApiClient?.connect()
    }

    private fun checkGPSEnabled(): Boolean {
        return isLocationEnabled()
    }

    private fun showAlert() {
        val dialog = android.app.AlertDialog.Builder(this)
        dialog.setTitle("Enable Location")
            .setMessage("Your Locations Settings is set to 'Off'.\nPlease Enable Location to use this app!")
            .setPositiveButton("Location Settings") { paramDialogInterface, paramInt ->
                val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(myIntent)
            }
            .setNegativeButton("Cancel") { paramDialogInterface, paramInt -> }
        dialog.show()
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER)
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                android.app.AlertDialog.Builder(this)
                    .setTitle("Location Permission Needed")
                    .setMessage("This app needs the Location Permissions!\nPlease accept to use location functionality.")
                    .setPositiveButton("OK", DialogInterface.OnClickListener { dialog, which ->
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_CODE)
                    })
                    .create()
                    .show()

            } else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_LOCATION_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_LONG).show()
                        refreshWeatherLocation()
                    }
                } else {
                    // permission denied, boo! Disable the functionality that depends on this permission.
                    Toast.makeText(this, "Permission denied. Using Colombo weather.", Toast.LENGTH_LONG).show()
                    publishDefaultSriLankaLocation()
                }
                return
            }
        }
    }

    /**
     * Dashboard ANR fix:
     * - Never call blocking location APIs on main thread
     * - One-shot location fetch with timeout, then update weather coordinates
     */
    private fun refreshWeatherLocation() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastWeatherLocationRequestMs < 10_000L) return
        lastWeatherLocationRequestMs = now

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            publishDefaultSriLankaLocation()
            return
        }

        lifecycleScope.launch {
            val cancellation = CancellationTokenSource()
            try {
                val loc = withTimeoutOrNull(6_000L) {
                    withContext(Dispatchers.IO) {
                        fusedLocationClient.getCurrentLocation(
                            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                            cancellation.token
                        ).await()
                    }
                }
                if (loc != null) publishWeatherCoordinates(loc) else publishDefaultSriLankaLocation()
            } catch (_: Exception) {
                publishDefaultSriLankaLocation()
            } finally {
                cancellation.cancel()
            }
        }
    }

    private fun publishWeatherCoordinates(location: Location?) {
        if (location == null) {
            publishDefaultSriLankaLocation()
            return
        }
        val coords = mutableListOf<String>()
        coords.add(location.latitude.toString())
        coords.add(location.longitude.toString())
        coords.add(defaultSriLankaCity)
        weatherViewModel.updateCoordinates(coords)
    }

    private fun publishDefaultSriLankaLocation() {
        weatherViewModel.updateCoordinates(
            listOf(defaultSriLankaLat, defaultSriLankaLon, defaultSriLankaCity)
        )
    }

    private fun updateChatbotFabVisibility() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.frame_layout)
        val shouldHideFab = currentFragment is SocialMediaPostsFragment ||
            currentFragment is SMCreatePostFragment ||
            currentFragment is ChatbotFragment ||
            currentFragment is SurveyFormFragment ||
            currentFragment is SavedSurveysFragment ||
            currentFragment is SurveyDetailFragment
        chatbotFab.visibility = if (shouldHideFab) View.GONE else View.VISIBLE
    }

    // toggleThemeMode removed to fix ANR and use SettingsFragment instead

    /**
     * Theme switches can recreate the Activity before session prefs are re-synced.
     * Do a short retry before redirecting to login, otherwise user appears "logged out".
     */
    private fun ensureSignedInOrRedirect() {
        if (AuthHelper.isSignedIn()) return
        Handler(Looper.getMainLooper()).postDelayed({
            if (AuthHelper.isSignedIn()) return@postDelayed
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }, 500)
    }

    override fun onStart() {
        super.onStart()
        // One-shot location refresh for weather (async, timeout, no UI blocking)
        refreshWeatherLocation()
    }

    override fun onStop() {
        super.onStop()
    }
}
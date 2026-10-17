package com.project.farmingapp.view.dashboard

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.google.gson.JsonObject
import com.project.farmingapp.R
import com.project.farmingapp.adapter.DashboardEcomItemAdapter
import com.project.farmingapp.adapter.OfflineDashboardEcomAdapter
import com.project.farmingapp.databinding.FragmentDashboardBinding
import com.project.farmingapp.model.WeatherApi
import com.project.farmingapp.model.data.WeatherRootList
import com.project.farmingapp.utilities.CellClickListener
import com.project.farmingapp.utilities.pulseTapFeedback
import com.project.farmingapp.view.apmc.ApmcFragment
import com.project.farmingapp.model.data.OfflineAgricultureArticles
import com.project.farmingapp.utilities.AgriAssetPaths
import com.project.farmingapp.utilities.LeafletMapWebViewHelper
import com.project.farmingapp.view.articles.ArticleListFragment
import com.project.farmingapp.view.articles.FruitsFragment
import com.project.farmingapp.view.ecommerce.CartFragment
import com.project.farmingapp.view.ecommerce.EcommerceItemFragment
import com.project.farmingapp.view.ecommerce.OfflineEcommerceData
import com.project.farmingapp.view.weather.WeatherFragment
import com.project.farmingapp.view.survey.SavedSurveysFragment
import com.project.farmingapp.view.survey.SurveyFormFragment
import com.project.farmingapp.viewmodel.ArticleViewModel
import com.project.farmingapp.viewmodel.EcommViewModel
import com.project.farmingapp.viewmodel.FieldSurveyViewModel
import com.project.farmingapp.viewmodel.WeatherViewModel
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.random.Random


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [dashboardFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class dashboardFragment : Fragment(), CellClickListener {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    lateinit var weatherFragment: WeatherFragment
    lateinit var fruitsFragment: FruitsFragment
    lateinit var articleListFragment: ArticleListFragment
    private lateinit var viewModel: WeatherViewModel
    private lateinit var viewModel2: EcommViewModel
    private lateinit var surveyViewModel: FieldSurveyViewModel
    var data: WeatherRootList? = null
    private var latestWeatherCity: String = "Colombo"
    lateinit var sharedPreferences: SharedPreferences
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        viewModel = ViewModelProvider(requireActivity())[WeatherViewModel::class.java]
        viewModel2 = ViewModelProvider(requireActivity())[EcommViewModel::class.java]
        surveyViewModel = ViewModelProvider(requireActivity())[FieldSurveyViewModel::class.java]

        viewModel2.loadAllEcommItems()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)

        viewModel.getCoordinates().observe(viewLifecycleOwner, Observer {
            Log.d("DashFrag", it.toString())
            latestWeatherCity = it.getOrNull(2) ?: "Colombo"
            viewModel.updateNewData()
        })

        viewModel.newDataTrial.observe(viewLifecycleOwner, Observer { weatherData ->
            if (weatherData.list.isNotEmpty()) {
                Log.d("Observed Here", "Yes")
                val tempC = (weatherData.list[0].main.temp - 273).toInt()
                binding.weathTempTextWeathFrag.text =
                    tempC.toString() + "\u2103"
                binding.humidityTextWeathFrag.text =
                    "Humidity: " + weatherData.list[0].main.humidity.toString() + " %"
                binding.windTextWeathFrag.text =
                    "Wind: " + weatherData.list[0].wind.speed.toString() + " km/hr"
                binding.weatherCityTitle.text = latestWeatherCity
                val iconcode = weatherData.list[0].weather[0].icon
                val iconurl = "https://openweathermap.org/img/w/" + iconcode + ".png"
                Glide.with(requireContext()).load(iconurl)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(binding.weathIconImageWeathFrag)
            } else {
                binding.weathTempTextWeathFrag.text = "--"
                binding.humidityTextWeathFrag.text = "Weather unavailable (offline)"
                binding.windTextWeathFrag.text = ""
                binding.weatherCityTitle.text = latestWeatherCity
            }
        })


        viewModel2.ecommLiveData.observe(viewLifecycleOwner, Observer {
            if (it.isEmpty()) {
                showOfflineDashboardProducts()
                return@Observer
            }
            val itemsToShow = (0 until it.size).shuffled().take(minOf(4, it.size))
            val adapterEcomm =
                DashboardEcomItemAdapter(requireContext(), it, itemsToShow, this)
            binding.dashboardEcommRecycler.adapter = adapterEcomm
            binding.dashboardEcommRecycler.scheduleLayoutAnimation()
        })

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        (activity as AppCompatActivity).supportActionBar?.title = "Agri Lanka"
        binding.dashboardEcommRecycler.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.dashboardEcommRecycler.setHasFixedSize(true)
        binding.dashboardEcommRecycler.itemAnimator = null
        view.post { setupHomeSatellitePreview() }

        // Kick off a fresh weather fetch. If coordinates LiveData already fired before
        // onStart (race-condition window), this ensures we never stay on placeholder values.
        viewModel.updateNewData()
        binding.homeCartButton.setOnClickListener {
            binding.homeCartButton.pulseTapFeedback()
            val cartFragment = CartFragment()
            activity!!.supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.frame_layout, cartFragment, "homeCart")
                .setReorderingAllowed(true)
                .addToBackStack("homeCart")
                .commit()
        }

        binding.weatherCard.setOnClickListener {
            binding.weatherCard.pulseTapFeedback()
            weatherFragment = WeatherFragment()

            val transaction = activity!!.supportFragmentManager
                .beginTransaction()
                .replace(R.id.frame_layout, weatherFragment, "name2")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .setReorderingAllowed(true)
                .addToBackStack("name")
                .commit()
            data?.let { it1 -> viewModel.messageToB(it1) }
        }

        // Survey hero card
        binding.startSurveyBtn.setOnClickListener {
            binding.startSurveyBtn.pulseTapFeedback()
            openFreshSurveyForm()
        }
        binding.viewSurveysBtn.setOnClickListener {
            binding.viewSurveysBtn.pulseTapFeedback()
            openFragment(SavedSurveysFragment(), "savedSurveysFrag")
        }
        surveyViewModel.unsyncedCount.observe(viewLifecycleOwner) { count ->
            if (count > 0) {
                binding.surveyUnsyncedBadge.text = "$count pending sync"
                binding.surveyUnsyncedBadge.visibility = android.view.View.VISIBLE
            } else {
                binding.surveyUnsyncedBadge.visibility = android.view.View.GONE
            }
        }

        Glide.with(requireContext())
            .load(AgriAssetPaths.fileUri(AgriAssetPaths.PINEAPPLE))
            .centerCrop()
            .placeholder(R.drawable.fruits)
            .into(binding.dashArticlePreviewPineapple)
        Glide.with(requireContext())
            .load(AgriAssetPaths.fileUri(AgriAssetPaths.TEA_LEAVES))
            .centerCrop()
            .placeholder(R.drawable.plant3)
            .into(binding.dashArticlePreviewTea)
        Glide.with(requireContext())
            .load(AgriAssetPaths.fileUri(AgriAssetPaths.POTATO))
            .centerCrop()
            .placeholder(R.drawable.method2)
            .into(binding.dashArticlePreviewPotato)

        binding.cat1.setOnClickListener {
            binding.cat1.pulseTapFeedback()
            openOfflineArticleFromDashboard("Pineapple")
        }
        binding.cat2.setOnClickListener {
            binding.cat2.pulseTapFeedback()
            openOfflineArticleFromDashboard("Tea")
        }
        binding.cat3.setOnClickListener {
            binding.cat3.pulseTapFeedback()
            openOfflineArticleFromDashboard("Potato")
        }

        binding.cat5.setOnClickListener {
            binding.cat5.pulseTapFeedback()
            articleListFragment = ArticleListFragment()
            val transaction = activity!!.supportFragmentManager
                .beginTransaction()
                .replace(R.id.frame_layout, articleListFragment, "articlesListFrag")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .setReorderingAllowed(true)
                .addToBackStack("articlesListFrag")
                .commit()
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment dashboardFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            dashboardFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    private fun openFragment(fragment: androidx.fragment.app.Fragment, tag: String) {
        activity?.supportFragmentManager?.beginTransaction()
            ?.setCustomAnimations(
                R.anim.slide_in_right, R.anim.slide_out_left,
                R.anim.slide_in_left, R.anim.slide_out_right
            )
            ?.replace(R.id.frame_layout, fragment, tag)
            ?.setReorderingAllowed(true)
            ?.addToBackStack(tag)
            ?.commit()
    }

    private fun openFreshSurveyForm() {
        val fm = activity?.supportFragmentManager ?: return
        fm.popBackStack("surveyFormFrag", androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        openFragment(SurveyFormFragment.newInstance(), "surveyFormFrag")
    }

    private fun openOfflineArticleFromDashboard(title: String) {
        val bundle = OfflineAgricultureArticles.bundleForTitle(title) ?: return
        val frag = FruitsFragment()
        frag.arguments = bundle
        activity?.supportFragmentManager?.beginTransaction()
            ?.setCustomAnimations(
                R.anim.slide_in_right, R.anim.slide_out_left,
                R.anim.slide_in_left, R.anim.slide_out_right
            )
            ?.replace(R.id.frame_layout, frag, title)
            ?.setReorderingAllowed(true)
            ?.addToBackStack("dashArticle_$title")
            ?.commit()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onStop() {
        super.onStop()

    }

    override fun onCellClickListener(name: String) {
        if (name.startsWith("offline_")) {
            val offlineItem = OfflineEcommerceData.getItems(requireContext()).firstOrNull { it.id == name }
            if (offlineItem != null) {
                val ecommerceItemFragment = EcommerceItemFragment()
                ecommerceItemFragment.arguments = OfflineEcommerceData.toBundle(offlineItem)
                activity!!.supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.frame_layout, ecommerceItemFragment, name)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .setReorderingAllowed(true)
                    .addToBackStack("name")
                    .commit()
                return
            }
        }
        val ecommerceItemFragment = EcommerceItemFragment()

        val transaction = activity!!.supportFragmentManager
            .beginTransaction()
            .replace(R.id.frame_layout, ecommerceItemFragment, name)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .setReorderingAllowed(true)
            .addToBackStack("name")
            .commit()
    }

    private fun showOfflineDashboardProducts() {
        val offlineItems = OfflineEcommerceData.getItems(requireContext()).shuffled().take(4)
        val adapter = OfflineDashboardEcomAdapter(offlineItems) { selected ->
            onCellClickListener(selected.id)
        }
        binding.dashboardEcommRecycler.adapter = adapter
        binding.dashboardEcommRecycler.scheduleLayoutAnimation()
    }

    private fun setupHomeSatellitePreview() {
        val wv = binding.homeSatelliteWebView
        wv.visibility = View.VISIBLE
        // Software rendering can be extremely slow on some devices (jank + blank tiles).
        // Keep hardware acceleration enabled for smooth map rendering.
        wv.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        LeafletMapWebViewHelper.applyLeafletWebSettings(wv)
        LeafletMapWebViewHelper.enableConsoleLogging(wv)
        val assetLoader = LeafletMapWebViewHelper.createAssetLoader(requireContext())
        wv.webViewClient = LeafletMapWebViewHelper.createClient(assetLoader) { view ->
            view.visibility = View.VISIBLE
            Log.d("DashboardFragment", "Satellite map loaded, resizing and centering")
            LeafletMapWebViewHelper.runLeafletResizeAndCenter(view)
        }
        Log.d("DashboardFragment", "Loading map URL: ${LeafletMapWebViewHelper.mapPageUrl()}")
        wv.loadUrl(LeafletMapWebViewHelper.mapPageUrl())

        binding.homeSatelliteCard.setOnClickListener {
            val apmcFragment = ApmcFragment()
            activity!!.supportFragmentManager
                .beginTransaction()
                .replace(R.id.frame_layout, apmcFragment, "apmcFromHomeSatellite")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .setReorderingAllowed(true)
                .addToBackStack("apmcFromHomeSatellite")
                .commit()
        }
    }

    private fun handleIntentSchemeUrl(view: android.webkit.WebView?, rawUrl: String): Boolean {
        if (!rawUrl.startsWith("intent://", ignoreCase = true)) return false
        return try {
            val intent = Intent.parseUri(rawUrl, Intent.URI_INTENT_SCHEME)
            val browserFallbackUrl = intent.getStringExtra("browser_fallback_url")
            val safeUrl = if (!browserFallbackUrl.isNullOrBlank()) {
                browserFallbackUrl
            } else {
                rawUrl.replaceFirst("intent://", "https://").substringBefore("#Intent")
            }
            view?.loadUrl(safeUrl)
            true
        } catch (_: Exception) {
            false
        }
    }
}

package com.project.farmingapp.view.weather

import android.os.Bundle

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.project.farmingapp.R
import com.project.farmingapp.adapter.CurrentWeatherAdapter
import com.project.farmingapp.adapter.WeatherAdapter
import com.project.farmingapp.databinding.FragmentWeatherBinding
import com.project.farmingapp.model.WeatherApi
import com.project.farmingapp.model.data.Weather
import com.project.farmingapp.model.data.WeatherList
import com.project.farmingapp.model.data.WeatherRootList
import com.project.farmingapp.viewmodel.WeatherListener
import com.project.farmingapp.viewmodel.WeatherViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [WeatherFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class WeatherFragment : Fragment(), WeatherListener {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    lateinit var Adapter: WeatherAdapter
    lateinit var Adapter2: CurrentWeatherAdapter

    private lateinit var viewModel: WeatherViewModel


    private var _binding: FragmentWeatherBinding? = null
    private val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        // init ViewModel
        viewModel = ViewModelProvider(requireActivity())[WeatherViewModel::class.java]

        val bundle = this.arguments
        if (bundle != null) {
            Log.d("WeatherFrag Bundle", bundle.getString("key") ?: "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentWeatherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        (activity as AppCompatActivity).supportActionBar?.title = "Weather"

        binding.rcylrWeather.layoutManager = LinearLayoutManager(requireContext())
        binding.rcylrWeather.setHasFixedSize(false)
        binding.rcylrWeather.itemAnimator = null
        binding.rcylrWeather.isNestedScrollingEnabled = false

        binding.currentWeatherRcycl.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.currentWeatherRcycl.setHasFixedSize(true)
        binding.currentWeatherRcycl.itemAnimator = null

        val city = viewModel.getCoordinates().value ?: listOf("6.9271", "79.8612", "Colombo")
        binding.weatherCity.text = city.getOrNull(2) ?: "Colombo"

        viewModel.updateNewData()
        viewModel.newDataTrial.observe(viewLifecycleOwner, Observer { weatherData ->
            if (_binding == null) return@Observer
            try {
                bindWeatherData(weatherData)
            } catch (e: Exception) {
                Log.e("WeatherFrag", "Could not display weather", e)
            }
        })
    }

    private fun bindWeatherData(newWeatherData: WeatherRootList?) {
        if (newWeatherData == null || newWeatherData.list.isEmpty()) {
            binding.heroTemp.text = "--"
            binding.heroCondition.text = "Weather unavailable. Check your internet connection."
            binding.heroFeelsLike.text = ""
            binding.heroHumidity.text = "--"
            binding.heroWind.text = "--"
            binding.heroMinMax.text = "--"
            return
        }

        val first = newWeatherData.list[0]

        // Populate hero card
        val tempC = (first.main.temp - 273.15).toInt()
        binding.heroTemp.text = "$tempC\u00B0C"
        binding.heroCondition.text = first.weather[0].description.replaceFirstChar { c ->
            if (c.isLowerCase()) c.titlecase() else c.toString()
        }
        binding.heroFeelsLike.text = "Feels like $tempC\u00B0C"
        binding.heroHumidity.text = "${first.main.humidity}%"
        binding.heroWind.text = "${first.wind.speed.toInt()} km/h"
        val minC = (first.main.temp_min - 273.15f).toInt()
        val maxC = (first.main.temp_max - 273.15f).toInt()
        binding.heroMinMax.text = "$minC/$maxC\u00B0C"
        binding.weatherDateToday.text = first.dt_txt.substring(0, 10)

        val iconUrl = "https://openweathermap.org/img/w/${first.weather[0].icon}.png"
        Glide.with(this)
            .load(iconUrl)
            .placeholder(R.drawable.ic_launcher_foreground)
            .into(binding.heroIcon)

        // Split hourly (today) vs daily (noon of each day)
        val firstDay = first.dt_txt.substring(8, 10)
        val hourlyEntries = newWeatherData.list
            .filter { it.dt_txt.substring(8, 10) == firstDay }

        val forecastEntries = newWeatherData.list
            .filter { it.dt_txt.substring(11, 13) == "12" }

        Adapter = WeatherAdapter(requireContext(), forecastEntries)
        Adapter2 = CurrentWeatherAdapter(requireContext(), hourlyEntries)

        binding.currentWeatherRcycl.adapter = Adapter2
        binding.currentWeatherRcycl.scheduleLayoutAnimation()

        binding.rcylrWeather.adapter = Adapter
        binding.rcylrWeather.scheduleLayoutAnimation()
    }
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment WeatherFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            WeatherFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onSuccess(authRepo: LiveData<String>) {
        authRepo.observe(this, Observer {
            Log.d("Frag", authRepo.value.toString())
        })
//        Toast.makeText(this.context, "SS", Toast.LENGTH_LONG).show()
    }
}

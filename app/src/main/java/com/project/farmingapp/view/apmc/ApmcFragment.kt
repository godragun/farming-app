package com.project.farmingapp.view.apmc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.material.switchmaterial.SwitchMaterial
import com.project.farmingapp.R
import com.project.farmingapp.utilities.LeafletMapWebViewHelper
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

enum class CropQuality {
    HIGH, MEDIUM, LOW
}

data class CropZone(
    val center: LatLng,
    val radius: Double,
    val quality: CropQuality
)

data class DistrictGeoData(
    val center: LatLng,
    val fallbackBoundary: List<LatLng>,
    val tip: String,
    val rainfall: String,
    val soilType: String,
    val bestCrops: String,
    val cropZones: List<CropZone>
)

class ApmcFragment : Fragment(), OnMapReadyCallback {

    private var param1: String? = null
    private var param2: String? = null

    private var spinner1: Spinner? = null
    private var spinner2: Spinner? = null
    private var dateValueTextApmc: TextView? = null
    private var textApmcWarning: TextView? = null
    private var agriSuitabilityText: TextView? = null
    private var districtStatsTitle: TextView? = null
    private var districtRainfallText: TextView? = null
    private var districtSoilText: TextView? = null
    private var districtBestCropsText: TextView? = null
    private var cropZoneToggle: SwitchMaterial? = null
    private var fallbackWebView: WebView? = null
    private var useWebFallback: Boolean = false
    private var mapWebViewReady: Boolean = false
    private val pendingJsQueue = mutableListOf<String>()

    private var googleMap: GoogleMap? = null
    private var districtPolygon: Polygon? = null
    private val cropZoneCircles = mutableListOf<Circle>()
    private var selectedDistrictName: String? = null

    private lateinit var states: Array<String>
    private lateinit var districtByProvince: Map<String, Array<String>>
    private lateinit var districtMapData: Map<String, DistrictGeoData>
    private val geoJsonBoundaries = mutableMapOf<String, List<LatLng>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_apmc, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        (activity as AppCompatActivity).supportActionBar?.title = "Satellite View"

        spinner1 = view.findViewById(R.id.spinner1)
        spinner2 = view.findViewById(R.id.spinner2)
        dateValueTextApmc = view.findViewById(R.id.dateValueTextApmc)
        textApmcWarning = view.findViewById(R.id.textAPMCWarning)
        agriSuitabilityText = view.findViewById(R.id.agriSuitabilityText)
        districtStatsTitle = view.findViewById(R.id.districtStatsTitle)
        districtRainfallText = view.findViewById(R.id.districtRainfallText)
        districtSoilText = view.findViewById(R.id.districtSoilText)
        districtBestCropsText = view.findViewById(R.id.districtBestCropsText)
        cropZoneToggle = view.findViewById(R.id.cropZoneToggle)
        fallbackWebView = view.findViewById(R.id.satelliteFallbackWebView)

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        dateValueTextApmc?.text = sdf.format(Date())

        initDistrictData()
        setupMapOrFallback(view)
        setupSpinnerFlow()
        setupCropZoneToggle()
        showInitialState()
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                loadDistrictBoundariesFromGeoJson()
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isMapToolbarEnabled = false
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        googleMap?.mapType = GoogleMap.MAP_TYPE_SATELLITE
        renderSriLankaOverview()
    }

    private fun initMapFragment() {
        val mapFragment = (childFragmentManager.findFragmentById(R.id.satelliteMapContainer)
            as? SupportMapFragment) ?: SupportMapFragment.newInstance().also {
            childFragmentManager.beginTransaction()
                .replace(R.id.satelliteMapContainer, it)
                .commitNow()
        }
        mapFragment.getMapAsync(this)
    }

    private fun setupMapOrFallback(rootView: View) {
        val mapKey = getString(R.string.google_maps_key)
        useWebFallback = mapKey.isBlank() || mapKey.contains("REPLACE_WITH", ignoreCase = true)
        if (useWebFallback) {
            rootView.findViewById<View>(R.id.satelliteMapContainer).visibility = View.GONE
            fallbackWebView?.visibility = View.VISIBLE
            fallbackWebView?.let { wv ->
                wv.setLayerType(View.LAYER_TYPE_NONE, null)   // hardware path — required for tiles
                LeafletMapWebViewHelper.applyLeafletWebSettings(wv)
                // builtInZoomControls already enabled by applyLeafletWebSettings; ensure visible
                wv.settings.displayZoomControls = false
                val assetLoader = LeafletMapWebViewHelper.createAssetLoader(requireContext())
                wv.webViewClient = LeafletMapWebViewHelper.createClient(assetLoader) { view ->
                    mapWebViewReady = true
                    LeafletMapWebViewHelper.runLeafletResizeAndCenter(view)
                    pendingJsQueue.forEach { js -> view.evaluateJavascript(js, null) }
                    pendingJsQueue.clear()
                }
                wv.loadUrl(LeafletMapWebViewHelper.mapPageUrl())
            }
            agriSuitabilityText?.text =
                "Satellite map loaded. Select a province and district to see crop zone overlays."
        } else {
            rootView.findViewById<View>(R.id.satelliteMapContainer).visibility = View.VISIBLE
            fallbackWebView?.visibility = View.GONE
            initMapFragment()
        }
    }

    private fun runJs(js: String) {
        if (mapWebViewReady) {
            fallbackWebView?.evaluateJavascript(js, null)
        } else {
            pendingJsQueue.add(js)
        }
    }

    private fun initDistrictData() {
        states = arrayOf(
            "None",
            "Western Province",
            "Central Province",
            "Southern Province",
            "Northern Province",
            "Eastern Province",
            "North Western Province",
            "North Central Province",
            "Uva Province",
            "Sabaragamuwa Province"
        )

        districtByProvince = mapOf(
            "Western Province" to arrayOf("None", "Colombo", "Gampaha", "Kalutara"),
            "Central Province" to arrayOf("None", "Kandy", "Matale", "Nuwara Eliya"),
            "Southern Province" to arrayOf("None", "Galle", "Matara", "Hambantota"),
            "Northern Province" to arrayOf("None", "Jaffna", "Kilinochchi", "Mannar", "Vavuniya", "Mullaitivu"),
            "Eastern Province" to arrayOf("None", "Trincomalee", "Batticaloa", "Ampara"),
            "North Western Province" to arrayOf("None", "Kurunegala", "Puttalam"),
            "North Central Province" to arrayOf("None", "Anuradhapura", "Polonnaruwa"),
            "Uva Province" to arrayOf("None", "Badulla", "Monaragala"),
            "Sabaragamuwa Province" to arrayOf("None", "Ratnapura", "Kegalle")
        )

        districtMapData = mapOf(
            "Colombo" to buildDistrictData("Colombo", 6.9271, 79.8612, 0.10, 0.13, "Humid lowland zone: best for leafy vegetables and peri-urban horticulture.", "2400", "Alluvial + lateritic", "Leafy greens, banana, chili"),
            "Gampaha" to buildDistrictData("Gampaha", 7.0840, 79.9990, 0.12, 0.14, "Wet-zone mixed farming potential with coconut and vegetables.", "2300", "Lateritic loam", "Coconut, pineapple, vegetables"),
            "Kalutara" to buildDistrictData("Kalutara", 6.5854, 79.9607, 0.11, 0.13, "High rainfall belt suitable for rubber and spice integration.", "2800", "Lateritic + red-yellow podzolic", "Rubber, pepper, banana"),
            "Kandy" to buildDistrictData("Kandy", 7.2906, 80.6337, 0.12, 0.14, "Mid-country elevation supports tea, vegetables and spices.", "1900", "Reddish brown earth", "Tea, pepper, cabbage"),
            "Matale" to buildDistrictData("Matale", 7.4675, 80.6234, 0.12, 0.14, "Intermediate rainfall with strong spice-crop suitability.", "1750", "Reddish brown earth", "Chili, onion, spices"),
            "Nuwara Eliya" to buildDistrictData("Nuwara Eliya", 6.9497, 80.7891, 0.11, 0.13, "Cool climate upland: ideal for high-value vegetable cultivation.", "2100", "Mountain regosols", "Carrot, potato, leek"),
            "Galle" to buildDistrictData("Galle", 6.0535, 80.2210, 0.10, 0.12, "Coastal wet zone supports paddy and coconut-based systems.", "2600", "Alluvial + coastal", "Paddy, coconut, cinnamon"),
            "Matara" to buildDistrictData("Matara", 5.9549, 80.5549, 0.10, 0.12, "Strong rainfall and home-garden systems for mixed crops.", "2300", "Red-yellow podzolic", "Banana, pepper, paddy"),
            "Hambantota" to buildDistrictData("Hambantota", 6.1241, 81.1185, 0.14, 0.16, "Dry zone with irrigation-dependent crop planning.", "1050", "Reddish brown earth", "Groundnut, maize, sesame"),
            "Jaffna" to buildDistrictData("Jaffna", 9.6615, 80.0255, 0.12, 0.12, "Semi-arid north suitable for onion and drought-tolerant crops.", "1250", "Calcareous soils", "Onion, chili, pulses"),
            "Kilinochchi" to buildDistrictData("Kilinochchi", 9.3961, 80.3982, 0.13, 0.14, "Flat terrain and tanks support seasonal paddy programs.", "1350", "Reddish brown earth", "Paddy, maize, green gram"),
            "Mannar" to buildDistrictData("Mannar", 8.9770, 79.9041, 0.12, 0.13, "Dry and coastal conditions favor pulses and palmyrah systems.", "1100", "Sandy loam", "Black gram, sesame, onion"),
            "Vavuniya" to buildDistrictData("Vavuniya", 8.7514, 80.4971, 0.12, 0.14, "Reservoir-fed cultivation suitable for grains and legumes.", "1400", "Reddish brown earth", "Paddy, maize, cowpea"),
            "Mullaitivu" to buildDistrictData("Mullaitivu", 9.2671, 80.8142, 0.12, 0.14, "Dry coastal district with irrigation-led field crop potential.", "1300", "Sandy clay loam", "Paddy, chili, groundnut"),
            "Trincomalee" to buildDistrictData("Trincomalee", 8.5874, 81.2152, 0.13, 0.15, "Dry-zone east with strong paddy and maize potential.", "1550", "Reddish brown earth", "Paddy, maize, pulses"),
            "Batticaloa" to buildDistrictData("Batticaloa", 7.7102, 81.6924, 0.13, 0.15, "Lagoon-adjacent lands suit paddy and coconut systems.", "1650", "Alluvial + sandy loam", "Paddy, coconut, banana"),
            "Ampara" to buildDistrictData("Ampara", 7.3018, 81.6747, 0.14, 0.16, "Large irrigated tracts suitable for rice and sugarcane.", "1700", "Reddish brown earth", "Paddy, sugarcane, maize"),
            "Kurunegala" to buildDistrictData("Kurunegala", 7.4863, 80.3647, 0.13, 0.15, "Intermediate zone with coconut triangle advantages.", "1600", "Reddish brown latosols", "Coconut, paddy, maize"),
            "Puttalam" to buildDistrictData("Puttalam", 8.0362, 79.8283, 0.13, 0.15, "Dry coastal belt good for coconut and legumes.", "1200", "Sandy loam", "Coconut, chili, pulses"),
            "Anuradhapura" to buildDistrictData("Anuradhapura", 8.3114, 80.4037, 0.15, 0.17, "Major dry-zone farming district with tank irrigation.", "1350", "Reddish brown earth", "Paddy, maize, onion"),
            "Polonnaruwa" to buildDistrictData("Polonnaruwa", 7.9397, 81.0006, 0.14, 0.16, "Irrigated plains favor high-output rice production.", "1450", "Alluvial + reddish brown", "Paddy, maize, soybean"),
            "Badulla" to buildDistrictData("Badulla", 6.9934, 81.0550, 0.12, 0.14, "Highland and intermediate climates support tea and vegetables.", "1750", "Mountain soils", "Tea, vegetables, spices"),
            "Monaragala" to buildDistrictData("Monaragala", 6.8720, 81.3507, 0.15, 0.17, "Dry-intermediate terrain suitable for field crops.", "1250", "Reddish brown earth", "Maize, sesame, chili"),
            "Ratnapura" to buildDistrictData("Ratnapura", 6.6828, 80.3992, 0.11, 0.13, "High rainfall district ideal for rubber and tea mixes.", "3000", "Red-yellow podzolic", "Rubber, tea, pepper"),
            "Kegalle" to buildDistrictData("Kegalle", 7.2513, 80.3464, 0.11, 0.13, "Wet-intermediate district suited to rubber and paddy.", "2600", "Lateritic loam", "Rubber, paddy, banana")
        )
    }

    private fun buildDistrictData(
        district: String,
        lat: Double,
        lon: Double,
        latDelta: Double,
        lonDelta: Double,
        tip: String,
        rainfallMm: String,
        soil: String,
        crops: String
    ): DistrictGeoData {
        val center = LatLng(lat, lon)
        val zoneOffset = (district.hashCode().rem(7)) * 0.0008
        return DistrictGeoData(
            center = center,
            fallbackBoundary = listOf(
                LatLng(lat + (latDelta * 0.95), lon - (lonDelta * 0.25)),
                LatLng(lat + (latDelta * 0.80), lon + (lonDelta * 0.40)),
                LatLng(lat + (latDelta * 0.15), lon + (lonDelta * 0.95)),
                LatLng(lat - (latDelta * 0.70), lon + (lonDelta * 0.50)),
                LatLng(lat - (latDelta * 0.85), lon - (lonDelta * 0.35)),
                LatLng(lat - (latDelta * 0.20), lon - (lonDelta * 0.90))
            ),
            tip = tip,
            rainfall = "$rainfallMm mm/year",
            soilType = soil,
            bestCrops = crops,
            cropZones = listOf(
                CropZone(
                    center = LatLng(lat + (latDelta * 0.28) + zoneOffset, lon - (lonDelta * 0.18)),
                    radius = 4500.0,
                    quality = CropQuality.HIGH
                ),
                CropZone(
                    center = LatLng(lat - (latDelta * 0.08), lon + (lonDelta * 0.20) + zoneOffset),
                    radius = 3900.0,
                    quality = CropQuality.MEDIUM
                ),
                CropZone(
                    center = LatLng(lat - (latDelta * 0.26), lon - (lonDelta * 0.05)),
                    radius = 3200.0,
                    quality = CropQuality.LOW
                )
            )
        )
    }

    private fun loadDistrictBoundariesFromGeoJson() {
        try {
            val jsonText = requireContext().assets.open("sri_lanka_districts.geojson")
                .bufferedReader()
                .use { it.readText() }
            val root = JSONObject(jsonText)
            val features = root.optJSONArray("features") ?: JSONArray()
            for (i in 0 until features.length()) {
                val feature = features.optJSONObject(i) ?: continue
                val properties = feature.optJSONObject("properties") ?: JSONObject()
                val districtName = properties.optString("district")
                    .ifBlank { properties.optString("name") }
                    .ifBlank { properties.optString("DISTRICT") }
                if (districtName.isBlank()) continue
                val geometry = feature.optJSONObject("geometry") ?: continue
                val boundary = extractBoundaryFromGeometry(geometry)
                if (boundary.size >= 4) {
                    geoJsonBoundaries[normalizeDistrictName(districtName)] = boundary
                }
            }
        } catch (_: Exception) {
            geoJsonBoundaries.clear()
        }
    }

    private fun extractBoundaryFromGeometry(geometry: JSONObject): List<LatLng> {
        val type = geometry.optString("type")
        val coordinates = geometry.optJSONArray("coordinates") ?: return emptyList()
        return when (type) {
            "Polygon" -> polygonArrayToLatLngList(coordinates.optJSONArray(0))
            "MultiPolygon" -> {
                val firstPolygon = coordinates.optJSONArray(0) ?: return emptyList()
                polygonArrayToLatLngList(firstPolygon.optJSONArray(0))
            }
            else -> emptyList()
        }
    }

    private fun polygonArrayToLatLngList(points: JSONArray?): List<LatLng> {
        if (points == null) return emptyList()
        val result = mutableListOf<LatLng>()
        for (i in 0 until points.length()) {
            val pair = points.optJSONArray(i) ?: continue
            if (pair.length() < 2) continue
            val lon = pair.optDouble(0, Double.NaN)
            val lat = pair.optDouble(1, Double.NaN)
            if (!lat.isNaN() && !lon.isNaN()) {
                result.add(LatLng(lat, lon))
            }
        }
        return result
    }

    private fun setupSpinnerFlow() {
        val provinceAdapter = ArrayAdapter(
            requireContext().applicationContext,
            android.R.layout.simple_spinner_dropdown_item,
            states
        )
        spinner1?.adapter = provinceAdapter

        spinner1?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                showInitialState()
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    spinner2?.adapter = ArrayAdapter(
                        requireContext().applicationContext,
                        android.R.layout.simple_spinner_dropdown_item,
                        arrayOf("None")
                    )
                    showInitialState()
                    return
                }
                val province = states[position]
                val districts = districtByProvince[province] ?: arrayOf("None")
                spinner2?.adapter = ArrayAdapter(
                    requireContext().applicationContext,
                    android.R.layout.simple_spinner_dropdown_item,
                    districts
                )
                textApmcWarning?.text = "Please select district"
                textApmcWarning?.visibility = View.VISIBLE
            }
        }

        spinner2?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                showInitialState()
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val district = parent?.getItemAtPosition(position)?.toString().orEmpty()
                if (district.isBlank() || district == "None") {
                    textApmcWarning?.text = "Please select district"
                    textApmcWarning?.visibility = View.VISIBLE
                    renderSriLankaOverview()
                    return
                }
                textApmcWarning?.visibility = View.GONE
                renderDistrict(district)
            }
        }
    }

    private fun setupCropZoneToggle() {
        cropZoneToggle?.setOnCheckedChangeListener { _, isChecked ->
            updateLegendVisualState(isChecked)
            selectedDistrictName?.let { renderDistrict(it) } ?: renderSriLankaOverview()
        }
        updateLegendVisualState(cropZoneToggle?.isChecked == true)
    }

    private fun updateLegendVisualState(enabled: Boolean) {
        val alpha = if (enabled) 1f else 0.45f
        view?.findViewById<View>(R.id.cropZoneLegendGroup)?.alpha = alpha
    }

    private fun showInitialState() {
        selectedDistrictName = null
        textApmcWarning?.text = "Please select province and district"
        textApmcWarning?.visibility = View.VISIBLE
        agriSuitabilityText?.text = "Select a district to see satellite map and planting insights."
        districtStatsTitle?.text = "District Stats"
        districtRainfallText?.text = "Rainfall: -- mm/year"
        districtSoilText?.text = "Soil: --"
        districtBestCropsText?.text = "Best crops: --"
        renderSriLankaOverview()
    }

    private fun renderSriLankaOverview() {
        if (useWebFallback) {
            runJs("showSriLankaOverview()")
            agriSuitabilityText?.text =
                "Sri Lanka satellite overview. Select a province and district to see crop zone overlays."
            return
        }
        val map = googleMap ?: return
        clearMapOverlays()
        val sriLankaCenter = LatLng(7.8731, 80.7718)
        val bounds = LatLngBounds.builder()
            .include(LatLng(5.90, 79.30))
            .include(LatLng(9.95, 81.95))
            .build()
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(sriLankaCenter, 7.3f))
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 40))
        agriSuitabilityText?.text =
            "Sri Lanka satellite overview. Select district to show GeoJSON boundaries, crop zones, and local stats."
    }

    private fun renderDistrict(district: String) {
        val data = districtMapData[district] ?: return
        selectedDistrictName = district
        if (useWebFallback) {
            val boundaryFromGeoJson = geoJsonBoundaries[normalizeDistrictName(district)]
            val boundary = boundaryFromGeoJson ?: data.fallbackBoundary
            val boundaryJson = boundary.joinToString(",", "[", "]") { pt ->
                "[${pt.latitude},${pt.longitude}]"
            }
            runJs("showDistrictBoundary('${boundaryJson.replace("'", "\\'")}')")
            if (cropZoneToggle?.isChecked == true) {
                val zonesJson = data.cropZones.joinToString(",", "[", "]") { zone ->
                    "{\"lat\":${zone.center.latitude},\"lon\":${zone.center.longitude}," +
                    "\"radius\":${zone.radius},\"quality\":\"${zone.quality}\"}"
                }
                runJs("showCropZones('${zonesJson.replace("'", "\\'")}')")
            } else {
                runJs("clearZones()")
            }
            agriSuitabilityText?.text = data.tip
            districtStatsTitle?.text = "District Stats - $district"
            districtRainfallText?.text = "Rainfall: ${data.rainfall}"
            districtSoilText?.text = "Soil: ${data.soilType}"
            districtBestCropsText?.text = "Best crops: ${data.bestCrops}"
            return
        }
        val map = googleMap ?: return
        clearMapOverlays()

        val boundaryFromGeoJson = geoJsonBoundaries[normalizeDistrictName(district)]
        val boundary = boundaryFromGeoJson ?: data.fallbackBoundary

        districtPolygon = map.addPolygon(
            PolygonOptions()
                .addAll(boundary)
                .strokeColor(0xFF66BB6A.toInt())
                .fillColor(0x333CB371)
                .strokeWidth(3.6f)
        )
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(data.center, 10.8f))

        if (cropZoneToggle?.isChecked == true) {
            addCropZones(data)
        }
        agriSuitabilityText?.text = data.tip
        districtStatsTitle?.text = "District Stats - $district"
        districtRainfallText?.text = "Rainfall: ${data.rainfall}"
        districtSoilText?.text = "Soil: ${data.soilType}"
        districtBestCropsText?.text = "Best crops: ${data.bestCrops}"
    }

    private fun addCropZones(data: DistrictGeoData) {
        val map = googleMap ?: return
        data.cropZones.forEach { zone ->
            val circle = map.addCircle(
                CircleOptions()
                    .center(zone.center)
                    .radius(zone.radius)
                    .strokeColor(strokeColorFor(zone.quality))
                    .fillColor(fillColorFor(zone.quality))
                    .strokeWidth(3f)
            )
            cropZoneCircles.add(circle)
        }
    }

    private fun strokeColorFor(quality: CropQuality): Int {
        return when (quality) {
            CropQuality.HIGH -> 0xFF43A047.toInt()
            CropQuality.MEDIUM -> 0xFFF9A825.toInt()
            CropQuality.LOW -> 0xFFD32F2F.toInt()
        }
    }

    private fun fillColorFor(quality: CropQuality): Int {
        return when (quality) {
            CropQuality.HIGH -> 0x3343A047
            CropQuality.MEDIUM -> 0x33F9A825
            CropQuality.LOW -> 0x33D32F2F
        }
    }

    private fun normalizeDistrictName(name: String): String {
        return name.trim().lowercase(Locale.getDefault())
    }

    private fun clearMapOverlays() {
        districtPolygon?.remove()
        districtPolygon = null
        cropZoneCircles.forEach { it.remove() }
        cropZoneCircles.clear()
    }


    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ApmcFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}

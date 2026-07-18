package com.project.farmingapp.view.articles

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.RotateAnimation
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.project.farmingapp.R
import com.project.farmingapp.utilities.hide
import com.project.farmingapp.utilities.show
import com.project.farmingapp.viewmodel.ArticleListener
import com.project.farmingapp.viewmodel.ArticleViewModel


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FruitsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

class FruitsFragment : Fragment(), ArticleListener {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private lateinit var viewModel: ArticleViewModel
    private var param2: String? = null
    private var param3: String? = null
    val desc = ""
    private var rootView: View? = null
    private var progressArticle: ProgressBar? = null
    private var tempTextFruitFragArt: TextView? = null
    private var monthTextFruitFragArt: TextView? = null
    private var titleTextFruitFragArt: TextView? = null
    private var descTextValueFruitFragArt: TextView? = null
    private var processTextValueFruitFragArt: TextView? = null
    private var soilTextValueFruitFragArt: TextView? = null
    private var stateTextValueFruitFragArt: TextView? = null
    private var imageFruitFragArt: android.widget.ImageView? = null
    private var attr1ValueFruitFragArt: TextView? = null
    private var attr2ValueFruitFragArt: TextView? = null
    private var attr3ValueFruitFragArt: TextView? = null
    private var attr4ValueFruitFragArt: TextView? = null
    private var diseaseTextValueFruitFragArt: TextView? = null
    private var descTextTitleFruitFragArt: TextView? = null
    private var descToggleBtnFruitFragArt: android.widget.ImageButton? = null
    private var marketChart: LineChart? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
            param3 = it.getString("name")
        }

        viewModel = androidx.lifecycle.ViewModelProvider(requireActivity())[ArticleViewModel::class.java]
//        viewModel.getAllArticles(tag!!)

        val tag = this.tag.toString()

        Log.d("I'm called 2", viewModel.message3.value.toString())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_fruits, container, false)
        rootView = root
        progressArticle = root.findViewById(R.id.progressArticle)
        tempTextFruitFragArt = root.findViewById(R.id.tempTextFruitFragArt)
        monthTextFruitFragArt = root.findViewById(R.id.monthTextFruitFragArt)
        titleTextFruitFragArt = root.findViewById(R.id.titleTextFruitFragArt)
        descTextValueFruitFragArt = root.findViewById(R.id.descTextValueFruitFragArt)
        processTextValueFruitFragArt = root.findViewById(R.id.processTextValueFruitFragArt)
        soilTextValueFruitFragArt = root.findViewById(R.id.soilTextValueFruitFragArt)
        stateTextValueFruitFragArt = root.findViewById(R.id.stateTextValueFruitFragArt)
        imageFruitFragArt = root.findViewById(R.id.imageFruitFragArt)
        attr1ValueFruitFragArt = root.findViewById(R.id.attr1ValueFruitFragArt)
        attr2ValueFruitFragArt = root.findViewById(R.id.attr2ValueFruitFragArt)
        attr3ValueFruitFragArt = root.findViewById(R.id.attr3ValueFruitFragArt)
        attr4ValueFruitFragArt = root.findViewById(R.id.attr4ValueFruitFragArt)
        diseaseTextValueFruitFragArt = root.findViewById(R.id.diseaseTextValueFruitFragArt)
        descTextTitleFruitFragArt = root.findViewById(R.id.descTextTitleFruitFragArt)
        descToggleBtnFruitFragArt = root.findViewById(R.id.descToggleBtnFruitFragArt)
        marketChart = root.findViewById(R.id.marketPriceChartFruitFragArt)
        marketChart?.let { configureMarketChart(it) }

        viewModel.message1.observe(viewLifecycleOwner, Observer {
            progressArticle?.show()

            val attributes: Map<String, String> = it.get("attributes") as Map<String, String>
            val desc = it.get("description").toString()

            Log.d("I'm Called", "Yes")
            val diseases: List<String> = it.get("diseases") as List<String>

            Log.d("Diseases2", it.get("diseases").toString())
            tempTextFruitFragArt?.text = attributes["Temperature"].toString()
            monthTextFruitFragArt?.text = attributes["Time"].toString()

            titleTextFruitFragArt?.text = it.get("title").toString()
            descTextValueFruitFragArt?.text = desc
            processTextValueFruitFragArt?.text = it.get("process").toString()
            soilTextValueFruitFragArt?.text = it.get("soil").toString()
            stateTextValueFruitFragArt?.text = it.get("state").toString()

            val images: List<String> = it.get("images") as List<String>
            Glide.with(this)
                .load(images[0])
                .into(imageFruitFragArt!!)

            attr1ValueFruitFragArt?.text = attributes["Weight"].toString()
            attr2ValueFruitFragArt?.text = attributes["Vitamins"].toString()
            attr3ValueFruitFragArt?.text = attributes["Tree Height"].toString()
            attr4ValueFruitFragArt?.text = attributes["growthTime"].toString()

            val diseaseText = diseaseTextValueFruitFragArt
            diseaseText?.text = ""
            for (i in 0..diseases.size - 1) {

                diseaseText?.text =
                    diseaseText?.text.toString() +
                            (i + 1).toString() + ". " + diseases[i].toString() + "\n"
            }
            progressArticle?.hide()

            // Market prices: if Firestore contains a field, use it; otherwise fall back to stable demo series.
            val title = it.get("title")?.toString().orEmpty()
            val prices = (it.get("marketPrices") as? List<*>)?.mapNotNull { v ->
                when (v) {
                    is Number -> v.toFloat()
                    is String -> v.toFloatOrNull()
                    else -> null
                }
            }.orEmpty()
            renderMarketPrices(title, prices.takeIf { p -> p.size >= 2 })
        })

        return root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment FruitsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            FruitsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        (activity as AppCompatActivity).supportActionBar?.title = "Articles"

        val params = descTextTitleFruitFragArt?.layoutParams

        var toggle = 0


        descToggleBtnFruitFragArt?.setOnClickListener {

            if (toggle == 0) {
                descTextValueFruitFragArt?.maxLines = Integer.MAX_VALUE
                toggle = 1

                val rotateAnim = RotateAnimation(
                    0.0f, 180f,
                    RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                    RotateAnimation.RELATIVE_TO_SELF, 0.5f
                )
                rotateAnim.duration = 2
                rotateAnim.fillAfter = true
                descToggleBtnFruitFragArt?.startAnimation(rotateAnim)
            } else if (toggle == 1) {
                descTextValueFruitFragArt?.maxLines = 3
                toggle = 0
                val rotateAnim = RotateAnimation(
                    180f, 0f,
                    RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                    RotateAnimation.RELATIVE_TO_SELF, 0.5f
                )
                rotateAnim.duration = 2
                rotateAnim.fillAfter = true
                descToggleBtnFruitFragArt?.startAnimation(rotateAnim)

            }
        }


        if (arguments?.getBoolean("offline_article", false) == true) {
            bindOfflineArticle()
            return
        }

        // New
        val newData = viewModel.message3.value
        if (newData.isNullOrEmpty()) {
            progressArticle?.hide()
            Toast.makeText(requireContext(), "Article details unavailable online. Showing offline article.", Toast.LENGTH_SHORT).show()
            bindOfflineArticle()
            return
        }

        Log.d("New data length", newData.size.toString())
        val newDataLength = newData.size

        for (a in 0 until newDataLength) {
            if (newData[a].data!!.get("title") == this.tag) {


                var data = newData[a].data

                val attributes: Map<String, String> =
                    data!!.get("attributes") as Map<String, String>
                val desc = data!!.get("description").toString()
                val diseases: List<String> = data!!.get("diseases") as List<String>

                Log.d("I'm Called3", attributes.get("Temperature").toString())
                Log.d("I'm Called", "Yes")
                Log.d("Diseases2", data!!.get("diseases").toString())

                tempTextFruitFragArt?.text = attributes.get("Temperature").toString()
                monthTextFruitFragArt?.text = attributes.get("Time").toString()

                titleTextFruitFragArt?.text = data!!.get("title").toString()
                descTextValueFruitFragArt?.text = desc
                processTextValueFruitFragArt?.text = data!!.get("process").toString()
                soilTextValueFruitFragArt?.text = data!!.get("soil").toString()
                stateTextValueFruitFragArt?.text = data!!.get("state").toString()

                val images: List<String> = data!!.get("images") as List<String>
                Glide.with(this)
                    .load(images[0])
                    .into(imageFruitFragArt!!)

                attr1ValueFruitFragArt?.text = attributes.get("Weight").toString()
                attr2ValueFruitFragArt?.text = attributes.get("Vitamins").toString()
                attr3ValueFruitFragArt?.text = attributes.get("Tree Height").toString()
                attr4ValueFruitFragArt?.text = attributes.get("growthTime").toString()

                diseaseTextValueFruitFragArt?.text = ""
                for (i in 0..diseases.size - 1) {

                    diseaseTextValueFruitFragArt?.text =
                        diseaseTextValueFruitFragArt?.text.toString() +
                                (i + 1).toString() + ". " + diseases[i].toString() + "\n"
                }
                progressArticle?.hide()

                val title = data.get("title")?.toString().orEmpty()
                val prices = (data.get("marketPrices") as? List<*>)?.mapNotNull { v ->
                    when (v) {
                        is Number -> v.toFloat()
                        is String -> v.toFloatOrNull()
                        else -> null
                    }
                }.orEmpty()
                renderMarketPrices(title, prices.takeIf { p -> p.size >= 2 })
            }
        }

    }

    override fun onStarted() {
        Log.d("FruitsFragment", "onStarted")
    }

    override fun onSuccess(authRepo: LiveData<String>) {
        authRepo.observe(viewLifecycleOwner, Observer {
            Log.d("Fruit", "Success")
        })
    }

    override fun onFailure(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun bindOfflineArticle() {
        val args = arguments
        progressArticle?.hide()
        val title = args?.getString("offline_title", "Offline Article").orEmpty()
        titleTextFruitFragArt?.text = title
        descTextValueFruitFragArt?.text = args?.getString("offline_desc", "Article data unavailable.")
        val howGrown = args?.getString("offline_how_grown", "-").orEmpty()
        val process = args?.getString("offline_process", "-").orEmpty()
        processTextValueFruitFragArt?.text = "How it is grown: $howGrown\n\nFarm process: $process"
        soilTextValueFruitFragArt?.text = args?.getString("offline_soil", "-")
        stateTextValueFruitFragArt?.text = args?.getString("offline_state", "-")
        tempTextFruitFragArt?.text = args?.getString("offline_temp", "-")
        monthTextFruitFragArt?.text = args?.getString("offline_time", "-")
        attr1ValueFruitFragArt?.text = args?.getString("offline_weight", "-")
        attr2ValueFruitFragArt?.text = args?.getString("offline_vitamins", "-")
        attr3ValueFruitFragArt?.text = args?.getString("offline_tree_height", "-")
        attr4ValueFruitFragArt?.text = args?.getString("offline_growth_time", "-")
        diseaseTextValueFruitFragArt?.text = args?.getString("offline_diseases", "-")

        val imageAsset = args?.getString("offline_image_asset").orEmpty()
        if (imageAsset.isNotBlank()) {
            Glide.with(this).load("file:///android_asset/$imageAsset").into(imageFruitFragArt!!)
        } else {
            imageFruitFragArt?.setImageResource(R.drawable.ic_launcher_foreground)
        }

        // Offline market prices: stable demo series by title.
        renderMarketPrices(title, null)
    }

    private fun configureMarketChart(chart: LineChart) {
        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setPinchZoom(true)
        chart.setNoDataText("Market prices not available")
        chart.legend.isEnabled = false

        chart.axisRight.isEnabled = false
        chart.axisLeft.textColor = resources.getColor(R.color.fontColor, null)
        chart.axisLeft.setDrawGridLines(true)
        chart.axisLeft.gridColor = 0x22FFFFFF

        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.textColor = resources.getColor(R.color.fontColor, null)
        chart.xAxis.granularity = 1f
    }

    private fun renderMarketPrices(title: String, prices: List<Float>?) {
        val chart = marketChart ?: return
        val series = prices ?: buildFallbackPriceSeries(title)
        val entries = series.mapIndexed { idx, value -> Entry(idx.toFloat(), value) }

        val dataSet = LineDataSet(entries, "Market price").apply {
            color = resources.getColor(R.color.colorPrimary, null)
            lineWidth = 2.2f
            setDrawCircles(true)
            circleRadius = 3.5f
            setCircleColor(resources.getColor(R.color.colorPrimary, null))
            setDrawValues(false)
            setDrawFilled(true)
            fillAlpha = 60
            fillColor = resources.getColor(R.color.colorPrimary, null)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val day = value.toInt() + 1
                return "D$day"
            }
        }
        chart.data = LineData(dataSet)
        chart.invalidate()
    }

    private fun buildFallbackPriceSeries(title: String): List<Float> {
        val base = 50 + (kotlin.math.abs(title.hashCode()) % 120)
        val seed = kotlin.math.abs((title.hashCode() * 1103515245 + 12345))
        val deltas = listOf(-8, -3, 2, 6, 1, -4, 5).mapIndexed { i, d ->
            val wobble = ((seed shr (i * 3)) and 0x7) - 3
            d + wobble
        }
        var cur = base.toFloat()
        return deltas.map { dd ->
            cur = (cur + dd).coerceAtLeast(10f)
            cur
        }
    }
}
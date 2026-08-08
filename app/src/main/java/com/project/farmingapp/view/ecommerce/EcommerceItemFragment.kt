package com.project.farmingapp.view.ecommerce

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import android.widget.ImageView
import androidx.viewpager2.widget.ViewPager2
import com.project.farmingapp.adapter.ProductImageAdapter
import com.project.farmingapp.R
import com.project.farmingapp.adapter.AttributesNormalAdapter
import com.project.farmingapp.adapter.AttributesSelectionAdapter
import com.project.farmingapp.supabase.RemoteDocument
import com.project.farmingapp.supabase.SupabaseDb
import com.project.farmingapp.utilities.AuthHelper
import com.project.farmingapp.utilities.CellClickListener
import com.project.farmingapp.databinding.FragmentEcommerceItemBinding
import com.project.farmingapp.utilities.LocalCartStore
import com.project.farmingapp.utilities.formatLkr
import com.project.farmingapp.utilities.parseAmount
import com.project.farmingapp.utilities.pulseTapFeedback
import com.project.farmingapp.viewmodel.EcommViewModel
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [EcommItemFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class EcommerceItemFragment : Fragment(), CellClickListener {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private lateinit var viewmodel: EcommViewModel
    private var param2: String? = null
    private var selectionAttribute = mutableMapOf<String, Any>()
    private var currentItemId: Any? = null
    val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
    private var _binding: FragmentEcommerceItemBinding? = null
    private val binding get() = _binding!!
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        viewmodel = ViewModelProvider(requireActivity())[EcommViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEcommerceItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment EcommItemFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            EcommerceItemFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    private fun setupImageSlider(urls: List<String>) {
        val slider = binding.posterSlider as ViewPager2
        slider.adapter = ProductImageAdapter(urls)
        val dots = binding.sliderDots
        dots.removeAllViews()
        if (urls.size > 1) {
            repeat(urls.size) { i ->
                val dot = ImageView(requireContext())
                dot.setImageResource(
                    if (i == 0) android.R.drawable.presence_online
                    else android.R.drawable.presence_invisible
                )
                dot.layoutParams = android.widget.LinearLayout.LayoutParams(16, 16).also {
                    it.setMargins(4, 0, 4, 0)
                }
                dots.addView(dot)
            }
            slider.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    for (i in 0 until dots.childCount) {
                        (dots.getChildAt(i) as? ImageView)?.setImageResource(
                            if (i == position) android.R.drawable.presence_online
                            else android.R.drawable.presence_invisible
                        )
                    }
                }
            })
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        (activity as AppCompatActivity).supportActionBar?.title = "E-Commerce"
        binding.loadingText.text = "Loading..."


        val color1Params = binding.color1.layoutParams
        val color2Params = binding.color2.layoutParams
        val color3Params = binding.color3.layoutParams
        val color4Params = binding.color4.layoutParams

        val density = resources.displayMetrics.density
        color1Params.width = (density * 40).toInt()
        color1Params.height = (density * 35).toInt()
        binding.color1.layoutParams = color1Params

        binding.color1.setOnClickListener {
            color1Params.width = (density * 40).toInt()
            color1Params.height = (density * 35).toInt()
            binding.color1.layoutParams = color1Params

            color3Params.width = (density * 30).toInt()
            color3Params.height = (density * 25).toInt()
            binding.color3.layoutParams = color3Params

            color4Params.width = (density * 30).toInt()
            color4Params.height = (density * 25).toInt()
            binding.color4.layoutParams = color4Params

            color2Params.width = (density * 30).toInt()
            color2Params.height = (density * 25).toInt()
            binding.color2.layoutParams = color2Params
        }

        binding.color2.setOnClickListener {
            color1Params.width = (density * 30).toInt()
            color1Params.height = (density * 25).toInt()
            binding.color1.layoutParams = color1Params

            color3Params.width = (density * 30).toInt()
            color3Params.height = (density * 25).toInt()
            binding.color3.layoutParams = color3Params

            color4Params.width = (density * 30).toInt()
            color4Params.height = (density * 25).toInt()
            binding.color4.layoutParams = color4Params

            color2Params.width = (density * 40).toInt()
            color2Params.height = (density * 35).toInt()
            binding.color2.layoutParams = color2Params

        }

        binding.increaseQtyBtn.setOnClickListener {
            binding.increaseQtyBtn.pulseTapFeedback()
            binding.quantityCountEcomm.text =
                (binding.quantityCountEcomm.text.toString().toInt() + 1).toString()
        }

        binding.decreaseQtyBtn.setOnClickListener {
            binding.decreaseQtyBtn.pulseTapFeedback()
            if (binding.quantityCountEcomm.text.toString().toInt() != 1) {
                binding.quantityCountEcomm.text =
                    (binding.quantityCountEcomm.text.toString().toInt() - 1).toString()
            }
        }

        if (arguments?.getBoolean("offline_item", false) == true) {
            bindOfflineItem()
            return
        }

        val currentTag = arguments?.getString("name").orEmpty().ifBlank { this.tag.orEmpty() }
        val allData = viewmodel.ecommLiveData.value.orEmpty()
        val matchedProduct = allData.firstOrNull { it.id == currentTag }
        if (matchedProduct != null) {
            bindOnlineProduct(matchedProduct)
        } else if (currentTag.isNotBlank()) {
            viewmodel.getSpecificItem(currentTag).observe(viewLifecycleOwner, Observer { fetched ->
                if (fetched != null && fetched.exists()) {
                    bindOnlineProduct(fetched)
                } else {
                    binding.progressEcommItem.visibility = View.GONE
                    binding.loadingText.visibility = View.VISIBLE
                    binding.loadingText.text = "Product unavailable. Please open from E-Commerce list."
                    binding.addToCart.isEnabled = false
                    binding.buynow.isEnabled = false
                }
            })
        } else {
            binding.progressEcommItem.visibility = View.GONE
            binding.loadingText.visibility = View.VISIBLE
            binding.loadingText.text = "Product unavailable. Please open from E-Commerce list."
            binding.addToCart.isEnabled = false
            binding.buynow.isEnabled = false
            return
        }

        binding.addToCart.setOnClickListener {
            binding.addToCart.pulseTapFeedback()
            val itemId = currentItemId?.toString().orEmpty()
            if (itemId.isBlank() || itemId == "null") {
                Toast.makeText(requireContext(), "Product unavailable", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.addToCart.isClickable = false
            binding.progressEcommItem.visibility = View.VISIBLE
            binding.loadingText.text = "Adding to Cart..."
            binding.loadingText.visibility = View.GONE
            addItemToCart(itemId, binding.quantityCountEcomm.text.toString().toInt())

        }

        binding.buynow.setOnClickListener {
            binding.buynow.pulseTapFeedback()
            val itemId = currentItemId?.toString().orEmpty()
            if (itemId.isBlank() || itemId == "null") {
                Toast.makeText(requireContext(), "Product unavailable", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
//            var product_id = ArrayList<String>()
//            var item_cost=ArrayList<Int>()
//            var item_qty=ArrayList<Int>()
            val productPrice = parseAmount(binding.productPrice.text.toString())


//            var totalPrice = quantityCountEcomm.text.toString().toInt()*productPrice[1].toString().toInt() + deliverycost.text.toString().toInt()

//            product_id.add(currentItemId as String)
//            item_cost.add(totalPrice)
//            item_qty.add(quantityCountEcomm.text.toString().toInt())

            Intent(activity!!.applicationContext, RazorPayActivity::class.java).also {
                it.putExtra("productId", itemId)
                it.putExtra("itemCost",productPrice.toString())
                it.putExtra("quantity", binding.quantityCountEcomm.text.toString())
                it.putExtra("deliveryCost", parseAmount(binding.deliverycost.text.toString()).toString())
                startActivity(it)
            }

        }
    }

    private fun bindOnlineProduct(
        specificData: RemoteDocument
    ) {
        currentItemId = specificData.id

        binding.productTitle.text = specificData.getString("title").orEmpty().ifBlank { "Product" }
        binding.productShortDescription.text = specificData.getString("shortDesc").orEmpty()
        binding.productPrice.text = formatLkr(parseAmount(specificData.getString("price").orEmpty()))
        binding.productLongDesc.text = specificData.getString("longDesc").orEmpty()
        binding.howToUseText.text = specificData.getString("howtouse").orEmpty()
        binding.deliverycost.text = specificData.getString("delCharge").orEmpty().ifBlank { "0" }
        binding.Rating.rating = specificData.get("rating").toString().toFloatOrNull() ?: 4.0f
        val attributes = specificData.get("attributes") as? Map<String, Any> ?: emptyMap()

        if (attributes.contains("Color")) {
            binding.colorLinear.visibility = View.VISIBLE
            binding.colorTitle.visibility = View.VISIBLE
        } else {
            binding.colorLinear.visibility = View.GONE
            binding.colorTitle.visibility = View.GONE
        }

        val allSelectionAttributes = mutableListOf<MutableMap<String, Any>>()
        val allNormalAttributes = mutableListOf<MutableMap<String, Any>>()
        for ((key, value) in attributes) {
            val selectionMap = mutableMapOf<String, Any>()
            val normalMap = mutableMapOf<String, Any>()

            if (value is ArrayList<*> && key != "Color") {
                selectionMap[key] = value
                allSelectionAttributes.add(selectionMap)
            }

            if (value is String) {
                normalMap[key] = value
                allNormalAttributes.add(normalMap)
            }
        }

        val adapter = AttributesSelectionAdapter(requireContext(), allSelectionAttributes, this)
        binding.recyclerSelectionAttributes.adapter = adapter
        binding.recyclerSelectionAttributes.layoutManager = LinearLayoutManager(requireContext())

        val adapter2 = AttributesNormalAdapter(requireContext(), allNormalAttributes)
        binding.recyclerNormalAttributes.adapter = adapter2
        binding.recyclerNormalAttributes.layoutManager = LinearLayoutManager(requireContext())

        binding.progressEcommItem.visibility = View.GONE
        binding.loadingText.visibility = View.GONE

        val allImages = specificData.get("imageUrl") as? List<String> ?: emptyList()
        setupImageSlider(allImages)
    }

    private fun bindOfflineItem() {
        val args = arguments ?: return
        currentItemId = args.getString("offline_item_id", "offline_item")
        val imageRes = args.getInt("offline_image_res", 0)
        val imageAsset = args.getString("offline_image_asset")
        val price = args.getInt("offline_price", 0)
        val deliveryCharge = args.getInt("offline_delivery_charge", 0)
        val rating = args.getFloat("offline_rating", 4.0f)

        binding.productTitle.text = args.getString("offline_title", "Offline Product")
        binding.productShortDescription.text = args.getString("offline_short_desc", "Product available in offline mode.")
        binding.productPrice.text = formatLkr(price)
        binding.productLongDesc.text = args.getString("offline_long_desc", "")
        binding.howToUseText.text = args.getString("offline_how_to_use", "")
        binding.deliverycost.text = deliveryCharge.toString()
        binding.Rating.rating = rating
        binding.colorLinear.visibility = View.GONE
        binding.colorTitle.visibility = View.GONE
        binding.recyclerSelectionAttributes.adapter = null
        binding.recyclerNormalAttributes.adapter = null

        if (!imageAsset.isNullOrBlank()) {
            setupImageSlider(listOf("file:///android_asset/$imageAsset"))
        } else if (imageRes != 0) {
            val imageName = resources.getResourceEntryName(imageRes)
            setupImageSlider(listOf("android.resource://${requireContext().packageName}/drawable/$imageName"))
        } else {
            setupImageSlider(emptyList())
        }

        binding.progressEcommItem.visibility = View.GONE
        binding.loadingText.visibility = View.GONE

        binding.addToCart.setOnClickListener {
            binding.addToCart.pulseTapFeedback()
            val itemId = currentItemId?.toString().orEmpty()
            if (itemId.isBlank()) {
                Toast.makeText(activity!!.applicationContext, "Product unavailable", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.progressEcommItem.visibility = View.VISIBLE
            binding.loadingText.visibility = View.GONE
            addItemToCart(itemId, binding.quantityCountEcomm.text.toString().toInt())
        }

        binding.buynow.setOnClickListener {
            binding.buynow.pulseTapFeedback()
            Toast.makeText(
                activity!!.applicationContext,
                "Offline demo mode: purchase flow unavailable.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCellClickListener(name: String) {
        val selectionAttributeAllData = name.split(" ") as List<Any>

        Log.d("EcommItem", selectionAttributeAllData[0].toString())
        Log.d("EcommItem", selectionAttributeAllData[1].toString())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.cart_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId){
            R.id.cart_item -> {
                val cartFragment = CartFragment()
                val transaction = activity!!.supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.frame_layout, cartFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .setReorderingAllowed(true)
                    .addToBackStack("cart")
                    .commit()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openCartScreen() {
        val cartFragment = CartFragment()
        activity!!.supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(R.id.frame_layout, cartFragment, "cartFromItem")
            .setReorderingAllowed(true)
            .addToBackStack("cartFromItem")
            .commit()
    }

    private fun addItemToCart(itemId: String, quantity: Int) {
        val safeQuantity = quantity.coerceAtLeast(1)
        val currentDateTime = sdf.format(Date())
        val timeMs = System.currentTimeMillis()

        // Guest/demo and offline fallback: keep cart functional without remote writes.
        if (AuthHelper.guestMode) {
            LocalCartStore.addOrUpdate(requireContext(), itemId, safeQuantity, currentDateTime)
            Toast.makeText(activity!!.applicationContext, "Item Added", Toast.LENGTH_SHORT).show()
            binding.progressEcommItem.visibility = View.GONE
            binding.loadingText.visibility = View.GONE
            binding.addToCart.isClickable = true
            openCartScreen()
            return
        }

        val uid = AuthHelper.requireUid()
        executor.execute {
            try {
                val row = JSONObject()
                    .put("uid", uid)
                    .put("product_id", itemId)
                    .put("quantity", safeQuantity)
                    .put("time_ms", timeMs)
                SupabaseDb.upsert("cart_items", row, onConflict = "uid,product_id")
                activity?.runOnUiThread {
                    if (!isAdded || _binding == null) return@runOnUiThread
                    Toast.makeText(activity!!.applicationContext, "Item Added", Toast.LENGTH_SHORT).show()
                    binding.progressEcommItem.visibility = View.GONE
                    binding.loadingText.visibility = View.GONE
                    binding.addToCart.isClickable = true
                    openCartScreen()
                }
            } catch (_: Exception) {
                activity?.runOnUiThread {
                    if (!isAdded || _binding == null) return@runOnUiThread
                    LocalCartStore.addOrUpdate(requireContext(), itemId, safeQuantity, currentDateTime)
                    Toast.makeText(
                        activity!!.applicationContext,
                        "Saved to local cart (offline mode)",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.progressEcommItem.visibility = View.GONE
                    binding.loadingText.visibility = View.GONE
                    binding.addToCart.isClickable = true
                    openCartScreen()
                }
            }
        }
    }
}
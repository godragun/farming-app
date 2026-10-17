package com.project.farmingapp.view.ecommerce

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.project.farmingapp.R
import com.project.farmingapp.adapter.CartItemsAdapter
import com.project.farmingapp.supabase.SupabaseDb
import com.project.farmingapp.utilities.AuthHelper
import com.project.farmingapp.utilities.CartItemBuy
import com.project.farmingapp.utilities.LocalCartStore
import com.project.farmingapp.utilities.formatLkr
import com.project.farmingapp.viewmodel.EcommViewModel
import java.util.concurrent.Executors
import kotlin.collections.HashMap

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class CartFragment : Fragment(), CartItemBuy {

    private var param1: String? = null
    private var param2: String? = null
    var isOpened: Boolean = false
    var totalCount = 0
    var totalPrice = 0
    var items = HashMap<String, Any>()
    private var usingLocalStore: Boolean = false
    lateinit var ecommViewModel: EcommViewModel
    private var totalItemsValue: android.widget.TextView? = null
    private var totalCostValue: android.widget.TextView? = null
    private var recyclerCart: androidx.recyclerview.widget.RecyclerView? = null
    private var progressCart: android.view.View? = null
    private var loadingTitleText: android.widget.TextView? = null
    private var buyAllBtn: com.google.android.material.button.MaterialButton? = null
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        ecommViewModel = EcommViewModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_cart, container, false)
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            CartFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as AppCompatActivity).supportActionBar?.title = "Cart"
        isOpened = true

        totalItemsValue = view.findViewById(R.id.totalItemsValue)
        totalCostValue = view.findViewById(R.id.totalCostValue)
        recyclerCart = view.findViewById(R.id.recyclerCart)
        progressCart = view.findViewById(R.id.progress_cart)
        loadingTitleText = view.findViewById(R.id.loadingTitleText)
        buyAllBtn = view.findViewById(R.id.buyAllBtn)

        if (AuthHelper.guestMode) {
            usingLocalStore = true
            renderItems(LocalCartStore.getAll(requireContext()))
        } else {
            loadRemoteCart()
        }

        buyAllBtn?.setOnClickListener {
            if (items.isEmpty()) {
                Toast.makeText(activity!!.applicationContext, "Cart is empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val paymentFragment = PaymentFragment()
            paymentFragment.arguments = Bundle().apply {
                putInt("total_items", totalCount)
                putInt("total_price", totalPrice)
            }
            activity!!.supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.frame_layout, paymentFragment, "paymentFrag")
                .setReorderingAllowed(true)
                .addToBackStack("paymentFrag")
                .commit()
        }
    }

    private fun loadRemoteCart() {
        val uid = AuthHelper.runCatching { requireUid() }.getOrNull()
        if (uid == null) {
            // Session lost: show whatever is stored on the device rather than crashing.
            usingLocalStore = true
            renderItems(LocalCartStore.getAll(requireContext()))
            return
        }
        executor.execute {
            try {
                val docs = SupabaseDb.select("cart_items", SupabaseDb.eq("uid", uid))
                val cart = HashMap<String, Any>()
                for (doc in docs) {
                    val productId = doc.getString("productId")
                        ?: doc.getString("product_id")
                        ?: continue
                    val qty = doc.getLong("quantity")?.toInt() ?: 1
                    val time = doc.get("time")?.toString()
                        ?: doc.get("time_ms")?.toString()
                        ?: ""
                    cart[productId] = hashMapOf<String, Any>(
                        "quantity" to qty,
                        "time" to time
                    )
                }
                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    if (cart.isNotEmpty()) {
                        usingLocalStore = false
                        renderItems(cart)
                    } else {
                        val localItems = LocalCartStore.getAll(requireContext())
                        if (localItems.isNotEmpty()) {
                            usingLocalStore = true
                            renderItems(localItems)
                        } else {
                            Toast.makeText(
                                activity!!.applicationContext,
                                "Item Not Exist",
                                Toast.LENGTH_SHORT
                            ).show()
                            totalCount = 0
                            totalPrice = 0
                            updateSummary()
                            progressCart?.visibility = View.GONE
                            loadingTitleText?.visibility = View.GONE
                        }
                    }
                }
            } catch (_: Exception) {
                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    val localItems = LocalCartStore.getAll(requireContext())
                    if (localItems.isNotEmpty()) {
                        usingLocalStore = true
                        renderItems(localItems)
                    } else {
                        Toast.makeText(
                            activity!!.applicationContext,
                            "Unable to load cart right now.",
                            Toast.LENGTH_SHORT
                        ).show()
                        progressCart?.visibility = View.GONE
                        loadingTitleText?.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun addToOrders(productId: String, quantity: Int, itemCost: Int, deliveryCost: Int) {
        Intent(activity!!.applicationContext, RazorPayActivity::class.java).also {
            it.putExtra("productId", productId)
            it.putExtra("itemCost", itemCost.toString())
            it.putExtra("quantity", quantity.toString())
            it.putExtra("deliveryCost", deliveryCost.toString())
            startActivity(it)
        }
    }

    private fun updateSummary() {
        if (!isOpened) {
            return
        }
        totalItemsValue?.text = totalCount.toString()
        totalCostValue?.text = formatLkr(totalPrice)
    }

    private fun renderItems(newItems: HashMap<String, Any>) {
        val ctx = context ?: return
        items = newItems
        totalCount = items.size
        totalPrice = 0

        val offlineCatalog = OfflineEcommerceData.getItems(ctx)
        val onlineQuantities = LinkedHashMap<String, Int>()
        for ((key, value) in items) {
            val currVal = value as? Map<*, *> ?: continue
            val quantity = currVal["quantity"].toString().toIntOrNull() ?: 1
            if (key.startsWith("offline_")) {
                val offlineItem = offlineCatalog.firstOrNull { it.id == key }
                if (offlineItem != null) {
                    totalPrice += (quantity * offlineItem.price) + offlineItem.deliveryCharge
                }
            } else {
                onlineQuantities[key] = quantity
            }
        }

        updateSummary()
        val adapter = CartItemsAdapter(this@CartFragment, items, this@CartFragment, usingLocalStore)
        recyclerCart?.adapter = adapter
        recyclerCart?.layoutManager = LinearLayoutManager(ctx)
        progressCart?.visibility = View.GONE
        loadingTitleText?.visibility = View.GONE

        // Price each online product on its own. (The old code observed one shared LiveData once per
        // item, so every observer added every product's price and totals were wrong.)
        if (onlineQuantities.isNotEmpty() && !executor.isShutdown) {
            val snapshot = LinkedHashMap(onlineQuantities)
            executor.execute {
                var extra = 0
                for ((productId, quantity) in snapshot) {
                    try {
                        val doc = SupabaseDb.selectOne("products", SupabaseDb.eq("id", productId))
                        if (doc != null && doc.exists()) {
                            val price = doc.get("price")?.toString()?.toIntOrNull() ?: 0
                            val delivery = doc.get("delCharge")?.toString()?.toIntOrNull()
                                ?: doc.get("del_charge")?.toString()?.toIntOrNull()
                                ?: 0
                            extra += quantity * price + delivery
                        }
                    } catch (e: Exception) {
                        Log.w("CartFragment", "Could not price product $productId", e)
                    }
                }
                activity?.runOnUiThread {
                    if (!isAdded || items !== newItems) return@runOnUiThread
                    totalPrice += extra
                    updateSummary()
                }
            }
        }
    }
}
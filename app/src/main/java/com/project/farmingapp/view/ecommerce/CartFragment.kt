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
import com.project.farmingapp.utilities.AuthHelper
import com.project.farmingapp.utilities.LocalCartStore
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.project.farmingapp.PrePaymentFragment
import com.project.farmingapp.R
import com.project.farmingapp.adapter.CartItemsAdapter
import com.project.farmingapp.utilities.CartItemBuy
import com.project.farmingapp.utilities.formatLkr
import com.project.farmingapp.viewmodel.EcommViewModel
import kotlin.collections.HashMap

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CartFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
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
        // Inflate the layout for this fragment
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_cart, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment CartFragment.
         */
        // TODO: Rename and change types and number of parameters
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
        val firebaseDatabase = FirebaseDatabase.getInstance()
        val cartRef =
            firebaseDatabase.getReference(AuthHelper.requireUid()).child("cart")

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
            return
        }

        val postListener = object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
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

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {

                    @Suppress("UNCHECKED_CAST")
                    val data = dataSnapshot.value as? HashMap<String, Any> ?: hashMapOf()
                    usingLocalStore = false
                    renderItems(data)
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
        }


        cartRef.addValueEventListener(postListener)


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

    override fun addToOrders(productId: String, quantity: Int, itemCost: Int, deliveryCost: Int) {
        Intent(activity!!.applicationContext, RazorPayActivity::class.java).also {
            //  it.putExtra("tp", "123")
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
        items = newItems
        totalCount = items.size
        totalPrice = 0
        for ((key, value) in items) {
            val currVal = value as? Map<*, *> ?: continue
            val quantity = currVal["quantity"].toString().toIntOrNull() ?: 1
            if (key.startsWith("offline_")) {
                val offlineItem = OfflineEcommerceData.getItems(requireContext())
                    .firstOrNull { it.id == key }
                if (offlineItem != null) {
                    totalPrice += (quantity * offlineItem.price) + offlineItem.deliveryCharge
                }
                updateSummary()
                continue
            }
            Log.d("Total Items", key)
            Log.d("Total Items", value.toString())
            ecommViewModel.getSpecificItem(key)
                .observe(viewLifecycleOwner, Observer {
                    if (it == null || !it.exists()) {
                        return@Observer
                    }
                    val onlinePrice = it.get("price").toString().toIntOrNull() ?: 0
                    val onlineDelivery = it.get("delCharge").toString().toIntOrNull() ?: 0
                    totalPrice += quantity * onlinePrice + onlineDelivery
                    updateSummary()
                })
        }

        updateSummary()
        val adapter = CartItemsAdapter(this@CartFragment, items, this@CartFragment, usingLocalStore)
        recyclerCart?.adapter = adapter
        recyclerCart?.layoutManager = LinearLayoutManager(requireContext())
        progressCart?.visibility = View.GONE
        loadingTitleText?.visibility = View.GONE
    }

}
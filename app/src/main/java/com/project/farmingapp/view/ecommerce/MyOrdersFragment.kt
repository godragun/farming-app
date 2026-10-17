package com.project.farmingapp.view.ecommerce

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import com.project.farmingapp.R
import com.project.farmingapp.adapter.MyOrdersAdapter
import com.project.farmingapp.databinding.FragmentMyOrdersBinding
import com.project.farmingapp.supabase.SupabaseDb
import com.project.farmingapp.utilities.AuthHelper
import com.project.farmingapp.utilities.CartItemBuy
import com.project.farmingapp.utilities.CellClickListener
import com.project.farmingapp.utilities.LocalOrderStore
import java.util.concurrent.Executors
import kotlin.collections.HashMap

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class MyOrdersFragment : Fragment(), CellClickListener, CartItemBuy {
    private var param1: String? = null
    private var param2: String? = null

    var orders = HashMap<String, Any>()
    private var _binding: FragmentMyOrdersBinding? = null
    private val binding get() = _binding!!
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MyOrdersFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.title = "My Orders"

        if (AuthHelper.guestMode) {
            @Suppress("UNCHECKED_CAST")
            orders = LocalOrderStore.getAll(requireContext()) as HashMap<String, Any>
            bindOrders()
            return
        }

        val uid = AuthHelper.runCatching { requireUid() }.getOrNull()
        if (uid == null) {
            @Suppress("UNCHECKED_CAST")
            orders = LocalOrderStore.getAll(requireContext()) as HashMap<String, Any>
            bindOrders()
            return
        }
        executor.execute {
            try {
                val docs = SupabaseDb.select(
                    "orders",
                    "${SupabaseDb.eq("uid", uid)}&${SupabaseDb.order("time_ms", ascending = false)}"
                )
                val map = HashMap<String, Any>()
                for (doc in docs) {
                    @Suppress("UNCHECKED_CAST")
                    val raw = doc.get("raw") as? Map<String, Any>
                    if (raw != null) {
                        map[doc.id] = HashMap(raw)
                    } else {
                        map[doc.id] = hashMapOf<String, Any>(
                            "productId" to (doc.getString("productId") ?: doc.getString("product_id").orEmpty()),
                            "quantity" to (doc.getLong("quantity") ?: 1),
                            "itemCost" to (doc.get("cost")?.toString()?.toIntOrNull() ?: 0),
                            "deliveryCost" to 0,
                            "deliveryStatus" to (doc.getString("status").orEmpty()),
                            "pincode" to "",
                            "time" to (doc.get("time")?.toString() ?: doc.get("time_ms")?.toString().orEmpty()),
                            "name" to "",
                            "locality" to "",
                            "city" to "",
                            "state" to "",
                            "mobile" to (doc.getString("phone").orEmpty())
                        )
                    }
                }
                activity?.runOnUiThread {
                    if (!isAdded || _binding == null) return@runOnUiThread
                    if (map.isEmpty()) {
                        @Suppress("UNCHECKED_CAST")
                        orders = LocalOrderStore.getAll(requireContext()) as HashMap<String, Any>
                    } else {
                        orders = map
                    }
                    bindOrders()
                }
            } catch (_: Exception) {
                activity?.runOnUiThread {
                    if (!isAdded || _binding == null) return@runOnUiThread
                    @Suppress("UNCHECKED_CAST")
                    orders = LocalOrderStore.getAll(requireContext()) as HashMap<String, Any>
                    bindOrders()
                }
            }
        }
    }

    private fun bindOrders() {
        if (orders.isEmpty()) return
        @Suppress("UNCHECKED_CAST")
        val adapterOrders = orders as HashMap<String, Object>
        val myOrdersAdapter = MyOrdersAdapter(
            this@MyOrdersFragment,
            adapterOrders,
            this@MyOrdersFragment,
            this@MyOrdersFragment
        )
        val b = _binding ?: return
        b.myOrderRecycler.adapter = myOrdersAdapter
        b.myOrderRecycler.layoutManager = LinearLayoutManager(b.root.context)
    }

    override fun onCellClickListener(name: String) {
        val ecommerceItemFragment = EcommerceItemFragment()
        activity!!.supportFragmentManager
            .beginTransaction()
            .replace(R.id.frame_layout, ecommerceItemFragment, name)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .setReorderingAllowed(true)
            .addToBackStack("ecommItem")
            .commit()
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
}

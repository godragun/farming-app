package com.project.farmingapp.view.ecommerce

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.chip.ChipGroup
import com.project.farmingapp.R
import com.project.farmingapp.adapter.EcommerceAdapter
import com.project.farmingapp.adapter.OfflineEcommerceAdapter
import com.project.farmingapp.adapter.OfflineEcommerceItem
import com.project.farmingapp.utilities.CellClickListener
import com.project.farmingapp.viewmodel.EcommViewModel


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [EcommerceFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

class EcommerceFragment : Fragment(), CellClickListener {
    private lateinit var viewmodel: EcommViewModel
    private var adapter: EcommerceAdapter? = null
    lateinit var ecommerceItemFragment: EcommerceItemFragment
    private var chipGroup: ChipGroup? = null
    private var recycler: androidx.recyclerview.widget.RecyclerView? = null
    private var searchInput: EditText? = null
    private var allOnlineItems = listOf<com.project.farmingapp.supabase.RemoteDocument>()
    private var categoryOnlineItems = listOf<com.project.farmingapp.supabase.RemoteDocument>()
    private var offlineCategoryItems = listOf<OfflineEcommerceItem>()
    private var currentMode: String = "online_all"
    private var currentQuery: String = ""
    private var currentType: String = "all"

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

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
        val root = inflater.inflate(R.layout.fragment_ecommerce, container, false)
        chipGroup = root.findViewById(R.id.chipgrp)
        recycler = root.findViewById(R.id.ecommrcyclr)
        searchInput = root.findViewById(R.id.ecommSearchInput)

        recycler?.layoutManager = GridLayoutManager(requireContext(), 2)

        viewmodel.ecommLiveData.observe(viewLifecycleOwner, Observer { items ->
            allOnlineItems = items ?: emptyList()
            if (currentType == "all") {
                if (allOnlineItems.isEmpty()) {
                    showOfflineItems("all")
                } else {
                    currentMode = "online_all"
                    applySearchFilter(currentQuery)
                }
            }
        })

        viewmodel.specificCategoryItems.observe(viewLifecycleOwner, Observer { items ->
            categoryOnlineItems = items ?: emptyList()
            if (currentType != "all") {
                if (categoryOnlineItems.isEmpty()) {
                    showOfflineItems(currentType)
                } else {
                    currentMode = "online_category"
                    applySearchFilter(currentQuery)
                }
            }
        })

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        setHasOptionsMenu(true)
        (activity as AppCompatActivity).supportActionBar?.title = "E-Commerce"

        chipGroup?.check(R.id.chip1)
        viewmodel.loadAllEcommItems()
        searchInput?.doAfterTextChanged {
            currentQuery = it?.toString().orEmpty()
            applySearchFilter(currentQuery)
        }

        chipGroup?.setOnCheckedChangeListener { _, checkedId ->

            when (checkedId) {
                R.id.chip1 -> {
                    currentType = "all"
                    viewmodel.loadAllEcommItems()
                }
                R.id.chip2 -> {
                    currentType = "fertilizer"
                    viewmodel.getSpecificCategoryItems("fertilizer")
                }

                R.id.chip3 -> {
                    currentType = "pestiside"
                    viewmodel.getSpecificCategoryItems("pestiside")
                }

                R.id.chip4 -> {
                    currentType = "machine"
                    viewmodel.getSpecificCategoryItems("machine")
                }
            }
        }
    }

    private fun showOfflineItems(type: String) {
        currentMode = "offline"
        val offlineItems = OfflineEcommerceData.getItems(requireContext())
        offlineCategoryItems = if (type == "all") offlineItems else offlineItems.filter { it.type == type }
        applySearchFilter(currentQuery)
    }

    private fun applySearchFilter(query: String) {
        val normalized = query.trim().lowercase()
        when (currentMode) {
            "online_all" -> {
                val filtered = allOnlineItems.filter { document ->
                    val title = document.getString("title").orEmpty().lowercase()
                    val shortDesc = document.getString("shortDesc").orEmpty().lowercase()
                    normalized.isBlank() || title.contains(normalized) || shortDesc.contains(normalized)
                }
                adapter = EcommerceAdapter(requireContext(), filtered, this)
                recycler?.adapter = adapter
                recycler?.scheduleLayoutAnimation()
            }
            "online_category" -> {
                val filtered = categoryOnlineItems.filter { document ->
                    val title = document.getString("title").orEmpty().lowercase()
                    val shortDesc = document.getString("shortDesc").orEmpty().lowercase()
                    normalized.isBlank() || title.contains(normalized) || shortDesc.contains(normalized)
                }
                adapter = EcommerceAdapter(requireContext(), filtered, this)
                recycler?.adapter = adapter
                recycler?.scheduleLayoutAnimation()
            }
            else -> {
                val filteredItems = offlineCategoryItems.filter { item ->
                    val title = item.title.lowercase()
                    val shortDesc = item.shortDesc.lowercase()
                    normalized.isBlank() || title.contains(normalized) || shortDesc.contains(normalized)
                }
                recycler?.adapter = OfflineEcommerceAdapter(filteredItems) {
                    openOfflineItemDetails(it)
                }
                recycler?.scheduleLayoutAnimation()
            }
        }
    }

    private fun openOfflineItemDetails(item: OfflineEcommerceItem) {
        val offlineFragment = EcommerceItemFragment()
        val bundle = OfflineEcommerceData.toBundle(item)
        offlineFragment.arguments = bundle
        activity!!.supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(R.id.frame_layout, offlineFragment, item.id)
            .setReorderingAllowed(true)
            .addToBackStack("offlineEcommItem")
            .commit()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment EcommerceFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            EcommerceFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onCellClickListener(name: String) {
        ecommerceItemFragment = EcommerceItemFragment()
        val bundle = Bundle()
        bundle.putString("name", name)
        ecommerceItemFragment.setArguments(bundle)

        val transaction = activity!!.supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(R.id.frame_layout, ecommerceItemFragment, name)
            .setReorderingAllowed(true)
            .addToBackStack("ecommItem")
            .commit()

    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.cart_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
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
}
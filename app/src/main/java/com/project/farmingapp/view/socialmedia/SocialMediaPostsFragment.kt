package com.project.farmingapp.view.socialmedia

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.project.farmingapp.R
import com.project.farmingapp.adapter.OfflinePostItem
import com.project.farmingapp.adapter.OfflineSMPostAdapter
import com.project.farmingapp.adapter.SMPostListAdapter
import com.project.farmingapp.viewmodel.SocialMediaViewModel
import com.project.farmingapp.databinding.FragmentSocialMediaPostsBinding
import com.project.farmingapp.utilities.AgriAssetPaths
import com.project.farmingapp.utilities.pulseTapFeedback

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SocialMediaPostsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SocialMediaPostsFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var viewModel: SocialMediaViewModel
    lateinit var smCreatePostFragment: SMCreatePostFragment
    private var adapter : SMPostListAdapter? = null
    private var _binding: FragmentSocialMediaPostsBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        viewModel = ViewModelProvider(requireActivity())[SocialMediaViewModel::class.java]

//        viewModel.loadPosts()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSocialMediaPostsBinding.inflate(inflater, container, false)
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
         * @return A new instance of fragment SocialMediaPostsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SocialMediaPostsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    fun getData() {
        val firebaseFirestore: FirebaseFirestore = FirebaseFirestore.getInstance()


        firebaseFirestore.collection("posts").orderBy("timeStamp", Query.Direction.DESCENDING).get()
            .addOnSuccessListener {
                if (!isAdded || _binding == null) return@addOnSuccessListener
                Log.d("Posts data", it.documents.toString())
                if (it.documents.isNullOrEmpty()) {
                    showOfflinePosts()
                } else {
                    adapter = SMPostListAdapter(requireContext(), it.documents)
                    binding.postsRecycler.adapter = adapter
                    binding.postsRecycler.scheduleLayoutAnimation()
                }
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                showOfflinePosts()
            }
    }

    private fun showOfflinePosts() {
        val staticPosts = listOf(
            OfflinePostItem(
                name = "Agri Lanka Team",
                title = "Apple Leaf Spots: Quick Solution",
                description = "Mix neem-based spray and apply every 5-7 days in the evening. Remove heavily infected leaves and keep spacing for airflow.",
                uploadedAt = System.currentTimeMillis() - 2 * 60 * 60 * 1000L,
                imageRes = R.drawable.plant3,
                imageAssetName = null
            ),
            OfflinePostItem(
                name = "Farmer Support",
                title = "Paddy Field Water Management",
                description = "Use alternate wetting and drying to reduce water usage while protecting yield. Check soil moisture before re-irrigation.",
                uploadedAt = System.currentTimeMillis() - 5 * 60 * 60 * 1000L,
                imageRes = R.drawable.method2,
                imageAssetName = AgriAssetPaths.PADDY_WATER_MANAGEMENT
            ),
            OfflinePostItem(
                name = "Colombo Growers",
                title = "Compost Tip for Home Gardens",
                description = "Use 2:1 dry-to-wet material ratio for fast composting. Turn weekly and keep moisture like a squeezed sponge.",
                uploadedAt = System.currentTimeMillis() - 24 * 60 * 60 * 1000L,
                imageRes = R.drawable.disease,
                imageAssetName = AgriAssetPaths.PLANTS_I_GREW_POST
            ),
            OfflinePostItem(
                name = "Soil Care Hub",
                title = "How to prepare seed beds",
                description = "Loosen topsoil 15-20 cm, add organic compost, and keep moisture balanced before sowing.",
                uploadedAt = System.currentTimeMillis() - 36 * 60 * 60 * 1000L,
                imageRes = R.drawable.method2,
                imageAssetName = AgriAssetPaths.SEED_SPREADING
            ),
            OfflinePostItem(
                name = "Farm Circle",
                title = "Morning irrigation tip",
                description = "Irrigate early morning for better absorption and lower evaporation loss.",
                uploadedAt = System.currentTimeMillis() - 48 * 60 * 60 * 1000L,
                imageRes = R.drawable.plant3,
                imageAssetName = AgriAssetPaths.SPRINKLING_SYSTEM_POST
            ),
            OfflinePostItem(
                name = "Crop Watch",
                title = "Early disease identification",
                description = "Check underside of leaves weekly for spots or insects to act before spread.",
                uploadedAt = System.currentTimeMillis() - 72 * 60 * 60 * 1000L,
                imageRes = R.drawable.disease,
                imageAssetName = null
            ),
            OfflinePostItem(
                name = "Agri Lanka",
                title = "New plantation way",
                description = "Contour beds, stable moisture, and quality planting material help roots establish fast and cut runoff after rains.",
                uploadedAt = System.currentTimeMillis() - 80 * 60 * 60 * 1000L,
                imageRes = R.drawable.method2,
                imageAssetName = AgriAssetPaths.NEW_PLANTATION_WAY_POST
            )
        )
        val mergedPosts = LocalPostDraftStore.allDrafts() + staticPosts
        binding.postsRecycler.adapter = OfflineSMPostAdapter(mergedPosts)
        binding.postsRecycler.scheduleLayoutAnimation()
        Toast.makeText(requireContext(), "Showing demo posts in offline mode.", Toast.LENGTH_SHORT).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        (activity as AppCompatActivity).supportActionBar?.title = "Social Media"
        binding.postsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.postsRecycler.setHasFixedSize(true)
        binding.postsRecycler.itemAnimator = null

        getData()
        smCreatePostFragment = SMCreatePostFragment()
        binding.createPostFloating.setOnClickListener {
            binding.createPostFloating.pulseTapFeedback()
            requireActivity().supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.frame_layout, smCreatePostFragment, "smCreate")
                .setReorderingAllowed(true)
                .addToBackStack("smCreate")
                .commit()
        }
    }


}
package com.project.farmingapp.view.user

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.service.autofill.UserData
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.project.farmingapp.model.SupabaseUploader
import java.util.UUID
import com.project.farmingapp.R
import com.project.farmingapp.adapter.PostListUserProfileAdapter
import com.project.farmingapp.databinding.FragmentUserBinding
import com.project.farmingapp.utilities.AuthHelper
import com.project.farmingapp.utilities.CellClickListener
import com.project.farmingapp.viewmodel.ArticleViewModel
import com.project.farmingapp.viewmodel.UserDataViewModel
import com.project.farmingapp.viewmodel.UserProfilePostsViewModel
import kotlin.collections.ArrayList
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private lateinit var viewModel: UserProfilePostsViewModel
private lateinit var userDataViewModel: UserDataViewModel

/**
 * A simple [Fragment] subclass.
 * Use the [UserFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class UserFragment : Fragment(), CellClickListener {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null  
    private val PICK_IMAGE_REQUEST = 71
    private var filePath: Uri? = null
    private var postID: UUID? = null
    private var bitmap: Bitmap? = null
    private var uploadProfOrBack: Int? = null

    val firebaseFirestore = FirebaseFirestore.getInstance()
    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!
  
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }


        viewModel = ViewModelProvider(requireActivity())[UserProfilePostsViewModel::class.java]

        userDataViewModel = ViewModelProvider(requireActivity())[UserDataViewModel::class.java]

        viewModel.getAllPosts(AuthHelper.requireEmail())
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        viewModel.liveData1.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                viewModel.getAllPostsOfUser(it)
            }
        })


        viewModel.liveData2.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                Log.d("Live Data In Frag", it.toString())
            }
        })

        viewModel.userProfilePostsLiveData2.observe(viewLifecycleOwner, Observer {
            Log.d("Some Part 2", it.toString())
        })

        _binding = FragmentUserBinding.inflate(inflater, container, false)
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
         * @return A new instance of fragment UserFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            UserFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        (activity as AppCompatActivity).supportActionBar?.title = "Profile"

//        addAboutTextUserFrag.paintFlags = Paint.UNDERLINE_TEXT_FLAG
        binding.cityEditUserProfile.visibility = View.GONE

        viewModel.userProfilePostsLiveData.observe(viewLifecycleOwner, Observer {

            Log.d("Some Part", it.toString())
        })

        userDataViewModel.userliveData.observe(viewLifecycleOwner, Observer {
            Log.d("User Fragment", it.data.toString())
        })

//        aboutValueUserProfileFrag.setOnClickListener {
//            aboutValueEditUserProfileFrag.visibility = View.VISIBLE
//            addAboutTextUserFrag.visibility = View.GONE
//            aboutValueEditUserProfileFrag.setText(aboutValueUserProfileFrag.text.toString())
//            aboutValueUserProfileFrag.visibility = View.GONE
//            saveBtnAboutUserProfileFrag.visibility = View.VISIBLE
//        }

//        addAboutTextUserFrag.setOnClickListener {
//            aboutValueEditUserProfileFrag.visibility = View.VISIBLE
//            saveBtnAboutUserProfileFrag.visibility = View.VISIBLE
//        }

//        saveBtnAboutUserProfileFrag.setOnClickListener {
//            aboutValueUserProfileFrag.visibility = View.VISIBLE
//            aboutValueUserProfileFrag.text = aboutValueEditUserProfileFrag.text
//            saveBtnAboutUserProfileFrag.visibility = View.GONE
//            userDataViewModel.updateUserField(activity!!.applicationContext, firebaseAuth.currentUser!!.email.toString() as String, aboutValueEditUserProfileFrag.text.toString() as String, null)
//            aboutValueEditUserProfileFrag.visibility = View.GONE
//        }

        binding.uploadProgressBarProfile.visibility = View.GONE
        binding.uploadBackProgressProfile.visibility = View.GONE


        binding.uploadUserBackgroundImage.setOnClickListener {
            val intent = Intent()
            intent.type = "image/* video/*"
            intent.action = Intent.ACTION_PICK
            startActivityForResult(
                Intent.createChooser(intent, "Select Picture"),
                PICK_IMAGE_REQUEST
            )
            uploadProfOrBack = 1

            Toast.makeText(activity!!.applicationContext, "Uploading Background Image", Toast.LENGTH_SHORT).show()
        }

        binding.uploadProfilePictureImage.setOnClickListener {
            val intent = Intent()
            intent.type = "image/* video/*"
            intent.action = Intent.ACTION_PICK
            startActivityForResult(
                Intent.createChooser(intent, "Select Picture"),
                PICK_IMAGE_REQUEST
            )
            uploadProfOrBack = 0
            Toast.makeText(activity!!.applicationContext, "Uploading your Image", Toast.LENGTH_SHORT).show()
        }


        userDataViewModel.userliveData.observe(viewLifecycleOwner, Observer {
            Log.d("User Data in VM Frag", it.get("name").toString())
            Log.d("Data in User", it.toString())
            binding.userNameUserProfileFrag.text = it!!.getString("name")
            val city = it?.getString("city")
            if(city == null){
                binding.userCityUserProfileFrag.text = "City: "
            } else{
                binding.userCityUserProfileFrag.text = "City: " + it.getString("city")
            }

            if(it?.get("profileImage") == null || it?.getString("profileImage").isNullOrBlank()){
                binding.uploadProfilePictureImage.visibility = View.VISIBLE
            } else{
                binding.uploadProfilePictureImage.visibility = View.GONE
                Glide.with(activity!!.applicationContext).load(it?.get("profileImage"))
                    .into(binding.userImageUserFrag)
            }


            if(it?.get("backImage") == null || it?.getString("backImage").isNullOrBlank()){
                binding.uploadUserBackgroundImage.visibility = View.VISIBLE
            } else{
                binding.uploadUserBackgroundImage.visibility = View.GONE
                Glide.with(activity!!.applicationContext).load(it?.getString("backImage"))
                    .into(binding.userBackgroundImage)
            }

            val posts = it.get("posts") as List<String>
            binding.userPostsCountUserProfileFrag.text = "Posts: " + posts.size.toString()
            binding.userEmailUserProfileFrag.text = AuthHelper.requireEmail()
            val about = it?.getString("about")

            if (about == null || about == "") {
                binding.aboutValueUserProfileFrag.visibility = View.GONE
                binding.aboutValueEditUserProfileFrag.visibility = View.GONE
                binding.saveBtnAboutUserProfileFrag.visibility = View.GONE
            } else {
                binding.aboutValueUserProfileFrag.visibility = View.VISIBLE
                binding.aboutValueEditUserProfileFrag.visibility = View.GONE
                binding.saveBtnAboutUserProfileFrag.visibility = View.GONE
                binding.aboutValueUserProfileFrag.text = about
            }

        })

        binding.imageEdit.setOnClickListener {
            binding.uploadProfilePictureImage.visibility = View.VISIBLE
            binding.uploadUserBackgroundImage.visibility = View.VISIBLE
            binding.imageChecked.visibility = View.VISIBLE
            binding.imageEdit.visibility = View.GONE
            binding.cityEditUserProfile.setText(binding.userCityUserProfileFrag.text.toString().removePrefix("City: "))
            binding.cityEditUserProfile.visibility = View.VISIBLE
            binding.aboutValueEditUserProfileFrag.visibility = View.VISIBLE
            binding.aboutValueEditUserProfileFrag.setText(binding.aboutValueUserProfileFrag.text.toString())
            binding.aboutValueUserProfileFrag.visibility = View.GONE
        }

        binding.imageChecked.setOnClickListener {
            binding.uploadProfilePictureImage.visibility = View.GONE
            binding.uploadUserBackgroundImage.visibility = View.GONE
            binding.imageEdit.visibility = View.VISIBLE
            binding.cityEditUserProfile.visibility = View.GONE
            binding.imageChecked.visibility = View.GONE
            userDataViewModel.updateUserField(
                activity!!.applicationContext,
                AuthHelper.requireEmail(),
                binding.aboutValueEditUserProfileFrag.text.toString(),
                binding.cityEditUserProfile.text.toString()
            )
            userDataViewModel.getUserData(AuthHelper.requireEmail())
            binding.aboutValueEditUserProfileFrag.visibility = View.GONE
            binding.aboutValueUserProfileFrag.visibility = View.VISIBLE
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data == null || data.data == null) {
                return
            }

            filePath = data.data
            val capturedPath = filePath ?: return
            lifecycleScope.launch {
                val bm: Bitmap? = try {
                    withContext(Dispatchers.IO) {
                        MediaStore.Images.Media.getBitmap(activity!!.contentResolver, capturedPath)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
                bitmap = bm
                if (bm != null) {
                    Log.d("UserFragment", bm.toString())
                    if (uploadProfOrBack == 0) {
                        binding.uploadProgressBarProfile.visibility = View.VISIBLE
                        binding.uploadProfilePictureImage.visibility = View.GONE
                    } else if (uploadProfOrBack == 1) {
                        binding.uploadBackProgressProfile.visibility = View.VISIBLE
                        binding.uploadUserBackgroundImage.visibility = View.GONE
                    }
                    uploadImage2()
                }
            }
        }

    }


    private fun uploadImage2() {
        if (filePath == null) return
        val uri = filePath ?: return
        val userId = AuthHelper.requireUid()
        binding.uploadProgressBarProfile.visibility = View.VISIBLE
        binding.uploadBackProgressProfile.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val cacheFile = withContext(Dispatchers.IO) { copyUriToCacheFile(uri, "profile_upload") }
                val mime = requireContext().contentResolver.getType(uri) ?: "image/jpeg"
                val publicUrl = withContext(Dispatchers.IO) {
                    SupabaseUploader.uploadFile(
                        file = cacheFile,
                        mimeType = mime,
                        subfolder = "uploads/$userId",
                        originalFileName = cacheFile.name
                    )
                }
                cacheFile.delete()
                uploadUserPhotos(publicUrl, UUID.randomUUID())
            } catch (e: Exception) {
                Toast.makeText(activity?.applicationContext, "Upload error: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.uploadProgressBarProfile.visibility = View.GONE
                binding.uploadBackProgressProfile.visibility = View.GONE
                binding.uploadProfilePictureImage.visibility = View.VISIBLE
                binding.uploadUserBackgroundImage.visibility = View.VISIBLE
            }
        }
    }

    private fun copyUriToCacheFile(uri: Uri, prefix: String): File {
        val mime = requireContext().contentResolver.getType(uri).orEmpty()
        val ext = when {
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            else -> "jpg"
        }
        val out = File(requireContext().cacheDir, "${prefix}_${System.currentTimeMillis()}.$ext")
        requireContext().contentResolver.openInputStream(uri).use { input ->
            if (input == null) throw IOException("Failed to open input stream")
            FileOutputStream(out).use { output -> input.copyTo(output) }
        }
        return out
    }

    private fun uploadUserPhotos(uri: String, postID: UUID) {
        val field = if (uploadProfOrBack == 0) "profileImage" else "backImage"
        firebaseFirestore.collection("users").document(AuthHelper.requireEmail())
            .update(field, uri)
            .addOnSuccessListener {
                Toast.makeText(activity?.applicationContext, "Profile updated", Toast.LENGTH_SHORT).show()
                binding.uploadProgressBarProfile.visibility = View.GONE
                binding.uploadBackProgressProfile.visibility = View.GONE
                binding.imageEdit.visibility = View.VISIBLE
                binding.imageChecked.visibility = View.GONE
                if (uploadProfOrBack == 0) binding.userImageUserFrag.setImageBitmap(bitmap)
                else binding.userBackgroundImage.setImageBitmap(bitmap)
                userDataViewModel.getUserData(AuthHelper.requireEmail())
            }
            .addOnFailureListener {
                binding.uploadProgressBarProfile.visibility = View.GONE
                binding.uploadBackProgressProfile.visibility = View.GONE
                binding.uploadProfilePictureImage.visibility = View.VISIBLE
                binding.uploadUserBackgroundImage.visibility = View.VISIBLE
                Toast.makeText(activity?.applicationContext, "Failed to save profile", Toast.LENGTH_SHORT).show()
            }
    }





    override fun onCellClickListener(name: String) {
        val dialog = AlertDialog.Builder(activity)

        dialog.setTitle("Your Post")
            .setMessage("Do you want to edit your post?")
            .setPositiveButton("View") { dialogInterface, i ->

            }
            .setNegativeButton("Delete") { dialogInterface, i ->
                userDataViewModel.deleteUserPost(AuthHelper.requireEmail(), name)
                userDataViewModel.getUserData(AuthHelper.requireEmail())
                viewModel.getAllPosts(AuthHelper.requireEmail())
            }
            .setNeutralButton("Cancel"){
                dialogInterface, i ->

            }
            .show()

        Toast.makeText(activity!!.applicationContext, "You Clicked" + name.toString(), Toast.LENGTH_SHORT).show()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.liveData3.observe(this, Observer {
            Log.d("All Posts", it.toString())
            val adapter = PostListUserProfileAdapter(requireContext(), it, this)
            binding.userProfilePostsRecycler.adapter = adapter

            binding.userProfilePostsRecycler.layoutManager =
                LinearLayoutManager(activity!!.applicationContext)
            adapter.notifyDataSetChanged()

        })
    }
}
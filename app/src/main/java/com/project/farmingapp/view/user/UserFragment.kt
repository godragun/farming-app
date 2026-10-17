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
import com.project.farmingapp.model.SupabaseUploader
import java.util.UUID
import com.project.farmingapp.R
import com.project.farmingapp.adapter.PostListUserProfileAdapter
import com.project.farmingapp.databinding.FragmentUserBinding
import com.project.farmingapp.supabase.FieldAlias
import com.project.farmingapp.supabase.SupabaseDb
import com.project.farmingapp.utilities.AuthHelper
import com.project.farmingapp.utilities.CellClickListener
import com.project.farmingapp.viewmodel.UserDataViewModel
import com.project.farmingapp.viewmodel.UserProfilePostsViewModel
import kotlin.collections.ArrayList
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.Executors
import org.json.JSONObject

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

    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!
    private val executor = Executors.newSingleThreadExecutor()
  
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }


        viewModel = ViewModelProvider(requireActivity())[UserProfilePostsViewModel::class.java]

        userDataViewModel = ViewModelProvider(requireActivity())[UserDataViewModel::class.java]

        currentEmail()?.let { viewModel.getAllPosts(it) }
    }

    /** Email of the signed-in user (or guest), or null if the session is gone. Never throws. */
    private fun currentEmail(): String? = AuthHelper.runCatching { requireEmail() }.getOrNull()

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
            val b = _binding ?: return@Observer
            if (it == null) return@Observer
            Log.d("Data in User", it.toString())
            b.userNameUserProfileFrag.text = it.getString("name").orEmpty()
            val city = it.getString("city")
            if (city == null) {
                b.userCityUserProfileFrag.text = "City: "
            } else {
                b.userCityUserProfileFrag.text = "City: $city"
            }

            val ctx = context?.applicationContext ?: return@Observer
            if (it.getString("profileImage").isNullOrBlank()) {
                b.uploadProfilePictureImage.visibility = View.VISIBLE
            } else {
                b.uploadProfilePictureImage.visibility = View.GONE
                Glide.with(ctx).load(it.getString("profileImage")).into(b.userImageUserFrag)
            }

            if (it.getString("backImage").isNullOrBlank()) {
                b.uploadUserBackgroundImage.visibility = View.VISIBLE
            } else {
                b.uploadUserBackgroundImage.visibility = View.GONE
                Glide.with(ctx).load(it.getString("backImage")).into(b.userBackgroundImage)
            }

            // "posts" is null for brand-new profiles; a hard cast here crashed the screen.
            val postCount = (it.get("posts") as? List<*>)?.size ?: 0
            b.userPostsCountUserProfileFrag.text = "Posts: $postCount"
            b.userEmailUserProfileFrag.text = currentEmail().orEmpty()
            val about = it.getString("about")

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
            val email = currentEmail()
            if (email != null) {
                userDataViewModel.updateUserField(
                    activity!!.applicationContext,
                    email,
                    binding.aboutValueEditUserProfileFrag.text.toString(),
                    binding.cityEditUserProfile.text.toString()
                )
                userDataViewModel.getUserData(email)
            }
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
            val picked = filePath ?: return
            // Show the picked image straight from its Uri via Glide. The previous code decoded the
            // whole photo into a Bitmap (MediaStore.getBitmap), which runs out of memory on large
            // camera photos - an OutOfMemoryError is not an Exception, so it killed the app.
            val b = _binding ?: return
            if (uploadProfOrBack == 0) {
                b.uploadProgressBarProfile.visibility = View.VISIBLE
                b.uploadProfilePictureImage.visibility = View.GONE
                Glide.with(this).load(picked).into(b.userImageUserFrag)
            } else if (uploadProfOrBack == 1) {
                b.uploadBackProgressProfile.visibility = View.VISIBLE
                b.uploadUserBackgroundImage.visibility = View.GONE
                Glide.with(this).load(picked).into(b.userBackgroundImage)
            }
            uploadImage2()
        }

    }

    private fun uploadImage2() {
        if (filePath == null) return
        val uri = filePath ?: return
        val userId = AuthHelper.runCatching { requireUid() }.getOrNull()
        if (userId == null) {
            Toast.makeText(activity?.applicationContext, "Please log in to update your profile", Toast.LENGTH_SHORT).show()
            return
        }
        val appContext = context?.applicationContext ?: return
        _binding?.uploadProgressBarProfile?.visibility = View.VISIBLE
        _binding?.uploadBackProgressProfile?.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val cacheFile = withContext(Dispatchers.IO) { copyUriToCacheFile(appContext, uri, "profile_upload") }
                val mime = appContext.contentResolver.getType(uri) ?: "image/jpeg"
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
                Toast.makeText(appContext, "Upload error: ${e.message}", Toast.LENGTH_SHORT).show()
                _binding?.let { b ->
                    b.uploadProgressBarProfile.visibility = View.GONE
                    b.uploadBackProgressProfile.visibility = View.GONE
                    b.uploadProfilePictureImage.visibility = View.VISIBLE
                    b.uploadUserBackgroundImage.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun copyUriToCacheFile(context: android.content.Context, uri: Uri, prefix: String): File {
        val mime = context.contentResolver.getType(uri).orEmpty()
        val ext = when {
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            else -> "jpg"
        }
        val out = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.$ext")
        context.contentResolver.openInputStream(uri).use { input ->
            if (input == null) throw IOException("Failed to open input stream")
            FileOutputStream(out).use { output -> input.copyTo(output) }
        }
        return out
    }

    private fun uploadUserPhotos(uri: String, postID: UUID) {
        val field = if (uploadProfOrBack == 0) "profileImage" else "backImage"
        val email = currentEmail() ?: return
        executor.execute {
            try {
                val snake = FieldAlias.toSnakeRow(mapOf(field to uri))
                val patch = JSONObject()
                snake.forEach { (k, v) -> patch.put(k, v ?: JSONObject.NULL) }
                SupabaseDb.update("users", SupabaseDb.eq("email", email), patch)
                activity?.runOnUiThread {
                    if (!isAdded || _binding == null) return@runOnUiThread
                    Toast.makeText(activity?.applicationContext, "Profile updated", Toast.LENGTH_SHORT).show()
                    binding.uploadProgressBarProfile.visibility = View.GONE
                    binding.uploadBackProgressProfile.visibility = View.GONE
                    binding.imageEdit.visibility = View.VISIBLE
                    binding.imageChecked.visibility = View.GONE
                    // The picked image is already shown (Glide); nothing else to decode here.
                    userDataViewModel.getUserData(email)
                }
            } catch (_: Exception) {
                activity?.runOnUiThread {
                    if (!isAdded || _binding == null) return@runOnUiThread
                    binding.uploadProgressBarProfile.visibility = View.GONE
                    binding.uploadBackProgressProfile.visibility = View.GONE
                    binding.uploadProfilePictureImage.visibility = View.VISIBLE
                    binding.uploadUserBackgroundImage.visibility = View.VISIBLE
                    Toast.makeText(activity?.applicationContext, "Failed to save profile", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }





    override fun onCellClickListener(name: String) {
        val dialog = AlertDialog.Builder(activity)

        dialog.setTitle("Your Post")
            .setMessage("Do you want to edit your post?")
            .setPositiveButton("View") { dialogInterface, i ->

            }
            .setNegativeButton("Delete") { dialogInterface, i ->
                val email = currentEmail()
                if (email != null) {
                    userDataViewModel.deleteUserPost(email, name)
                    userDataViewModel.getUserData(email)
                    viewModel.getAllPosts(email)
                }
            }
            .setNeutralButton("Cancel"){
                dialogInterface, i ->

            }
            .show()

        Toast.makeText(activity!!.applicationContext, "You Clicked" + name.toString(), Toast.LENGTH_SHORT).show()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.liveData3.observe(viewLifecycleOwner, Observer {
            val b = _binding ?: return@Observer
            val ctx = context ?: return@Observer
            Log.d("All Posts", it.toString())
            val adapter = PostListUserProfileAdapter(ctx, it, this)
            b.userProfilePostsRecycler.adapter = adapter

            b.userProfilePostsRecycler.layoutManager = LinearLayoutManager(ctx)
            adapter.notifyDataSetChanged()

        })
    }
}
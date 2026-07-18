package com.project.farmingapp.view.socialmedia

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.project.farmingapp.R
import com.project.farmingapp.model.SupabaseUploader
import com.project.farmingapp.utilities.AuthHelper
import com.project.farmingapp.databinding.FragmentSMCreatePostBinding
import com.project.farmingapp.utilities.pulseTapFeedback
import com.project.farmingapp.viewmodel.UserDataViewModel
import com.project.farmingapp.viewmodel.UserProfilePostsViewModel
import java.io.IOException
import java.io.File
import java.io.FileOutputStream
import java.util.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SMCreatePostFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SMCreatePostFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private val PICK_IMAGE_REQUEST = 71
    private var filePath: Uri? = null
    private var postID: UUID? = null
    private var bitmap: Bitmap? = null
    lateinit var socialMediaPostsFragment: SocialMediaPostsFragment
    lateinit var userDataViewModel : UserDataViewModel
    val db = FirebaseFirestore.getInstance()
    val data2 = HashMap<String, Any>()
    private var _binding: FragmentSMCreatePostBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        userDataViewModel = ViewModelProvider(requireActivity())[UserDataViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSMCreatePostBinding.inflate(inflater, container, false)
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
         * @return A new instance of fragment SMCreatePostFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SMCreatePostFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        (activity as AppCompatActivity).supportActionBar?.title = "Social Media"

        binding.progressCreatePost.visibility = View.GONE
        binding.progressTitle.visibility = View.GONE

        data2["uploadType"] = ""
        binding.uploadImagePreview.setOnClickListener {
            binding.uploadImagePreview.pulseTapFeedback()
            val intent = Intent()
            intent.type = "image/* video/*"
            intent.action = Intent.ACTION_PICK
            startActivityForResult(
                Intent.createChooser(intent, "Select Picture"),
                PICK_IMAGE_REQUEST
            )
        }

        val displayName = AuthHelper.currentDisplayName()
        if (displayName.isNullOrBlank()) {
            db.collection("users").document(AuthHelper.requireEmail())
                .get()
                .addOnCompleteListener {
                    val data = it.result
                    data2["name"] = data!!.getString("name").toString()
                    Log.d("Google User", data!!.getString("name") ?: "")
                }
        } else {
            data2["name"] = displayName.toString()
            Log.d("Normal User", displayName)
        }

        binding.createPostBtnSM.setOnClickListener {
            binding.createPostBtnSM.pulseTapFeedback()
            val titleText = binding.postTitleSM.text.toString().trim()
            val descText = binding.descPostSM.text.toString().trim()

            if (titleText.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Please enter title",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                if (AuthHelper.guestMode) {
                    LocalPostDraftStore.addDraft(titleText, descText)
                    Toast.makeText(
                        requireContext(),
                        "Post saved in offline mode.",
                        Toast.LENGTH_SHORT
                    ).show()
                    openPostsList()
                    return@setOnClickListener
                }
                uploadImage()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data == null || data.data == null) {
                return
            }

            filePath = data.data
            binding.uploadImagePreview.setImageURI(filePath)
            try {
                val lastIndex = filePath.toString().length - 1
                val type =
                    filePath.toString().slice((filePath.toString().lastIndexOf(".") + 1)..lastIndex)

                Log.d("File Type", filePath.toString())

                if (filePath.toString().contains("png") || filePath.toString().contains("jpg") || filePath.toString().contains("jpeg") || filePath.toString().contains("image") || filePath.toString().contains("images")){
                    data2["uploadType"] = "image"
                } else if(filePath.toString().contains("videos") || filePath.toString().contains("video") || filePath.toString().contains("mp4")){
                    data2["uploadType"] = "video"
                }

                Log.d("File Type 3", data2["uploadType"].toString())
                bitmap = MediaStore.Images.Media.getBitmap(activity!!.contentResolver, filePath)

//                uploadImage().setImageBitmap(bitmap)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun uploadImage() {
        binding.progressCreatePost.visibility = View.VISIBLE
        binding.progressTitle.visibility = View.VISIBLE
        if (filePath != null) {
            val uri = filePath!!
            val userId = AuthHelper.requireUid()
            lifecycleScope.launch {
                try {
                    val cacheFile = withContext(Dispatchers.IO) { copyUriToCacheFile(uri, "post_upload") }
                    val mime = requireContext().contentResolver.getType(uri) ?: "image/jpeg"
                    val url = withContext(Dispatchers.IO) {
                        SupabaseUploader.uploadFile(
                            file = cacheFile,
                            mimeType = mime,
                            subfolder = "uploads/$userId",
                            originalFileName = cacheFile.name
                        )
                    }
                    cacheFile.delete()
                    addUploadRecordWithImageToDb(url, UUID.randomUUID())
                } catch (e: Exception) {
                    binding.progressCreatePost.visibility = View.GONE
                    binding.progressTitle.visibility = View.GONE
                    LocalPostDraftStore.addDraft(
                        binding.postTitleSM.text.toString().trim(),
                        binding.descPostSM.text.toString().trim()
                    )
                    Toast.makeText(requireContext(), "Upload failed. Saved offline.", Toast.LENGTH_LONG).show()
                    openPostsList()
                }
            }
        } else {
            data2["uploadType"] = ""
            addUploadRecordWithImageToDb(null, null)
        }
    }

    private fun copyUriToCacheFile(uri: Uri, prefix: String): File {
        val mime = requireContext().contentResolver.getType(uri).orEmpty()
        val ext = when {
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            mime.contains("mp3") -> "mp3"
            mime.contains("wav") -> "wav"
            else -> "jpg"
        }
        val out = File(requireContext().cacheDir, "${prefix}_${System.currentTimeMillis()}.$ext")
        requireContext().contentResolver.openInputStream(uri).use { input ->
            if (input == null) throw IOException("Failed to open input stream")
            FileOutputStream(out).use { output -> input.copyTo(output) }
        }
        return out
    }

    private fun addUploadRecordWithTextToDb() {
        addUploadRecordWithImageToDb(null, null)
    }

    private fun addUploadRecordWithImageToDb(uri: String?, postID: UUID?) {

        if (!uri.isNullOrEmpty()) {
            data2["imageUrl"] = uri.toString()
            data2["imageID"] = postID.toString()

        }

        val data3 = HashMap<String, Any>()
        val postTimeStamp = System.currentTimeMillis()

        data2["userID"] = AuthHelper.requireEmail()
        data2["timeStamp"] = postTimeStamp
        data2["title"] = binding.postTitleSM.text.toString()
        data2["description"] = binding.descPostSM.text.toString()


        db.collection("posts")
            .add(data2)
            .addOnSuccessListener { documentReference ->

                val data = HashMap<String, Any>()
                val posts = arrayListOf<String>()
                val postRecordID = documentReference.id.toString()

                posts.add(postRecordID)
                data["posts"] = posts

                db.collection("users")
                    .document(AuthHelper.requireEmail())
                    .update("posts", FieldValue.arrayUnion(postRecordID))
                    .addOnSuccessListener { documentReference ->
                        Toast.makeText(
                            requireContext(),
                            "Post Created",
                            Toast.LENGTH_LONG
                        ).show()

                        binding.progressCreatePost.visibility = View.GONE
                        binding.progressTitle.visibility = View.GONE
                        userDataViewModel.getUserData(AuthHelper.requireEmail())
                        openPostsList()
                    }
                    .addOnFailureListener { e ->
                        LocalPostDraftStore.addDraft(
                            binding.postTitleSM.text.toString().trim(),
                            binding.descPostSM.text.toString().trim()
                        )
                        Toast.makeText(requireContext(), "Post saved offline.", Toast.LENGTH_LONG).show()
                        binding.progressCreatePost.visibility = View.GONE
                        binding.progressTitle.visibility = View.GONE
                        openPostsList()
                    }
            }
            .addOnFailureListener { e ->
                LocalPostDraftStore.addDraft(
                    binding.postTitleSM.text.toString().trim(),
                    binding.descPostSM.text.toString().trim()
                )
                Toast.makeText(requireContext(), "Post saved offline.", Toast.LENGTH_LONG).show()
                binding.progressCreatePost.visibility = View.GONE
                binding.progressTitle.visibility = View.GONE
                openPostsList()
            }
    }

    private fun openPostsList() {
        socialMediaPostsFragment = SocialMediaPostsFragment()
        requireActivity().supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_left,
                R.anim.slide_out_right,
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
            .replace(R.id.frame_layout, socialMediaPostsFragment, "smPostList")
            .setReorderingAllowed(true)
            .addToBackStack("smPostList")
            .commit()
    }
}
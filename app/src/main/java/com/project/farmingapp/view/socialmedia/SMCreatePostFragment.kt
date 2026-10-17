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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.project.farmingapp.R
import com.project.farmingapp.databinding.FragmentSMCreatePostBinding
import com.project.farmingapp.model.SupabaseUploader
import com.project.farmingapp.supabase.FieldAlias
import com.project.farmingapp.supabase.SupabaseDb
import com.project.farmingapp.utilities.AuthHelper
import com.project.farmingapp.utilities.pulseTapFeedback
import com.project.farmingapp.viewmodel.UserDataViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.Executors

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class SMCreatePostFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null
    private val PICK_IMAGE_REQUEST = 71
    private var filePath: Uri? = null
    private var postID: UUID? = null
    private var bitmap: Bitmap? = null
    lateinit var socialMediaPostsFragment: SocialMediaPostsFragment
    lateinit var userDataViewModel: UserDataViewModel
    val data2 = HashMap<String, Any>()
    private var _binding: FragmentSMCreatePostBinding? = null
    private val binding get() = _binding!!
    private val executor = Executors.newSingleThreadExecutor()

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
    ): View {
        _binding = FragmentSMCreatePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
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
            executor.execute {
                try {
                    val email = AuthHelper.requireEmail()
                    val user = SupabaseDb.selectOne("users", SupabaseDb.eq("email", email), idKey = "email")
                    val name = user?.getString("name").orEmpty()
                    if (name.isNotBlank()) {
                        data2["name"] = name
                    }
                    Log.d("Google User", name)
                } catch (_: Exception) {
                }
            }
        } else {
            data2["name"] = displayName
            Log.d("Normal User", displayName)
        }

        binding.createPostBtnSM.setOnClickListener {
            binding.createPostBtnSM.pulseTapFeedback()
            val titleText = binding.postTitleSM.text.toString().trim()
            val descText = binding.descPostSM.text.toString().trim()

            if (titleText.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter title", Toast.LENGTH_SHORT).show()
            } else {
                if (AuthHelper.guestMode) {
                    LocalPostDraftStore.addDraft(titleText, descText)
                    Toast.makeText(requireContext(), "Post saved in offline mode.", Toast.LENGTH_SHORT).show()
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
                val pathStr = filePath.toString()
                if (pathStr.contains("png") || pathStr.contains("jpg") || pathStr.contains("jpeg")
                    || pathStr.contains("image") || pathStr.contains("images")
                ) {
                    data2["uploadType"] = "image"
                } else if (pathStr.contains("videos") || pathStr.contains("video") || pathStr.contains("mp4")) {
                    data2["uploadType"] = "video"
                }

                // NOTE: the picked media is streamed straight from its Uri when uploading.
                // Decoding it into a full-size Bitmap here was never used and could run out of
                // memory on large photos (and fails outright for videos).
                Log.d("File Type 3", data2["uploadType"].toString())
            } catch (e: Exception) {
                Log.w("SMCreatePost", "Could not inspect picked file", e)
            }
        }
    }

    private fun uploadImage() {
        val titleText = _binding?.postTitleSM?.text?.toString()?.trim().orEmpty()
        val descText = _binding?.descPostSM?.text?.toString()?.trim().orEmpty()
        _binding?.progressCreatePost?.visibility = View.VISIBLE
        _binding?.progressTitle?.visibility = View.VISIBLE
        val uri = filePath
        if (uri != null) {
            val appContext = requireContext().applicationContext
            val userId = AuthHelper.runCatching { requireUid() }.getOrNull()
            if (userId == null) {
                // Session expired: keep the user's text instead of crashing.
                LocalPostDraftStore.addDraft(titleText, descText)
                Toast.makeText(appContext, "Please log in again. Post saved offline.", Toast.LENGTH_LONG).show()
                _binding?.progressCreatePost?.visibility = View.GONE
                _binding?.progressTitle?.visibility = View.GONE
                openPostsList()
                return
            }
            lifecycleScope.launch {
                try {
                    val cacheFile = withContext(Dispatchers.IO) {
                        copyUriToCacheFile(appContext, uri, "post_upload")
                    }
                    val mime = appContext.contentResolver.getType(uri) ?: "image/jpeg"
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
                    // The view may already be gone (user navigated away mid-upload).
                    Log.w("SMCreatePost", "Upload failed", e)
                    LocalPostDraftStore.addDraft(titleText, descText)
                    Toast.makeText(appContext, "Upload failed. Saved offline.", Toast.LENGTH_LONG).show()
                    if (isAdded) {
                        _binding?.progressCreatePost?.visibility = View.GONE
                        _binding?.progressTitle?.visibility = View.GONE
                        openPostsList()
                    }
                }
            }
        } else {
            data2["uploadType"] = ""
            addUploadRecordWithImageToDb(null, null)
        }
    }

    private fun copyUriToCacheFile(context: android.content.Context, uri: Uri, prefix: String): File {
        val mime = context.contentResolver.getType(uri).orEmpty()
        val ext = when {
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            mime.contains("mp3") -> "mp3"
            mime.contains("wav") -> "wav"
            else -> "jpg"
        }
        val out = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.$ext")
        context.contentResolver.openInputStream(uri).use { input ->
            if (input == null) throw IOException("Failed to open input stream")
            FileOutputStream(out).use { output -> input.copyTo(output) }
        }
        return out
    }

    private fun addUploadRecordWithImageToDb(uri: String?, postID: UUID?) {
        if (!uri.isNullOrEmpty()) {
            data2["imageUrl"] = uri
            data2["imageID"] = postID.toString()
        }

        val appContext = context?.applicationContext
        val email = AuthHelper.runCatching { requireEmail() }.getOrNull()
        val titleText = _binding?.postTitleSM?.text?.toString().orEmpty()
        val descText = _binding?.descPostSM?.text?.toString().orEmpty()
        if (email == null) {
            LocalPostDraftStore.addDraft(titleText.trim(), descText.trim())
            appContext?.let { Toast.makeText(it, "Please log in again. Post saved offline.", Toast.LENGTH_LONG).show() }
            _binding?.progressCreatePost?.visibility = View.GONE
            _binding?.progressTitle?.visibility = View.GONE
            if (isAdded) openPostsList()
            return
        }

        data2["userID"] = email
        data2["timeStamp"] = System.currentTimeMillis()
        data2["title"] = titleText
        data2["description"] = descText

        val payload = HashMap<String, Any>(data2)
        executor.execute {
            try {
                val snake = FieldAlias.toSnakeRow(payload.mapValues { it.value as Any? })
                val row = JSONObject()
                snake.forEach { (k, v) ->
                    when (v) {
                        null -> row.put(k, JSONObject.NULL)
                        is List<*> -> row.put(k, JSONArray(v))
                        else -> row.put(k, v)
                    }
                }
                val inserted = SupabaseDb.insert("posts", row)
                val postRecordID = inserted?.id.orEmpty()
                if (postRecordID.isBlank()) throw IllegalStateException("Missing post id")

                val user = SupabaseDb.selectOne("users", SupabaseDb.eq("email", email), idKey = "email")
                val posts = (user?.getList<String>("posts") ?: emptyList()).toMutableList()
                posts.add(postRecordID)
                SupabaseDb.update(
                    "users",
                    SupabaseDb.eq("email", email),
                    JSONObject().put("posts", JSONArray(posts))
                )

                activity?.runOnUiThread {
                    appContext?.let { Toast.makeText(it, "Post Created", Toast.LENGTH_LONG).show() }
                    if (!isAdded || _binding == null) return@runOnUiThread
                    _binding?.progressCreatePost?.visibility = View.GONE
                    _binding?.progressTitle?.visibility = View.GONE
                    userDataViewModel.getUserData(email)
                    openPostsList()
                }
            } catch (e: Exception) {
                Log.w("SMCreatePost", "Saving post failed", e)
                LocalPostDraftStore.addDraft(titleText.trim(), descText.trim())
                activity?.runOnUiThread {
                    appContext?.let { Toast.makeText(it, "Post saved offline.", Toast.LENGTH_LONG).show() }
                    if (!isAdded || _binding == null) return@runOnUiThread
                    _binding?.progressCreatePost?.visibility = View.GONE
                    _binding?.progressTitle?.visibility = View.GONE
                    openPostsList()
                }
            }
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

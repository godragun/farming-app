package com.project.farmingapp.view.ai

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.project.farmingapp.BuildConfig
import com.project.farmingapp.R
import com.project.farmingapp.utilities.LogUtil
import com.project.farmingapp.utilities.pulseTapFeedback
import com.project.farmingapp.utilities.toast
import com.project.farmingapp.viewmodel.LeafScannerViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Leaf Scanner — sends a camera photo directly to the Roboflow
 * "leaves-and-deceased-leaves" ViT classification workflow and displays the
 * result (leaf condition, confidence, and ranked predictions).
 *
 * The Roboflow API key is read from [BuildConfig.ROBOFLOW_API_KEY], which is
 * injected via `local.properties` → `build.gradle`.  See README for setup.
 */
class LeafScannerFragment : Fragment() {

    private lateinit var viewModel: LeafScannerViewModel

    // Views
    private lateinit var preview: ImageView
    private lateinit var statusText: TextView
    private lateinit var progress: ProgressBar
    private lateinit var btnCamera: MaterialButton
    private lateinit var btnGallery: MaterialButton
    private lateinit var btnAnalyze: MaterialButton
    private lateinit var resultCard: MaterialCardView
    private lateinit var resultLabel: TextView
    private lateinit var resultConfidence: TextView
    private lateinit var resultPredictionsList: LinearLayout

    private var currentPhotoPath: String = ""
    private var photoUri: Uri? = null

    // -----------------------------------------------------------------------
    // Activity-result launchers
    // -----------------------------------------------------------------------

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && currentPhotoPath.isNotEmpty()) {
                LogUtil.cameraDebug("LeafScanner photo OK: $currentPhotoPath")
                showPreview(currentPhotoPath)
                btnAnalyze.isEnabled = true
                statusText.text = getString(R.string.leaf_scanner_ready)
            } else {
                LogUtil.cameraError("LeafScanner photo failed / cancelled")
                requireContext().toast(getString(R.string.leaf_scanner_photo_failed))
            }
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                // Copy from content URI to a temp file so we can encode it
                val tempFile = File(requireContext().cacheDir, "leaf_gallery_input.jpg")
                try {
                    requireContext().contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    currentPhotoPath = tempFile.absolutePath
                    showPreview(currentPhotoPath)
                    btnAnalyze.isEnabled = true
                    statusText.text = getString(R.string.leaf_scanner_ready)
                } catch (e: Exception) {
                    requireContext().toast(getString(R.string.leaf_scanner_photo_failed))
                }
            }
        }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchCamera()
            else requireContext().toast(getString(R.string.leaf_scanner_camera_denied))
        }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_leaf_scanner, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.supportActionBar?.title =
            getString(R.string.leaf_scanner_title)

        viewModel = ViewModelProvider(this)[LeafScannerViewModel::class.java]

        preview               = view.findViewById(R.id.leafScannerPreview)
        statusText            = view.findViewById(R.id.leafScannerStatus)
        progress              = view.findViewById(R.id.leafScannerProgress)
        btnCamera             = view.findViewById(R.id.btnLeafScannerCamera)
        btnGallery            = view.findViewById(R.id.btnLeafScannerGallery)
        btnAnalyze            = view.findViewById(R.id.btnLeafScannerAnalyze)
        resultCard            = view.findViewById(R.id.leafScannerResultCard)
        resultLabel           = view.findViewById(R.id.leafScannerResultLabel)
        resultConfidence      = view.findViewById(R.id.leafScannerResultConfidence)
        resultPredictionsList = view.findViewById(R.id.leafScannerPredictionsList)

        btnCamera.setOnClickListener {
            btnCamera.pulseTapFeedback()
            ensureCameraThenLaunch()
        }
        btnGallery.setOnClickListener {
            btnGallery.pulseTapFeedback()
            pickImageLauncher.launch("image/*")
        }
        btnAnalyze.setOnClickListener {
            btnAnalyze.pulseTapFeedback()
            analyzePhoto()
        }

        // Observe ViewModel
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            progress.visibility = if (loading == true) View.VISIBLE else View.GONE
            btnAnalyze.isEnabled = loading != true && currentPhotoPath.isNotEmpty()
            if (loading == true) statusText.text = getString(R.string.leaf_scanner_analyzing)
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (!err.isNullOrBlank()) {
                statusText.text = err
                requireContext().toast(err)
            }
        }

        viewModel.result.observe(viewLifecycleOwner) { res ->
            if (res == null) return@observe
            resultCard.visibility = View.VISIBLE

            val confPct = (res.confidence * 100).toInt()
            resultLabel.text = if (res.diagnosisAvailable) {
                getString(R.string.leaf_scanner_result_label, res.top, confPct)
            } else {
                getString(R.string.leaf_scanner_low_confidence, confPct)
            }
            resultConfidence.text = getString(R.string.leaf_scanner_confidence, confPct)

            // Populate ranked predictions list (top 5 max)
            resultPredictionsList.removeAllViews()
            res.allPredictions.take(5).forEach { pred ->
                val tv = TextView(requireContext()).apply {
                    val pct = (pred.confidence * 100).toInt()
                    text = "• ${pred.`class`} — $pct%"
                    textSize = 13f
                    setPadding(0, 4, 0, 4)
                }
                resultPredictionsList.addView(tv)
            }

            statusText.text = getString(R.string.leaf_scanner_done)
        }
    }

    // -----------------------------------------------------------------------
    // Camera helpers
    // -----------------------------------------------------------------------

    private fun ensureCameraThenLaunch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            launchCamera()
        }
    }

    private fun launchCamera() {
        try {
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val photoFile = File.createTempFile("LEAF_${ts}_", ".jpg", dir)
            currentPhotoPath = photoFile.absolutePath
            photoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )
            takePictureLauncher.launch(photoUri)
        } catch (e: Exception) {
            LogUtil.cameraError("LeafScanner camera launch failed", e)
            requireContext().toast(e.message ?: getString(R.string.leaf_scanner_photo_failed))
        }
    }

    private fun showPreview(path: String) {
        Glide.with(this).load(File(path)).centerCrop().into(preview)
        resultCard.visibility = View.GONE
        viewModel.clearResult()
    }

    private fun analyzePhoto() {
        if (currentPhotoPath.isEmpty()) {
            requireContext().toast(getString(R.string.leaf_scanner_need_photo))
            return
        }

        // Load API key: try BuildConfig first, fall back to a no-op that shows
        // a clear error message via the ViewModel.
        val apiKey = try {
            BuildConfig.ROBOFLOW_API_KEY
        } catch (_: Exception) {
            ""
        }

        viewModel.analyze(File(currentPhotoPath), apiKey)
    }
}

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
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.project.farmingapp.R
import com.project.farmingapp.utilities.ImageCompressor
import com.project.farmingapp.utilities.LogUtil
import com.project.farmingapp.utilities.pulseTapFeedback
import com.project.farmingapp.utilities.toast
import com.project.farmingapp.viewmodel.CropDoctorViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * AI Crop Doctor — camera photo → FastAPI /predict-disease (Roboflow).
 */
class CropDoctorFragment : Fragment() {

    private lateinit var viewModel: CropDoctorViewModel
    private lateinit var preview: ImageView
    private lateinit var hintText: TextView
    private lateinit var statusText: TextView
    private lateinit var progress: ProgressBar
    private lateinit var btnCamera: MaterialButton
    private lateinit var btnAnalyze: MaterialButton
    private lateinit var resultCard: MaterialCardView
    private lateinit var resultMeta: TextView
    private lateinit var recommendationSi: TextView
    private lateinit var preventionSi: TextView
    private lateinit var recommendationEn: TextView

    private var currentPhotoPath: String = ""
    private var compressedPath: String = ""
    private var photoUri: Uri? = null
    private var cropHint: String? = null
    private var autoOpenCamera: Boolean = false

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && currentPhotoPath.isNotEmpty()) {
                LogUtil.cameraDebug("Crop Doctor photo OK: $currentPhotoPath")
                showPreview(currentPhotoPath)
                btnAnalyze.isEnabled = true
                statusText.text = getString(R.string.crop_doctor_ready)
            } else {
                LogUtil.cameraError("Crop Doctor photo failed or cancelled")
                requireContext().toast(getString(R.string.crop_doctor_photo_failed))
            }
        }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchCamera()
            else requireContext().toast(getString(R.string.crop_doctor_camera_denied))
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cropHint = arguments?.getString(ARG_CROP_HINT)
        autoOpenCamera = arguments?.getBoolean(ARG_AUTO_CAMERA, false) == true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_crop_doctor, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.supportActionBar?.title =
            getString(R.string.nav_crop_doctor)

        viewModel = ViewModelProvider(this)[CropDoctorViewModel::class.java]
        preview = view.findViewById(R.id.cropDoctorPreview)
        hintText = view.findViewById(R.id.cropDoctorHintText)
        statusText = view.findViewById(R.id.cropDoctorStatus)
        progress = view.findViewById(R.id.cropDoctorProgress)
        btnCamera = view.findViewById(R.id.btnCropDoctorCamera)
        btnAnalyze = view.findViewById(R.id.btnCropDoctorAnalyze)
        resultCard = view.findViewById(R.id.cropDoctorResultCard)
        resultMeta = view.findViewById(R.id.cropDoctorResultMeta)
        recommendationSi = view.findViewById(R.id.cropDoctorRecommendationSi)
        preventionSi = view.findViewById(R.id.cropDoctorPreventionSi)
        recommendationEn = view.findViewById(R.id.cropDoctorRecommendationEn)

        if (!cropHint.isNullOrBlank()) {
            hintText.visibility = View.VISIBLE
            hintText.text = getString(R.string.crop_doctor_crop_hint, cropHint)
        }

        btnCamera.setOnClickListener {
            btnCamera.pulseTapFeedback()
            ensureCameraThenLaunch()
        }
        btnAnalyze.setOnClickListener {
            btnAnalyze.pulseTapFeedback()
            analyzePhoto()
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            progress.visibility = if (loading == true) View.VISIBLE else View.GONE
            btnAnalyze.isEnabled = loading != true && currentPhotoPath.isNotEmpty()
            if (loading == true) {
                statusText.text = getString(R.string.crop_doctor_analyzing)
            }
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
            val confPct = ((res.confidence ?: 0.0) * 100).toInt()
            resultMeta.text = getString(
                R.string.crop_doctor_meta,
                res.crop ?: "-",
                res.disease ?: "-",
                confPct,
                res.severity ?: "-"
            )
            recommendationSi.text = res.recommendation_sinhala
                ?: res.recommendation
                ?: ""
            preventionSi.text = listOfNotNull(
                res.prevention_sinhala?.let { getString(R.string.crop_doctor_prevention_si, it) },
                null
            ).firstOrNull().orEmpty().ifBlank {
                res.prevention?.let { getString(R.string.crop_doctor_prevention_en, it) }.orEmpty()
            }
            recommendationEn.text = buildString {
                append(res.recommendation.orEmpty())
                if (!res.prevention.isNullOrBlank()) {
                    if (isNotEmpty()) append("\n\n")
                    append(getString(R.string.crop_doctor_prevention_en, res.prevention))
                }
                if (!res.engine.isNullOrBlank()) {
                    if (isNotEmpty()) append("\n")
                    append("Engine: ${res.engine}")
                }
            }
            statusText.text = getString(R.string.crop_doctor_done)
        }

        if (autoOpenCamera && savedInstanceState == null) {
            view.post { ensureCameraThenLaunch() }
        }
    }

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
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val photoFile = File.createTempFile("CROPDOC_${timeStamp}_", ".jpg", storageDir)
            currentPhotoPath = photoFile.absolutePath
            compressedPath = ""
            photoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )
            LogUtil.cameraDebug("Crop Doctor camera: $photoUri")
            takePictureLauncher.launch(photoUri)
        } catch (e: Exception) {
            LogUtil.cameraError("Crop Doctor camera launch failed", e)
            requireContext().toast(e.message ?: "Camera failed")
        }
    }

    private fun showPreview(path: String) {
        Glide.with(this).load(File(path)).centerCrop().into(preview)
        resultCard.visibility = View.GONE
        viewModel.clearResult()
    }

    private fun analyzePhoto() {
        if (currentPhotoPath.isEmpty()) {
            requireContext().toast(getString(R.string.crop_doctor_need_photo))
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val source = File(currentPhotoPath)
            val out = File(requireContext().cacheDir, "crop_doctor_upload.jpg")
            val ok = ImageCompressor.compressImage(
                inputPath = source.absolutePath,
                outputPath = out.absolutePath,
                maxWidth = 1024,
                maxHeight = 1024,
                quality = 85
            )
            compressedPath = if (ok && out.exists()) out.absolutePath else currentPhotoPath
            viewModel.analyze(File(compressedPath), cropHint)
        }
    }

    companion object {
        private const val ARG_CROP_HINT = "crop_hint"
        private const val ARG_AUTO_CAMERA = "auto_camera"

        fun newInstance(cropHint: String? = null, autoOpenCamera: Boolean = false): CropDoctorFragment {
            return CropDoctorFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CROP_HINT, cropHint)
                    putBoolean(ARG_AUTO_CAMERA, autoOpenCamera)
                }
            }
        }
    }
}

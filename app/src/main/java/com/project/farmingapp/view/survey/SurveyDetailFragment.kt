package com.project.farmingapp.view.survey

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.project.farmingapp.R
import com.project.farmingapp.databinding.FragmentSurveyDetailBinding
import com.project.farmingapp.model.FieldSurveyRepository
import com.project.farmingapp.model.data.FieldSurvey
import com.project.farmingapp.viewmodel.FieldSurveyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SurveyDetailFragment : Fragment() {

    private var _binding: FragmentSurveyDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: FieldSurveyViewModel
    private var mediaPlayer: MediaPlayer? = null
    private var surveyId: Int = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSurveyDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[FieldSurveyViewModel::class.java]
        (activity as AppCompatActivity).supportActionBar?.title = "Survey Details"

        surveyId = arguments?.getInt("survey_id", -1) ?: -1
        if (surveyId == -1) {
            Toast.makeText(requireContext(), "Survey not found", Toast.LENGTH_SHORT).show()
            return
        }

        loadSurvey()

        binding.detailSyncNowBtn.setOnClickListener { viewModel.syncAll() }

        viewModel.consumeSyncResult()
        viewModel.syncResult.observe(viewLifecycleOwner) { event ->
            if (event == null) return@observe
            if (event != FieldSurveyViewModel.SyncEvent.Syncing) viewModel.consumeSyncResult()
            val msg = when (event) {
                FieldSurveyViewModel.SyncEvent.Syncing -> "Uploading…"
                is FieldSurveyViewModel.SyncEvent.Success -> "✔ Synced!"
                FieldSurveyViewModel.SyncEvent.NoInternet -> "⚠ No internet. Saved locally."
                FieldSurveyViewModel.SyncEvent.NothingToSync -> "✔ Already synced"
                is FieldSurveyViewModel.SyncEvent.Error -> "✗ ${event.message}"
            }
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            if (event is FieldSurveyViewModel.SyncEvent.Success) loadSurvey()
        }
    }

    private fun loadSurvey() {
        val repo = FieldSurveyRepository(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            val survey = try {
                withContext(Dispatchers.IO) { repo.getSurveyById(surveyId) }
            } catch (e: Exception) { null } ?: return@launch
            if (_binding != null) bindSurvey(survey) // back on Main
        }
    }

    private fun bindSurvey(survey: FieldSurvey) {
        val index = surveyId
        binding.detailSurveyTitle.text = "Survey #%03d".format(index)
        binding.detailSurveyDate.text = "Saved ${formatDate(survey.savedAt)}"
        binding.detailSyncBadge.text = if (survey.isSynced) "✔ Synced" else "⏳ Pending"

        binding.detailFarmerName.text = survey.farmerName.ifEmpty { "—" }
        binding.detailLocation.text = survey.locationText.ifEmpty { "—" }
        binding.detailCropType.text = survey.cropType.ifEmpty { "—" }
        binding.detailSoilCondition.text = survey.soilCondition.ifEmpty { "—" }
        binding.detailIrrigation.text = survey.irrigationType.ifEmpty { "—" }
        binding.detailYield.text = if (survey.estimatedYieldKg.isEmpty()) "—" else "${survey.estimatedYieldKg} kg"
        binding.detailPestNotes.text = survey.pestNotes.ifEmpty { "No notes recorded" }

        val localImage = survey.imagePath.takeIf { it.isNotEmpty() && File(it).exists() }
        val remoteImage = survey.imageUrl.takeIf { it.isNotEmpty() }
        when {
            localImage != null -> {
                binding.detailPhotoCard.visibility = View.VISIBLE
                Glide.with(this).load(File(localImage)).into(binding.detailPhotoPreview)
            }
            remoteImage != null -> {
                binding.detailPhotoCard.visibility = View.VISIBLE
                Glide.with(this).load(remoteImage).into(binding.detailPhotoPreview)
            }
        }

        val localAudio = survey.audioPath.takeIf { it.isNotEmpty() && File(it).exists() }
        val remoteAudio = survey.audioUrl.takeIf { it.isNotEmpty() }
        val audioSource = localAudio ?: remoteAudio
        if (audioSource != null) {
            binding.detailAudioCard.visibility = View.VISIBLE
            binding.detailPlayAudioBtn.setOnClickListener { playAudio(audioSource) }
        }

        binding.detailSyncNowBtn.isEnabled = !survey.isSynced
        binding.detailSyncNowBtn.alpha = if (survey.isSynced) 0.5f else 1.0f
    }

    private fun playAudio(path: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                setOnPreparedListener { it.start() }
                setOnCompletionListener {
                    _binding?.detailPlayAudioBtn?.text = "▶  Play Voice Note"
                    it.release()
                    mediaPlayer = null
                }
                setOnErrorListener { mp, _, _ ->
                    _binding?.detailPlayAudioBtn?.text = "▶  Play Voice Note"
                    mp.release()
                    mediaPlayer = null
                    true
                }
                prepareAsync()
            }
            binding.detailPlayAudioBtn.text = "⏸  Playing…"
        } catch (e: Exception) {
            mediaPlayer?.release()
            mediaPlayer = null
            context?.let { Toast.makeText(it, "Could not play the voice note", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun formatDate(millis: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - millis
        return when {
            diff < 86_400_000 -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))
            diff < 172_800_000 -> "yesterday"
            else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer?.release()
        _binding = null
    }
}

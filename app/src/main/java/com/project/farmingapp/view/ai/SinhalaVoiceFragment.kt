package com.project.farmingapp.view.ai

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.project.farmingapp.R
import com.project.farmingapp.utilities.pulseTapFeedback
import com.project.farmingapp.utilities.toast
import com.project.farmingapp.view.dashboard.DashboardActivity
import com.project.farmingapp.viewmodel.VoiceAiViewModel
import java.io.File

/**
 * Sinhala Voice Agricultural AI screen.
 * Talks to local FastAPI (see AgriAiConfig.BASE_URL).
 * When needs_photo=true, offers handoff to Crop Doctor (camera → Roboflow).
 */
class SinhalaVoiceFragment : Fragment() {

    private lateinit var viewModel: VoiceAiViewModel
    private lateinit var inputEdit: TextInputEditText
    private lateinit var btnAsk: MaterialButton
    private lateinit var btnRecord: MaterialButton
    private lateinit var btnPlayTts: MaterialButton
    private lateinit var btnOpenCropDoctor: MaterialButton
    private lateinit var statusText: TextView
    private lateinit var resultSi: TextView
    private lateinit var resultEn: TextView
    private lateinit var resultMeta: TextView
    private lateinit var progress: ProgressBar

    private var mediaRecorder: MediaRecorder? = null
    private var recordingPath: String = ""
    private var isRecording = false
    private var mediaPlayer: MediaPlayer? = null
    private var lastTtsBase64: String? = null
    private var lastCropHint: String? = null

    private val audioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) toggleRecording()
            else requireContext().toast(getString(R.string.sinhala_voice_mic_denied))
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_sinhala_voice, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.supportActionBar?.title =
            getString(R.string.nav_sinhala_voice)

        viewModel = ViewModelProvider(this)[VoiceAiViewModel::class.java]
        inputEdit = view.findViewById(R.id.voiceInputEdit)
        btnAsk = view.findViewById(R.id.btnAskText)
        btnRecord = view.findViewById(R.id.btnRecord)
        btnPlayTts = view.findViewById(R.id.btnPlayTts)
        btnOpenCropDoctor = view.findViewById(R.id.btnOpenCropDoctor)
        statusText = view.findViewById(R.id.voiceStatusText)
        resultSi = view.findViewById(R.id.voiceResultSinhala)
        resultEn = view.findViewById(R.id.voiceResultEnglish)
        resultMeta = view.findViewById(R.id.voiceResultMeta)
        progress = view.findViewById(R.id.voiceProgress)

        btnAsk.setOnClickListener {
            btnAsk.pulseTapFeedback()
            viewModel.askText(inputEdit.text?.toString().orEmpty())
        }
        btnRecord.setOnClickListener {
            btnRecord.pulseTapFeedback()
            ensureMicThenRecord()
        }
        btnPlayTts.setOnClickListener {
            btnPlayTts.pulseTapFeedback()
            playTts()
        }
        btnOpenCropDoctor.setOnClickListener {
            btnOpenCropDoctor.pulseTapFeedback()
            (activity as? DashboardActivity)?.openCropDoctor(
                cropHint = lastCropHint,
                autoOpenCamera = true
            )
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            progress.visibility = if (loading == true) View.VISIBLE else View.GONE
            statusText.text = if (loading == true) getString(R.string.sinhala_voice_thinking) else statusText.text
        }
        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (!err.isNullOrBlank()) {
                statusText.text = err
                requireContext().toast(err)
            }
        }
        viewModel.transcribedText.observe(viewLifecycleOwner) { text ->
            if (!text.isNullOrBlank()) {
                inputEdit.setText(text)
            }
        }
        viewModel.result.observe(viewLifecycleOwner) { res ->
            if (res == null) return@observe
            resultSi.text = res.response_sinhala.orEmpty()
            resultEn.text = res.response_english.orEmpty()
            val crop = res.crop_sinhala ?: res.crop ?: "-"
            val symptom = res.symptom_sinhala ?: res.symptom ?: "-"
            resultMeta.text = getString(
                R.string.sinhala_voice_meta,
                res.intent ?: "-",
                crop,
                symptom,
                if (res.needs_photo == true) getString(R.string.yes) else getString(R.string.no)
            )
            statusText.text = getString(R.string.sinhala_voice_done)
            lastTtsBase64 = res.tts_audio_base64
            btnPlayTts.visibility =
                if (!lastTtsBase64.isNullOrBlank()) View.VISIBLE else View.GONE

            lastCropHint = res.crop ?: res.crop_sinhala
            val needsPhoto = res.needs_photo == true ||
                res.next_action == "request_leaf_photo"
            btnOpenCropDoctor.visibility = if (needsPhoto) View.VISIBLE else View.GONE
            if (needsPhoto) {
                statusText.text = getString(R.string.sinhala_voice_needs_photo)
            }
        }
    }

    private fun ensureMicThenRecord() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            toggleRecording()
        }
    }

    private fun toggleRecording() {
        if (isRecording) {
            stopRecordingAndSend()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        try {
            val file = File(requireContext().cacheDir, "sinhala_voice_${System.currentTimeMillis()}.m4a")
            recordingPath = file.absolutePath
            mediaRecorder = if (android.os.Build.VERSION.SDK_INT >= 31) {
                MediaRecorder(requireContext())
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(recordingPath)
                prepare()
                start()
            }
            isRecording = true
            btnRecord.text = getString(R.string.sinhala_voice_stop)
            statusText.text = getString(R.string.sinhala_voice_listening)
        } catch (e: Exception) {
            statusText.text = e.message
            requireContext().toast(e.message ?: "Record failed")
        }
    }

    private fun stopRecordingAndSend() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {
        }
        mediaRecorder = null
        isRecording = false
        btnRecord.text = getString(R.string.sinhala_voice_record)
        statusText.text = getString(R.string.sinhala_voice_thinking)
        val file = File(recordingPath)
        val typed = inputEdit.text?.toString()
        viewModel.askAudio(file, typed)
    }

    private fun playTts() {
        val b64 = lastTtsBase64 ?: return
        try {
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            val out = File(requireContext().cacheDir, "tts_reply.mp3")
            out.writeBytes(bytes)
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(out.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            requireContext().toast(e.message ?: "TTS play failed")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {
        }
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

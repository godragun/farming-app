package com.project.farmingapp.fieldguard.ui

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.ToneGenerator
import android.media.AudioManager
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.project.farmingapp.R
import com.project.farmingapp.databinding.FragmentFieldguardIncidentBinding
import com.project.farmingapp.fieldguard.agent.AgentTraceStep
import com.project.farmingapp.fieldguard.agent.FieldGuardNotifier
import com.project.farmingapp.fieldguard.agent.NodeIndicatorState
import com.project.farmingapp.fieldguard.scenario.DemoScenarios
import java.io.ByteArrayOutputStream

class FieldGuardIncidentFragment : Fragment() {
    private var _binding: FragmentFieldguardIncidentBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: FieldGuardViewModel
    private var scenarioId: String = ""
    private var started = false

    private val takePicturePreview = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap == null) {
            Toast.makeText(requireContext(), R.string.fieldguard_camera_failed, Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        val stream = ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, stream)
        val b64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        val dataUrl = "data:image/jpeg;base64,$b64"
        binding.incidentFrame.setImageBitmap(bitmap)
        viewModel.runCamera(dataUrl)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scenarioId = arguments?.getString(ARG_SCENARIO) ?: ""
        viewModel = ViewModelProvider(requireActivity())[FieldGuardViewModel::class.java]
        FieldGuardNotifier.ensureChannels(requireContext().applicationContext)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFieldguardIncidentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val title = if (scenarioId == DemoScenarios.CAMERA) {
            getString(R.string.fieldguard_use_camera)
        } else {
            val sc = DemoScenarios.byId(scenarioId)
            if (sc != null) {
                val id = resources.getIdentifier(sc.titleRes, "string", requireContext().packageName)
                if (id != 0) getString(id) else sc.id
            } else scenarioId
        }
        binding.incidentTitle.text = title

        viewModel.traces.observe(viewLifecycleOwner) { steps ->
            renderTraces(steps)
        }
        viewModel.indicator.observe(viewLifecycleOwner) { state ->
            applyIndicator(state)
        }
        viewModel.frameDataUrl.observe(viewLifecycleOwner) { dataUrl ->
            showDataUrl(dataUrl)
        }
        viewModel.running.observe(viewLifecycleOwner) { running ->
            binding.incidentProgress.visibility = if (running == true) View.VISIBLE else View.GONE
        }
        viewModel.lastResult.observe(viewLifecycleOwner) { result ->
            if (result == null) return@observe
            binding.decisionText.text = when {
                !result.success -> getString(R.string.fieldguard_error_fmt, result.error ?: "")
                else -> result.decisionSummary.ifBlank { result.status }
            }
            binding.incidentStatus.text = getString(R.string.fieldguard_status_fmt, result.status)
        }
        viewModel.statusMessage.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrBlank() && viewModel.lastResult.value == null) {
                binding.incidentStatus.text = msg
            }
        }

        if (!started) {
            started = true
            if (scenarioId == DemoScenarios.CAMERA) {
                takePicturePreview.launch(null)
            } else {
                viewModel.runScenario(scenarioId)
            }
        }
    }

    private fun renderTraces(steps: List<AgentTraceStep>) {
        binding.traceList.removeAllViews()
        steps.forEach { step ->
            val row = layoutInflater.inflate(R.layout.item_fieldguard_trace, binding.traceList, false)
            row.findViewById<TextView>(R.id.traceHeader).text =
                "Step ${step.step} · ${step.kind} · ${step.modelName} · ${step.elapsedMs}ms"
            val body = buildString {
                if (step.toolName.isNotBlank()) append("tool: ${step.toolName}\n")
                if (step.arguments.isNotBlank()) append("args: ${step.arguments}\n")
                if (step.result.isNotBlank()) append("result: ${step.result}\n")
                if (step.detail.isNotBlank()) append(step.detail)
            }
            row.findViewById<TextView>(R.id.traceBody).text = body.trim()
            binding.traceList.addView(row)
        }
    }

    private fun applyIndicator(state: NodeIndicatorState) {
        binding.nodePattern.text = getString(R.string.fieldguard_node_pattern_fmt, state.pattern)
        binding.nodeBuzzer.text = if (state.buzzerOn) {
            getString(R.string.fieldguard_buzzer_on)
        } else {
            getString(R.string.fieldguard_buzzer_off)
        }
        val drawable = binding.nodeLed.background.mutate()
        if (drawable is GradientDrawable) {
            drawable.setColor(Color.parseColor(state.ledColor))
        } else {
            binding.nodeLed.setBackgroundColor(Color.parseColor(state.ledColor))
        }
        if (state.buzzerOn) {
            try {
                ToneGenerator(AudioManager.STREAM_ALARM, 60)
                    .startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
            } catch (_: Exception) {
            }
        }
    }

    private fun showDataUrl(dataUrl: String?) {
        if (dataUrl.isNullOrBlank()) return
        try {
            val b64 = dataUrl.substringAfter("base64,", missingDelimiterValue = "")
            if (b64.isBlank()) return
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return
            binding.incidentFrame.setImageBitmap(bitmap)
        } catch (_: Exception) {
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_SCENARIO = "scenario_id"

        fun newInstance(scenarioId: String): FieldGuardIncidentFragment {
            return FieldGuardIncidentFragment().apply {
                arguments = Bundle().apply { putString(ARG_SCENARIO, scenarioId) }
            }
        }
    }
}

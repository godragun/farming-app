package com.project.farmingapp.fieldguard.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.project.farmingapp.R
import com.project.farmingapp.databinding.FragmentFieldguardSettingsBinding

class FieldGuardSettingsFragment : Fragment() {
    private var _binding: FragmentFieldguardSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: FieldGuardViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[FieldGuardViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFieldguardSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.settingsModel.text = viewModel.modelName()
        binding.settingsBaseUrl.text = viewModel.baseUrl()
        binding.testConnectionBtn.setOnClickListener {
            binding.testResult.text = getString(R.string.fieldguard_testing)
            viewModel.testConnection()
        }
        viewModel.connectionTest.observe(viewLifecycleOwner) { result ->
            if (result == null) return@observe
            binding.testResult.text = if (result.ok) {
                getString(
                    R.string.fieldguard_test_ok,
                    result.latencyMs,
                    result.model,
                    result.reply
                )
            } else {
                getString(R.string.fieldguard_test_fail, result.error ?: "unknown", result.latencyMs)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

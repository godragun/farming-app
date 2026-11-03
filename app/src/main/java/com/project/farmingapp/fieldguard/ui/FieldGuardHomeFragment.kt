package com.project.farmingapp.fieldguard.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.project.farmingapp.R
import com.project.farmingapp.databinding.FragmentFieldguardHomeBinding
import com.project.farmingapp.fieldguard.scenario.DemoScenarios
import com.project.farmingapp.utilities.pulseTapFeedback

class FieldGuardHomeFragment : Fragment() {
    private var _binding: FragmentFieldguardHomeBinding? = null
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
        _binding = FragmentFieldguardHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.fieldguardHistoryBtn.setOnClickListener {
            it.pulseTapFeedback()
            open(FieldGuardHistoryFragment(), "fieldGuardHistory")
        }
        binding.fieldguardSettingsBtn.setOnClickListener {
            it.pulseTapFeedback()
            open(FieldGuardSettingsFragment(), "fieldGuardSettings")
        }
        binding.fieldguardCameraBtn.setOnClickListener {
            it.pulseTapFeedback()
            open(FieldGuardIncidentFragment.newInstance(DemoScenarios.CAMERA), "fieldGuardIncident")
        }

        binding.fieldguardScenarioList.removeAllViews()
        DemoScenarios.ALL.forEach { scenario ->
            val card = layoutInflater.inflate(R.layout.item_fieldguard_scenario, binding.fieldguardScenarioList, false)
            card.findViewById<TextView>(R.id.scenarioTitle).text =
                stringByName(scenario.titleRes)
            card.findViewById<TextView>(R.id.scenarioDesc).text =
                stringByName(scenario.descriptionRes)
            card.findViewById<TextView>(R.id.scenarioExpected).text =
                getString(R.string.fieldguard_expected_fmt, scenario.expectedOutcome)
            card.setOnClickListener {
                it.pulseTapFeedback()
                open(FieldGuardIncidentFragment.newInstance(scenario.id), "fieldGuardIncident")
            }
            binding.fieldguardScenarioList.addView(card)
        }
    }

    private fun stringByName(name: String): String {
        val id = resources.getIdentifier(name, "string", requireContext().packageName)
        return if (id != 0) getString(id) else name
    }

    private fun open(fragment: Fragment, tag: String) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right, R.anim.slide_out_left,
                R.anim.slide_in_left, R.anim.slide_out_right
            )
            .replace(R.id.frame_layout, fragment, tag)
            .addToBackStack(tag)
            .setReorderingAllowed(true)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = FieldGuardHomeFragment()
    }
}

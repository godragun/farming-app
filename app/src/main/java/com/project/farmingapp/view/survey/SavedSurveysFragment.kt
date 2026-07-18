package com.project.farmingapp.view.survey

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.project.farmingapp.R
import com.project.farmingapp.adapter.SurveyAdapter
import com.project.farmingapp.databinding.FragmentSavedSurveysBinding
import com.project.farmingapp.model.data.FieldSurvey
import com.project.farmingapp.viewmodel.FieldSurveyViewModel

class SavedSurveysFragment : Fragment() {

    private var _binding: FragmentSavedSurveysBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: FieldSurveyViewModel
    private lateinit var adapter: SurveyAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSavedSurveysBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[FieldSurveyViewModel::class.java]
        (activity as AppCompatActivity).supportActionBar?.title = "Saved Surveys"

        setupRecycler()
        observeViewModel()

        binding.syncAllBtn.setOnClickListener { viewModel.syncAll() }
        binding.newSurveyFab.setOnClickListener { openNewSurvey() }
    }

    private fun setupRecycler() {
        adapter = SurveyAdapter(
            onView = { survey -> openSurveyDetail(survey) },
            onSync = { survey ->
                viewModel.syncAll()
            },
            onDelete = { survey ->
                viewModel.deleteSurvey(survey)
            }
        )
        binding.savedSurveysRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.savedSurveysRecycler.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.allSurveys.observe(viewLifecycleOwner) { surveys ->
            adapter.submitList(surveys)
            binding.emptySurveysState.visibility = if (surveys.isEmpty()) View.VISIBLE else View.GONE
            binding.savedSurveysRecycler.visibility = if (surveys.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.unsyncedCount.observe(viewLifecycleOwner) { count ->
            binding.unsyncedCountText.text = if (count == 0) "All synced ✔" else "$count unsynced survey(s)"
        }

        viewModel.syncResult.observe(viewLifecycleOwner) { event ->
            val (msg, color) = when (event) {
                FieldSurveyViewModel.SyncEvent.Syncing ->
                    "Uploading…" to R.color.colorPrimary
                is FieldSurveyViewModel.SyncEvent.Success ->
                    "✔ Synced ${event.count} survey(s) successfully" to R.color.colorPrimary
                FieldSurveyViewModel.SyncEvent.NoInternet ->
                    "⚠ No internet. Surveys saved locally." to R.color.colorThird
                FieldSurveyViewModel.SyncEvent.NothingToSync ->
                    "✔ All surveys already synced" to R.color.colorPrimary
                is FieldSurveyViewModel.SyncEvent.Error ->
                    "✗ ${event.message}" to R.color.colorThird
            }
            showSyncBanner(msg, color)
        }
    }

    private fun showSyncBanner(message: String, colorRes: Int) {
        binding.syncStatusBanner.text = message
        binding.syncStatusBanner.setBackgroundColor(requireContext().getColor(colorRes))
        binding.syncStatusBanner.visibility = View.VISIBLE
        binding.syncStatusBanner.postDelayed({ binding.syncStatusBanner.visibility = View.GONE }, 3000)
    }

    private fun openNewSurvey() {
        val fm = activity?.supportFragmentManager ?: return
        // Always open a fresh form (avoid reusing an old instance/state)
        fm.popBackStack("surveyFormFrag", androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        fm.beginTransaction()
            ?.setCustomAnimations(
                R.anim.slide_in_right, R.anim.slide_out_left,
                R.anim.slide_in_left, R.anim.slide_out_right
            )
            ?.replace(R.id.frame_layout, SurveyFormFragment.newInstance(), "surveyFormFrag")
            ?.setReorderingAllowed(true)
            ?.addToBackStack("surveyFormFrag")
            ?.commit()
    }

    private fun openSurveyDetail(survey: FieldSurvey) {
        val fragment = SurveyDetailFragment().apply {
            arguments = Bundle().apply { putInt("survey_id", survey.id) }
        }
        activity?.supportFragmentManager?.beginTransaction()
            ?.setCustomAnimations(
                R.anim.slide_in_right, R.anim.slide_out_left,
                R.anim.slide_in_left, R.anim.slide_out_right
            )
            ?.replace(R.id.frame_layout, fragment, "surveyDetailFrag")
            ?.setReorderingAllowed(true)
            ?.addToBackStack("surveyDetailFrag")
            ?.commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

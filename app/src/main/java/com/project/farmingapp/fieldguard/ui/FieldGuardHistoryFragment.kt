package com.project.farmingapp.fieldguard.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.project.farmingapp.R
import com.project.farmingapp.databinding.FragmentFieldguardHistoryBinding
import com.project.farmingapp.fieldguard.agent.SafetyRules
import com.project.farmingapp.fieldguard.data.FieldGuardIncident
import java.text.DateFormat
import java.util.Date

class FieldGuardHistoryFragment : Fragment() {
    private var _binding: FragmentFieldguardHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: FieldGuardViewModel
    private val adapter = IncidentAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[FieldGuardViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFieldguardHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.historyRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.historyRecycler.adapter = adapter
        viewModel.incidents.observe(viewLifecycleOwner) { list ->
            binding.historyEmpty.visibility = if (list.isNullOrEmpty()) View.VISIBLE else View.GONE
            adapter.submit(list.orEmpty())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class IncidentAdapter : RecyclerView.Adapter<IncidentAdapter.VH>() {
        private var items: List<FieldGuardIncident> = emptyList()

        fun submit(list: List<FieldGuardIncident>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_fieldguard_incident, parent, false)
            return VH(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(items[position])
        }

        class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val type = itemView.findViewById<TextView>(R.id.incidentType)
            private val meta = itemView.findViewById<TextView>(R.id.incidentMeta)
            private val evidence = itemView.findViewById<TextView>(R.id.incidentEvidence)

            fun bind(item: FieldGuardIncident) {
                type.text = item.type
                val whenStr = DateFormat.getDateTimeInstance().format(Date(item.createdAt))
                meta.text = "${item.status} · ${SafetyRules.confidenceLabel(item.modelConfidenceEstimate)} · $whenStr"
                evidence.text = item.evidenceSummary
            }
        }
    }
}

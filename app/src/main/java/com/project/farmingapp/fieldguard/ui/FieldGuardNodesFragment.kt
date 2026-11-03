package com.project.farmingapp.fieldguard.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.project.farmingapp.R
import com.project.farmingapp.databinding.FragmentFieldguardNodesBinding
import com.project.farmingapp.fieldguard.data.IotReading
import com.project.farmingapp.fieldguard.iot.DemoNode
import com.project.farmingapp.fieldguard.iot.DemoNodes
import com.project.farmingapp.fieldguard.iot.IotDemoPattern
import com.project.farmingapp.utilities.pulseTapFeedback
import java.text.DateFormat
import java.util.Date

class FieldGuardNodesFragment : Fragment() {
    private var _binding: FragmentFieldguardNodesBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: IotViewModel
    private val adapter = NodesAdapter { node ->
        Toast.makeText(
            requireContext(),
            getString(R.string.fieldguard_iot_detail_soon, stringByName(node.titleRes)),
            Toast.LENGTH_SHORT
        ).show()
        // Step 2 will open FieldGuardNodeDetailFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[IotViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFieldguardNodesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.nodesRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.nodesRecycler.adapter = adapter

        binding.demoHumidCool.setOnClickListener {
            it.pulseTapFeedback()
            viewModel.applyPattern(IotDemoPattern.HUMID_COOL_NIGHTS)
        }
        binding.demoDryHot.setOnClickListener {
            it.pulseTapFeedback()
            viewModel.applyPattern(IotDemoPattern.DRY_HOT_WEEK)
        }
        binding.demoOffline.setOnClickListener {
            it.pulseTapFeedback()
            viewModel.applyPattern(IotDemoPattern.NODE_OFFLINE)
        }
        binding.demoMotionNight.setOnClickListener {
            it.pulseTapFeedback()
            viewModel.applyPattern(IotDemoPattern.MOTION_AT_NIGHT)
            Toast.makeText(requireContext(), R.string.fieldguard_iot_demo_motion_hint, Toast.LENGTH_LONG).show()
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right, R.anim.slide_out_left,
                    R.anim.slide_in_left, R.anim.slide_out_right
                )
                .replace(
                    R.id.frame_layout,
                    FieldGuardIncidentFragment.newInstance("elephant_night"),
                    "fieldGuardIncident"
                )
                .addToBackStack("fieldGuardIncident")
                .setReorderingAllowed(true)
                .commit()
        }
        binding.demoReset.setOnClickListener {
            it.pulseTapFeedback()
            viewModel.applyPattern(IotDemoPattern.NORMAL)
        }

        viewModel.busy.observe(viewLifecycleOwner) { busy ->
            binding.iotProgress.visibility = if (busy == true) View.VISIBLE else View.GONE
        }
        viewModel.status.observe(viewLifecycleOwner) { msg ->
            binding.iotStatus.text = msg.orEmpty()
        }
        viewModel.latestReadings.observe(viewLifecycleOwner) { readings ->
            adapter.submit(DemoNodes.ALL, readings.orEmpty())
        }

        viewModel.ensureSeeded()
    }

    private fun stringByName(name: String): String {
        val id = resources.getIdentifier(name, "string", requireContext().packageName)
        return if (id != 0) getString(id) else name
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = FieldGuardNodesFragment()
    }

    private class NodesAdapter(
        private val onClick: (DemoNode) -> Unit
    ) : RecyclerView.Adapter<NodesAdapter.VH>() {
        private var nodes: List<DemoNode> = emptyList()
        private var latest: Map<String, IotReading> = emptyMap()

        fun submit(nodes: List<DemoNode>, readings: List<IotReading>) {
            this.nodes = nodes
            this.latest = readings.associateBy { it.nodeId }
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_fieldguard_iot_node, parent, false)
            return VH(v)
        }

        override fun getItemCount() = nodes.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(nodes[position], latest[nodes[position].nodeId], onClick)
        }

        class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val title = itemView.findViewById<TextView>(R.id.nodeTitle)
            private val district = itemView.findViewById<TextView>(R.id.nodeDistrict)
            private val readings = itemView.findViewById<TextView>(R.id.nodeReadings)
            private val meta = itemView.findViewById<TextView>(R.id.nodeMeta)

            fun bind(node: DemoNode, reading: IotReading?, onClick: (DemoNode) -> Unit) {
                val ctx = itemView.context
                val titleId = ctx.resources.getIdentifier(node.titleRes, "string", ctx.packageName)
                val descId = ctx.resources.getIdentifier(node.descRes, "string", ctx.packageName)
                title.text = if (titleId != 0) ctx.getString(titleId) else node.nodeId
                district.text = if (descId != 0) {
                    ctx.getString(descId)
                } else {
                    "${node.district} · ${node.cropHint}"
                }
                if (reading == null) {
                    readings.text = ctx.getString(R.string.fieldguard_iot_waiting)
                    meta.text = ""
                } else {
                    val motion = if (reading.pirMotion) {
                        ctx.getString(R.string.fieldguard_iot_motion_yes)
                    } else {
                        ctx.getString(R.string.fieldguard_iot_motion_no)
                    }
                    val online = if (reading.online) {
                        ctx.getString(R.string.fieldguard_iot_online)
                    } else {
                        ctx.getString(R.string.fieldguard_iot_offline)
                    }
                    readings.text = ctx.getString(
                        R.string.fieldguard_iot_readings_fmt,
                        reading.temperatureC,
                        reading.humidityPct,
                        motion
                    )
                    val whenStr = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(Date(reading.timestampMs))
                    meta.text = ctx.getString(
                        R.string.fieldguard_iot_meta_fmt,
                        reading.batteryPct,
                        reading.linkType,
                        online,
                        whenStr
                    )
                }
                itemView.setOnClickListener { onClick(node) }
            }
        }
    }
}

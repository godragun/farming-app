package com.project.farmingapp.view.chatbot

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.project.farmingapp.R
import com.project.farmingapp.utilities.pulseTapFeedback

class ChatbotFragment : Fragment() {

    private lateinit var chatContainer: LinearLayout
    private lateinit var chatScroll: ScrollView
    private lateinit var chatInput: EditText
    private lateinit var sendBtn: Button
    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_chatbot, container, false)
        chatContainer = view.findViewById(R.id.chatContainer)
        chatScroll = view.findViewById(R.id.chatScroll)
        chatInput = view.findViewById(R.id.chatInput)
        sendBtn = view.findViewById(R.id.sendBtn)
        prefs = requireContext().getSharedPreferences("agri_chatbot_prefs", Context.MODE_PRIVATE)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.title = "Agri Chatbot"
        addBotMessage("Hi! I can help with crops, fertilizer, pests, irrigation, and weather planning.")

        sendBtn.setOnClickListener {
            sendBtn.pulseTapFeedback()
            val prompt = chatInput.text.toString().trim()
            if (prompt.isEmpty()) return@setOnClickListener
            addUserMessage(prompt)
            chatInput.setText("")
            addBotMessage(generateResponse(prompt))
        }
    }

    private fun generateResponse(prompt: String): String {
        val text = prompt.lowercase()
        val lastTopic = prefs.getString(KEY_LAST_TOPIC, "") ?: ""
        val response = when {
            text.contains("weather") || text.contains("rain") || text.contains("temperature") -> {
                saveTopic("weather")
                "For weather-sensitive farming, check morning humidity and rain chance before spraying. Avoid pesticide spray when rain is expected within 6 hours."
            }
            text.contains("fertilizer") || text.contains("compost") || text.contains("nutrient") -> {
                saveTopic("fertilizer")
                "Use compost as base nutrition, then apply balanced fertilizer in split doses. For vegetables, feed every 10-14 days and water after application."
            }
            text.contains("pest") || text.contains("disease") || text.contains("fungus") -> {
                saveTopic("pest")
                "Remove infected leaves early, improve airflow, and use targeted spray in evening. Repeat in 5-7 day intervals depending on severity."
            }
            text.contains("rice") -> {
                saveTopic("rice")
                "Rice needs stage-based water control: shallow water after transplanting, maintain at tillering stage, and reduce before harvest."
            }
            text.contains("tea") -> {
                saveTopic("tea")
                "Tea grows best in well-drained acidic soil. Prune regularly, apply nutrients after pruning, and maintain mulch for moisture."
            }
            text.contains("chilli") || text.contains("pepper") -> {
                saveTopic("chilli")
                "Chilli prefers warm climate and full sunlight. Keep spacing 30-45 cm, and avoid waterlogging to reduce root issues."
            }
            text.contains("coconut") -> {
                saveTopic("coconut")
                "For coconut, apply nutrients around the drip line and mulch to conserve moisture. Deep watering helps during dry months."
            }
            text.contains("how to grow") -> {
                val topic = if (lastTopic.isNotBlank()) lastTopic else "crops"
                "For $topic, start with good soil drainage, correct spacing, timely watering, and weekly crop monitoring for pests."
            }
            text.contains("more") && lastTopic.isNotBlank() -> {
                "More on $lastTopic: keep records of watering, nutrient timing, and pest observations. Small consistent actions improve yield."
            }
            else -> {
                "I can help with crop growth, fertilizer schedules, weather planning, and pest control. Ask for a crop name (rice, tea, chilli, coconut)."
            }
        }
        return response
    }

    private fun saveTopic(topic: String) {
        prefs.edit().putString(KEY_LAST_TOPIC, topic).apply()
    }

    private fun addUserMessage(message: String) = addMessageBubble("You: $message", true)

    private fun addBotMessage(message: String) = addMessageBubble("Bot: $message", false)

    private fun addMessageBubble(message: String, isUser: Boolean) {
        val tv = TextView(requireContext())
        tv.text = message
        tv.setPadding(24, 18, 24, 18)
        tv.setTextColor(resources.getColor(R.color.fontColor, null))
        tv.background = resources.getDrawable(R.drawable.round_borderless_general, null)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = 10
        params.gravity = if (isUser) Gravity.END else Gravity.START
        tv.layoutParams = params
        chatContainer.addView(tv)
        chatScroll.post { chatScroll.fullScroll(View.FOCUS_DOWN) }
    }

    companion object {
        private const val KEY_LAST_TOPIC = "last_topic"
    }
}

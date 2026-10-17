package com.project.farmingapp.view.chatbot

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.media.MediaRecorder
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.project.farmingapp.BuildConfig
import com.project.farmingapp.R
import com.project.farmingapp.model.ai.AgriAiClient
import com.project.farmingapp.model.ai.VoiceAssistantResponse
import com.project.farmingapp.model.ai.VoiceTextRequest
import com.project.farmingapp.model.ai.WhisperSinhalaClient
import com.project.farmingapp.model.ai.WhisperTranscriptionError
import com.project.farmingapp.utilities.pulseTapFeedback
import com.project.farmingapp.utilities.toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.concurrent.Executors

class ChatbotFragment : Fragment() {

    private lateinit var chatContainer: LinearLayout
    private lateinit var chatScroll: ScrollView
    private lateinit var chatInput: EditText
    private lateinit var sendBtn: Button
    private lateinit var btnChatbotVoice: Button
    private lateinit var voiceStatusText: TextView
    private lateinit var prefs: android.content.SharedPreferences

    private val executor = Executors.newSingleThreadExecutor()

    private var mediaRecorder: MediaRecorder? = null
    private var recordingPath: String = ""
    private var isRecording = false

    private val audioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startSinhalaSpeech()
            else requireContext().toast(getString(R.string.chatbot_voice_mic_denied))
        }

    private val speechLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            voiceStatusText.visibility = View.GONE
            if (spoken.isNotEmpty()) {
                chatInput.setText(spoken)
                processUserQuery(spoken)
            }
        }

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
        btnChatbotVoice = view.findViewById(R.id.btnChatbotVoice)
        voiceStatusText = view.findViewById(R.id.chatbotVoiceStatus)
        prefs = requireContext().getSharedPreferences("agri_chatbot_prefs", Context.MODE_PRIVATE)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.nav_agri_chatbot)
        addBotMessage(
            "ආයුබෝවන්! මම Agri Chatbot. බෝග වගාව, පොහොර, පළිබෝධ, වාරිමාර්ග සහ කාලගුණය පිළිබඳව සිංහලෙන් හෝ ඉංග්‍රීසියෙන් විමසන්න. හඬින් ඇසීමට '🎤 Voice' ඔබන්න.\n\n" +
            "Hi! I can help with crops, fertilizer, pests, irrigation, and weather. Type your question or tap '🎤 Voice' to speak in Sinhala (powered by Whisper Small Sinhala v3)."
        )

        sendBtn.setOnClickListener {
            sendBtn.pulseTapFeedback()
            val prompt = chatInput.text.toString().trim()
            if (prompt.isEmpty()) return@setOnClickListener
            processUserQuery(prompt)
            chatInput.setText("")
        }

        btnChatbotVoice.setOnClickListener {
            btnChatbotVoice.pulseTapFeedback()
            ensureMicThenRecord()
        }
    }

    private fun processUserQuery(prompt: String) {
        addUserMessage(prompt)
        // First try to enrich with AI backend if connected; otherwise instant offline knowledge
        queryAiOrFallback(prompt)
    }

    private fun queryAiOrFallback(prompt: String) {
        val fallbackResponse = generateResponse(prompt)
        try {
            AgriAiClient.api.voiceAssistantText(VoiceTextRequest(prompt))
                .enqueue(object : Callback<VoiceAssistantResponse> {
                    override fun onResponse(
                        call: Call<VoiceAssistantResponse>,
                        response: Response<VoiceAssistantResponse>
                    ) {
                        if (response.isSuccessful && response.body() != null) {
                            val body = response.body()
                            val isSinhalaPrompt = isSinhalaText(prompt)
                            val answer = if (isSinhalaPrompt && !body?.response_sinhala.isNullOrBlank()) {
                                body?.response_sinhala.orEmpty()
                            } else {
                                body?.response_english ?: body?.response_sinhala ?: fallbackResponse
                            }
                            addBotMessage(answer)
                        } else {
                            addBotMessage(fallbackResponse)
                        }
                    }

                    override fun onFailure(call: Call<VoiceAssistantResponse>, t: Throwable) {
                        // Offline fallback with rich Sinhala/English agricultural Q&A
                        addBotMessage(fallbackResponse)
                    }
                })
        } catch (_: Exception) {
            addBotMessage(fallbackResponse)
        }
    }

    private fun ensureMicThenRecord() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            startSinhalaSpeech()
        }
    }

    private fun startSinhalaSpeech() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "si-LK")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "si-LK")
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.chatbot_voice_listening))
        }
        try {
            voiceStatusText.visibility = View.VISIBLE
            voiceStatusText.text = getString(R.string.chatbot_voice_listening)
            speechLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            toggleRecording()
        }
    }

    private fun toggleRecording() {
        if (isRecording) {
            stopRecordingAndTranscribe()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        try {
            val file = File(requireContext().cacheDir, "chatbot_voice_${System.currentTimeMillis()}.m4a")
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
            btnChatbotVoice.text = "⏹ Stop"
            voiceStatusText.visibility = View.VISIBLE
            voiceStatusText.text = getString(R.string.chatbot_voice_listening)
        } catch (e: Exception) {
            voiceStatusText.visibility = View.GONE
            requireContext().toast(e.message ?: "Record failed")
        }
    }

    private fun stopRecordingAndTranscribe() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {
        }
        mediaRecorder = null
        isRecording = false
        btnChatbotVoice.text = getString(R.string.chatbot_voice_btn)
        voiceStatusText.text = getString(R.string.chatbot_voice_transcribing)

        val audioFile = File(recordingPath)
        val hfApiKey = try { BuildConfig.HUGGINGFACE_API_KEY } catch (_: Exception) { "" }

        executor.execute {
            var transcribedText = ""
            try {
                transcribedText = WhisperSinhalaClient.transcribeAudio(audioFile, hfApiKey)
            } catch (e: WhisperTranscriptionError.ModelLoading) {
                activity?.runOnUiThread {
                    voiceStatusText.visibility = View.GONE
                    requireContext().toast("Model loading on Hugging Face (${e.estimatedTime.toInt()}s). Please retry.")
                }
                return@execute
            } catch (e: Exception) {
                android.util.Log.w("ChatbotFrag", "Direct whisper error: ${e.message}")
            }

            activity?.runOnUiThread {
                voiceStatusText.visibility = View.GONE
                if (transcribedText.isNotBlank()) {
                    chatInput.setText(transcribedText)
                    processUserQuery(transcribedText)
                } else {
                    requireContext().toast("හඬ හඳුනාගැනීම අසාර්ථක විය. කරුණාකර ප්‍රශ්නය ටයිප් කරන්න.")
                }
            }
        }
    }

    internal fun generateResponse(prompt: String): String {
        val lastTopic = if (::prefs.isInitialized) prefs.getString(KEY_LAST_TOPIC, "") ?: "" else ""
        val response = ChatbotKnowledgeEngine.generateResponse(prompt, lastTopic)
        val detectedTopic = ChatbotKnowledgeEngine.detectTopic(prompt)
        if (detectedTopic.isNotEmpty()) {
            saveTopic(detectedTopic)
        }
        return response
    }

    private fun isSinhalaText(str: String): Boolean {
        for (ch in str) {
            if (ch.code in 0x0D80..0x0DFF) return true
        }
        return false
    }

    private fun saveTopic(topic: String) {
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_LAST_TOPIC, topic).apply()
        }
    }

    private fun addUserMessage(message: String) = addMessageBubble("You: $message", true)

    private fun addBotMessage(message: String) = addMessageBubble("Bot: $message", false)

    private fun addMessageBubble(message: String, isUser: Boolean) {
        activity?.runOnUiThread {
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {
        }
        mediaRecorder = null
        executor.shutdown()
    }

    companion object {
        private const val KEY_LAST_TOPIC = "last_topic"
    }
}

object ChatbotKnowledgeEngine {

    fun detectTopic(prompt: String): String {
        val text = prompt.lowercase()
        val words = text.split(Regex("""[\s,.\-!?]+"""))
        val isRice = "වී" in words || text.contains("ගොයම්") || text.contains("කුඹුර") || text.contains("rice") || text.contains("paddy") || text.contains("වී වගා")

        return when {
            isRice -> "rice"
            text.contains("තේ") || text.contains("tea") -> "tea"
            text.contains("පොල්") || text.contains("coconut") || text.contains("pol") -> "coconut"
            text.contains("මිරිස්") || text.contains("chilli") || text.contains("pepper") -> "chilli"
            text.contains("තක්කාලි") || text.contains("tomato") -> "tomato"
            text.contains("කුරුඳු") || text.contains("cinnamon") -> "cinnamon"
            text.contains("පොහොර") || text.contains("fertilizer") || text.contains("urea") -> "fertilizer"
            text.contains("පළිබෝධ") || text.contains("රෝග") || text.contains("pest") || text.contains("disease") -> "pest"
            text.contains("කාලගුණ") || text.contains("වැස්ස") || text.contains("weather") || text.contains("rain") || text.contains("yala") || text.contains("maha") -> "weather"
            text.contains("වතුර") || text.contains("වාරිමාර්ග") || text.contains("irrigation") || text.contains("water") -> "irrigation"
            else -> ""
        }
    }

    fun generateResponse(prompt: String, lastTopic: String = ""): String {
        val text = prompt.lowercase()
        val words = text.split(Regex("""[\s,.\-!?]+"""))
        val isRice = "වී" in words || text.contains("ගොයම්") || text.contains("කුඹුර") || text.contains("rice") || text.contains("paddy") || text.contains("වී වගා")

        return when {
            // Chilli / Pepper / මිරිස් (checked with high priority)
            text.contains("මිරිස්") || text.contains("chilli") || text.contains("chili") || text.contains("pepper") -> {
                if (text.contains("කොඩ") || text.contains("කහ") || text.contains("curl") || text.contains("yellow")) {
                    "මිරිස් කොළ කොඩවීම (Leaf curl) පැළ මැක්කන්, සුදු මැස්සන් හෝ මයිටාවන් නිසා ඇතිවේ. කොහොඹ තෙල් සබන් මිශ්‍රණය සතියකට වරක් යොදන්න. ආසාදිත පැළ ඉවත් කරන්න.\n(Chilli leaf curl is spread by thrips/mites. Spray neem oil soap solution weekly and remove infected plants)."
                } else {
                    "මිරිස් වගාව: හොඳින් හිරු එළිය වැටෙන, ජලය නොරැඳෙන බුරුල් පසක් තෝරාගන්න. පැළ අතර පරතරය සෙ.මී. 45ක් තබා කොම්පෝස්ට් වැඩිපුර යොදන්න.\n(Chilli prefers warm full sunlight with 45cm spacing and well-drained fertile soil)."
                }
            }

            // Paddy / Rice / වී
            isRice -> {
                if (text.contains("කහ") || text.contains("yellow") || text.contains("පොහොර") || text.contains("fertilizer")) {
                    "වී වගාවේ ගොයම් කහ පැහැ ගැන්වීමට ප්‍රධාන හේතුව නයිට්‍රජන් (යූරියා) ඌනතාවයයි. නිර්දේශිත යූරියා ප්‍රමාණය කොටස් වශයෙන් දමන්න. ජලය බැසයාම පාලනය කර කොළ පාළු රෝග ලක්ෂණ ඇත්නම් ට්‍රයිසයික්ලසෝල් (Tricyclazole) යොදන්න.\n(For rice yellowing, ensure split Urea application and check for rice blast)."
                } else if (text.contains("පළිබෝධ") || text.contains("කෘමි") || text.contains("pest") || text.contains("hopper") || text.contains("දළඹු")) {
                    "ගොයම් පණු උවදුරු (ගොක් මැස්සා, කොළ හකුළන දළඹුවා, දුඹුරු පැළ මැක්කා) සඳහා කුඹුරේ ජලය හිස් කර දින 2ක් වියළෙන්න හරින්න. අවශ්‍ය නම් නියමිත පළිබෝධනාශක හෝ කොහොඹ ඇට සාරය යොදන්න.\n(For paddy pests like planthoppers, drain water for 2 days or apply neem extract)."
                } else {
                    "වී වගාව: පැළ සිටුවා දින 7ක් යනතුරු නොගැඹුරු ජලය තබන්න. ගොයම් කරල් පිරෙන කාලයේ ජලය හොඳින් පවත්වා ගන්න. අස්වැන්න නෙළීමට සති 2කට පෙර ජලය සම්පූර්ණයෙන්ම කපා හරින්න.\n(Rice requires shallow water after transplanting and water drainage 2 weeks before harvest)."
                }
            }

            // Tea / තේ
            text.contains("තේ") || text.contains("tea") -> {
                if (text.contains("කප්පාදු") || text.contains("prun")) {
                    "තේ කප්පාදුවෙන් පසු කැපුම් තුවාල මත දිලීර නාශකයක් (තඹ මිශ්‍රණයක්) ආලේප කරන්න. පසෙහි තෙතමනය රැකීමට වසුන් යොදන්න.\n(Prune tea bushes during favorable moisture conditions and treat cuts with copper fungicide)."
                } else if (text.contains("රෝග") || text.contains("දිලීර") || text.contains("blight") || text.contains("අංගමාර")) {
                    "තේ බිබිලි අංගමාරය (Blister blight) වළක්වා ගැනීමට සෙවන ගස් නිසි ලෙස නියාමනය කර කොපර් ඔක්සික්ලෝරයිඩ් දියරය ඉසින්න.\n(For tea blister blight, regulate shade trees and spray copper oxychloride)."
                } else {
                    "තේ වගාව: පී.එච්. අගය (pH) 4.5–5.5 අතර ආම්ලික පසක හොඳින් වැඩේ. T-65 හෝ T-200 නිර්දේශිත තේ පොහොර මිශ්‍රණය යොදා මතුපිට වසුන් යොදන්න.\n(Tea thrives in well-drained acidic soil pH 4.5-5.5. Apply recommended tea fertilizer mix)."
                }
            }

            // Coconut / පොල්
            text.contains("පොල්") || text.contains("coconut") || text.contains("pol") -> {
                if (text.contains("කුරුමිණි") || text.contains("beetle") || text.contains("weevil")) {
                    "පොල් රතු කුරුමිණියා සඳහා ගසේ තුවාල තාර ගා ආවරණය කරන්න. කළු කුරුමිණියා සඳහා කරටි අතු අස්සේ ලුණු සහ වැලි මිශ්‍රණයක් හෝ නැප්තලින් බෝල දමන්න.\n(For coconut red palm weevil, seal wounds with tar. For rhinoceros beetle, apply salt-sand mix)."
                } else {
                    "පොල් වගාව: ගස වටා මීටර් 1.5–2 රවුමට පොල් පොහොර මිශ්‍රණය (APM) යොදා පස් සමග කලවම් කරන්න. වියළි කාලයේදී පොල් ලෙලි හෝ කොළ වසුන් යොදන්න.\n(Apply coconut fertilizer mix in a 1.5m radius from trunk and mulch with husks during dry spells)."
                }
            }


            // Tomato / තක්කාලි
            text.contains("තක්කාලි") || text.contains("tomato") -> {
                "තක්කාලි වගාවේ අංගමාරය වැළැක්වීමට යටි කොළ කප්පාදු කර වාතාශ්‍රය සලසන්න. මැලවීම පාලනයට කැල්සියම් සහ සමබර පොහොර යොදා බිම නොගෑවෙන සේ ආධාරක බඳින්න.\n(For tomatoes, prune bottom foliage for airflow, stake plants off ground, and ensure adequate calcium)."
            }

            // Cinnamon / කුරුඳු
            text.contains("කුරුඳු") || text.contains("කුරුදු") || text.contains("cinnamon") -> {
                "කුරුඳු වගාව: පඳුරුවල පැති අතු නිසි කලට කපා දඬු ඍජුව වැඩෙන්නට හරින්න. පොතු ගැලවීමේ ගුණාත්මකභාවය වැඩි කිරීමට මෝසම් වර්ෂාවට පෙර පොහොර යොදන්න.\n(Cinnamon: prune lateral shoots so stems grow straight. Apply fertilizer before monsoon for best peeling quality)."
            }

            // Fertilizer / පොහොර
            text.contains("පොහොර") || text.contains("fertilizer") || text.contains("compost") || text.contains("urea") || text.contains("යූරියා") -> {
                "පොහොර යෙදීමේ මූලධර්ම: මූලික පෝෂණයට හොඳින් දිරූ කොම්පෝස්ට් යොදන්න. රසායනික පොහොර (යූරියා, TSP, MOP) එක්වර නොදමා කොටස් වශයෙන් දමන්න. පොහොර දැමූ විගස පස තෙත් කරන්න.\n(Fertilizer tips: base with organic compost, apply chemical fertilizers in split doses, and always water after applying)."
            }

            // Pests & Diseases / පළිබෝධ සහ රෝග
            text.contains("පළිබෝධ") || text.contains("රෝග") || text.contains("කෘමි") || text.contains("දිලීර") || text.contains("pest") || text.contains("disease") || text.contains("fungus") -> {
                "රෝග හා පළිබෝධ පාලනය: රෝගී කොළ කපා විනාශ කරන්න. කොළ ස්කෑන් කර නිවැරදි රෝගය හඳුනාගැනීමට Navigation Drawer හි ඇති '🌿 Leaf Scanner (Roboflow)' හෝ 'AI Crop Doctor' භාවිතා කරන්න!\n(To identify leaf disease, use '🌿 Leaf Scanner (Roboflow)' or 'AI Crop Doctor' from the menu)."
            }

            // Weather & Seasons / කාලගුණය සහ කන්න
            text.contains("කාලගුණ") || text.contains("වැස්ස") || text.contains("කන්න") || text.contains("weather") || text.contains("rain") || text.contains("yala") || text.contains("maha") || text.contains("යල") || text.contains("මහ") -> {
                "කන්න තොරතුරු: ශ්‍රී ලංකාවේ ප්‍රධාන කන්න දෙකකි:\n• මහ කන්නය: සැප්තැම්බර් - මාර්තු (ඊසානදිග මෝසම)\n• යල කන්නය: මැයි - අගෝස්තු (නිරිතදිග මෝසම)\nවැසි අපේක්ෂිත දිනවල කෘමිනාශක ඉසීමෙන් වළකින්න.\n(Sri Lanka farming has Maha [Sep-Mar] and Yala [May-Aug] seasons. Avoid spraying chemicals before rain)."
            }

            // Irrigation / ජලය / වාරිමාර්ග
            text.contains("වතුර") || text.contains("ජල") || text.contains("වාරිමාර්ග") || text.contains("irrigation") || text.contains("water") -> {
                "වාරිමාර්ග ක්‍රම: එළවළු සඳහා බිංදු ජල සම්පාදනය (Drip) ජලය 60%කින් ඉතිරි කරයි. උදෑසන හෝ සවස් කාලයේ ජලය සැපයීමෙන් වාෂ්පීභවනය අවම වේ. පස වටා වසුන් යොදන්න.\n(Watering tips: Drip irrigation saves 60% water. Water early morning or evening and use organic mulch)."
            }

            // General / Follow-up
            (text.contains("more") || text.contains("තව")) && lastTopic.isNotBlank() -> {
                "තවත් තොරතුරු ($lastTopic): පසෙහි තෙතමනය සහ පළිබෝධ නිරීක්ෂණය දිනපතා සිදුකරන්න. අවශ්‍ය නම් Navigation Drawer හි ඇති 'AI Crop Doctor' වෙතින් ඡායාරූපයක් යවා විශ්ලේෂණය කරන්න.\n(Keep consistent records. You can also analyze photos in Crop Doctor)."
            }

            else -> {
                "මම ශ්‍රී ලංකාවේ ගොවීන්ට උපකාරී වන Agri Chatbot. වී, තේ, පොල්, මිරිස්, තක්කාලි, පොහොර, කාලගුණය හෝ පළිබෝධ ගැන විමසන්න. හඬින් ඇසීමට '🎤 Voice' ඔබන්න.\n\nI am Agri Chatbot for Sri Lankan farmers. Ask about rice, tea, coconut, chilli, fertilizer, weather, or pests. Tap '🎤 Voice' to speak in Sinhala."
            }
        }
    }
}

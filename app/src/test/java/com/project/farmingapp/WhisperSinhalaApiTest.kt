package com.project.farmingapp

import com.project.farmingapp.model.ai.WhisperSinhalaClient
import com.project.farmingapp.model.ai.WhisperSinhalaConfig
import com.project.farmingapp.view.chatbot.ChatbotFragment
import org.junit.Assert.*
import org.junit.Test

class WhisperSinhalaApiTest {

    @Test
    fun config_matchesHuggingFaceModelSpecifications() {
        assertEquals("Lingalingeswaran/whisper-small-sinhala_v3", WhisperSinhalaConfig.MODEL_ID)
        assertEquals(
            "https://api-inference.huggingface.co/models/Lingalingeswaran/whisper-small-sinhala_v3",
            WhisperSinhalaConfig.HF_API_URL
        )
        assertEquals(
            "https://router.huggingface.co/hf-inference/models/Lingalingeswaran/whisper-small-sinhala_v3",
            WhisperSinhalaConfig.HF_ROUTER_URL
        )
        assertEquals("si", WhisperSinhalaConfig.LANGUAGE_SI)
    }

    @Test
    fun parseTranscriptionResponse_parsesJsonObjectCorrectly() {
        val json = """{"text": "මගේ මිරිස් කොළ කහ වෙලා මොකද කරන්නේ"}"""
        val parsed = WhisperSinhalaClient.parseTranscriptionResponse(json)
        assertEquals("මගේ මිරිස් කොළ කහ වෙලා මොකද කරන්නේ", parsed)
    }

    @Test
    fun parseTranscriptionResponse_parsesJsonArrayCorrectly() {
        val json = """[{"text": "වී වගාවට යූරියා දමන්නේ කෙසේද"}]"""
        val parsed = WhisperSinhalaClient.parseTranscriptionResponse(json)
        assertEquals("වී වගාවට යූරියා දමන්නේ කෙසේද", parsed)
    }

    @Test
    fun parseTranscriptionResponse_parsesTranscriptionKey() {
        val json = """{"transcription": "තේ වගාවේ බිබිලි අංගමාරය"}"""
        val parsed = WhisperSinhalaClient.parseTranscriptionResponse(json)
        assertEquals("තේ වගාවේ බිබිලි අංගමාරය", parsed)
    }

    @Test
    fun parseTranscriptionResponse_handlesEmpty() {
        val parsed = WhisperSinhalaClient.parseTranscriptionResponse("{}")
        assertEquals("", parsed)
    }

    @Test
    fun chatbotKnowledge_answersSinhalaPaddyQuestion() {
        val fragment = ChatbotFragment()
        val answer = fragment.generateResponse("ගොයම් කහ වෙලා මොකක්ද පොහොර")
        assertTrue("Answer should contain urea / nitrogen advice in Sinhala", answer.contains("යූරියා") || answer.contains("නයිට්‍රජන්"))
    }

    @Test
    fun chatbotKnowledge_answersSinhalaTeaQuestion() {
        val fragment = ChatbotFragment()
        val answer = fragment.generateResponse("තේ වගාවේ බිබිලි අංගමාරය රෝගය")
        assertTrue("Answer should contain tea blight advice in Sinhala", answer.contains("අංගමාරය") || answer.contains("තේ"))
    }

    @Test
    fun chatbotKnowledge_answersSinhalaCoconutQuestion() {
        val fragment = ChatbotFragment()
        val answer = fragment.generateResponse("පොල් ගස්වල කුරුමිණියා උවදුර")
        assertTrue("Answer should contain beetle advice", answer.contains("කුරුමිණි") || answer.contains("පොල්"))
    }

    @Test
    fun chatbotKnowledge_answersSinhalaChilliQuestion() {
        val fragment = ChatbotFragment()
        val answer = fragment.generateResponse("මිරිස් කොළ කොඩවීම පාලනය කරන්නේ කොහොමද")
        assertTrue("Answer should contain chilli leaf curl advice", answer.contains("කොඩවීම") || answer.contains("කොහොඹ"))
    }

    @Test
    fun chatbotKnowledge_answersSeasonsQuestion() {
        val fragment = ChatbotFragment()
        val answer = fragment.generateResponse("ශ්‍රී ලංකාවේ යල සහ මහ කන්න මොනවාද")
        assertTrue("Answer should describe Yala and Maha seasons", answer.contains("මහ කන්නය") && answer.contains("යල කන්නය"))
    }

    @Test
    fun chatbotKnowledge_answersFertilizerQuestion() {
        val fragment = ChatbotFragment()
        val answer = fragment.generateResponse("කොම්පෝස්ට් සහ යූරියා පොහොර යෙදීම")
        assertTrue("Answer should explain fertilizer and compost", answer.contains("කොම්පෝස්ට්") || answer.contains("පොහොර"))
    }

    @Test
    fun chatbotKnowledge_answersEnglishQuestions() {
        val fragment = ChatbotFragment()
        val answer = fragment.generateResponse("how to manage rice water")
        assertTrue("Answer should explain rice water management", answer.contains("Rice") || answer.contains("water"))
    }
}

package com.project.farmingapp

import com.project.farmingapp.model.ai.LeafScanError
import com.project.farmingapp.model.ai.RoboflowLeafClient
import com.project.farmingapp.model.ai.RoboflowLeafConfig
import org.junit.Assert.*
import org.junit.Test

class RoboflowLeafApiTest {

    private val sampleRoboflowResponse = """
        [
          {
            "predictions": {
              "image": {
                "width": 640,
                "height": 480
              },
              "predictions": [
                {
                  "class": "Tomato_healthy",
                  "class_id": 0,
                  "confidence": 0.885
                },
                {
                  "class": "Tomato_Early_blight",
                  "class_id": 1,
                  "confidence": 0.082
                },
                {
                  "class": "Tomato_Late_blight",
                  "class_id": 2,
                  "confidence": 0.033
                }
              ],
              "top": "Tomato_healthy",
              "confidence": 0.885
            }
          }
        ]
    """.trimIndent()

    @Test
    fun parseResponse_parsesTopAndPredictionsCorrectly() {
        val result = RoboflowLeafClient.parseResponse(sampleRoboflowResponse)

        assertEquals("Tomato_healthy", result.top)
        assertEquals(0.885, result.confidence, 0.001)
        assertTrue(result.diagnosisAvailable)
        assertEquals(3, result.allPredictions.size)

        assertEquals("Tomato_healthy", result.allPredictions[0].`class`)
        assertEquals(0, result.allPredictions[0].class_id)
        assertEquals(0.885, result.allPredictions[0].confidence, 0.001)

        assertEquals("Tomato_Early_blight", result.allPredictions[1].`class`)
        assertEquals(0.082, result.allPredictions[1].confidence, 0.001)
    }

    @Test
    fun parseResponse_unwrapsLiveOutputsEnvelope() {
        val live = """
            {"outputs":[{"predictions":{"image":{"width":480,"height":320},"predictions":[{"class":"Tomato_healthy","class_id":0,"confidence":0.91}],"top":"Tomato_healthy","confidence":0.91,"prediction_type":"classification"}}]}
        """.trimIndent()
        val result = RoboflowLeafClient.parseResponse(live)
        assertEquals("Tomato_healthy", result.top)
        assertEquals(0.91, result.confidence, 0.001)
        assertTrue(result.diagnosisAvailable)
        assertEquals(1, result.allPredictions.size)
    }

    @Test
    fun parseResponse_lowConfidenceSetsDiagnosisAvailableFalse() {
        val lowConfJson = """
            [
              {
                "predictions": {
                  "top": "Pepper_bell_Bacterial_spot",
                  "confidence": 0.15,
                  "predictions": [
                    { "class": "Pepper_bell_Bacterial_spot", "class_id": 0, "confidence": 0.15 }
                  ]
                }
              }
            ]
        """.trimIndent()

        val result = RoboflowLeafClient.parseResponse(lowConfJson)
        assertEquals("Pepper_bell_Bacterial_spot", result.top)
        assertEquals(0.15, result.confidence, 0.001)
        assertFalse("Expected diagnosisAvailable=false when confidence < MIN_CONFIDENCE", result.diagnosisAvailable)
    }

    @Test
    fun classifyBase64_validatesBlankApiKey() {
        try {
            RoboflowLeafClient.classifyBase64("dummyBase64", "   ")
            fail("Expected LeafScanError.MissingApiKey")
        } catch (e: LeafScanError.MissingApiKey) {
            // Success
        }
    }

    @Test
    fun classifyBase64_validatesBlankImage() {
        try {
            RoboflowLeafClient.classifyBase64("  ", "rf_test_key")
            fail("Expected LeafScanError.EmptyImage")
        } catch (e: LeafScanError.EmptyImage) {
            // Success
        }
    }

    @Test
    fun config_matchesRoboflowWorkflowSpecifications() {
        assertEquals("garbagedetectionws", RoboflowLeafConfig.WORKSPACE)
        assertEquals("leaves-and-deceased-leaves-vleaves-and-deceased-leaves-z3m7h-1-vit-base-patch16-224-in21k-t1-logic", RoboflowLeafConfig.WORKFLOW_ID)
        assertEquals(
            "https://serverless.roboflow.com/garbagedetectionws/workflows/leaves-and-deceased-leaves-vleaves-and-deceased-leaves-z3m7h-1-vit-base-patch16-224-in21k-t1-logic",
            RoboflowLeafConfig.ENDPOINT
        )
    }
}

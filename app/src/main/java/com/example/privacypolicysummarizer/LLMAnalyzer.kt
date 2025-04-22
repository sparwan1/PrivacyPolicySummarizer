// LLMAnalyzer.kt
package com.example.privacypolicysummarizer

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

suspend fun analyzePolicyViaLLM(policyText: String): String? {
    return try {
        println("📄 Sending ${policyText.length} chars to LLM")
        val client = OkHttpClient()
        val mediaType = "application/json".toMediaType()
        val json = JSONObject().put("policy_text", policyText).toString()
        val body = json.toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://privacypolicysummarizer.onrender.com/analyze_policy")
            .post(body)
            .build()

        println("🌍 Sending request to: ${request.url}")

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            println("❌ HTTP error: ${response.code}")
            println("❌ Body: ${response.body?.string()}")
            return null
        }

        val result = response.body?.string()
        println("✅ LLM Response: ${result?.take(500)}...")
        return result
    } catch (e: Exception) {
        println("❌ Exception during LLM call: ${e.message}")
        e.printStackTrace()
        return null
    }
}

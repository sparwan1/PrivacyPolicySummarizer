package com.example.privacypolicysummarizer.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit
import okhttp3.Dns
import java.net.InetAddress

object PrivacyPolicyFetcher {


    private val client = OkHttpClient.Builder()
        .dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                return InetAddress.getAllByName(hostname).toList()
            }
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    data class PrivacyPolicyResult(val url: String, val plainText: String)
    /**
     * Fetches the privacy policy content for the given package from the Play Store.
     *
     * @param packageName The application's package name.
     * @return Privacy policy content as plain text, or null if not found.
     */
    suspend fun fetchPrivacyPolicyContent(packageName: String): PrivacyPolicyResult? {
        return withContext(Dispatchers.IO) {
            try {
                val playStoreUrl = "https://play.google.com/store/apps/details?id=$packageName"
                println("Fetching Play Store page for $packageName")

                val playStoreRequest = Request.Builder().url(playStoreUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()

                val playStoreResponse = client.newCall(playStoreRequest).execute()
                println("Play Store response code: ${playStoreResponse.code}")

                if (!playStoreResponse.isSuccessful) {
                    println("Play Store request failed with status: ${playStoreResponse.code}")
                    return@withContext null
                }

                val playStoreHtml = playStoreResponse.body?.string()
                playStoreResponse.close()

                if (playStoreHtml == null || playStoreHtml.isBlank()) {
                    println("Empty response from Play Store")
                    return@withContext null
                }

                println("Play Store HTML length: ${playStoreHtml.length}")

                val playStoreDoc = Jsoup.parse(playStoreHtml)
                val privacyPolicyUrl = findPrivacyPolicyUrl(playStoreDoc)

                println("Extracted privacy policy URL: $privacyPolicyUrl")

                if (privacyPolicyUrl == null) {
                    println("No privacy policy URL found for $packageName")
                    return@withContext null
                }

                val policyRequest = Request.Builder().url(privacyPolicyUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()

                val policyResponse = client.newCall(policyRequest).execute()
                println("Policy response code: ${policyResponse.code}")

                if (!policyResponse.isSuccessful) {
                    println("Failed to fetch privacy policy: ${policyResponse.code}")
                    return@withContext null
                }

                val policyHtml = policyResponse.body?.string()
                policyResponse.close()

                if (policyHtml == null || policyHtml.isBlank()) {
                    println("Empty privacy policy content")
                    return@withContext null
                }

                val policyDoc = Jsoup.parse(policyHtml)
                val plainText = extractPlainText(policyDoc)

                println("Successfully extracted ${plainText.length} characters of plain text")
                return@withContext PrivacyPolicyResult(privacyPolicyUrl, plainText)

            } catch (e: Exception) {
                println("Exception: ${e.message}")
                e.printStackTrace()
                return@withContext null
            }
        }
    }

    /**
     * Extract the privacy policy URL from the Play Store page
     */
    private fun findPrivacyPolicyUrl(doc: Document): String? {
        println("Looking for privacy policy links...")

        val links = doc.select("a[href]")
        for (link in links) {
            val href = link.attr("href")
            val text = link.text().lowercase()

            // Debug output
            if ("privacy" in text || "privacy" in href) {
                println("Found potential link: $text -> $href")
            }

            // Skip Google's policy
            if (href.contains("policies.google.com")) {
                continue
            }

            if ((text.contains("privacy") || href.contains("privacy")) && href.startsWith("http")) {
                return href
            }
        }

        println("No matching privacy policy link found (excluding Google).")
        return null
    }

    /**
     * Extract plain text from HTML, removing scripts, styles, and HTML tags
     */
    private fun extractPlainText(doc: Document): String {
        val elements = doc.body().select("p, li")

        // Join them with two line breaks to simulate paragraphs
        return elements.joinToString("\n\n") { it.text().trim() }
    }
}
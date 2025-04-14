package com.example.privacypolicysummarizer.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.util.concurrent.TimeUnit

object PrivacyPolicyFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Fetches the privacy policy content for the given package from the Play Store.
     *
     * @param packageName The application's package name.
     * @return Privacy policy content as plain text, or null if not found.
     */
    suspend fun fetchPrivacyPolicyContent(packageName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val playStoreUrl = "https://play.google.com/store/apps/details?id=$packageName"
                
                // First get the Play Store page
                val playStoreRequest = Request.Builder().url(playStoreUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .build()
                
                val playStoreResponse = client.newCall(playStoreRequest).execute()
                if (!playStoreResponse.isSuccessful) {
                    println("Failed to fetch Play Store page: ${playStoreResponse.code}")
                    return@withContext null
                }
                
                val playStoreHtml = playStoreResponse.body?.string()
                playStoreResponse.close()
                
                if (playStoreHtml == null) {
                    println("Empty response from Play Store")
                    return@withContext null
                }
                
                // Parse the Play Store page to find the privacy policy URL
                val playStoreDoc = Jsoup.parse(playStoreHtml)
                
                // Look for privacy policy link - it's typically in a div with "Privacy Policy" text
                val privacyPolicyUrl = findPrivacyPolicyUrl(playStoreDoc)
                
                if (privacyPolicyUrl == null) {
                    println("No privacy policy URL found for $packageName")
                    return@withContext null
                }
                
                println("Found privacy policy URL: $privacyPolicyUrl")
                
                // Now fetch the privacy policy content
                val policyRequest = Request.Builder().url(privacyPolicyUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .build()
                
                val policyResponse = client.newCall(policyRequest).execute()
                if (!policyResponse.isSuccessful) {
                    println("Failed to fetch privacy policy: ${policyResponse.code}")
                    return@withContext null
                }
                
                val policyHtml = policyResponse.body?.string()
                policyResponse.close()
                
                if (policyHtml == null) {
                    println("Empty privacy policy")
                    return@withContext null
                }
                
                // Extract plain text from the HTML
                val policyDoc = Jsoup.parse(policyHtml)
                val plainText = extractPlainText(policyDoc)
                
                println("Successfully extracted ${plainText.length} characters of plain text")
                plainText
                
            } catch (e: Exception) {
                e.printStackTrace()
                println("Error fetching privacy policy: ${e.message}")
                null
            }
        }
    }
    
    /**
     * Extract the privacy policy URL from the Play Store page
     */
    private fun findPrivacyPolicyUrl(doc: Document): String? {
        // Look for elements containing "privacy policy" text
        val privacyLinks = doc.select("a:containsOwn(Privacy Policy), a:containsOwn(privacy policy), a[href*=privacy]")
        
        if (privacyLinks.isNotEmpty()) {
            for (link in privacyLinks) {
                val href = link.attr("href")
                if (href.isNotEmpty() && !href.startsWith("#")) {
                    // If it's a relative URL, make it absolute
                    return if (href.startsWith("http")) href else "https://play.google.com$href"
                }
            }
        }
        
        return null
    }
    
    /**
     * Extract plain text from HTML, removing scripts, styles, and HTML tags
     */
    private fun extractPlainText(doc: Document): String {
        // Remove script, style elements and hidden elements
        doc.select("script, style, [style*=display:none], [style*=display: none]").remove()
        
        // Get text from body - this removes all HTML tags
        var text = doc.body().text()
        
        // Clean up the text
        text = text.replace("\\s+".toRegex(), " ") // Replace multiple whitespaces with a single space
            .replace("(\\.\\s*){2,}".toRegex(), ". ") // Remove multiple periods
            .trim()
            
        return text
    }
}
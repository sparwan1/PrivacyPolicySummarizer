package com.example.privacypolicysummarizer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.privacypolicysummarizer.network.PrivacyPolicyFetcher
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf

data class SummaryItem(val riskLevel: String, val justification: String, val snippet: String)

val dummySummaryData = mapOf(
    "Email Address" to SummaryItem(
        riskLevel = "Red",
        justification = "The policy mentions that email addresses may be shared with marketing partners.",
        snippet = "We may share your email address with our trusted third-party affiliates for promotional purposes."
    ),
    "Credit Card Number and Home Address" to SummaryItem(
        riskLevel = "Yellow",
        justification = "Used for billing only, not shared with third parties.",
        snippet = "We collect your credit card details to process your payments securely."
    ),
    "Location" to SummaryItem(
        riskLevel = "Green",
        justification = "Used only to improve app experience, not shared externally.",
        snippet = "Location helps us personalize app content."
    ),
    "Social Security Number" to SummaryItem(
        riskLevel = "Red",
        justification = "The policy indicates the collection and exposure of SSNs, which are highly sensitive.",
        snippet = "Social Security Numbers are handled with strict data protection measures."
    ),
    "Ads and Marketing" to SummaryItem(
        riskLevel = "Yellow",
        justification = "The policy outlines the use of personal data for targeted ads and marketing campaigns.",
        snippet = "User data may be shared with marketing partners to deliver personalized advertisements."
    ),
    "Collecting PII of Children" to SummaryItem(
        riskLevel = "Red",
        justification = "The policy indicates the collection of children's personal data without sufficient safeguards.",
        snippet = "Children's data is collected without verifiable parental consent, posing significant privacy risks."
    ),
    "Sharing with Law Enforcement" to SummaryItem(
        riskLevel = "Yellow",
        justification = "Data may be disclosed to law enforcement agencies under certain legal circumstances.",
        snippet = "User data might be shared with law enforcement upon receiving a valid legal request."
    ),
    "Policy Change Notification" to SummaryItem(
        riskLevel = "Green",
        justification = "The policy includes provisions for notifying users about changes in data practices.",
        snippet = "Users will be informed ahead of time if there are significant changes to privacy policies."
    ),
    "Control of Data" to SummaryItem(
        riskLevel = "Green",
        justification = "The policy provides users with options to manage their personal data.",
        snippet = "Users have the right to access, correct, or delete their personal data as per the guidelines."
    ),
    "Data Aggregation" to SummaryItem(
        riskLevel = "Yellow",
        justification = "Data may be aggregated from multiple sources to create profiles.",
        snippet = "User data might be combined and analyzed for trends and insights, impacting privacy."
    )
)
val riskColorMap = mapOf(
    "Green" to Color(0xFF4CAF50),
    "Yellow" to Color(0xFFFFC107),
    "Red" to Color(0xFFF44336)
)

val riskScoreMap = mapOf(
    "Green" to "10/10",
    "Yellow" to "5/10",
    "Red" to "0/10"
)

val iconMap = mapOf(
    "Email Address" to Icons.Default.Email,
    "Credit Card Number and Home Address" to Icons.Default.Payment,
    "Location" to Icons.Default.LocationOn,
    "Social Security Number" to Icons.Default.Security,
    "Ads and Marketing" to Icons.Default.Campaign,
    "Collecting PII of Children" to Icons.Default.Face,
    "Sharing with Law Enforcement" to Icons.Default.Gavel,
    "Policy Change Notification" to Icons.Default.Notifications,
    "Control of Data" to Icons.Default.Settings,
    "Data Aggregation" to Icons.Default.BarChart
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSummaryScreen(packageName: String) {
    var isLoading by remember { mutableStateOf(true) }
    var summaryData by remember { mutableStateOf<Map<String, SummaryItem>>(emptyMap()) }
    val extractedMap = mutableMapOf<String, SummaryItem>()
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Privacy Policy Summary") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            val context = LocalContext.current
            val appName = try {
                val packageManager = context.packageManager
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                packageName
            }

            Text(
                text = "App: $appName",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 50.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Analyzing privacy policy...", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                summaryData.forEach { (title, item) ->
                    ExpandableSummaryCard(title = title, item = item)
                }
            }

            // Background logging of real extracted data
            val fileName = "${packageName.replace('.', '_')}_privacy_policy.txt"
            val file = File(context.filesDir, fileName)

            LaunchedEffect(Unit) {
                if (!file.exists()) {
                    println("⚠️ File not found for package: $packageName")
                    println("🌐 Attempting to fetch policy dynamically...")

                    val policy = PrivacyPolicyFetcher.fetchPrivacyPolicyContent(packageName)
                    if (policy != null) {
                        println("✅ Dynamically fetched policy for $packageName (${policy.toString().length} chars)")

                        file.writeText(policy.toString())
                    } else {
                        println("❌ Failed to fetch policy dynamically.")
                    }
                }

                if (file.exists()) {
                    val fullText = file.readText()
//                    val chunks = splitTextIntoChunks(fullText)
//
//                    println("📦 Total chunks: ${chunks.size}")
//
//                    chunks.forEachIndexed { index, chunk ->
//                        println("📤 Sending chunk ${index + 1}/${chunks.size} (${chunk.length} chars)")

                    val result = withContext(Dispatchers.IO) {
                        analyzePolicyViaLLM(fullText)
                    }


                    if (result != null) {
                        println("✅ Full result from LLM:\n${result.take(2000)}")

                        // Clean out Markdown-style code block if present
                        val cleanedJson = result
                            .replace("```json", "")
                            .replace("```", "")
                            .trim()

                        try {
                            val outer = JSONObject(result)
                            val innerText = outer.getString("response")

                            // Try to locate the actual JSON block inside the response string
                            val jsonStart = innerText.indexOf('{')
                            val jsonEnd = innerText.lastIndexOf('}')
                            if (jsonStart == -1 || jsonEnd == -1 || jsonEnd <= jsonStart) {
                                println("❌ Could not extract inner JSON from LLM response")
                                return@LaunchedEffect
                            }

                            val jsonString = innerText.substring(jsonStart, jsonEnd + 1)
                            val json = JSONObject(jsonString)

                            for (key in json.keys()) {
                                val obj = json.getJSONObject(key)
                                val riskLevel = obj.getString("risk_level")
                                val justification = obj.getString("justification")
                                val snippet = obj.getString("snippet")

                                extractedMap[key] = SummaryItem(
                                    riskLevel = riskLevel,
                                    justification = justification,
                                    snippet = snippet
                                )
                            }

                            summaryData = extractedMap
                        } catch (e: Exception) {
                            println("❌ JSON parsing failed: ${e.message}")
                            e.printStackTrace()
                        }

                    } else {
                        println("❌ LLM returned null or failed")
                    }
                }
                summaryData = extractedMap
                isLoading = false
            }
        }
    }
}

@Composable
fun ExpandableSummaryCard(title: String, item: SummaryItem) {
    var expanded by remember { mutableStateOf(false) }

    val icon = iconMap[title] ?: Icons.Default.Info
    val riskColor = riskColorMap[item.riskLevel] ?: Color.Gray
    val score = riskScoreMap[item.riskLevel] ?: "?"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(riskColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "$title - $score",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = item.justification,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.snippet,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        }
    }
}

fun extractSummaryFromText(policyText: String): Map<String, SummaryItem> {
    val keywordsMap = mapOf(
        "Email Address" to listOf("email"),
        "Credit Card Number and Home Address" to listOf("credit card", "billing address", "home address"),
        "Location" to listOf("location", "gps", "geo"),
        "Social Security Number" to listOf("ssn", "social security"),
        "Ads and Marketing" to listOf("ads", "advertising", "marketing"),
        "Collecting PII of Children" to listOf("children", "child", "under 13", "minor"),
        "Sharing with Law Enforcement" to listOf("law enforcement", "police", "government", "legal request"),
        "Policy Change Notification" to listOf("notify", "update", "change", "modification"),
        "Control of Data" to listOf("access", "delete", "correct", "edit", "opt-out"),
        "Data Aggregation" to listOf("aggregate", "combine", "analyze", "profile")
    )

    val summary = mutableMapOf<String, SummaryItem>()

    val paragraphs = policyText.split(Regex("\\n\\s*\\n"))

    for ((category, keywords) in keywordsMap) {
        val matches = paragraphs.filter { para ->
            keywords.any { keyword -> keyword in para.lowercase() }
        }

        if (matches.isNotEmpty()) {
            val joined = matches.joinToString("\n\n") { it.trim() }
            summary[category] = SummaryItem(
                riskLevel = "Yellow",
                justification = "Detected ${matches.size} related paragraphs for \"$category\".",
                snippet = joined
            )
        }
    }

    return summary
}

fun splitTextIntoChunks(text: String, chunkSize: Int = 3000): List<String> {
    val chunks = mutableListOf<String>()
    var start = 0
    while (start < text.length) {
        val end = minOf(start + chunkSize, text.length)
        chunks.add(text.substring(start, end))
        start = end
    }
    return chunks
}

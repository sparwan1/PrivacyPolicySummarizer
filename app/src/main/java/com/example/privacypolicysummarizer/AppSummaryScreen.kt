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
                if (summaryData.isNotEmpty()) {
                    val totalPoints = summaryData.values.sumOf { item ->
                        val scoreStr = riskScoreMap[item.riskLevel] ?: "0/10"
                        val numericScore = scoreStr.substringBefore("/").toIntOrNull() ?: 0
                        numericScore
                    }
                    
                    Text(
                        text = "Privacy Score(User Control): $totalPoints %",
                        style = MaterialTheme.typography.titleLarge,
                        color = when {
                            totalPoints >= summaryData.size * 8 -> Color(0xFF4CAF50)
                            totalPoints >= summaryData.size * 5 -> Color(0xFFFFC107)
                            else -> Color(0xFFF44336)
                        },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    summaryData.forEach { (title, item) ->
                        ExpandableSummaryCard(title = title, item = item)
                    }
                } else if (!isLoading) {
                    Text(
                        text = "😔 We’re sorry, we couldn't load the privacy policy.\nPlease try again later.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp)
                    )
                }
            }

            LaunchedEffect(Unit) {
                val summaryFileName = "${packageName.replace('.', '_')}_summary.json"
                val summaryFile = File(context.filesDir, summaryFileName)
                val policyFile = File(context.filesDir, "${packageName.replace('.', '_')}_privacy_policy.txt")

                // Step 1: Fetch and cache privacy policy if needed
                if (!policyFile.exists()) {
                    println("⚠️ Policy file not found for package: $packageName")
                    val policy = PrivacyPolicyFetcher.fetchPrivacyPolicyContent(packageName)
                    if (policy != null) {
                        println("✅ Fetched and saved policy for $packageName (${policy.toString().length} chars)")
                        policyFile.writeText(policy.toString())
                    } else {
                        println("❌ Failed to fetch policy")
                        isLoading = false
                        return@LaunchedEffect
                    }
                }

                val fullText = policyFile.readText()

                // Step 2: If summary is cached, load and parse it directly
                val jsonString = if (summaryFile.exists()) {
                    println("📂 Using cached summary from $summaryFileName")
                    summaryFile.readText()
                } else {
                    println("📤 Sending policy to LLM...")
                    val result = withContext(Dispatchers.IO) {
                        analyzePolicyViaLLM(fullText)
                    }

                    if (result == null) {
                        println("❌ LLM failed to return a response.")
                        isLoading = false
                        return@LaunchedEffect
                    }

                    try {
                        val outer = JSONObject(result)
                        val responseString = outer.getString("response")
                        val start = responseString.indexOf('{')
                        val end = responseString.lastIndexOf('}')
                        if (start == -1 || end == -1 || end <= start) {
                            println("❌ Could not extract valid JSON block.")
                            isLoading = false
                            return@LaunchedEffect
                        }
                        val cleanJson = responseString.substring(start, end + 1)
                        summaryFile.writeText(cleanJson) // ✅ Cache the summary
                        cleanJson
                    } catch (e: Exception) {
                        println("❌ JSON parse error: ${e.message}")
                        isLoading = false
                        return@LaunchedEffect
                    }
                }

                // Step 3: Parse the summary JSON into SummaryItems
                try {
                    val json = JSONObject(jsonString)
                    val extractedMap = mutableMapOf<String, SummaryItem>()

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
                    println("❌ Final summary parse failed: ${e.message}")
                }

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
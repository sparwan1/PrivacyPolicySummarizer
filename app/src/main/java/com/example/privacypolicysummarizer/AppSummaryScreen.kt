package com.example.privacypolicysummarizer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.example.privacypolicysummarizer.network.PrivacyPolicyFetcher
import java.io.File


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
    "Collecting PII of Children" to Icons.Default.Face, // or use another icon like Icons.Default.ChildCare if available
    "Sharing with Law Enforcement" to Icons.Default.Gavel,
    "Policy Change Notification" to Icons.Default.Notifications,
    "Control of Data" to Icons.Default.Settings,
    "Data Aggregation" to Icons.Default.BarChart
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSummaryScreen(packageName: String) {
    val context = LocalContext.current
    val fileName = "${packageName.replace('.', '_')}_privacy_policy.txt"
    val file = File(context.filesDir, fileName)

    var policyText by remember { mutableStateOf<String?>(null) }
    var summaryMap by remember { mutableStateOf<Map<String, SummaryItem>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(false) }

    // Load or fetch policy at runtime
    LaunchedEffect(Unit) {
        if (file.exists()) {
            println("Loading saved policy for $packageName")
            val text = file.readText()
            policyText = text
            summaryMap = extractSummaryFromText(text)
        } else {
            println("Policy not found, fetching for $packageName")
            isLoading = true
            val result = PrivacyPolicyFetcher.fetchPrivacyPolicyContent(packageName)
            if (result != null) {
                // Save both .txt and .url
                file.writeText(result.plainText)
                File(context.filesDir, "${packageName.replace('.', '_')}_privacy_policy_url.txt").writeText(result.url)

                policyText = result.plainText
                summaryMap = extractSummaryFromText(result.plainText)

                println("Fetched and saved runtime policy for $packageName")
            } else {
                println("Failed to fetch policy for $packageName")
            }
            isLoading = false
        }
    }

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
            val appName = try {
                val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                context.packageManager.getApplicationLabel(appInfo).toString()
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
                CircularProgressIndicator()
            } else if (summaryMap.isEmpty()) {
                Text(
                    text = "No privacy-related information found in policy.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                summaryMap.forEach { (title, item) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = item.snippet,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
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

// At the bottom of AppSummaryScreen.kt (outside of Composables)
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

    for ((category, keywords) in keywordsMap) {
        val paragraphs = policyText.split(Regex("\\n\\s*\\n"))

        val matches = paragraphs.filter { para ->
            keywords.any { keyword ->
                keyword in para.lowercase()
            }
        }

        if (matches.isNotEmpty()) {
            val joined = matches.joinToString("\n\n")
            summary[category] = SummaryItem(
                riskLevel = "Yellow",
                justification = "Detected ${matches.size} paragraphs related to \"$category\".",
                snippet = joined
            )
        }

//        val allMatches = keywords.flatMap { keyword ->
//            val regex = Regex(".{0,300}$keyword.{0,300}", RegexOption.IGNORE_CASE)
//            regex.findAll(policyText).map { it.value }.toList()
//        }
//
//        if (allMatches.isNotEmpty()) {
//            val cleanedMatches = allMatches.map { match ->
//                match.replace("\n", " ")
//                    .replace(Regex("\\s+"), " ")
//                    .trim()
//            }
//
//            val combinedText = cleanedMatches.joinToString("\n\n")
//
//            summary[category] = SummaryItem(
//                riskLevel = "Yellow",
//                justification = "Detected ${cleanedMatches.size} mentions of \"$category\" in the policy.",
//                snippet = combinedText
//            )
//        }
    }

    return summary
}
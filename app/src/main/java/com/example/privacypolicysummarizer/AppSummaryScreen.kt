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


data class SummaryItem(val riskLevel: String, val justification: String, val snippet: String)


val dummySummaryData = mapOf(
    "Email Address" to SummaryItem(
        riskLevel = "Red",
        justification = "The policy mentions that email addresses may be shared with marketing partners.",
        snippet = "We may share your email address with our trusted third-party affiliates for promotional purposes."
    ),
    "Credit Card" to SummaryItem(
        riskLevel = "Yellow",
        justification = "Used for billing only, not shared with third parties.",
        snippet = "We collect your credit card details to process your payments securely."
    ),
    "Location" to SummaryItem(
        riskLevel = "Green",
        justification = "Used only to improve app experience, not shared externally.",
        snippet = "Location helps us personalize app content."
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
    "Credit Card" to Icons.Default.Payment,
    "Location" to Icons.Default.LocationOn
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSummaryScreen(packageName: String) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Privacy Policy Summary") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
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


            dummySummaryData.forEach { (title, item) ->
                ExpandableSummaryCard(title = title, item = item)
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

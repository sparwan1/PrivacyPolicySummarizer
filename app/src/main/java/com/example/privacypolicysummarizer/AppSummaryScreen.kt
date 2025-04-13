package com.example.privacypolicysummarizer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment

data class SummaryItem(val riskLevel: String, val justification: String, val snippet: String)

val dummySummaryData = mapOf(
    "Email Address" to SummaryItem(
        riskLevel = "Red",
        justification = "The policy mentions that email addresses may be shared with marketing partners.",
        snippet = "We may share your email address with our trusted third-party affiliates for promotional purposes."
    ),
    "Credit Card" to SummaryItem(
        riskLevel = "Yellow",
        justification = "The policy states that credit card and billing information is collected and used solely for processing payments.",
        snippet = "We collect your billing address and credit card details to complete your purchase transactions."
    )
    // ... add other items as needed
)

@Composable
fun AppSummaryScreen(packageName: String) {
    val scrollState = rememberScrollState()
    // In a real app, summary data would be fetched based on [packageName]
    val summaryKey = dummySummaryData.keys.firstOrNull() ?: "Email Address"
    val summary = dummySummaryData[summaryKey]!!

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("App Summary") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Text(
                text = "App: $packageName",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            dummySummaryData.forEach { (title, item) ->
                SummaryItemView(title = title, item = item)
            }
        }
    }
}

@Composable
fun SummaryItemView(title: String, item: SummaryItem) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = when(item.riskLevel) {
                                "Green" -> Color(0xFF4CAF50)
                                "Yellow" -> Color(0xFFFFC107)
                                else -> Color(0xFFF44336)
                            }
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "$title - ${if(item.riskLevel == "Green") "10/10" else if(item.riskLevel == "Yellow") "5/10" else "0/10"}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            if(expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = item.justification, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = item.snippet, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}
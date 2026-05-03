package com.b2.ultraprocessed.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.b2.ultraprocessed.ui.theme.DarkBg
import com.b2.ultraprocessed.ui.theme.Emerald400
import com.b2.ultraprocessed.ui.theme.Emerald500
import java.util.Locale

@Composable
fun HistoryScreen(
    historyItems: List<HistoryItemUi>,
    historySummary: HistoryUsageSummaryUi,
    onBack: () -> Unit,
    onClearItem: (HistoryItemUi) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
    ) {
        AppHeader(
            title = "History",
            subtitle = AppBrand.name,
            navigationAction = backHeaderAction(onBack),
        )

        UsageSummaryCard(summary = historySummary)

        Surface(
            color = Color.White.copy(alpha = 0.03f),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Stored locally via Room DB · No cloud sync",
                    color = Color.White.copy(alpha = 0.25f),
                    fontSize = 11.sp,
                )
            }
        }

        if (historyItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No scans yet. Scan or upload an ingredient label to build local history.",
                    color = Color.White.copy(alpha = 0.4f),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 20.dp,
                    vertical = 12.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(historyItems, key = { it.id }) { item ->
                    val verdict = when {
                        item.isBarcodeLookupOnly ->
                            Triple("USDA", Color(0xFF38BDF8), Icons.Default.QrCode)
                        item.novaGroup == 1 ->
                            Triple("PASS", Emerald400, Icons.Default.CheckCircle)
                        item.novaGroup == 2 || item.novaGroup == 3 ->
                            Triple("CAUTION", Color(0xFFFBBF24), Icons.Default.Warning)
                        else ->
                            Triple("AVOID", Color(0xFFF87171), Icons.Default.Warning)
                    }
                    Surface(
                        color = verdict.second.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, verdict.second.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(verdict.second.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(verdict.third, contentDescription = null, tint = verdict.second, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.productName,
                                    color = Color.White.copy(alpha = 0.78f),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (item.isBarcodeLookupOnly) {
                                        "Barcode lookup · ${item.scannedAt}"
                                    } else {
                                        "NOVA ${item.novaGroup} · ${item.scannedAt}"
                                    },
                                    color = Color.White.copy(alpha = 0.25f),
                                    fontSize = 11.sp,
                                )
                                if (item.capturedImagePath != null) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Saved image: ${item.capturedImagePath.substringAfterLast('/')}",
                                        color = Color.White.copy(alpha = 0.22f),
                                        fontSize = 10.sp,
                                    )
                                }
                                if (item.estimatedTokens > 0 || item.modelName.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = listOfNotNull(
                                            item.modelName.takeIf { it.isNotBlank() },
                                            if (item.estimatedTokens > 0) "${formatTokens(item.estimatedTokens)} tokens" else null,
                                            if (item.estimatedCostUsd > 0.0) formatMoney(item.estimatedCostUsd) + " est." else null,
                                        ).joinToString(" · "),
                                        color = Color.White.copy(alpha = 0.22f),
                                        fontSize = 10.sp,
                                    )
                                }
                            }
                            Text(
                                text = verdict.first,
                                color = verdict.second,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.4.sp,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                onClick = { onClearItem(item) },
                                color = Color.White.copy(alpha = 0.04f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color.White.copy(alpha = 0.08f),
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(10.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        AppFooter(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun UsageSummaryCard(
    summary: HistoryUsageSummaryUi,
) {
    Surface(
        color = Color.White.copy(alpha = 0.04f),
        shape = RoundedCornerShape(22.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Emerald500.copy(alpha = 0.12f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(Emerald500.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Money, contentDescription = null, tint = Emerald400, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Usage snapshot",
                        color = Color.White.copy(alpha = 0.72f),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${summary.totalScans} scans · estimated from stored model usage",
                        color = Color.White.copy(alpha = 0.34f),
                        fontSize = 11.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricPill(
                    label = "Tokens",
                    value = formatTokens(summary.totalTokens),
                    modifier = Modifier.weight(1f),
                )
                MetricPill(
                    label = "Estimated cost",
                    value = formatMoney(summary.estimatedCostUsd),
                    modifier = Modifier.weight(1f),
                )
            }

            if (summary.modelUsage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    summary.modelUsage.take(4).forEach { usage ->
                        Surface(
                            color = Color.White.copy(alpha = 0.03f),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                Text(
                                    text = usage.modelName,
                                    color = Color.White.copy(alpha = 0.78f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${usage.scans} scan${if (usage.scans == 1) "" else "s"} · ${formatTokens(usage.estimatedTokens)} tokens · ${formatMoney(usage.estimatedCostUsd)}",
                                    color = Color.White.copy(alpha = 0.32f),
                                    fontSize = 10.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.34f),
                fontSize = 11.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun formatTokens(tokens: Int): String {
    return String.format(Locale.US, "%,d", tokens.coerceAtLeast(0))
}

private fun formatMoney(value: Double): String {
    return String.format(Locale.US, "$%.4f", value.coerceAtLeast(0.0))
}

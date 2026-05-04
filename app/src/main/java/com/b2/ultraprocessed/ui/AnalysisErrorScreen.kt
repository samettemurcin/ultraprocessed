package com.b2.ultraprocessed.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.b2.ultraprocessed.R
import com.b2.ultraprocessed.ui.theme.Amber400
import com.b2.ultraprocessed.ui.theme.DarkBg
import com.b2.ultraprocessed.ui.theme.Emerald500
import com.b2.ultraprocessed.ui.theme.Red400
import com.b2.ultraprocessed.ui.theme.Red500

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnalysisErrorScreen(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
    ) {
        AppHeader(
            title = stringResource(R.string.analysis_error_title),
            subtitle = "Zest",
            navigationAction = backHeaderAction(onRetry),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Red500.copy(alpha = 0.12f), CircleShape)
                    .border(1.dp, Red500.copy(alpha = 0.24f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Red400,
                    modifier = Modifier.size(28.dp),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = message,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = UiTextSizes.Body,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
            )

            if (isRateLimitMessage(message)) {
                Spacer(modifier = Modifier.height(18.dp))
                Surface(
                    color = Color(0x16F59E0B),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44F59E0B)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        UiSectionHeader(
                            text = stringResource(R.string.analysis_rate_limit_title),
                            accentColor = Amber400,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.analysis_rate_limit_body),
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = UiTextSizes.BodySmall,
                            lineHeight = 17.sp,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            InfoChip(stringResource(R.string.analysis_rate_limit_chip_wait))
                            InfoChip(stringResource(R.string.analysis_rate_limit_chip_retry))
                            InfoChip(stringResource(R.string.analysis_rate_limit_chip_usage))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
            ) {
                Text(
                    text = stringResource(R.string.analysis_try_again),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        AppFooter(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp)
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        )
    }
}

@Composable
private fun InfoChip(label: String) {
    Surface(
        color = Color.White.copy(alpha = 0.06f),
        shape = RoundedCornerShape(999.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = UiTextSizes.Caption,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

private fun isRateLimitMessage(message: String): Boolean {
    val lower = message.lowercase()
    return "429" in lower || "rate limit" in lower || "quota exceeded" in lower
}

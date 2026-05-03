package com.b2.ultraprocessed.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.b2.ultraprocessed.ui.theme.DarkBg
import com.b2.ultraprocessed.ui.theme.DarkerBg
import com.b2.ultraprocessed.ui.theme.Amber400
import com.b2.ultraprocessed.ui.theme.Emerald500
import java.io.File
import kotlinx.coroutines.launch

@Composable
fun ResultsScreen(
    result: ScanResultUi,
    onScanAgain: () -> Unit,
    onOpenHistory: () -> Unit,
    chatEnabled: Boolean,
    onAskAboutResult: suspend (String, (String) -> Unit) -> Result<com.b2.ultraprocessed.network.llm.ResultChatReply>,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
    ) {
        AppHeader(
            title = if (result.isBarcodeLookupOnly) "Product found" else "Analysis",
            subtitle = if (result.isBarcodeLookupOnly) {
                "USDA FoodData Central · barcode lookup"
            } else {
                "Nova Classification"
            },
            navigationAction = backHeaderAction(onScanAgain),
            actions = listOf(
                AppHeaderAction(
                    icon = Icons.Default.History,
                    contentDescription = "History",
                    onClick = onOpenHistory,
                    testTag = AppTestTags.HEADER_ACTION_HISTORY,
                ),
            ),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
        ) {
            if (result.isBarcodeLookupOnly) {
                BarcodeLookupResultBody(result = result)
            } else {
                FullAnalysisResultBody(
                    result = result,
                    chatEnabled = chatEnabled,
                    onAskAboutResult = onAskAboutResult,
                )
            }

            Spacer(modifier = Modifier.height(36.dp))
        }

        Surface(
            color = DarkerBg,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Button(
                    onClick = onScanAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                ) {
                    Text(
                        text = if (result.isBarcodeLookupOnly) "Scan again" else "Scan another label",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onOpenHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Default.History, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open history")
                }

                Spacer(modifier = Modifier.height(16.dp))

                AppFooter()

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .width(112.dp)
                            .height(4.dp)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun BarcodeLookupResultBody(result: ScanResultUi) {
    Text(
        text = result.productName,
        color = Color.White.copy(alpha = 0.92f),
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 28.sp,
    )
    if (!result.brandOwner.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = result.brandOwner.orEmpty(),
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    if (!result.scannedBarcode.isNullOrBlank()) {
        Surface(
            color = Color.White.copy(alpha = 0.06f),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "BARCODE",
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = result.scannedBarcode.orEmpty(),
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(14.dp))
    Surface(
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(100.dp),
    ) {
        Text(
            text = "Source: ${result.sourceLabel}",
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = result.summary,
        color = Color.White.copy(alpha = 0.55f),
        fontSize = 15.sp,
        lineHeight = 22.sp,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Database reference, not medical advice. For a full label read, scan the ingredient panel with the camera.",
        color = Color.White.copy(alpha = 0.34f),
        fontSize = 11.sp,
        lineHeight = 16.sp,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FullAnalysisResultBody(
    result: ScanResultUi,
    chatEnabled: Boolean,
    onAskAboutResult: suspend (String, (String) -> Unit) -> Result<com.b2.ultraprocessed.network.llm.ResultChatReply>,
) {
    val verdict = verdictColors(result.novaGroup)
    val confidenceLabel = confidenceBandLabel(result.confidence)
    val headline = shopperHeadline(result.novaGroup)
    val ingredientItems = remember(result.ingredientAssessments) {
        result.ingredientAssessments
    }

    Text(
        text = result.productName,
        color = Color.White.copy(alpha = 0.92f),
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Spacer(modifier = Modifier.height(8.dp))
    Surface(
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(100.dp),
    ) {
        Text(
            text = "Source: ${result.sourceLabel}",
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
    if (!result.brandOwner.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Brand: ${result.brandOwner}",
            color = Color.White.copy(alpha = 0.42f),
            fontSize = 12.sp,
        )
    }
    if (!result.scannedBarcode.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Barcode: ${result.scannedBarcode}",
            color = Color.White.copy(alpha = 0.42f),
            fontSize = 12.sp,
        )
    }

    Spacer(modifier = Modifier.height(16.dp))
    ScannedLabelPhotoSection(imagePath = result.labelImagePath)

    Spacer(modifier = Modifier.height(18.dp))

    Surface(
        color = verdict.cardColor,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, verdict.borderColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(verdict.pillColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = verdict.label.take(1),
                    color = verdict.textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = verdict.label,
                color = verdict.textColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = headline,
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp),
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "NOVA ${result.novaGroup} · ${verdict.subLabel}",
                color = Color.White.copy(alpha = 0.32f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp,
            )
            Text(
                text = "$confidenceLabel signal (${(result.confidence * 100).toInt()}%) · ${result.engineLabel}",
                color = Color.White.copy(alpha = 0.28f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp),
            )
        }
    }

    Text(
        text = result.summary,
        color = Color.White.copy(alpha = 0.55f),
        fontSize = 15.sp,
        lineHeight = 22.sp,
    )

    Spacer(modifier = Modifier.height(18.dp))

    Surface(
        color = Color.White.copy(alpha = 0.04f),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "LABEL DETAIL",
                color = Color.White.copy(alpha = 0.32f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
            )
            Spacer(modifier = Modifier.height(10.dp))
            IngredientChips(items = ingredientItems)
            if (result.allergens.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = Color(0x0E38BDF8),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2638BDF8)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "ALLERGENS",
                            color = Color(0xFF7DD3FC),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.4.sp,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            result.allergens.forEach { allergen ->
                                AllergenBubble(allergen)
                            }
                        }
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    ResultChatSection(
        enabled = chatEnabled,
        onAskAboutResult = onAskAboutResult,
        result = result,
    )

    Spacer(modifier = Modifier.height(16.dp))

    if (result.warnings.isNotEmpty()) {
        DataWarningBlock(warnings = result.warnings)
        Spacer(modifier = Modifier.height(16.dp))
    }

    Text(
        text = "NOVA groups foods by processing (1 = minimal → 4 = ultra-processed). This app analyzes visible label evidence and is not a full nutrition or allergen audit.",
        color = Color.White.copy(alpha = 0.34f),
        fontSize = 11.sp,
        lineHeight = 16.sp,
    )
}

@Composable
private fun ScannedLabelPhotoSection(imagePath: String?) {
    if (imagePath.isNullOrBlank()) return
    val file = File(imagePath)
    if (!file.isFile || !file.canRead()) return

    val bitmap = remember(imagePath) { decodeSampledBitmap(imagePath, maxSidePx = 960) }
    if (bitmap == null) return

    Text(
        text = "YOUR SCAN",
        color = Color.White.copy(alpha = 0.32f),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Surface(
        color = Color.White.copy(alpha = 0.04f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Scanned label photo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This is the image analyzed for ingredient extraction. Zoom the real ingredient list when you can.",
                color = Color.White.copy(alpha = 0.38f),
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
        }
    }
}

private fun decodeSampledBitmap(path: String, maxSidePx: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / sample > maxSidePx || bounds.outHeight / sample > maxSidePx) {
        sample *= 2
    }
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    return BitmapFactory.decodeFile(path, opts)
}

private fun confidenceBandLabel(confidence: Float): String = when {
    confidence >= 0.75f -> "Strong"
    confidence >= 0.55f -> "Moderate"
    else -> "Low"
}

private fun shopperHeadline(novaGroup: Int): String = when (novaGroup) {
    1 -> "Likely a simpler choice—fewer industrial additives flagged on this list."
    2, 3 -> "Processed—compare with shorter lists and fewer additives when you can."
    else -> "High ultra-processing risk on this label—worth swapping if you want to cut UPFs."
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IngredientChips(
    items: List<IngredientBubbleUi>,
) {
    if (items.isEmpty()) {
        Text(
            text = "Per-ingredient NOVA data was not returned by the API for this scan.",
            color = Color.White.copy(alpha = 0.38f),
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
        return
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { item ->
            IngredientBubble(
                item = item,
            )
        }
    }
}

@Composable
private fun IngredientBubble(
    item: IngredientBubbleUi,
) {
    val palette = ingredientPalette(item.novaGroup)
    Surface(
        color = palette.fill,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.border),
        modifier = Modifier.widthIn(max = 180.dp),
    ) {
        Text(
            text = item.name,
            color = palette.text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 15.sp,
            modifier = Modifier
                .widthIn(max = 180.dp)
                .padding(horizontal = 10.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun AllergenBubble(label: String) {
    Surface(
        color = Color(0x1438BDF8),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2D38BDF8)),
        modifier = Modifier.widthIn(max = 180.dp),
    ) {
        Text(
            text = label,
            color = Color(0xFFBDE7FF),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 15.sp,
            modifier = Modifier
                .widthIn(max = 180.dp)
                .padding(horizontal = 10.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun DataWarningBlock(warnings: List<String>) {
    Surface(
        color = Color(0x16F59E0B),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44F59E0B)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "DATA WARNING",
                color = Amber400,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Spacer(modifier = Modifier.height(6.dp))
            warnings.forEach { warning ->
                Text(
                    text = warning,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ResultChatSection(
    enabled: Boolean,
    result: ScanResultUi,
    onAskAboutResult: suspend (String, (String) -> Unit) -> Result<com.b2.ultraprocessed.network.llm.ResultChatReply>,
) {
    val scope = rememberCoroutineScope()
    var input by remember(result.productName, result.rawIngredientText, result.summary) { mutableStateOf("") }
    var isSending by remember(result.productName, result.rawIngredientText, result.summary) { mutableStateOf(false) }
    var statusMessage by remember(result.productName, result.rawIngredientText, result.summary) { mutableStateOf<String?>(null) }
    val messages = remember(result.productName, result.rawIngredientText, result.summary) {
        mutableStateListOf<ResultChatMessageUi>()
    }

    Text(
        text = "ASK ABOUT THIS RESULT",
        color = Color.White.copy(alpha = 0.32f),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Surface(
        color = Color.White.copy(alpha = 0.04f),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Ask only about this scan. The assistant is locked to the current result and refuses unrelated or injected instructions.",
                color = Color.White.copy(alpha = 0.42f),
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = Color.White.copy(alpha = 0.03f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 260.dp),
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (messages.isEmpty()) {
                        Text(
                            text = "Try asking what ingredient is the biggest concern, whether any allergens were detected, or how the NOVA group was chosen.",
                            color = Color.White.copy(alpha = 0.36f),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                        )
                    } else {
                        messages.forEach { message ->
                            ChatMessageBubble(message = message)
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }

            if (!statusMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = statusMessage.orEmpty(),
                    color = Amber400.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                enabled = enabled && !isSending,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = if (enabled) {
                            "Ask about this scan only"
                        } else {
                            "Add an API key to enable chat"
                        },
                    )
                },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(16.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "No off-topic questions. No prompt injection.",
                    color = Color.White.copy(alpha = 0.28f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                )
                IconButton(
                    onClick = {
                        val question = input.trim()
                        if (!enabled || isSending || question.isBlank()) return@IconButton
                        messages.add(
                            ResultChatMessageUi(
                                id = "u${messages.size}",
                                role = ResultChatRole.User,
                                text = question,
                            ),
                        )
                        input = ""
                        statusMessage = "Checking the result context..."
                        isSending = true
                        scope.launch {
                            val reply = onAskAboutResult(question) { status ->
                                scope.launch {
                                    statusMessage = status
                                }
                            }
                            isSending = false
                            reply.onSuccess { resultReply ->
                                messages.add(
                                    ResultChatMessageUi(
                                        id = "a${messages.size}",
                                        role = ResultChatRole.Assistant,
                                        text = resultReply.answer,
                                        allowed = resultReply.allowed,
                                    ),
                                )
                                statusMessage = null
                            }.onFailure { error ->
                                messages.add(
                                    ResultChatMessageUi(
                                        id = "e${messages.size}",
                                        role = ResultChatRole.Assistant,
                                        text = error.message.orEmpty().ifBlank {
                                            "The result assistant is temporarily unavailable."
                                        },
                                        allowed = false,
                                    ),
                                )
                                statusMessage = null
                            }
                        }
                    },
                    enabled = enabled && !isSending,
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Emerald500,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send question",
                            tint = Emerald500,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(message: ResultChatMessageUi) {
    val isUser = message.role == ResultChatRole.User
    val palette = if (isUser) {
        ChatPalette(
            fill = Color(0x1810B981),
            border = Color(0x4410B981),
            text = Color.White.copy(alpha = 0.88f),
        )
    } else if (message.allowed) {
        ChatPalette(
            fill = Color.White.copy(alpha = 0.06f),
            border = Color.White.copy(alpha = 0.09f),
            text = Color.White.copy(alpha = 0.82f),
        )
    } else {
        ChatPalette(
            fill = Color(0x16EF4444),
            border = Color(0x44EF4444),
            text = Color(0xFFFCA5A5),
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = palette.fill,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, palette.border),
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Text(
                text = message.text,
                color = palette.text,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
}

private data class ChatPalette(
    val fill: Color,
    val border: Color,
    val text: Color,
    val dot: Color = text,
)

private data class VerdictPalette(
    val label: String,
    val subLabel: String,
    val cardColor: Color,
    val borderColor: Color,
    val pillColor: Color,
    val textColor: Color,
)

private fun verdictColors(novaGroup: Int): VerdictPalette =
    when (novaGroup) {
        1 -> VerdictPalette(
            label = "NOVA 1",
            subLabel = "Minimally processed",
            cardColor = Color(0x1622C55E),
            borderColor = Color(0x4422C55E),
            pillColor = Color(0x2422C55E),
            textColor = Color(0xFF4ADE80),
        )
        2, 3 -> VerdictPalette(
            label = "NOVA 2-3",
            subLabel = "Processed",
            cardColor = Color(0x16F59E0B),
            borderColor = Color(0x44F59E0B),
            pillColor = Color(0x24F59E0B),
            textColor = Color(0xFFFBBF24),
        )
        else -> VerdictPalette(
            label = "NOVA 4",
            subLabel = "Ultra-processed",
            cardColor = Color(0x16EF4444),
            borderColor = Color(0x44EF4444),
            pillColor = Color(0x24EF4444),
            textColor = Color(0xFFF87171),
        )
    }

private data class IngredientPalette(
    val fill: Color,
    val border: Color,
    val text: Color,
)

private fun ingredientPalette(novaGroup: Int): IngredientPalette =
    when (novaGroup) {
        1 -> IngredientPalette(
            fill = Color(0x1422C55E),
            border = Color(0x3322C55E),
            text = Color(0xFFD1FAE5),
        )
        2 -> IngredientPalette(
            fill = Color(0x14F59E0B),
            border = Color(0x33F59E0B),
            text = Color(0xFFFDE68A),
        )
        3 -> IngredientPalette(
            fill = Color(0x16FB923C),
            border = Color(0x44FB923C),
            text = Color(0xFFFED7AA),
        )
        else -> IngredientPalette(
            fill = Color(0x16EF4444),
            border = Color(0x44EF4444),
            text = Color(0xFFFCA5A5),
        )
    }

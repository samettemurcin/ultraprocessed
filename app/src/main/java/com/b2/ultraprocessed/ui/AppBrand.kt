package com.b2.ultraprocessed.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.b2.ultraprocessed.ui.theme.Emerald400
import com.b2.ultraprocessed.ui.theme.Emerald600

object AppBrand {
    const val name = "Zest"
    const val monogram = "Z"
    const val subtitle = "Food reality check"
    const val loadingLine = "Scan labels. Decode additives. Make faster calls."
}

@Composable
fun AppBrandMark(
    modifier: Modifier = Modifier,
    sizeDp: Int = 40,
    fontSizeSp: Int = 16,
) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .background(
                Brush.linearGradient(listOf(Emerald400, Emerald600)),
                RoundedCornerShape((sizeDp * 0.35f).dp),
            )
            .border(
                1.dp,
                Color.White.copy(alpha = 0.12f),
                RoundedCornerShape((sizeDp * 0.35f).dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = AppBrand.monogram,
            color = Color.Black,
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSizeSp.sp,
        )
    }
}

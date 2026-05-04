package com.b2.ultraprocessed.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.b2.ultraprocessed.ui.theme.Emerald400
import com.b2.ultraprocessed.ui.theme.Emerald600

object AppBrand {
    const val name = "Zest"
    const val subtitle = "Healthier Picks"
    const val loadingLine = "Scan labels. Decode additives. Make faster calls."
}

@Composable
fun AppBrandMark(
    modifier: Modifier = Modifier,
    sizeDp: Int = 40,
    iconScale: Float = 0.46f,
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
        FoodDropletIcon(
            modifier = Modifier.size((sizeDp * iconScale).dp),
        )
    }
}

@Composable
fun FoodDropletIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.50f, h * 0.06f)
            cubicTo(w * 0.28f, h * 0.18f, w * 0.30f, h * 0.38f, w * 0.38f, h * 0.50f)
            cubicTo(w * 0.46f, h * 0.62f, w * 0.28f, h * 0.72f, w * 0.34f, h * 0.88f)
            cubicTo(w * 0.42f, h * 1.04f, w * 0.58f, h * 1.04f, w * 0.66f, h * 0.88f)
            cubicTo(w * 0.74f, h * 0.72f, w * 0.54f, h * 0.62f, w * 0.62f, h * 0.50f)
            cubicTo(w * 0.70f, h * 0.38f, w * 0.72f, h * 0.18f, w * 0.50f, h * 0.06f)
            close()
        }
        drawPath(path, Color.Black.copy(alpha = 0.90f))
        drawCircle(
            color = Color.Black.copy(alpha = 0.90f),
            radius = w * 0.07f,
            center = androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.24f),
        )
    }
}

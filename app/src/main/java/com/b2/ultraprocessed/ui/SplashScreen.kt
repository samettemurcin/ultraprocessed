package com.b2.ultraprocessed.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.b2.ultraprocessed.BuildConfig
import com.b2.ultraprocessed.ui.audio.AppSoundEvent
import com.b2.ultraprocessed.ui.theme.DarkerBg
import com.b2.ultraprocessed.ui.theme.Emerald400
import com.b2.ultraprocessed.ui.theme.Emerald500
import com.b2.ultraprocessed.ui.theme.SpaceGroteskFontFamily
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    displayDurationMillis: Long = 4_200L,
    onComplete: () -> Unit,
    onSoundEffect: (AppSoundEvent) -> Unit = {},
) {
    val transition = rememberInfiniteTransition(label = "splash")
    val ambientScale by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.26f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ambient-scale",
    )
    val logoScale by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "logo-scale",
    )
    val loadingAlpha by transition.animateFloat(
        initialValue = 0.28f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "loading-alpha",
    )

    LaunchedEffect(Unit) {
        onSoundEffect(AppSoundEvent.Startup)
        delay(displayDurationMillis.coerceAtLeast(0L))
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkerBg),
    ) {
        Box(
            modifier = Modifier
                .size(520.dp)
                .align(Alignment.Center)
                .scale(ambientScale)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Emerald500.copy(alpha = 0.10f), Color.Transparent),
                    ),
                ),
        )

        AppBrandMark(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 320.dp)
                .scale(logoScale),
            sizeDp = 88,
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 56.dp, start = 28.dp, end = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Benevolent",
                color = Color.White.copy(alpha = 0.90f),
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = UiTextSizes.HeroValue,
                letterSpacing = (-0.6).sp,
            )
            Text(
                text = "Bandwidth",
                color = Emerald400,
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = UiTextSizes.HeroValue,
                letterSpacing = (-0.6).sp,
            )
            Box(
                modifier = Modifier
                    .padding(vertical = 24.dp)
                    .size(width = 48.dp, height = 2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Emerald500, Color.Transparent),
                        ),
                    ),
            )
            Text(
                text = "PRESENTS",
                color = Color.White.copy(alpha = 0.42f),
                fontFamily = SpaceGroteskFontFamily,
                fontSize = UiTextSizes.Caption,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = AppBrand.name,
                color = Color.White.copy(alpha = 0.86f),
                fontFamily = SpaceGroteskFontFamily,
                fontSize = UiTextSizes.Display,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = AppBrand.subtitle,
                modifier = Modifier.widthIn(max = 280.dp),
                color = Color.White.copy(alpha = 0.28f),
                fontSize = UiTextSizes.BodySmall,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(30.dp))

            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Emerald400.copy(alpha = loadingAlpha), CircleShape),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppFooter()
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "v${BuildConfig.VERSION_NAME} · Android · Kotlin + Jetpack Compose",
                color = Color.White.copy(alpha = 0.12f),
                fontSize = UiTextSizes.Micro,
            )
        }
    }
}

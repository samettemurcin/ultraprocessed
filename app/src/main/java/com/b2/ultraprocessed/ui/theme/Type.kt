package com.b2.ultraprocessed.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.b2.ultraprocessed.R

val InterFontFamily = FontFamily(
    Font(R.font.inter_variable, FontWeight.Light),
    Font(R.font.inter_variable, FontWeight.Normal),
    Font(R.font.inter_variable, FontWeight.Medium),
    Font(R.font.inter_variable, FontWeight.SemiBold),
    Font(R.font.inter_variable, FontWeight.Bold),
    Font(R.font.inter_variable, FontWeight.ExtraBold),
)

val SpaceGroteskFontFamily = FontFamily(
    Font(R.font.space_grotesk_variable, FontWeight.Normal),
    Font(R.font.space_grotesk_variable, FontWeight.Medium),
    Font(R.font.space_grotesk_variable, FontWeight.SemiBold),
    Font(R.font.space_grotesk_variable, FontWeight.Bold),
)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.2.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 25.sp,
        lineHeight = 32.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.8.sp,
        lineHeight = 16.sp,
    ),
)

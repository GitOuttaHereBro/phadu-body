package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// System font stack rendered natively
val SystemFontStack = FontFamily.SansSerif

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = SystemFontStack,
        fontWeight = FontWeight.Black,
        fontSize = 48.sp,
        lineHeight = 48.sp,
        letterSpacing = (-1.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = SystemFontStack,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-1).sp
    ),
    titleLarge = TextStyle(
        fontFamily = SystemFontStack,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.5).sp
    ),
    titleMedium = TextStyle(
        fontFamily = SystemFontStack,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = SystemFontStack,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 26.sp, // Generous line height
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SystemFontStack,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 22.sp, // Generous line height
        letterSpacing = 0.25.sp
    ),
    labelLarge = TextStyle(
        fontFamily = SystemFontStack,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = SystemFontStack,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.sp
    )
)

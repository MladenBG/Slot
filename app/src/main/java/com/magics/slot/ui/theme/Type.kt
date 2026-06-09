package com.magics.slot.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val NeonFont = FontFamily.Monospace

val Typography = Typography(
    displayLarge  = TextStyle(fontFamily=NeonFont, fontWeight=FontWeight.Black, fontSize=52.sp, letterSpacing=2.sp, color=NeonGold),
    displayMedium = TextStyle(fontFamily=NeonFont, fontWeight=FontWeight.Bold,  fontSize=36.sp, letterSpacing=1.sp, color=NeonCyan),
    displaySmall  = TextStyle(fontFamily=NeonFont, fontWeight=FontWeight.Bold,  fontSize=28.sp, color=TextPrimary),
    headlineLarge = TextStyle(fontFamily=NeonFont, fontWeight=FontWeight.Bold,  fontSize=24.sp, color=NeonGold),
    headlineMedium= TextStyle(fontFamily=NeonFont, fontWeight=FontWeight.Bold,  fontSize=20.sp, color=TextPrimary),
    titleLarge    = TextStyle(fontFamily=NeonFont, fontWeight=FontWeight.Bold,  fontSize=18.sp, letterSpacing=0.15.sp, color=NeonCyan),
    titleMedium   = TextStyle(fontFamily=NeonFont, fontWeight=FontWeight.Medium,fontSize=14.sp, color=TextPrimary),
    bodyLarge     = TextStyle(fontFamily=NeonFont, fontWeight=FontWeight.Normal,fontSize=16.sp, color=TextPrimary),
    bodyMedium    = TextStyle(fontFamily=NeonFont, fontWeight=FontWeight.Normal,fontSize=14.sp, color=TextSec),
    labelLarge    = TextStyle(fontFamily=NeonFont, fontWeight=FontWeight.Bold,  fontSize=14.sp, letterSpacing=1.5.sp, color=DeepSpace),
    labelMedium   = TextStyle(fontFamily=NeonFont, fontWeight=FontWeight.Medium,fontSize=12.sp, letterSpacing=1.sp, color=TextPrimary),
    labelSmall    = TextStyle(fontFamily=NeonFont, fontWeight=FontWeight.Medium,fontSize=10.sp, letterSpacing=0.5.sp, color=TextSec),
)

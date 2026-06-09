package com.magics.slot.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magics.slot.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
//  NeonButton.kt  –  package com.magics.slot.ui.components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NeonButton(
    text      : String,
    onClick   : () -> Unit,
    modifier  : Modifier = Modifier,
    glowColor : Color    = NeonCyan,
    textColor : Color    = DeepSpace,
    enabled   : Boolean  = true,
    height    : Dp       = 48.dp,
    shape     : Shape    = RoundedCornerShape(12.dp),
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()

    val inf = rememberInfiniteTransition(label = "btn")
    val pulseScale by inf.animateFloat(1f, 1.04f, infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse), label = "bs")
    val glowA      by inf.animateFloat(0.4f, 0.9f, infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse), label = "ga")

    val scale = if (!enabled || pressed) 0.95f else pulseScale

    Box(
        modifier = modifier
            .scale(scale)
            .height(height)
            .clip(shape)
            .background(if (enabled)
                Brush.horizontalGradient(listOf(glowColor.copy(0.9f), glowColor.copy(0.6f)))
            else
                Brush.horizontalGradient(listOf(Color.Gray.copy(0.3f), Color.Gray.copy(0.2f)))
            )
            .border(1.5.dp,
                Brush.horizontalGradient(listOf(glowColor.copy(glowA), Color.White.copy(glowA * 0.2f), glowColor.copy(glowA))),
                shape)
            .clickable(src, null, enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = TextStyle(
            fontFamily    = NeonFont,
            fontWeight    = FontWeight.Black,
            fontSize      = 13.sp,
            letterSpacing = 2.sp,
            color         = if (enabled) textColor else Color.Gray,
            textAlign     = TextAlign.Center,
        ))
    }
}

// ── Circular SPIN button ──────────────────────────────────────────────────────
@Composable
fun SpinButton(
    onClick  : () -> Unit,
    spinning : Boolean,
    enabled  : Boolean,
    modifier : Modifier = Modifier,
    size     : Dp       = 92.dp,
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()

    val inf = rememberInfiniteTransition(label = "spin")
    val glow by inf.animateFloat(0.5f, 1f, infiniteRepeatable(tween(600, easing = EaseInOutSine), RepeatMode.Reverse), label = "sg")
    val scale = if (pressed) 0.92f else if (!spinning) 1f + (glow - 0.75f) * 0.06f else 1f

    Box(modifier = modifier.size(size).scale(scale), contentAlignment = Alignment.Center) {
        // Glow halo
        Box(Modifier.size(size + 12.dp).clip(CircleShape)
            .background(Brush.radialGradient(listOf(NeonCyan.copy(glow * 0.3f), Color.Transparent))))
        // Button
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Brush.radialGradient(
                    if (enabled) listOf(NeonCyan, NeonBlue) else listOf(Color.Gray, Color.DarkGray)
                ))
                .border(2.dp, Brush.sweepGradient(listOf(NeonCyan, NeonMagenta, NeonGold, NeonCyan)), CircleShape)
                .clickable(src, null, enabled && !spinning) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (spinning) "◼" else "SPIN",
                style = TextStyle(
                    fontFamily    = NeonFont,
                    fontWeight    = FontWeight.Black,
                    fontSize      = if (spinning) 22.sp else 16.sp,
                    letterSpacing = 1.sp,
                    color         = DeepSpace,
                    textAlign     = TextAlign.Center,
                )
            )
        }
    }
}

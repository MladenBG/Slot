package com.magics.slot.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magics.slot.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
//  BalanceCard.kt  –  package com.magics.slot.ui.components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BalanceCard(
    label     : String,
    amount    : Double,
    modifier  : Modifier = Modifier,
    glowColor : Color    = NeonCyan,
) {
    val animAmt by animateFloatAsState(amount.toFloat(), tween(500), label = "bal")

    val inf = rememberInfiniteTransition(label = "card")
    val ba  by inf.animateFloat(0.3f, 0.7f, infiniteRepeatable(tween(1200, easing = EaseInOutSine), RepeatMode.Reverse), label = "bA")

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(glowColor.copy(0.08f), SurfaceGlass)))
            .border(1.dp,
                Brush.linearGradient(listOf(glowColor.copy(ba), Color.Transparent, glowColor.copy(ba * 0.5f))),
                RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label.uppercase(), style = TextStyle(
                fontFamily = NeonFont, fontWeight = FontWeight.Medium,
                fontSize = 8.sp, letterSpacing = 2.sp, color = glowColor.copy(0.7f)
            ))
            Spacer(Modifier.height(2.dp))
            Text(String.format("%.2f", animAmt), style = TextStyle(
                fontFamily = NeonFont, fontWeight = FontWeight.Black,
                fontSize = 19.sp, letterSpacing = 0.5.sp,
                color = glowColor, textAlign = TextAlign.Center
            ))
        }
    }
}

// ── Win flash card ────────────────────────────────────────────────────────────
@Composable
fun WinCard(win: Float, modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "win")
    val g   by inf.animateFloat(0.6f, 1f, infiniteRepeatable(tween(400, easing = EaseInOutSine), RepeatMode.Reverse), label = "wg")

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.radialGradient(listOf(NeonGold.copy(0.25f), SurfaceGlass)))
            .border(2.dp, Brush.sweepGradient(listOf(NeonGold, NeonMagenta, NeonCyan, NeonGold)), RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("WIN!", style = TextStyle(fontFamily = NeonFont, fontWeight = FontWeight.Black,
                fontSize = 10.sp, letterSpacing = 3.sp, color = NeonGold.copy(g)))
            Text("+${String.format("%.2f", win)}", style = TextStyle(fontFamily = NeonFont,
                fontWeight = FontWeight.Black, fontSize = 28.sp, color = NeonGold))
        }
    }
}

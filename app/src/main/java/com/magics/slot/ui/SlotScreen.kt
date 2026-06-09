package com.magics.slot.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.magics.slot.*
import com.magics.slot.ui.components.*
import com.magics.slot.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
//  SlotScreen.kt  –  package com.magics.slot.ui
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SlotScreen(viewModel: SlotViewModel) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val spinning = ui.spinState == SpinState.SPINNING

    Box(Modifier.fillMaxSize().background(DeepSpace)) {

        // Layer 1 – Native OpenGL reels (full screen)
        ReelSurface(Modifier.fillMaxSize())

        // Layer 2 – Compose HUD
        Column(
            Modifier.fillMaxSize().systemBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Top bar ───────────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(12.dp, 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Logo
                Text("MAGICS\nSLOT", style = TextStyle(
                    fontFamily = NeonFont, fontWeight = FontWeight.Black,
                    fontSize = 13.sp, letterSpacing = 3.sp, lineHeight = 15.sp,
                    color = NeonCyan, textAlign = TextAlign.Center
                ))

                BalanceCard("Balance", ui.balance, Modifier.weight(1f).padding(horizontal = 8.dp), NeonGold)

                Column(horizontalAlignment = Alignment.End) {
                    IconBtn(if (ui.isMuted) "🔇" else "🔊") { viewModel.toggleMute() }
                    Spacer(Modifier.height(4.dp))
                    IconBtn(if (ui.postFX) "✨" else "▪") { viewModel.togglePostFX() }
                }

                if (ui.freeSpinsLeft > 0) FreeBadge(ui.freeSpinsLeft)
            }

            Spacer(Modifier.weight(1f))

            // ── Win card (center) ─────────────────────────────────────────────
            AnimatedVisibility(
                ui.showWin,
                enter = scaleIn(tween(220)) + fadeIn(),
                exit  = scaleOut(tween(280)) + fadeOut()
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    WinCard(ui.lastWin, Modifier.padding(horizontal = 48.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Bottom controls ────────────────────────────────────────────────
            BottomPanel(ui, spinning,
                onSpin      = { viewModel.spin() },
                onBetUp     = { viewModel.betUp() },
                onBetDown   = { viewModel.betDown() },
                onAutoSpin  = { viewModel.toggleAutoSpin() },
            )
        }

        // Layer 3 – Jackpot overlay
        AnimatedVisibility(
            ui.spinState == SpinState.JACKPOT,
            enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit  = scaleOut() + fadeOut()
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(0.88f)), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎰", fontSize = 80.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("MEGA JACKPOT!", style = TextStyle(fontFamily = NeonFont,
                        fontWeight = FontWeight.Black, fontSize = 34.sp,
                        letterSpacing = 4.sp, color = NeonGold, textAlign = TextAlign.Center))
                    Spacer(Modifier.height(8.dp))
                    Text("+${ui.lastWin.toInt()} KREDITA", style = TextStyle(fontFamily = NeonFont,
                        fontWeight = FontWeight.Black, fontSize = 26.sp,
                        color = NeonMagenta, textAlign = TextAlign.Center))
                }
            }
        }

        // Status message
        if (ui.message.isNotEmpty() && ui.spinState != SpinState.JACKPOT) {
            Box(Modifier.fillMaxWidth().align(Alignment.Center).padding(horizontal = 32.dp), Alignment.Center) {
                Text(ui.message, style = TextStyle(fontFamily = NeonFont, fontWeight = FontWeight.Bold,
                    fontSize = 14.sp, letterSpacing = 1.sp, color = NeonCyan, textAlign = TextAlign.Center))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BottomPanel(
    ui       : SlotUiState,
    spinning : Boolean,
    onSpin   : () -> Unit,
    onBetUp  : () -> Unit,
    onBetDown: () -> Unit,
    onAutoSpin: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Brush.verticalGradient(listOf(SurfaceGlass, SurfaceDark)))
            .border(1.dp,
                Brush.horizontalGradient(listOf(Color.Transparent, NeonCyan.copy(0.4f), Color.Transparent)),
                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(16.dp, 18.dp)
    ) {
        Column {
            // Stat row
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
                BalanceCard("Bet/Line",  ui.bet,      Modifier.weight(1f), NeonPurple)
                Spacer(Modifier.width(6.dp))
                BalanceCard("Total Bet", ui.totalBet, Modifier.weight(1f), NeonMagenta)
                Spacer(Modifier.width(6.dp))
                BalanceCard("Lines",     20.0,        Modifier.weight(0.7f), NeonBlue)
            }
            Spacer(Modifier.height(14.dp))

            // Action row
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
                // Bet column
                Column(Modifier.weight(1f), Arrangement.spacedBy(6.dp)) {
                    NeonButton("BET ▲", onBetUp,   Modifier.fillMaxWidth(), NeonCyan,    enabled = !spinning, height = 42.dp)
                    NeonButton("BET ▼", onBetDown, Modifier.fillMaxWidth(), NeonBlue,    enabled = !spinning, height = 42.dp)
                }
                Spacer(Modifier.width(14.dp))
                // SPIN
                SpinButton(onSpin, spinning, !spinning && ui.balance >= ui.totalBet, size = 96.dp)
                Spacer(Modifier.width(14.dp))
                // Auto / max column
                Column(Modifier.weight(1f), Arrangement.spacedBy(6.dp)) {
                    NeonButton(
                        if (ui.autoSpin) "AUTO ◼" else "AUTO ▶",
                        onAutoSpin,
                        Modifier.fillMaxWidth(),
                        if (ui.autoSpin) NeonRed else NeonGreen,
                        height = 42.dp
                    )
                    NeonButton("MAX BET", {}, Modifier.fillMaxWidth(), NeonGold, enabled = !spinning, height = 42.dp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun IconBtn(icon: String, onClick: () -> Unit) {
    Box(
        Modifier.size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceGlass)
            .border(1.dp, NeonCyan.copy(0.3f), RoundedCornerShape(8.dp))
            .clickable(MutableInteractionSource(), null) { onClick() },
        Alignment.Center
    ) { Text(icon, fontSize = 14.sp) }
}

@Composable
private fun FreeBadge(count: Int) {
    Box(
        Modifier.clip(RoundedCornerShape(8.dp))
            .background(Brush.linearGradient(listOf(NeonMagenta.copy(0.3f), NeonCyan.copy(0.2f))))
            .border(1.dp, NeonMagenta, RoundedCornerShape(8.dp))
            .padding(8.dp, 4.dp),
        Alignment.Center
    ) {
        Text("FREE\n×$count", style = TextStyle(fontFamily = NeonFont, fontWeight = FontWeight.Black,
            fontSize = 9.sp, letterSpacing = 1.sp, lineHeight = 12.sp,
            color = NeonMagenta, textAlign = TextAlign.Center))
    }
}

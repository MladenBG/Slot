package com.magics.slot.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.magics.slot.*
import com.magics.slot.ui.components.*
import com.magics.slot.ui.theme.*
import java.util.Locale

@Composable
fun SlotScreen(viewModel: SlotViewModel, onBackToLobby: () -> Unit) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val spinning = ui.spinState == SpinState.SPINNING

    Box(Modifier.fillMaxSize()) {
        // Layer 1 - Compose Reels Background
        ComposeReels(
            grid = ui.grid, 
            spinning = spinning, 
            reelCount = viewModel.currentSlotType.reels,
            slotType = viewModel.currentSlotType,
            isJackpot = ui.spinState == SpinState.JACKPOT || ui.isJackpot,
            modifier = Modifier.fillMaxSize()
        )

        // Layer 2 – Compose HUD
        Column(
            Modifier.fillMaxSize().systemBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Top bar (Sleek HUD, center space open for machine topper) ──────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left: Exit Button
                androidx.compose.material3.Button(
                    onClick = onBackToLobby,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("◀ EXIT", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }

                // Center Spacer (leaves machine jackpot marquee 100% visible!)
                Spacer(Modifier.weight(1f))

                // Right: Compact Balance Pill & Audio Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Balance Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F0B18).copy(alpha = 0.85f))
                            .border(1.dp, NeonGold, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("BALANCE", color = Color(0xFFFFD700), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text("$${String.format(Locale.US, "%.2f", ui.balance)}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconBtn(if (ui.isMuted) "🔇" else "🔊") { viewModel.toggleMute() }
                        IconBtn(if (ui.postFX) "✨" else "▪") { viewModel.togglePostFX() }
                    }
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
            BottomPanel(
                ui = ui,
                spinning = spinning,
                onSpin = { viewModel.spin() },
                onBetUp = { viewModel.betUp() },
                onBetDown = { viewModel.betDown() },
                onAutoSpin = { viewModel.toggleAutoSpin() },
                onMaxBet = { viewModel.maxBet() },
                onHelp = { viewModel.toggleHelp() }
            )
        }

        // Layer 3 – Spectacular Room-Shining MEGA JACKPOT Overlay
        AnimatedVisibility(
            ui.spinState == SpinState.JACKPOT || ui.isJackpot,
            enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit  = scaleOut() + fadeOut()
        ) {
            JackpotRoomGlowOverlay(
                winAmount = ui.lastWin,
                onDismiss = {
                    // Allows player to collect and continue
                }
            )
        }

        // Status message
        if (ui.message.isNotEmpty() && ui.spinState != SpinState.JACKPOT && !ui.isJackpot) {
            Box(Modifier.fillMaxWidth().align(Alignment.Center).padding(horizontal = 32.dp), Alignment.Center) {
                Text(ui.message, style = TextStyle(fontFamily = NeonFont, fontWeight = FontWeight.Bold,
                    fontSize = 14.sp, letterSpacing = 1.sp, color = NeonCyan, textAlign = TextAlign.Center))
            }
        }
        
        // Comprehensive Authentic Casino Rules & Paytable Dialog
        if (ui.showHelp) {
            PaytableAndRulesDialog(onDismiss = { viewModel.toggleHelp() })
        }

        // Game Over Overlay
        AnimatedVisibility(
            ui.spinState == SpinState.GAME_OVER,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(0.92f)), Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        "GAME OVER",
                        style = TextStyle(
                            fontFamily = NeonFont,
                            fontWeight = FontWeight.Black,
                            fontSize = 42.sp,
                            letterSpacing = 3.sp,
                            color = Color(0xFFFF3B30),
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Insufficient Credits.",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Please return to the lobby to refill credits.",
                        color = Color.LightGray.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(28.dp))
                    androidx.compose.material3.Button(
                        onClick = onBackToLobby,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = NeonGold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("◀ BACK TO LOBBY", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Jackpot Room-Shining Ambient Light & Particle Celebration Overlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun JackpotRoomGlowOverlay(
    winAmount: Float,
    onDismiss: () -> Unit
) {
    val infiniteTrans = rememberInfiniteTransition()

    // Rotating Sunburst Laser Rays
    val rayRotation by infiniteTrans.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Pulsating Room Glow
    val ambientPulse by infiniteTrans.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Floating Strobe Light Flash
    val strobeColor by infiniteTrans.animateColor(
        initialValue = Color(0xFFFFD700),
        targetValue = Color(0xFFFF0055),
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Animated Win Counter
    var displayedWin by remember { mutableStateOf(0) }
    LaunchedEffect(winAmount) {
        val target = winAmount.toInt()
        val steps = 30
        for (i in 1..steps) {
            displayedWin = (target * (i.toFloat() / steps)).toInt()
            kotlinx.coroutines.delay(35)
        }
        displayedWin = target
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        // ── GPU Canvas: Rotating Golden Sunburst & Radiant Ambient Room Wash ──
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = maxOf(size.width, size.height) * 1.2f

            // Full-room Radial Glow Wash
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to Color(0xFFFFD700).copy(alpha = ambientPulse * 0.70f),
                    0.3f to Color(0xFFFF8C00).copy(alpha = ambientPulse * 0.45f),
                    0.6f to Color(0xFFFF007F).copy(alpha = ambientPulse * 0.30f),
                    1.0f to Color.Transparent,
                    center = center,
                    radius = radius
                ),
                center = center,
                radius = radius
            )

            // 18 Rotating Sunburst Golden Laser Rays
            val rayCount = 18
            val sweep = (360f / rayCount) * (Math.PI / 180.0)
            val rotRad = rayRotation * (Math.PI / 180.0)

            for (i in 0 until rayCount) {
                val a1 = rotRad + i * sweep
                val a2 = a1 + sweep * 0.5f

                val p1 = Offset(
                    center.x + (radius * Math.cos(a1)).toFloat(),
                    center.y + (radius * Math.sin(a1)).toFloat()
                )
                val p2 = Offset(
                    center.x + (radius * Math.cos(a2)).toFloat(),
                    center.y + (radius * Math.sin(a2)).toFloat()
                )

                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(center.x, center.y)
                    lineTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    close()
                }

                drawPath(
                    path = path,
                    brush = Brush.radialGradient(
                        0.0f to Color(0xFFFFDF00).copy(alpha = 0.35f),
                        0.7f to Color(0xFFFF6600).copy(alpha = 0.15f),
                        1.0f to Color.Transparent,
                        center = center,
                        radius = radius
                    )
                )
            }
        }

        // ── Epic 3D Royal Gold Jackpot Center Card ────────────────────────────
        Box(
            modifier = Modifier
                .padding(24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF2B1A04),
                            Color(0xFF140901),
                            Color(0xFF261502)
                        )
                    )
                )
                .border(
                    3.dp,
                    Brush.linearGradient(
                        listOf(
                            strobeColor,
                            Color(0xFFFFD700),
                            Color(0xFF00FFFF),
                            strobeColor
                        )
                    ),
                    RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 28.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "👑 🎰 👑",
                    fontSize = 36.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "★ MEGA ROYAL JACKPOT ★",
                    style = TextStyle(
                        fontFamily = NeonFont,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        letterSpacing = 3.sp,
                        color = Color(0xFFFFD700),
                        textAlign = TextAlign.Center
                    )
                )

                Text(
                    text = "ALL 5 SEVENS ALIGNED ON ACTIVE PAYLINE!",
                    style = TextStyle(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp,
                        color = Color(0xFF00FFFF),
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(Modifier.height(8.dp))

                // Giant Golden Win Number Box
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF3D2303),
                                    Color(0xFF5E3905),
                                    Color(0xFF3D2303)
                                )
                            )
                        )
                        .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(16.dp))
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "+$displayedWin CREDITS 💰",
                        style = TextStyle(
                            fontFamily = NeonFont,
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp,
                            letterSpacing = 2.sp,
                            color = Color(0xFFFFE57F),
                            textAlign = TextAlign.Center
                        )
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "✨ THE CASINO ROOM SHINES IN YOUR HONOR! ✨",
                    color = Color(0xFFFF8C00),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Comprehensive Casino Paytable & Game Rules Dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PaytableAndRulesDialog(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable { onDismiss() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F0B18))
                .border(2.dp, NeonGold, RoundedCornerShape(20.dp))
                .padding(20.dp)
                .clickable(enabled = false) {}
        ) {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                item {
                    Text(
                        text = "🎰 PRAVILA IGRE I TABELA DOBITAKA 🎰",
                        style = TextStyle(
                            fontFamily = NeonFont,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = 2.sp,
                            color = NeonGold,
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "20 Fiksnih Linija Isplate • Isplate s Leva na Desno",
                        color = Color(0xFF00FFFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Section 1: Paytable
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF181226))
                            .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "TABELA MULTIPLIKATORA (Množi se sa ulogom po liniji):",
                            color = Color(0xFFFFD700),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        PaytableRow("7️⃣ SEVEN (Sedmica)", "5x = 5,000x + MEGA DŽEKPOT 👑", "4x = 200x", "3x = 50x")
                        PaytableRow("🃏 WILD (Džoker)", "5x = 100x (Zamenjuje sve)", "4x = 25x", "3x = 5x")
                        PaytableRow("🌀 SCATTER (Besplatni)", "5x = 50x + 15 Free Spins", "4x = 15x", "3x = 3x (9 FS)")
                        PaytableRow("💎 DIAMOND (Dijamant)", "5x = 50x", "4x = 15x", "3x = 4x")
                        PaytableRow("🏆 BAR 3 (Trostruki)", "5x = 75x", "4x = 20x", "3x = 5x")
                        PaytableRow("🥈 BAR 2 (Dvostruki)", "5x = 40x", "4x = 12x", "3x = 3x")
                        PaytableRow("🥉 BAR 1 (Jednostruki)", "5x = 25x", "4x = 8x", "3x = 2x")
                        PaytableRow("🍒 CHERRY (Višnjica)", "5x = 15x", "4x = 5x", "3x = 1x")
                    }
                }

                // Section 2: Special Features Explained
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF181226))
                            .border(1.dp, Color(0xFF00FFFF).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "POBEDNIČKA PRAVILA I BONUSA:",
                            color = Color(0xFF00FFFF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "• 🃏 WILD: Zamenjuje bilo koji običan simbol kako bi spojio najjaču dobitnu liniju.",
                            color = Color.White,
                            fontSize = 11.sp
                        )

                        Text(
                            text = "• 🌀 BESPLATNI SPINOVI (Free Spins): Kada se pojave 3 ili više SCATTER simbola bilo gde na ekranu, osvajate besplatne spinove (3 simbola = 9 spinova, 4 = 12, 5 = 15). Tokom besplatnih spinova ne troši se vaš novac, a svi dobici se uredno sabiraju!",
                            color = Color(0xFFFF80BF),
                            fontSize = 11.sp
                        )

                        Text(
                            text = "• 👑 MEGA ROYAL DŽEKPOT: Kada spojite 5 Sedmica (7-7-7-7-7) na bilo kojoj liniji (ili 3 na Vegas mašini), osvajate MEGA DŽEKPOT (+5,000x ulog) uz svetlosni šou gde cela soba i aparat sijaju zlatnim zracima!",
                            color = Color(0xFFFFD700),
                            fontSize = 11.sp
                        )
                    }
                }

                // Close Button
                item {
                    androidx.compose.material3.Button(
                        onClick = onDismiss,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = NeonGold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("ZATVORI PRAVILA ✖", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PaytableRow(title: String, five: String, four: String, three: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
        Text(five, color = Color(0xFFFFD700), fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1.8f), textAlign = TextAlign.End)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
fun formatVal(v: Double): String = String.format(Locale.GERMANY, "%,d", v.toLong())

@Composable
private fun BottomPanel(
    ui: SlotUiState,
    spinning: Boolean,
    onSpin: () -> Unit,
    onBetUp: () -> Unit,
    onBetDown: () -> Unit,
    onAutoSpin: () -> Unit,
    onMaxBet: () -> Unit,
    onHelp: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF1C1A2E), Color(0xFF0F0E1A))))
            .border(1.dp, Brush.horizontalGradient(listOf(Color.Transparent, NeonCyan.copy(0.4f), Color.Transparent)), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(8.dp, 16.dp)
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth > 560.dp) {
                // Wide Layout
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
                    ControlsLayout(ui, spinning, onSpin, onBetUp, onBetDown, onAutoSpin, onMaxBet, onHelp)
                }
            } else {
                // Narrow Layout
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
                        GlossyCircleButton(onHelp, listOf(Color(0xFFE52E53), Color(0xFF8B0D22)), size = 48.dp) { Text("i", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                        GlossyCircleButton(onBetDown, listOf(Color(0xFF389BF2), Color(0xFF094E96)), !spinning, size = 48.dp) { Text("-", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                        GoldDisplayPanel("TOTAL BET", formatVal(ui.totalBet))
                        GlossyCircleButton(onBetUp, listOf(Color(0xFF389BF2), Color(0xFF094E96)), !spinning, size = 48.dp) { Text("+", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                        GlossyCircleButton(onMaxBet, listOf(Color(0xFFBA54F5), Color(0xFF590E8F)), !spinning, size = 48.dp) { Text("MAX\nBET", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 10.sp) }
                    }
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
                        CyanDisplayPanel("TOTAL WIN", formatVal(ui.lastWin.toDouble()))
                        GlossyCapsuleButton(onSpin, onAutoSpin, listOf(Color(0xFF1ED760), Color(0xFF07541E)), !spinning || ui.autoSpin) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (ui.autoSpin) "STOP" else "SPIN", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                                Text(if (ui.autoSpin) "AUTOSPINNING" else "HOLD FOR AUTOSPIN", color = Color.White.copy(0.8f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlsLayout(ui: SlotUiState, spinning: Boolean, onSpin: () -> Unit, onBetUp: () -> Unit, onBetDown: () -> Unit, onAutoSpin: () -> Unit, onMaxBet: () -> Unit, onHelp: () -> Unit) {
    GlossyCircleButton(onHelp, listOf(Color(0xFFE52E53), Color(0xFF8B0D22))) { Text("i", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
    GlossyCircleButton(onBetDown, listOf(Color(0xFF389BF2), Color(0xFF094E96)), !spinning) { Text("-", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
    GoldDisplayPanel("TOTAL BET", formatVal(ui.totalBet))
    GlossyCircleButton(onBetUp, listOf(Color(0xFF389BF2), Color(0xFF094E96)), !spinning) { Text("+", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
    CyanDisplayPanel("TOTAL WIN", formatVal(ui.lastWin.toDouble()))
    GlossyCircleButton(onMaxBet, listOf(Color(0xFFBA54F5), Color(0xFF590E8F)), !spinning) { Text("MAX\nBET", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 12.sp) }
    GlossyCapsuleButton(onSpin, onAutoSpin, listOf(Color(0xFF1ED760), Color(0xFF07541E)), !spinning || ui.autoSpin) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (ui.autoSpin) "STOP" else "SPIN", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(if (ui.autoSpin) "AUTOSPINNING" else "HOLD FOR AUTOSPIN", color = Color.White.copy(0.8f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GoldDisplayPanel(label: String, value: String, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(Brush.linearGradient(listOf(Color(0xFFFFF2B2), Color(0xFFD4AF37), Color(0xFF85581A), Color(0xFFD4AF37), Color(0xFFFFF2B2)))).padding(2.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF0F0E0D)).padding(horizontal = 16.dp, vertical = 6.dp), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = Color(0xFFFCD34D), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun CyanDisplayPanel(label: String, value: String, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(Brush.linearGradient(listOf(Color(0xFF00FFFF), Color(0xFF0088FF), Color(0xFF00FFFF)))).padding(2.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF09121F)).padding(horizontal = 24.dp, vertical = 8.dp), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun GlossyCircleButton(onClick: () -> Unit, gradient: List<Color>, enabled: Boolean = true, size: Dp = 56.dp, content: @Composable BoxScope.() -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale = if (pressed) 0.92f else 1.0f
    Box(Modifier.size(size).graphicsLayer { scaleX = scale; scaleY = scale }.clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFFFF2B2), Color(0xFFD4AF37), Color(0xFF85581A), Color(0xFFD4AF37), Color(0xFFFFF2B2)))).padding(2.dp).clip(CircleShape).background(Color(0xFF151515)).padding(1.dp).clip(CircleShape).background(if (enabled) Brush.verticalGradient(gradient) else Brush.verticalGradient(listOf(Color.Gray, Color.DarkGray))).clickable(src, null, enabled) { onClick() }, Alignment.Center) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.45f).align(Alignment.TopCenter).background(Brush.verticalGradient(listOf(Color.White.copy(0.35f), Color.White.copy(0.0f)))))
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlossyCapsuleButton(onClick: () -> Unit, onLongClick: () -> Unit, gradient: List<Color>, enabled: Boolean = true, content: @Composable BoxScope.() -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale = if (pressed) 0.94f else 1.0f
    Box(Modifier.width(130.dp).height(56.dp).graphicsLayer { scaleX = scale; scaleY = scale }.clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(listOf(Color(0xFFFFF2B2), Color(0xFFD4AF37), Color(0xFF85581A), Color(0xFFD4AF37), Color(0xFFFFF2B2)))).padding(2.dp).clip(RoundedCornerShape(26.dp)).background(Color(0xFF151515)).padding(1.dp).clip(RoundedCornerShape(25.dp)).background(if (enabled) Brush.verticalGradient(gradient) else Brush.verticalGradient(listOf(Color.Gray, Color.DarkGray))).combinedClickable(src, null, enabled, onClick = onClick, onLongClick = onLongClick), Alignment.Center) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.45f).align(Alignment.TopCenter).background(Brush.verticalGradient(listOf(Color.White.copy(0.35f), Color.White.copy(0.0f)))))
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun IconBtn(icon: String, onClick: () -> Unit) {
    Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceGlass).border(1.dp, NeonCyan.copy(0.3f), RoundedCornerShape(8.dp)).clickable { onClick() }, Alignment.Center) { Text(icon, fontSize = 14.sp) }
}
@Composable
private fun FreeBadge(count: Int) {
    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Brush.linearGradient(listOf(NeonMagenta.copy(0.3f), NeonCyan.copy(0.2f)))).border(1.dp, NeonMagenta, RoundedCornerShape(8.dp)).padding(8.dp, 4.dp), Alignment.Center) {
        Text("FREE\n×$count", style = TextStyle(fontFamily = NeonFont, fontWeight = FontWeight.Black, fontSize = 9.sp, letterSpacing = 1.sp, lineHeight = 12.sp, color = NeonMagenta, textAlign = TextAlign.Center))
    }
}

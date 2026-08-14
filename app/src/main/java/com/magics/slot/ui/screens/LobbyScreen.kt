package com.magics.slot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magics.slot.SlotType

private data class LobbyCardStyle(
    val bgColors: List<Color>,
    val borderColors: List<Color>,
    val badgeText: String,
    val badgeColor: Color
)

@Composable
fun LobbyScreen(
    balance: Double = 1000.0,
    onRefill: () -> Unit = {},
    onSlotSelected: (SlotType) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF090614),
                        Color(0xFF160C2E),
                        Color(0xFF0C071B),
                        Color(0xFF040209)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            Spacer(Modifier.height(12.dp))

            // ── TOP CASINO HEADER ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF1E1738),
                                Color(0xFF2A1C4F),
                                Color(0xFF120B24)
                            )
                        )
                    )
                    .border(
                        2.dp,
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFFFFD700),
                                Color(0xFFFF8C00),
                                Color(0xFFFFD700)
                            )
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🎰 MAGICS ROYAL CASINO 🎰",
                        color = Color(0xFFFFD700),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF332005))
                            .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "👑 VIP CLUB • HIGH ROLLER LOBBY",
                            color = Color(0xFFFFE4B5),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0A0516))
                            .border(1.dp, Color(0xFF00FFFF).copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "GLOBAL PROGRESSIVE JACKPOT",
                                color = Color(0xFF00E5FF),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = "$1,245,789.50",
                                color = Color(0xFFFFD700),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // ── Player Wallet & Lobby Refill Bar ──────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0A0618))
                                .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Column {
                                Text("YOUR WALLET", color = Color(0xFFFFD700), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text("$${String.format(java.util.Locale.US, "%.2f", balance)}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                            }
                        }

                        Button(
                            onClick = onRefill,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1ED760)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Text("+ REFILL $1,000 💰", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SELECT SLOT MACHINE",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "5 GAMES AVAILABLE",
                    color = Color(0xFFFFD700),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(SlotType.entries) { type ->
                    LuxuryLobbyCard(type = type, onClick = { onSlotSelected(type) })
                }
            }
        }
    }
}

@Composable
fun LuxuryLobbyCard(type: SlotType, onClick: () -> Unit) {
    val style = when (type) {
        SlotType.WILD -> LobbyCardStyle(
            listOf(Color(0xFF38154D), Color(0xFF1F0B2E), Color(0xFF100518)),
            listOf(Color(0xFFFFD700), Color(0xFFBA54F5), Color(0xFFFFD700)),
            "FEATURED 🔥",
            Color(0xFFFF4500)
        )
        SlotType.LAS_VEGAS -> LobbyCardStyle(
            listOf(Color(0xFF3B2A0F), Color(0xFF221706), Color(0xFF100A02)),
            listOf(Color(0xFFFFD700), Color(0xFFFFA500), Color(0xFFFFD700)),
            "CLASSIC VEGAS 🎰",
            Color(0xFFFFD700)
        )
        SlotType.PHARAOH -> LobbyCardStyle(
            listOf(Color(0xFF4A1A10), Color(0xFF2B0F09), Color(0xFF140604)),
            listOf(Color(0xFFFF8C00), Color(0xFFFFD700), Color(0xFFFF8C00)),
            "JACKPOT 💰",
            Color(0xFFFF8C00)
        )
        SlotType.NEON_RUSH -> LobbyCardStyle(
            listOf(Color(0xFF0F3B4A), Color(0xFF08222B), Color(0xFF030F14)),
            listOf(Color(0xFF00FFFF), Color(0xFFFF00FF), Color(0xFF00FFFF)),
            "CYBERPUNK ⚡",
            Color(0xFF00FFFF)
        )
        SlotType.OCEAN -> LobbyCardStyle(
            listOf(Color(0xFF0C2B45), Color(0xFF051726), Color(0xFF020A12)),
            listOf(Color(0xFF00BFFF), Color(0xFF1E90FF), Color(0xFF00BFFF)),
            "BIG WINS 🌊",
            Color(0xFF00BFFF)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(style.bgColors))
            .border(2.dp, Brush.horizontalGradient(style.borderColors), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(style.badgeColor.copy(alpha = 0.25f))
                        .border(1.dp, style.badgeColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = style.badgeText,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "${type.reels} REELS • 20 LINES",
                    color = Color(0xFFFFD700),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = type.title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "MAX WIN 5,000X • 98.6% RTP • ${type.vipBadge}",
                color = Color.LightGray.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(10.dp))

            // ── Table Limits & Starting Money Pill ───────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F0B18).copy(alpha = 0.8f))
                        .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "💰 START: $${type.startingBalance.toInt()}",
                        color = Color(0xFFFFD700),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                val minTotal = type.betSteps.first() * 20.0
                val maxTotal = type.betSteps.last() * 20.0
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F0B18).copy(alpha = 0.8f))
                        .border(1.dp, Color(0xFF00FFFF).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "BET: $${minTotal.toInt()} - $${maxTotal.toInt()}",
                        color = Color(0xFF00FFFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text(
                    text = "PLAY NOW ▶",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}

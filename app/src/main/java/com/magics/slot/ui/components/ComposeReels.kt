package com.magics.slot.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.magics.slot.R
import com.magics.slot.SlotNativeBridge
import com.magics.slot.SlotType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────────────────────────────────────
//  ComposeReels.kt – Ultra High-Performance GPU Canvas Slot Machine Engine
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ComposeReels(
    grid: IntArray,
    spinning: Boolean,
    reelCount: Int = 5,
    slotType: SlotType = SlotType.WILD,
    isJackpot: Boolean = false,
    modifier: Modifier = Modifier
) {
    val rowCount = 3

    // Load symbol ImageBitmaps once (Zero allocations during gameplay)
    val symWild     = ImageBitmap.imageResource(R.drawable.sym_wild)     // 0
    val symScatter  = ImageBitmap.imageResource(R.drawable.sym_scatter)  // 1
    val symSeven    = ImageBitmap.imageResource(R.drawable.sym_seven)    // 2
    val symBar      = ImageBitmap.imageResource(R.drawable.sym_bar)      // 3, 4, 5
    val symDiamond  = ImageBitmap.imageResource(R.drawable.sym_diamond)  // 6
    val symCherry   = ImageBitmap.imageResource(R.drawable.sym_cherry)   // 7

    val symbolBitmaps = remember(symWild, symScatter, symSeven, symBar, symDiamond, symCherry) {
        listOf(
            symWild,    // 0: WILD
            symScatter, // 1: SCATTER
            symSeven,   // 2: SEVEN
            symBar,     // 3: BAR3
            symBar,     // 4: BAR2
            symBar,     // 5: BAR1
            symDiamond, // 6: DIAMOND
            symCherry   // 7: CHERRY
        )
    }

    // 3D Cabinet Theme Graphic
    val bgRes = when (slotType) {
        SlotType.LAS_VEGAS -> R.drawable.bg_vegas_cabinet
        SlotType.PHARAOH -> R.drawable.bg_pharaoh_cabinet
        SlotType.NEON_RUSH -> R.drawable.bg_neon_cabinet
        SlotType.OCEAN -> R.drawable.bg_ocean_cabinet
        else -> R.drawable.bg_cabinet_real
    }

    // Theme ColorFilter
    val colorFilter = when (slotType) {
        SlotType.LAS_VEGAS -> remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.85f); setToScale(1.15f, 1.05f, 0.95f, 1f) }) }
        SlotType.PHARAOH -> remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(1.25f, 1.10f, 0.60f, 1f) }) }
        SlotType.NEON_RUSH -> remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(0.85f, 1.45f, 1.45f, 1f) }) }
        SlotType.OCEAN -> remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(0.65f, 0.90f, 1.35f, 1f) }) }
        else -> null
    }

    // Container background & 3D bevel borders per theme
    val containerBg = when (slotType) {
        SlotType.NEON_RUSH -> Color(0xFF0F0B1E).copy(alpha = 0.95f)
        SlotType.PHARAOH -> Color(0xFF140B04).copy(alpha = 0.95f)
        SlotType.OCEAN -> Color(0xFF041221).copy(alpha = 0.95f)
        SlotType.LAS_VEGAS -> Color(0xFF1A1104).copy(alpha = 0.95f)
        else -> Color(0xFF0D0A1F).copy(alpha = 0.92f)
    }

    val infiniteTrans = rememberInfiniteTransition()
    val jackpotPulseAlpha by infiniteTrans.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val neonBorderBrush = if (isJackpot) {
        Brush.linearGradient(
            listOf(
                Color(0xFFFFD700).copy(alpha = jackpotPulseAlpha),
                Color(0xFFFF0055).copy(alpha = jackpotPulseAlpha),
                Color(0xFF00FFFF).copy(alpha = jackpotPulseAlpha),
                Color(0xFFFFD700).copy(alpha = jackpotPulseAlpha)
            )
        )
    } else {
        when (slotType) {
            SlotType.PHARAOH -> Brush.linearGradient(listOf(Color(0xFFFF2A4B), Color(0xFFFFD700), Color(0xFFFF2A4B)))
            SlotType.NEON_RUSH -> Brush.linearGradient(listOf(Color(0xFFFF00FF), Color(0xFF00FFFF), Color(0xFFFF00FF)))
            SlotType.OCEAN -> Brush.linearGradient(listOf(Color(0xFF00FFFF), Color(0xFF00BFFF), Color(0xFF00E5FF)))
            SlotType.LAS_VEGAS -> Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500), Color(0xFFFFD700)))
            else -> Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFFBA54F5), Color(0xFF00E5FF)))
        }
    }

    // Exact custom dimensions & offsets per machine screen cutout
    val reelBoxWidth = when (slotType) {
        SlotType.OCEAN -> 0.88f
        SlotType.NEON_RUSH -> 0.85f
        SlotType.LAS_VEGAS -> 0.82f
        SlotType.WILD -> 0.78f
        SlotType.PHARAOH -> 0.77f
        else -> 0.82f
    }
    val reelBoxAspect = when (slotType) {
        SlotType.LAS_VEGAS -> 0.98f
        SlotType.OCEAN -> 1.22f
        SlotType.NEON_RUSH -> 1.24f
        SlotType.PHARAOH -> 1.25f
        SlotType.WILD -> 1.33f
        else -> 1.30f
    }
    val reelOffsetY = when (slotType) {
        SlotType.OCEAN -> (-10).dp
        SlotType.NEON_RUSH -> (-16).dp
        SlotType.LAS_VEGAS -> 74.dp
        SlotType.PHARAOH -> 22.dp
        else -> 8.dp
    }

    // ── Persistent Reel State & Animators ───────────────────────────────────────
    // Holds the currently visible symbols for all 5 reels (3 symbols each)
    var currentSymbols by remember {
        mutableStateOf(IntArray(15) { idx -> grid.getOrElse(idx) { 0 } })
    }

    // Per-reel scroll position animators (Float values: from stepCount down to 0f)
    val animators = remember { List(5) { Animatable(0f) } }
    val reelStrips = remember { List(5) { mutableListOf<Int>() } }

    LaunchedEffect(spinning) {
        if (spinning) {
            // Build continuous physical strips for each reel
            for (r in 0 until reelCount) {
                val target0 = grid.getOrElse(r * 3 + 0) { 0 }
                val target1 = grid.getOrElse(r * 3 + 1) { 0 }
                val target2 = grid.getOrElse(r * 3 + 2) { 0 }
                val start0  = currentSymbols.getOrElse(r * 3 + 0) { target0 }
                val start1  = currentSymbols.getOrElse(r * 3 + 1) { target1 }
                val start2  = currentSymbols.getOrElse(r * 3 + 2) { target2 }

                // Fast casino strip length per reel: Reel 0=45, Reel 1=57, Reel 2=69, Reel 3=81, Reel 4=93
                val stepCount = 45 + r * 14
                val intermediate = List(stepCount - 3) { idx ->
                    ((r * 11 + idx * 7 + (1..7).random()) % 8)
                }

                // Strip structure: [Target(0..2)] + [Intermediate] + [Start(0..2)]
                val strip = listOf(target0, target1, target2) + intermediate + listOf(start0, start1, start2)
                reelStrips[r].clear()
                reelStrips[r].addAll(strip)

                animators[r].snapTo(stepCount.toFloat())
            }

            // Launch staggered animations concurrently for each reel
            for (r in 0 until reelCount) {
                val stepCount = (45 + r * 14).toFloat()
                // Staggered spin duration:
                // Reel 0: 1.80s, Reel 1: 2.30s, Reel 2: 2.80s, Reel 3: 3.30s, Reel 4: 3.80s
                val duration = 1800 + r * 500

                launch {
                    // Phase 1: High speed rotation with gradual deceleration into landing position
                    animators[r].animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = duration,
                            easing = CubicBezierEasing(0.12f, 0.05f, 0.15f, 1.0f)
                        )
                    )

                    // Sound on landing
                    withContext(Dispatchers.Default) {
                        SlotNativeBridge.nativePlaySound(SlotNativeBridge.Sound.REEL_STOP)
                    }

                    // Phase 2: Mechanical bounce (slight downward dip past target, then spring back)
                    animators[r].animateTo(
                        targetValue = -0.16f,
                        animationSpec = tween(durationMillis = 70, easing = FastOutLinearInEasing)
                    )
                    animators[r].animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
            }
        } else {
            // Update current static symbols when not spinning
            currentSymbols = grid.clone()
            for (r in 0 until 5) {
                animators[r].snapTo(0f)
                reelStrips[r].clear()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Theme Cabinet Background Image
        Image(
            painter = painterResource(id = bgRes),
            contentDescription = "Cabinet Theme Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // ── Main Slot Reels Screen (100% GPU Canvas) ──────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(reelBoxWidth)
                .aspectRatio(reelBoxAspect)
                .offset(y = reelOffsetY)
                .clip(RoundedCornerShape(8.dp))
                .background(containerBg)
                .border(2.dp, neonBorderBrush, RoundedCornerShape(8.dp))
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val totalW = size.width
                val totalH = size.height
                val reelW = totalW / reelCount
                val symH = totalH / rowCount

                // Draw each reel column
                for (r in 0 until reelCount) {
                    val reelLeft = r * reelW
                    val reelRight = reelLeft + reelW

                    // Clip drawing strictly inside this reel's slot window
                    clipRect(left = reelLeft, top = 0f, right = reelRight, bottom = totalH) {
                        val pos = animators[r].value
                        val strip = reelStrips[r]

                        if (strip.isNotEmpty() && pos != 0f) {
                            // Spinning mode: Draw scrolling strip
                            val baseIdx = if (pos >= 0) pos.toInt() else -1
                            val frac = if (pos >= 0) (pos - baseIdx) else -pos

                            for (row in -1..rowCount) {
                                val stripIndex = if (baseIdx >= 0) (baseIdx + row) else row
                                val sym = strip.getOrNull(stripIndex)
                                    ?: grid.getOrElse(r * rowCount + row.coerceIn(0, 2)) { 0 }
                                val safeSym = sym.coerceIn(0, symbolBitmaps.lastIndex)
                                val bmp = symbolBitmaps[safeSym]

                                val drawY = (row - frac) * symH
                                val padX = (reelW * 0.06f)
                                val padY = (symH * 0.06f)
                                val drawW = (reelW - padX * 2).toInt().coerceAtLeast(1)
                                val drawH = (symH - padY * 2).toInt().coerceAtLeast(1)

                                drawImage(
                                    image = bmp,
                                    dstOffset = IntOffset((reelLeft + padX).toInt(), (drawY + padY).toInt()),
                                    dstSize = IntSize(drawW, drawH),
                                    colorFilter = colorFilter
                                )
                            }
                        } else {
                            // Static / Settled mode: Draw static symbols
                            for (row in 0 until rowCount) {
                                val sym = grid.getOrElse(r * rowCount + row) { 0 }
                                val safeSym = sym.coerceIn(0, symbolBitmaps.lastIndex)
                                val bmp = symbolBitmaps[safeSym]

                                val drawY = row * symH
                                val padX = (reelW * 0.06f)
                                val padY = (symH * 0.06f)
                                val drawW = (reelW - padX * 2).toInt().coerceAtLeast(1)
                                val drawH = (symH - padY * 2).toInt().coerceAtLeast(1)

                                drawImage(
                                    image = bmp,
                                    dstOffset = IntOffset((reelLeft + padX).toInt(), (drawY + padY).toInt()),
                                    dstSize = IntSize(drawW, drawH),
                                    colorFilter = colorFilter
                                )
                            }
                        }

                        // Reel Top & Bottom 3D Cylinder Depth Shadows
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.0f to Color.Black.copy(alpha = 0.55f),
                                0.16f to Color.Transparent,
                                0.84f to Color.Transparent,
                                1.0f to Color.Black.copy(alpha = 0.55f)
                            ),
                            topLeft = Offset(reelLeft, 0f),
                            size = Size(reelW, totalH)
                        )
                    }

                    // Vertical Divider Line between reels (Subtle metallic dark groove)
                    if (r > 0) {
                        drawLine(
                            color = Color(0xFF1E1A33).copy(alpha = 0.9f),
                            start = Offset(reelLeft, 0f),
                            end = Offset(reelLeft, totalH),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }

                // Diagonal Glass Sheen Reflection Overlay
                drawRect(
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.03f)
                        )
                    ),
                    topLeft = Offset.Zero,
                    size = Size(totalW, totalH)
                )
            }
        }
    }
}


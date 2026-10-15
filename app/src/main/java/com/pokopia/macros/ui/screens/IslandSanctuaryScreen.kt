package com.pokopia.macros.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pokopia.macros.ui.PokopiaViewModel
import com.pokopia.macros.ui.sprites.DittoCanvasSprite
import com.pokopia.macros.ui.sprites.PorygonCanvasSprite
import com.pokopia.macros.ui.sprites.PorygonCyberMode
import com.pokopia.macros.ui.theme.PokopiaColors
import com.pokopia.macros.ui.vfx.PokopiaParticleMode
import com.pokopia.macros.ui.vfx.PokopiaParticleOverlay
import kotlin.math.roundToInt

/**
 * TAB 1: Pokopia Island Sanctuary (`IslandSanctuaryScreen.kt`)
 *
 * STRICT MAIN SCREEN CONSTRAINT:
 * Displays ONLY:
 * 1. Interactive Pokopia Island diorama header featuring Ditto (lounging/morphing on the beach)
 *    and Porygon (floating beside a holographic macro signboard). Tapping either character
 *    triggers a custom animation + synthesized voice chirp.
 * 2. Remaining / Consumed Calories (kcal) main ring (Adherence-Neutral).
 * 3. Protein (g), Carbohydrates (g), and Fats (g) primary progress bars/orbs (styled as Berry/Apricorn meters).
 * 4. Quick-action FAB (Porygon Lens) to trigger AI Photo Snap or Voice Log.
 *
 * Strictly omits micronutrient tables, weight graphs, exercise logs, water trackers, or recipe lists.
 */
@Composable
fun IslandSanctuaryScreen(
    viewModel: PokopiaViewModel,
    openAiCameraModalInitially: Boolean = false
) {
    val profile by viewModel.userProfile.collectAsState()
    val todayLogs by viewModel.todayLogs.collectAsState()
    val dittoState by viewModel.dittoMorphState.collectAsState()
    val statusBanner by viewModel.statusBanner.collectAsState()
    val aiPlateResult by viewModel.aiPlateResult.collectAsState()

    var showPorygonLensSheet by remember { mutableStateOf(openAiCameraModalInitially) }
    var voiceUtterance by remember { mutableStateOf("200g wild salmon with 180g jasmine rice and 100g avocado") }

    val targetCalories = profile?.targetCaloriesKcal ?: 1985
    val targetProtein = profile?.targetProteinG ?: 165
    val targetCarbs = profile?.targetCarbsG ?: 198
    val targetFat = profile?.targetFatG ?: 60

    val consumedCalories = todayLogs.sumOf { it.caloriesKcal }.roundToInt()
    val consumedProtein = todayLogs.sumOf { it.proteinG }.roundToInt()
    val consumedCarbs = todayLogs.sumOf { it.carbsG }.roundToInt()
    val consumedFat = todayLogs.sumOf { it.fatG }.roundToInt()

    val remainingCalories = targetCalories - consumedCalories

    Scaffold(
        containerColor = PokopiaColors.DeepOceanBg,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showPorygonLensSheet = true },
                containerColor = PokopiaColors.PorygonCyberCyan,
                contentColor = Color(0xFF041C24),
                icon = {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Porygon Lens")
                },
                text = {
                    Text(
                        text = "Porygon Lens (AI Snap / Voice)",
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Subtle ambient Ditto jelly bubbles & tropical leaves across the sanctuary
            PokopiaParticleOverlay(
                mode = PokopiaParticleMode.DITTO_JELLY_AND_LEAVES,
                intensity = 0.85f,
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. INTERACTIVE POKOPIA ISLAND DIORAMA HEADER (Ditto + Porygon + Holographic Signboard)
                PokopiaIslandHeaderCard(
                    dittoState = dittoState,
                    statusBanner = statusBanner,
                    onDittoTap = { viewModel.onDittoCharacterTapped() },
                    onPorygonTap = { viewModel.onPorygonCharacterTapped() }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 2. REMAINING / CONSUMED CALORIES (KCAL) MAIN RING (Adherence-Neutral)
                MainIslandCalorieRingCard(
                    consumedKcal = consumedCalories,
                    targetKcal = targetCalories,
                    remainingKcal = remainingCalories
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 3. PRIMARY MACRO BERRY / APRICORN ORBS & METERS (Protein, Carbs, Fats ONLY)
                BerryApricornMacroMetersCard(
                    proteinConsumed = consumedProtein,
                    proteinTarget = targetProtein,
                    carbsConsumed = consumedCarbs,
                    carbsTarget = targetCarbs,
                    fatConsumed = consumedFat,
                    fatTarget = targetFat
                )

                Spacer(modifier = Modifier.height(88.dp))
            }
        }
    }

    // Porygon Lens Modal (AI Vision Plate Snap + Voice/NLP Quick Log)
    if (showPorygonLensSheet) {
        AlertDialog(
            onDismissRequest = { showPorygonLensSheet = false },
            containerColor = PokopiaColors.LagoonSurface,
            title = {
                Text(
                    text = "Porygon Lens: Multimodal AI Logger",
                    style = MaterialTheme.typography.titleLarge,
                    color = PokopiaColors.PorygonCyberCyan
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Snap a meal photo for Gemini Vision volumetric plate estimation, or speak/type a natural language food log:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = {
                            showPorygonLensSheet = false
                            viewModel.runPorygonAiPlateScan(null)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PokopiaColors.PorygonCoral),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Capture Plate with Porygon AI Vision", fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = voiceUtterance,
                        onValueChange = { voiceUtterance = it },
                        label = { Text("Voice / Natural Language Food Log") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = {
                            showPorygonLensSheet = false
                            viewModel.logNaturalLanguageVoiceOrText(voiceUtterance)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = PokopiaColors.DittoLavenderLight)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Parse & Log with Voice NLP", color = PokopiaColors.TextCream)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPorygonLensSheet = false }) {
                    Text("Close", color = PokopiaColors.TextMutedSand)
                }
            }
        )
    }

    // AI Plate Recognition Result Confirmation Modal
    aiPlateResult?.let { analysis ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissAiPlateResult() },
            containerColor = PokopiaColors.DriftwoodCard,
            title = {
                Text(
                    text = analysis.summaryTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = PokopiaColors.PorygonCyberCyan
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = analysis.porygonCommentary,
                        style = MaterialTheme.typography.labelSmall,
                        color = PokopiaColors.WarmSand
                    )
                    analysis.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PokopiaColors.LagoonSurface, RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.Bold)
                                Text(
                                    "${item.estimatedGrams.toInt()}g • ${(item.confidence * 100).toInt()}% AI conf",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Text(
                                "${item.caloriesKcal.toInt()} kcal
${item.proteinG.toInt()}P ${item.carbsG.toInt()}C ${item.fatG.toInt()}F",
                                style = MaterialTheme.typography.labelSmall,
                                color = PokopiaColors.PorygonCyberCyan
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.logAllRecognizedAiItems(analysis) },
                    colors = ButtonDefaults.buttonColors(containerColor = PokopiaColors.TropicalFoliage)
                ) {
                    Text("Log Plate to Sanctuary", color = Color(0xFF062316), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissAiPlateResult() }) {
                    Text("Cancel", color = PokopiaColors.TextMutedSand)
                }
            }
        )
    }
}

@Composable
private fun PokopiaIslandHeaderCard(
    dittoState: com.pokopia.macros.ui.sprites.DittoMorphState,
    statusBanner: String,
    onDittoTap: () -> Unit,
    onPorygonTap: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, PokopiaColors.DriftwoodBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PokopiaColors.LagoonSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "POKOPIA ISLAND SANCTUARY",
                        style = MaterialTheme.typography.labelSmall,
                        color = PokopiaColors.TropicalFoliage
                    )
                    Text(
                        text = "Ditto & Porygon Habitat",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Box(
                    modifier = Modifier
                        .background(PokopiaColors.DriftwoodCard, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Tap Characters!",
                        style = MaterialTheme.typography.labelSmall,
                        color = PokopiaColors.WarmSand
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Diorama Stage: Tropical Beach Cove with Ditto on Warm Sand & Porygon by Hologram Sign
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(165.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF144552),
                                Color(0xFF1C6E7D),
                                Color(0xFFE3C896) // Warm tropical beach sand
                            )
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        DittoCanvasSprite(
                            morphState = dittoState,
                            onDittoTapped = onDittoTap,
                            modifier = Modifier.size(118.dp)
                        )
                        Text(
                            text = "Ditto (${dittoState.name.replace('_', ' ')})",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2B1B12)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PorygonCanvasSprite(
                            cyberMode = PorygonCyberMode.ISLAND_HOLOGRAM_COMPANION,
                            onPorygonTapped = onPorygonTap,
                            modifier = Modifier.size(114.dp)
                        )
                        Text(
                            text = "Porygon (Cyber-OS)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF102A36)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Holographic Signboard Dialogue
            Text(
                text = statusBanner,
                style = MaterialTheme.typography.bodyMedium,
                color = PokopiaColors.WarmSand
            )
        }
    }
}

@Composable
private fun MainIslandCalorieRingCard(
    consumedKcal: Int,
    targetKcal: Int,
    remainingKcal: Int
) {
    val rawProgress = (consumedKcal.toFloat() / targetKcal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1.15f)
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 700),
        label = "calorie_ring_progress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, PokopiaColors.DriftwoodBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PokopiaColors.DriftwoodCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "DAILY ISLAND ENERGY RING",
                style = MaterialTheme.typography.labelSmall,
                color = PokopiaColors.PorygonCyberCyan
            )
            Spacer(modifier = Modifier.height(14.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 20.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                    // Background track ring
                    drawArc(
                        color = PokopiaColors.DeepOceanBg,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Adherence-neutral ocean cyan -> lavender gradient arc
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                PokopiaColors.PorygonCyberCyan,
                                PokopiaColors.TropicalFoliage,
                                PokopiaColors.DittoLavenderLight,
                                PokopiaColors.PorygonCyberCyan
                            )
                        ),
                        startAngle = 135f,
                        sweepAngle = 270f * animatedProgress.coerceAtMost(1f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val label = if (remainingKcal >= 0) "KCAL REMAINING" else "KCAL OVER TARGET (NEUTRAL)"
                    Text(
                        text = kotlin.math.abs(remainingKcal).toString(),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PokopiaColors.TextCream
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = PokopiaColors.WarmSand
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$consumedKcal / $targetKcal kcal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PokopiaColors.PorygonCyberCyan
                    )
                }
            }
        }
    }
}

@Composable
private fun BerryApricornMacroMetersCard(
    proteinConsumed: Int,
    proteinTarget: Int,
    carbsConsumed: Int,
    carbsTarget: Int,
    fatConsumed: Int,
    fatTarget: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, PokopiaColors.DriftwoodBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PokopiaColors.DriftwoodCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "PRIMARY BERRY & APRICORN MACRO METERS",
                style = MaterialTheme.typography.labelSmall,
                color = PokopiaColors.WarmSand
            )

            BerryMacroRow(
                berryTitle = "Pecha Apricorn • Protein",
                consumedG = proteinConsumed,
                targetG = proteinTarget,
                orbColor = PokopiaColors.PechaProteinPink
            )

            BerryMacroRow(
                berryTitle = "Sitrus Apricorn • Carbohydrates",
                consumedG = carbsConsumed,
                targetG = carbsTarget,
                orbColor = PokopiaColors.SitrusCarbGold
            )

            BerryMacroRow(
                berryTitle = "Lum Apricorn • Healthy Fats",
                consumedG = fatConsumed,
                targetG = fatTarget,
                orbColor = PokopiaColors.LumFatMint
            )
        }
    }
}

@Composable
private fun BerryMacroRow(
    berryTitle: String,
    consumedG: Int,
    targetG: Int,
    orbColor: Color
) {
    val ratio = (consumedG.toFloat() / targetG.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val animatedRatio by animateFloatAsState(
        targetValue = ratio,
        animationSpec = tween(600),
        label = "berry_macro_bar"
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(orbColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = berryTitle, style = MaterialTheme.typography.titleMedium)
            }
            Text(
                text = "${consumedG}g / ${targetG}g",
                style = MaterialTheme.typography.bodyMedium,
                color = orbColor,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(PokopiaColors.DeepOceanBg)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedRatio)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(orbColor)
            )
        }
    }
}

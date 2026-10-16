package com.pokopia.macros.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pokopia.macros.data.local.DailyExpenditureEntity
import com.pokopia.macros.data.local.SyncedWorkoutItem
import com.pokopia.macros.data.local.WeightEntryEntity
import com.pokopia.macros.ui.PokopiaViewModel
import com.pokopia.macros.ui.sprites.PorygonCanvasSprite
import com.pokopia.macros.ui.sprites.PorygonCyberMode
import com.pokopia.macros.ui.theme.PokopiaColors
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

/**
 * TAB 5: Weight Loss & Coaching Log ("Porygon Research Lab" - `PorygonResearchLabScreen.kt`)
 *
 * - MacroFactor-style 10-day Exponential Moving Average (EMA) Trend Weight vs Scale Weight chart
 * - Dynamic TDEE Expenditure Engine chart (back-calculated from rolling calorie intake & EMA tissue change)
 * - Porygon Weekly Check-In Cyber-Lab Card with adherence-neutral target coaching
 * - Connected Ecosystem: Google Health Connect, Pixel Watch, Fitbit & Strava API v3 OAuth2 PKCE sync
 */
@Composable
fun PorygonResearchLabScreen(viewModel: PokopiaViewModel) {
    val context = LocalContext.current
    val weightHistory by viewModel.weightHistory.collectAsState()
    val expenditureHistory by viewModel.expenditureHistory.collectAsState()
    val checkIn by viewModel.weeklyCheckIn.collectAsState()
    val wearableSync by viewModel.todayWearableSync.collectAsState()
    val profile by viewModel.userProfile.collectAsState()

    var scaleInputKg by remember { mutableStateOf("76.8") }
    var showSettingsModal by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PokopiaColors.DeepOceanBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Cyber-Lab Header with Porygon & API/Goal Settings Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PorygonCanvasSprite(
                    cyberMode = PorygonCyberMode.CYBER_LAB_CHECKIN,
                    onPorygonTapped = { viewModel.onPorygonCharacterTapped() },
                    modifier = Modifier.size(68.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "PORYGON RESEARCH LAB // MACROFACTOR ENGINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = PokopiaColors.PorygonCyberCyan
                    )
                    Text(
                        text = "EMA Trend & Dynamic TDEE",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
            IconButton(onClick = { showSettingsModal = true }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = PokopiaColors.WarmSand)
            }
        }

        // 2. Porygon Weekly Check-In Card
        checkIn?.let { report ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, PokopiaColors.PorygonCyberCyan, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = PokopiaColors.LagoonSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PORYGON WEEKLY CHECK-IN READY",
                            style = MaterialTheme.typography.labelSmall,
                            color = PokopiaColors.PorygonCoral
                        )
                        Text(
                            text = "${report.confidencePercent}% Confidence",
                            style = MaterialTheme.typography.labelSmall,
                            color = PokopiaColors.TropicalFoliage
                        )
                    }
                    Text(
                        text = report.porygonDiagnosis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PokopiaColors.TextCream
                    )
                        )
                        Button(
                            onClick = { viewModel.acceptPorygonWeeklyCheckInTargets() },
                            colors = ButtonDefaults.buttonColors(containerColor = PokopiaColors.PorygonCyberCyan)
                        ) {
                            Text("Apply Check-In", color = Color(0xFF042029), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. 10-Day EMA Trend Weight vs Raw Scale Weight Card & Logger
        EmaTrendWeightChartCard(
            weightHistory = weightHistory,
            scaleInputKg = scaleInputKg,
            onScaleInputChange = { scaleInputKg = it },
            onLogWeight = {
                scaleInputKg.toDoubleOrNull()?.let { viewModel.logDailyScaleWeight(it) }
            }
        )

        // 4. Dynamic TDEE Expenditure Graph Card
        DynamicTdeeChartCard(expenditureHistory = expenditureHistory)

        // 5. Connected Fitness Ecosystem: Health Connect, Pixel Watch, Fitbit & Strava OAuth2 PKCE
        ConnectedEcosystemCard(
            wearableSync = wearableSync,
            onSyncHealthConnect = { viewModel.syncWearablesAndStrava(context, null) },
            onConnectStravaOAuth = { viewModel.syncWearablesAndStrava(context, "STRAVA") },
            onConnectFitbitOAuth = { viewModel.syncWearablesAndStrava(context, "FITBIT") }
        )

        Spacer(modifier = Modifier.height(60.dp))
    }

    if (showSettingsModal) {
        var geminiKey by remember(profile) { mutableStateOf(profile?.geminiApiKey.orEmpty()) }
        var stravaId by remember(profile) { mutableStateOf(profile?.stravaClientId.orEmpty()) }
        var weeklyRate by remember(profile) { mutableStateOf((profile?.targetWeeklyRateKg ?: -0.45).toString()) }

        AlertDialog(
            onDismissRequest = { showSettingsModal = false },
            containerColor = PokopiaColors.LagoonSurface,
            title = { Text("Pokopia API & Coaching Settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = geminiKey,
                        onValueChange = { geminiKey = it },
                        label = { Text("Gemini Vision API Key (Nutrola Plate Scan)") }
                    )
                    OutlinedTextField(
                        value = stravaId,
                        onValueChange = { stravaId = it },
                        label = { Text("Strava API v3 Client ID (OAuth2 PKCE)") }
                    )
                    OutlinedTextField(
                        value = weeklyRate,
                        onValueChange = { weeklyRate = it },
                        label = { Text("Target Weekly Rate (kg/week, e.g. -0.45)") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveApiAndCoachingSettings(
                            geminiKey = geminiKey,
                            stravaClientId = stravaId,
                            targetWeeklyRateKg = weeklyRate.toDoubleOrNull() ?: -0.45
                        )
                        showSettingsModal = false
                    }
                ) {
                    Text("Save Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsModal = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun EmaTrendWeightChartCard(
    weightHistory: List<WeightEntryEntity>,
    scaleInputKg: String,
    onScaleInputChange: (String) -> Unit,
    onLogWeight: () -> Unit
) {
    val latest = weightHistory.lastOrNull()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PokopiaColors.DriftwoodBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PokopiaColors.DriftwoodCard)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "10-DAY EMA TREND WEIGHT (W_trend,t)",
                        style = MaterialTheme.typography.labelSmall,
                        color = PokopiaColors.DittoLavenderLight
                    )
                    Text(
                        text = "Trend: ${latest?.emaTrendWeightKg ?: 77.0} kg  |  Scale: ${latest?.scaleWeightKg ?: 76.8} kg",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Icon(Icons.Default.MonitorWeight, contentDescription = null, tint = PokopiaColors.DittoLavenderLight)
            }

            // Dual-line Canvas Graph: Raw Scale Dots (Muted Sand) vs Smooth EMA Curve (Ditto Lavender)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(PokopiaColors.DeepOceanBg, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                if (weightHistory.size < 2) return@Canvas
                val minW = (weightHistory.minOf { minOf(it.scaleWeightKg, it.emaTrendWeightKg) } - 0.4).toFloat()
                val maxW = (weightHistory.maxOf { maxOf(it.scaleWeightKg, it.emaTrendWeightKg) } + 0.4).toFloat()
                val span = (maxW - minW).coerceAtLeast(0.5f)

                val emaPath = Path()
                weightHistory.forEachIndexed { idx, entry ->
                    val x = (idx.toFloat() / (weightHistory.size - 1).toFloat()) * size.width
                    val yScale = size.height - ((entry.scaleWeightKg.toFloat() - minW) / span) * size.height
                    val yTrend = size.height - ((entry.emaTrendWeightKg.toFloat() - minW) / span) * size.height

                    // Scale weight scatter dot
                    drawCircle(
                        color = PokopiaColors.WarmSand.copy(alpha = 0.65f),
                        radius = 4.5f,
                        center = Offset(x, yScale)
                    )

                    if (idx == 0) emaPath.moveTo(x, yTrend) else emaPath.lineTo(x, yTrend)
                }

                drawPath(
                    path = emaPath,
                    color = PokopiaColors.DittoLavenderLight,
                    style = Stroke(width = 4.5f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = scaleInputKg,
                    onValueChange = onScaleInputChange,
                    label = { Text("Today's Scale Weight (kg)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(
                    onClick = onLogWeight,
                    colors = ButtonDefaults.buttonColors(containerColor = PokopiaColors.DittoLavenderLight)
                ) {
                    Text("Log Weigh-In", color = Color(0xFF1F0C29), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DynamicTdeeChartCard(expenditureHistory: List<DailyExpenditureEntity>) {
    val latest = expenditureHistory.lastOrNull()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PokopiaColors.DriftwoodBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PokopiaColors.DriftwoodCard)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "DYNAMIC TDEE EXPENDITURE ENGINE (3500 KCAL = 1 LB TISSUE)",
                style = MaterialTheme.typography.labelSmall,
                color = PokopiaColors.PorygonCyberCyan
            )
            Text(
                text = "Rolling Expenditure: ${latest?.smoothedTdeeKcal?.roundToInt() ?: 2485} kcal/day",
                style = MaterialTheme.typography.titleMedium
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp)
                    .background(PokopiaColors.DeepOceanBg, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                if (expenditureHistory.size < 2) return@Canvas
                val minE = (expenditureHistory.minOf { it.smoothedTdeeKcal } - 120.0).toFloat()
                val maxE = (expenditureHistory.maxOf { it.smoothedTdeeKcal } + 120.0).toFloat()
                val span = (maxE - minE).coerceAtLeast(100f)

                val tdeePath = Path()
                expenditureHistory.forEachIndexed { idx, item ->
                    val x = (idx.toFloat() / (expenditureHistory.size - 1).toFloat()) * size.width
                    val y = size.height - ((item.smoothedTdeeKcal.toFloat() - minE) / span) * size.height
                    if (idx == 0) tdeePath.moveTo(x, y) else tdeePath.lineTo(x, y)
                }
                drawPath(
                    path = tdeePath,
                    color = PokopiaColors.PorygonCyberCyan,
                    style = Stroke(width = 4.5f)
                )
            }

            Text(
                text = "Calculated strictly from rolling nutrition intake vs EMA Trend Weight slope — immune to noisy wearable calorie inflation.",
                style = MaterialTheme.typography.labelSmall,
                color = PokopiaColors.WarmSand
            )
        }
    }
}

@Composable
private fun ConnectedEcosystemCard(
    wearableSync: com.pokopia.macros.data.local.WearableSyncEntity?,
    onSyncHealthConnect: () -> Unit,
    onConnectStravaOAuth: () -> Unit,
    onConnectFitbitOAuth: () -> Unit
) {
    val workouts = remember(wearableSync) {
        runCatching {
            Json.decodeFromString<List<SyncedWorkoutItem>>(wearableSync?.workoutsJson ?: "[]")
        }.getOrDefault(emptyList())
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PokopiaColors.DriftwoodBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PokopiaColors.DriftwoodCard)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CONNECTED ECOSYSTEM & WEARABLES",
                        style = MaterialTheme.typography.labelSmall,
                        color = PokopiaColors.TropicalFoliage
                    )
                    Text(
                        text = "Health Connect • Pixel Watch • Fitbit • Strava",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                IconButton(onClick = onSyncHealthConnect) {
                    Icon(Icons.Default.Sync, contentDescription = "Sync", tint = PokopiaColors.PorygonCyberCyan)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryMetricPill("Steps", "${wearableSync?.stepsCount ?: 10840}")
                TelemetryMetricPill("Active Burn", "${wearableSync?.activeCaloriesKcal ?: 855} kcal")
                TelemetryMetricPill("Resting HR", "${wearableSync?.restingHeartRateBpm ?: 54} bpm")
                TelemetryMetricPill("Sleep", "${(wearableSync?.sleepDurationMinutes ?: 468) / 60}h ${(wearableSync?.sleepDurationMinutes ?: 468) % 60}m")
            }

            workouts.forEach { workout ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PokopiaColors.LagoonSurface, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = PokopiaColors.PorygonCoral)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(workout.title, fontWeight = FontWeight.Bold)
                            Text("${workout.source} • ${workout.sportType} • ${workout.durationMinutes} min", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                Text(
                    text = "${workout.distanceKm} km | ${workout.avgHeartRateBpm} bpm | ${workout.elevationGainM}m elev",
                    style = MaterialTheme.typography.labelSmall,
                    color = PokopiaColors.PorygonCyberCyan
                )

                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onConnectStravaOAuth, modifier = Modifier.weight(1f)) {
                    Text("Strava OAuth2 PKCE", style = MaterialTheme.typography.labelSmall, color = PokopiaColors.PorygonCoral)
                }
                OutlinedButton(onClick = onConnectFitbitOAuth, modifier = Modifier.weight(1f)) {
                    Text("Fitbit OAuth2 PKCE", style = MaterialTheme.typography.labelSmall, color = PokopiaColors.PorygonCyberCyan)
                }
            }
        }
    }
}

@Composable
private fun TelemetryMetricPill(label: String, value: String) {
    Column(
        modifier = Modifier
            .background(PokopiaColors.DeepOceanBg, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontWeight = FontWeight.Bold, color = PokopiaColors.WarmSand)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

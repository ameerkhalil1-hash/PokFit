package com.pokopia.macros.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pokopia.macros.ui.PokopiaViewModel
import com.pokopia.macros.ui.sprites.DittoCanvasSprite
import com.pokopia.macros.ui.sprites.DittoMorphState
import com.pokopia.macros.ui.theme.PokopiaColors

/**
 * TAB 3: Meal Database — Ditto's Cafe (`DittosCafeScreen.kt`)
 *
 * - Saved custom meals & multi-serving meal-prep containers
 * - One-tap repeat logging (Ditto morphs into the logged meal icon & plays island marimba chime)
 * - Custom Recipe / Meal-Prep Container Builder
 * - Visual plate & today's meal log history
 */
@Composable
fun DittosCafeScreen(viewModel: PokopiaViewModel) {
    val savedMeals by viewModel.savedMeals.collectAsState()
    val todayLogs by viewModel.todayLogs.collectAsState()
    val allFoods by viewModel.allFoods.collectAsState()
    val dittoState by viewModel.dittoMorphState.collectAsState()

    var showRecipeBuilder by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PokopiaColors.DeepOceanBg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DittoCanvasSprite(
                    morphState = dittoState,
                    onDittoTapped = { viewModel.onDittoCharacterTapped() },
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "DITTO'S COZY ISLAND CAFE",
                        style = MaterialTheme.typography.labelSmall,
                        color = PokopiaColors.DittoLavenderLight
                    )
                    Text(
                        text = "Meal-Prep & Saved Recipes",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            Button(
                onClick = { showRecipeBuilder = true },
                colors = ButtonDefaults.buttonColors(containerColor = PokopiaColors.TropicalFoliage)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF062315))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Prep Batch", color = Color(0xFF062315), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            item {
                Text(
                    text = "MULTI-SERVING MEAL PREP CONTAINERS",
                    style = MaterialTheme.typography.labelSmall,
                    color = PokopiaColors.WarmSand
                )
            }

            items(savedMeals, key = { it.id }) { meal ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, PokopiaColors.DriftwoodBorder, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PokopiaColors.DriftwoodCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(meal.title, style = MaterialTheme.typography.titleMedium)
                                Text(meal.subtitle, style = MaterialTheme.typography.labelSmall)
                            }
                            Text(
                                "${meal.caloriesPerServing.toInt()} kcal",
                                style = MaterialTheme.typography.titleLarge,
                                color = PokopiaColors.PorygonCyberCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Per Container: ${meal.proteinPerServing.toInt()}g P • " +
                                    "${meal.carbsPerServing.toInt()}g C • ${meal.fatPerServing.toInt()}g F
" +
                                    "Batch: ${meal.servingsCount} containers (${meal.totalPrepWeightG.toInt()}g total)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PokopiaColors.WarmSand
                            )

                            Button(
                                onClick = { viewModel.logSavedMealFromCafe(meal, 1.0) },
                                colors = ButtonDefaults.buttonColors(containerColor = PokopiaColors.DittoLavenderLight)
                            ) {
                                Icon(Icons.Default.Replay, contentDescription = null, tint = Color(0xFF200D2A))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("1-Tap Log", color = Color(0xFF200D2A), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "TODAY'S VISUAL PLATE HISTORY (${todayLogs.size} ENTRIES)",
                    style = MaterialTheme.typography.labelSmall,
                    color = PokopiaColors.PorygonCyberCyan
                )
            }

            items(todayLogs, key = { it.id }) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = PokopiaColors.LagoonSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                Icons.Default.Fastfood,
                                contentDescription = null,
                                tint = PokopiaColors.DittoLavenderLight
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(log.foodName, fontWeight = FontWeight.Bold)
                                Text(
                                    "${log.mealSlot} • ${log.gramsLogged.toInt()}g • via ${log.loggedVia}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        Text(
                            "${log.caloriesKcal.toInt()} kcal
${log.proteinG.toInt()}P ${log.carbsG.toInt()}C ${log.fatG.toInt()}F",
                            style = MaterialTheme.typography.labelSmall,
                            color = PokopiaColors.TropicalFoliage
                        )
                    }
                }
            }
        }
    }

    if (showRecipeBuilder) {
        var title by remember { mutableStateOf("Island Teriyaki Salmon Meal Prep") }
        var servings by remember { mutableStateOf("4") }

        AlertDialog(
            onDismissRequest = { showRecipeBuilder = false },
            containerColor = PokopiaColors.LagoonSurface,
            title = { Text("Ditto's Multi-Container Meal Prep Builder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Meal-Prep Container Name") }
                    )
                    OutlinedTextField(
                        value = servings,
                        onValueChange = { servings = it },
                        label = { Text("Number of Prep Containers (Servings)") }
                    )
                    Text(
                        "Includes verified Pokopia Salmon (600g), Steamed Jasmine Rice (720g), and Tropical Avocado (240g) divided evenly across containers.",
                        style = MaterialTheme.typography.labelSmall,
                        color = PokopiaColors.WarmSand
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val count = servings.toIntOrNull()?.coerceIn(1, 14) ?: 4
                        val picked = allFoods.take(3).map { it to (it.servingSizeG * count) }
                        viewModel.createNewSavedMealInCafe(
                            title = title,
                            subtitle = "$count-Container Batch Prep • Ditto Cafe Verified",
                            servingsCount = count,
                            selectedFoods = picked
                        )
                        showRecipeBuilder = false
                    }
                ) {
                    Text("Save Batch Prep")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecipeBuilder = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

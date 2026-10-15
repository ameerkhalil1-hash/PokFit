package com.pokopia.macros.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pokopia.macros.data.local.FoodItemEntity
import com.pokopia.macros.data.local.MicronutrientCatalog
import com.pokopia.macros.ui.PokopiaViewModel
import com.pokopia.macros.ui.theme.PokopiaColors
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

/**
 * TAB 2: Food Database — Pokopia Pantry (`PokopiaPantryScreen.kt`)
 *
 * - Searchable local + OpenFoodFacts/USDA verified food database
 * - Full 105-Micronutrient Profile Explorer (Vitamins, Minerals, Amino Acids, Lipids, Carbs/Fiber, Bioactives)
 * - Custom Food Creation modal
 * - Gram / Serving size slider logger
 */
@Composable
fun PokopiaPantryScreen(viewModel: PokopiaViewModel) {
    val allFoods by viewModel.allFoods.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFoodForLog by remember { mutableStateOf<FoodItemEntity?>(null) }
    var selectedFoodForMicros by remember { mutableStateOf<FoodItemEntity?>(null) }
    var showCreateCustomFoodModal by remember { mutableStateOf(false) }

    val filteredFoods = remember(allFoods, searchQuery) {
        if (searchQuery.isBlank()) allFoods
        else allFoods.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.brand.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true) ||
                it.barcodeUpc?.contains(searchQuery) == true
        }
    }

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
            Column {
                Text(
                    text = "POKOPIA PANTRY • 105+ MICRONUTRIENTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = PokopiaColors.TropicalFoliage
                )
                Text(
                    text = "Dietitian-Verified Food Database",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Button(
                onClick = { showCreateCustomFoodModal = true },
                colors = ButtonDefaults.buttonColors(containerColor = PokopiaColors.DittoLavenderLight)
            ) {
                Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color(0xFF1F0D29))
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Food", color = Color(0xFF1F0D29), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Search foods, brands, or UPC barcodes...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(filteredFoods, key = { it.id }) { food ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, PokopiaColors.DriftwoodBorder, RoundedCornerShape(18.dp))
                        .clickable { selectedFoodForLog = food },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PokopiaColors.DriftwoodCard)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(food.name, style = MaterialTheme.typography.titleMedium)
                                    if (food.isDietitianVerified) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            Icons.Default.Verified,
                                            contentDescription = "Verified",
                                            tint = PokopiaColors.PorygonCyberCyan
                                        )
                                    }
                                }
                                Text(
                                    "${food.brand} • ${food.servingLabel} • UPC: ${food.barcodeUpc ?: "Local"}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Text(
                                "${food.caloriesPer100g.toInt()} kcal/100g",
                                style = MaterialTheme.typography.titleMedium,
                                color = PokopiaColors.WarmSand
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Per 100g: ${food.proteinPer100g}g P • ${food.carbsPer100g}g C • ${food.fatPer100g}g F",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PokopiaColors.PorygonCyberCyan
                            )
                            TextButton(onClick = { selectedFoodForMicros = food }) {
                                Icon(
                                    Icons.Default.Science,
                                    contentDescription = null,
                                    tint = PokopiaColors.TropicalFoliage
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "105 Micros",
                                    color = PokopiaColors.TropicalFoliage,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Gram Slider Food Logging Modal
    selectedFoodForLog?.let { food ->
        var grams by remember(food) { mutableFloatStateOf(food.servingSizeG.toFloat()) }
        val scale = grams / 100f
        AlertDialog(
            onDismissRequest = { selectedFoodForLog = null },
            containerColor = PokopiaColors.LagoonSurface,
            title = { Text("Log ${food.name}", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Portion Weight: ${grams.roundToInt()} grams",
                        style = MaterialTheme.typography.titleMedium,
                        color = PokopiaColors.WarmSand
                    )
                    Slider(
                        value = grams,
                        onValueChange = { grams = it },
                        valueRange = 10f..600f
                    )
                    Text(
                        "Calories: ${(food.caloriesPer100g * scale).roundToInt()} kcal
" +
                            "Protein: ${(food.proteinPer100g * scale).roundToInt()}g | " +
                            "Carbs: ${(food.carbsPer100g * scale).roundToInt()}g | " +
                            "Fat: ${(food.fatPer100g * scale).roundToInt()}g",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PokopiaColors.PorygonCyberCyan
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logFoodItem(food, grams.toDouble(), mealSlot = "Pantry Log")
                        selectedFoodForLog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PokopiaColors.TropicalFoliage)
                ) {
                    Text("Log to Sanctuary", color = Color(0xFF042213), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedFoodForLog = null }) {
                    Text("Cancel", color = PokopiaColors.TextMutedSand)
                }
            }
        )
    }

    // 105+ Micronutrient Breakdown Explorer Modal
    selectedFoodForMicros?.let { food ->
        val microMap = remember(food) {
            runCatching {
                Json.decodeFromString<Map<String, Double>>(food.micronutrientsPer100gJson)
            }.getOrDefault(emptyMap())
        }
        AlertDialog(
            onDismissRequest = { selectedFoodForMicros = null },
            containerColor = PokopiaColors.LagoonSurface,
            title = {
                Column {
                    Text(
                        "105-Micronutrient Lab Analysis",
                        style = MaterialTheme.typography.titleLarge,
                        color = PokopiaColors.PorygonCyberCyan
                    )
                    Text(
                        "${food.name} (per 100g)",
                        style = MaterialTheme.typography.labelSmall,
                        color = PokopiaColors.WarmSand
                    )
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(MicronutrientCatalog.ALL_SPECS, key = { it.key }) { spec ->
                        val amount = microMap[spec.key] ?: 0.0
                        val pctDv = if (spec.dailyValue > 0) {
                            ((amount / spec.dailyValue) * 100.0).roundToInt().coerceAtMost(300)
                        } else 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PokopiaColors.DriftwoodCard, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(spec.displayName, style = MaterialTheme.typography.bodyMedium)
                                Text(spec.category, style = MaterialTheme.typography.labelSmall)
                            }
                            Text(
                                "$amount ${spec.unit} ($pctDv% DV)",
                                style = MaterialTheme.typography.labelSmall,
                                color = PokopiaColors.TropicalFoliage
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedFoodForMicros = null }) {
                    Text("Close", color = PokopiaColors.PorygonCyberCyan)
                }
            }
        )
    }

    // Create Custom Food Modal
    if (showCreateCustomFoodModal) {
        var name by remember { mutableStateOf("") }
        var brand by remember { mutableStateOf("Pokopia Custom") }
        var upc by remember { mutableStateOf("") }
        var servingG by remember { mutableStateOf("100") }
        var kcal by remember { mutableStateOf("180") }
        var pro by remember { mutableStateOf("25") }
        var carb by remember { mutableStateOf("12") }
        var fat by remember { mutableStateOf("4") }

        AlertDialog(
            onDismissRequest = { showCreateCustomFoodModal = false },
            containerColor = PokopiaColors.LagoonSurface,
            title = { Text("Create Custom Verified Food", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Food Name") })
                    OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand / Source") })
                    OutlinedTextField(value = upc, onValueChange = { upc = it }, label = { Text("Optional UPC Barcode") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = servingG,
                            onValueChange = { servingG = it },
                            label = { Text("Serving (g)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = kcal,
                            onValueChange = { kcal = it },
                            label = { Text("Calories") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = pro,
                            onValueChange = { pro = it },
                            label = { Text("Pro (g)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = carb,
                            onValueChange = { carb = it },
                            label = { Text("Carb (g)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = fat,
                            onValueChange = { fat = it },
                            label = { Text("Fat (g)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCustomFoodToPantry(
                            name = name,
                            brand = brand,
                            barcodeUpc = upc,
                            servingG = servingG.toDoubleOrNull() ?: 100.0,
                            kcalPerServing = kcal.toDoubleOrNull() ?: 150.0,
                            proPerServing = pro.toDoubleOrNull() ?: 15.0,
                            carbPerServing = carb.toDoubleOrNull() ?: 15.0,
                            fatPerServing = fat.toDoubleOrNull() ?: 5.0
                        )
                        showCreateCustomFoodModal = false
                    }
                ) {
                    Text("Save to Pantry")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateCustomFoodModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

package com.pokopia.macros.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Complete 100+ Micronutrient catalog keys supported across Pokopia Pantry & Nutrola AI.
 * Kept strictly off the Main Island screen and surfaced in the Pantry & Drill-Down Modals.
 */
object MicronutrientCatalog {
    data class NutrientSpec(
        val key: String,
        val displayName: String,
        val unit: String,
        val category: String,
        val dailyValue: Double
    )

    val ALL_SPECS: List<NutrientSpec> = listOf(
        // Vitamins (17)
        NutrientSpec("vitamin_a_mcg", "Vitamin A (RAE)", "mcg", "Vitamins", 900.0),
        NutrientSpec("vitamin_b1_mg", "Thiamin (B1)", "mg", "Vitamins", 1.2),
        NutrientSpec("vitamin_b2_mg", "Riboflavin (B2)", "mg", "Vitamins", 1.3),
        NutrientSpec("vitamin_b3_mg", "Niacin (B3)", "mg", "Vitamins", 16.0),
        NutrientSpec("vitamin_b5_mg", "Pantothenic Acid (B5)", "mg", "Vitamins", 5.0),
        NutrientSpec("vitamin_b6_mg", "Pyridoxine (B6)", "mg", "Vitamins", 1.7),
        NutrientSpec("vitamin_b7_mcg", "Biotin (B7)", "mcg", "Vitamins", 30.0),
        NutrientSpec("vitamin_b9_mcg", "Folate (B9 DFE)", "mcg", "Vitamins", 400.0),
        NutrientSpec("folic_acid_mcg", "Folic Acid", "mcg", "Vitamins", 200.0),
        NutrientSpec("vitamin_b12_mcg", "Cobalamin (B12)", "mcg", "Vitamins", 2.4),
        NutrientSpec("vitamin_c_mg", "Vitamin C (Ascorbic Acid)", "mg", "Vitamins", 90.0),
        NutrientSpec("vitamin_d2_mcg", "Ergocalciferol (D2)", "mcg", "Vitamins", 10.0),
        NutrientSpec("vitamin_d3_mcg", "Cholecalciferol (D3)", "mcg", "Vitamins", 20.0),
        NutrientSpec("vitamin_e_mg", "Vitamin E (Alpha-Tocopherol)", "mg", "Vitamins", 15.0),
        NutrientSpec("vitamin_k1_mcg", "Phylloquinone (K1)", "mcg", "Vitamins", 120.0),
        NutrientSpec("vitamin_k2_mcg", "Menaquinone (K2 MK-7)", "mcg", "Vitamins", 90.0),
        NutrientSpec("choline_mg", "Choline", "mg", "Vitamins", 550.0),
        NutrientSpec("betaine_mg", "Betaine", "mg", "Vitamins", 250.0),
        NutrientSpec("inositol_mg", "Myo-Inositol", "mg", "Vitamins", 500.0),

        // Minerals & Electrolytes (17)
        NutrientSpec("calcium_mg", "Calcium", "mg", "Minerals", 1300.0),
        NutrientSpec("iron_mg", "Iron (Heme + Non-Heme)", "mg", "Minerals", 18.0),
        NutrientSpec("magnesium_mg", "Magnesium", "mg", "Minerals", 420.0),
        NutrientSpec("phosphorus_mg", "Phosphorus", "mg", "Minerals", 1250.0),
        NutrientSpec("potassium_mg", "Potassium", "mg", "Minerals", 4700.0),
        NutrientSpec("sodium_mg", "Sodium", "mg", "Minerals", 2300.0),
        NutrientSpec("chloride_mg", "Chloride", "mg", "Minerals", 2300.0),
        NutrientSpec("zinc_mg", "Zinc", "mg", "Minerals", 11.0),
        NutrientSpec("copper_mg", "Copper", "mg", "Minerals", 0.9),
        NutrientSpec("manganese_mg", "Manganese", "mg", "Minerals", 2.3),
        NutrientSpec("selenium_mcg", "Selenium", "mcg", "Minerals", 55.0),
        NutrientSpec("chromium_mcg", "Chromium", "mcg", "Minerals", 35.0),
        NutrientSpec("molybdenum_mcg", "Molybdenum", "mcg", "Minerals", 45.0),
        NutrientSpec("iodine_mcg", "Iodine", "mcg", "Minerals", 150.0),
        NutrientSpec("fluoride_mg", "Fluoride", "mg", "Minerals", 4.0),
        NutrientSpec("boron_mg", "Boron", "mg", "Minerals", 3.0),
        NutrientSpec("silicon_mg", "Silicon", "mg", "Minerals", 25.0),

        // Amino Acids (20)
        NutrientSpec("leucine_g", "L-Leucine (BCAA)", "g", "Amino Acids", 3.0),
        NutrientSpec("isoleucine_g", "L-Isoleucine (BCAA)", "g", "Amino Acids", 1.5),
        NutrientSpec("valine_g", "L-Valine (BCAA)", "g", "Amino Acids", 1.8),
        NutrientSpec("lysine_g", "L-Lysine", "g", "Amino Acids", 2.4),
        NutrientSpec("methionine_g", "L-Methionine", "g", "Amino Acids", 1.0),
        NutrientSpec("phenylalanine_g", "L-Phenylalanine", "g", "Amino Acids", 1.5),
        NutrientSpec("threonine_g", "L-Threonine", "g", "Amino Acids", 1.2),
        NutrientSpec("tryptophan_g", "L-Tryptophan", "g", "Amino Acids", 0.4),
        NutrientSpec("histidine_g", "L-Histidine", "g", "Amino Acids", 0.8),
        NutrientSpec("arginine_g", "L-Arginine", "g", "Amino Acids", 4.0),
        NutrientSpec("cysteine_g", "L-Cysteine", "g", "Amino Acids", 0.6),
        NutrientSpec("glutamine_g", "L-Glutamine", "g", "Amino Acids", 8.0),
        NutrientSpec("glycine_g", "Glycine", "g", "Amino Acids", 3.0),
        NutrientSpec("proline_g", "L-Proline", "g", "Amino Acids", 2.5),
        NutrientSpec("serine_g", "L-Serine", "g", "Amino Acids", 2.0),
        NutrientSpec("tyrosine_g", "L-Tyrosine", "g", "Amino Acids", 1.2),
        NutrientSpec("alanine_g", "L-Alanine", "g", "Amino Acids", 2.5),
        NutrientSpec("aspartic_acid_g", "L-Aspartic Acid", "g", "Amino Acids", 4.0),
        NutrientSpec("glutamic_acid_g", "L-Glutamic Acid", "g", "Amino Acids", 6.0),
        NutrientSpec("taurine_mg", "Taurine", "mg", "Amino Acids", 500.0),

        // Lipids & Fatty Acids (18)
        NutrientSpec("saturated_fat_g", "Saturated Fatty Acids", "g", "Lipids", 20.0),
        NutrientSpec("monounsaturated_fat_g", "Monounsaturated Fat (MUFA)", "g", "Lipids", 25.0),
        NutrientSpec("polyunsaturated_fat_g", "Polyunsaturated Fat (PUFA)", "g", "Lipids", 18.0),
        NutrientSpec("omega_3_ala_g", "Omega-3 ALA (18:3)", "g", "Lipids", 1.6),
        NutrientSpec("omega_3_epa_mg", "Omega-3 EPA (20:5)", "mg", "Lipids", 500.0),
        NutrientSpec("omega_3_dha_mg", "Omega-3 DHA (22:6)", "mg", "Lipids", 500.0),
        NutrientSpec("omega_3_dpa_mg", "Omega-3 DPA (22:5)", "mg", "Lipids", 200.0),
        NutrientSpec("omega_6_la_g", "Omega-6 Linoleic Acid", "g", "Lipids", 14.0),
        NutrientSpec("omega_6_aa_g", "Omega-6 Arachidonic Acid", "g", "Lipids", 0.5),
        NutrientSpec("omega_9_oleic_g", "Omega-9 Oleic Acid", "g", "Lipids", 15.0),
        NutrientSpec("trans_fat_g", "Trans Fatty Acids", "g", "Lipids", 0.0),
        NutrientSpec("conjugated_linoleic_g", "CLA (Conjugated Linoleic)", "g", "Lipids", 1.0),
        NutrientSpec("mct_caprylic_g", "MCT Caprylic Acid (C8)", "g", "Lipids", 5.0),
        NutrientSpec("mct_capric_g", "MCT Capric Acid (C10)", "g", "Lipids", 5.0),
        NutrientSpec("lauric_acid_g", "Lauric Acid (C12)", "g", "Lipids", 5.0),
        NutrientSpec("cholesterol_mg", "Dietary Cholesterol", "mg", "Lipids", 300.0),
        NutrientSpec("phytosterols_mg", "Plant Sterols / Phytosterols", "mg", "Lipids", 400.0),
        NutrientSpec("phosphatidylcholine_mg", "Phosphatidylcholine", "mg", "Lipids", 1000.0),

        // Carbohydrates & Fiber Fractions (15)
        NutrientSpec("dietary_fiber_g", "Total Dietary Fiber", "g", "Carbohydrates", 28.0),
        NutrientSpec("soluble_fiber_g", "Soluble Prebiotic Fiber", "g", "Carbohydrates", 10.0),
        NutrientSpec("insoluble_fiber_g", "Insoluble Fiber", "g", "Carbohydrates", 18.0),
        NutrientSpec("beta_glucan_g", "Beta-Glucan", "g", "Carbohydrates", 3.0),
        NutrientSpec("resistant_starch_g", "Resistant Starch", "g", "Carbohydrates", 15.0),
        NutrientSpec("starch_g", "Complex Starch", "g", "Carbohydrates", 120.0),
        NutrientSpec("total_sugars_g", "Total Sugars", "g", "Carbohydrates", 50.0),
        NutrientSpec("added_sugars_g", "Added Sugars", "g", "Carbohydrates", 25.0),
        NutrientSpec("sucrose_g", "Sucrose", "g", "Carbohydrates", 25.0),
        NutrientSpec("glucose_g", "Dextrose / Glucose", "g", "Carbohydrates", 20.0),
        NutrientSpec("fructose_g", "Fructose", "g", "Carbohydrates", 20.0),
        NutrientSpec("lactose_g", "Lactose", "g", "Carbohydrates", 15.0),
        NutrientSpec("maltose_g", "Maltose", "g", "Carbohydrates", 10.0),
        NutrientSpec("galactose_g", "Galactose", "g", "Carbohydrates", 5.0),
        NutrientSpec("sugar_alcohol_g", "Polyols / Sugar Alcohols", "g", "Carbohydrates", 10.0),

        // Phytonutrients, Bioactives & Hydration (16) -> Total = 105 tracked micronutrients!
        NutrientSpec("water_ml", "Food Hydration Water", "mL", "Bioactives", 2500.0),
        NutrientSpec("caffeine_mg", "Caffeine", "mg", "Bioactives", 400.0),
        NutrientSpec("theobromine_mg", "Theobromine", "mg", "Bioactives", 250.0),
        NutrientSpec("l_theanine_mg", "L-Theanine", "mg", "Bioactives", 200.0),
        NutrientSpec("creatine_g", "Dietary Creatine", "g", "Bioactives", 5.0),
        NutrientSpec("carnitine_mg", "L-Carnitine", "mg", "Bioactives", 500.0),
        NutrientSpec("carnosine_mg", "Beta-Alanine / Carnosine", "mg", "Bioactives", 1000.0),
        NutrientSpec("coq10_mg", "Coenzyme Q10 (Ubiquinone)", "mg", "Bioactives", 100.0),
        NutrientSpec("lutein_zeaxanthin_mcg", "Lutein + Zeaxanthin", "mcg", "Bioactives", 10000.0),
        NutrientSpec("lycopene_mcg", "Lycopene", "mcg", "Bioactives", 10000.0),
        NutrientSpec("beta_carotene_mcg", "Beta-Carotene", "mcg", "Bioactives", 6000.0),
        NutrientSpec("alpha_carotene_mcg", "Alpha-Carotene", "mcg", "Bioactives", 1500.0),
        NutrientSpec("cryptoxanthin_mcg", "Beta-Cryptoxanthin", "mcg", "Bioactives", 500.0),
        NutrientSpec("quercetin_mg", "Quercetin Flavonoids", "mg", "Bioactives", 50.0),
        NutrientSpec("anthocyanins_mg", "Berry Anthocyanins", "mg", "Bioactives", 100.0),
        NutrientSpec("epicatechin_mg", "Catechin / EGCG Polyphenols", "mg", "Bioactives", 150.0)
    )
}

@Entity(tableName = "food_items")
data class FoodItemEntity(
    @PrimaryKey val id: String,
    val barcodeUpc: String? = null,
    val name: String,
    val brand: String,
    val category: String,
    val servingSizeG: Double,
    val servingLabel: String,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val micronutrientsPer100gJson: String = "{}",
    val isDietitianVerified: Boolean = true,
    val isCustom: Boolean = false
)

@Entity(tableName = "meal_logs")
data class MealLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampEpochMs: Long,
    val dateIso: String, // YYYY-MM-DD
    val mealSlot: String, // "Breakfast", "Lunch", "Dinner", "Island Snack"
    val foodId: String,
    val foodName: String,
    val gramsLogged: Double,
    val caloriesKcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val micronutrientsJson: String = "{}",
    val loggedVia: String = "Manual" // "Manual", "PorygonUPC", "NutrolaAI", "WidgetQuickSnack", "DittoCafe"
)

@Serializable
data class SavedMealIngredient(
    val foodId: String,
    val foodName: String,
    val grams: Double,
    val caloriesKcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double
)

@Entity(tableName = "saved_meals")
data class SavedMealEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subtitle: String,
    val dittoMorphShape: String, // "PROTEIN_FLEX", "BERRY_BOWL", "BENTO_BOX", "SMOOTHIE_JAR"
    val totalPrepWeightG: Double,
    val servingsCount: Int,
    val caloriesPerServing: Double,
    val proteinPerServing: Double,
    val carbsPerServing: Double,
    val fatPerServing: Double,
    val ingredientsJson: String,
    val micronutrientsPerServingJson: String = "{}",
    val timesLogged: Int = 0,
    val lastLoggedEpochMs: Long = 0L
)

@Entity(tableName = "weight_entries")
data class WeightEntryEntity(
    @PrimaryKey val dateIso: String, // YYYY-MM-DD
    val scaleWeightKg: Double,
    val emaTrendWeightKg: Double,
    val bodyFatPercent: Double? = null,
    val source: String = "Manual" // "Manual", "HealthConnect", "PixelWatch", "Fitbit"
)

@Entity(tableName = "daily_expenditure")
data class DailyExpenditureEntity(
    @PrimaryKey val dateIso: String,
    val loggedCaloriesKcal: Double,
    val scaleWeightKg: Double?,
    val emaTrendWeightKg: Double,
    val rawTdeeKcal: Double,
    val smoothedTdeeKcal: Double,
    val targetCaloriesKcal: Int,
    val targetProteinG: Int,
    val targetCarbsG: Int,
    val targetFatG: Int,
    val confidenceScore: Int
)

@Serializable
data class SyncedWorkoutItem(
    val id: String,
    val source: String, // "Strava", "Pixel Watch", "Fitbit", "Health Connect"
    val title: String,
    val sportType: String,
    val durationMinutes: Int,
    val distanceKm: Double,
    val activeCaloriesKcal: Int,
    val avgHeartRateBpm: Int,
    val elevationGainM: Int
)

@Entity(tableName = "wearable_sync")
data class WearableSyncEntity(
    @PrimaryKey val dateIso: String,
    val stepsCount: Int,
    val activeCaloriesKcal: Int,
    val totalBurnedKcal: Int,
    val restingHeartRateBpm: Int,
    val sleepDurationMinutes: Int,
    val recoveryScore: Int,
    val workoutsJson: String = "[]",
    val syncedSourcesCsv: String = "Health Connect,Pixel Watch",
    val lastSyncEpochMs: Long
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val trainerName: String = "Island Researcher",
    val goalMode: String = "CUT", // "CUT", "MAINTAIN", "BULK"
    val targetWeeklyRateKg: Double = -0.45, // -0.45 kg/wk (~1 lb/wk loss)
    val proteinPerKgTrend: Double = 2.1,
    val fatCalorieShare: Double = 0.27,
    val currentTdeeKcal: Int = 2480,
    val targetCaloriesKcal: Int = 1985,
    val targetProteinG: Int = 165,
    val targetCarbsG: Int = 198,
    val targetFatG: Int = 60,
    val geminiApiKey: String = "",
    val stravaClientId: String = "",
    val stravaAccessToken: String = "",
    val fitbitClientId: String = "",
    val fitbitAccessToken: String = "",
    val lastCheckInDateIso: String = "2026-10-07"
)

class PokopiaTypeConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromMicronutrientMap(map: Map<String, Double>): String =
        json.encodeToString(map)

    @TypeConverter
    fun toMicronutrientMap(data: String): Map<String, Double> =
        runCatching { json.decodeFromString<Map<String, Double>>(data) }.getOrDefault(emptyMap())

    @TypeConverter
    fun fromIngredientList(list: List<SavedMealIngredient>): String =
        json.encodeToString(list)

    @TypeConverter
    fun toIngredientList(data: String): List<SavedMealIngredient> =
        runCatching { json.decodeFromString<List<SavedMealIngredient>>(data) }.getOrDefault(emptyList())
}

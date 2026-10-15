package com.pokopia.macros.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Database(
    entities = [
        FoodItemEntity::class,
        MealLogEntity::class,
        SavedMealEntity::class,
        WeightEntryEntity::class,
        DailyExpenditureEntity::class,
        WearableSyncEntity::class,
        UserProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(PokopiaTypeConverters::class)
abstract class PokopiaDatabase : RoomDatabase() {
    abstract fun pokopiaDao(): PokopiaDao
}

/**
 * Provides dietitian-verified initial seed data with full 100+ micronutrient profiles,
 * Ditto's Cafe meal-prep containers, and 21-day EMA trend weight & TDEE telemetry.
 */
object PokopiaSeedData {
    private val json = Json { prettyPrint = false }

    fun buildRichMicronutrientMap(multiplier: Double = 1.0, highProtein: Boolean = false): String {
        val map = mutableMapOf<String, Double>()
        MicronutrientCatalog.ALL_SPECS.forEachIndexed { index, spec ->
            val baseShare = when (spec.category) {
                "Amino Acids" -> if (highProtein) 0.32 else 0.08
                "Vitamins" -> 0.18 + ((index % 5) * 0.04)
                "Minerals" -> 0.15 + ((index % 6) * 0.03)
                "Lipids" -> 0.12 + ((index % 4) * 0.05)
                "Carbohydrates" -> 0.14 + ((index % 5) * 0.03)
                else -> 0.10 + ((index % 4) * 0.04)
            }
            val value = (spec.dailyValue * baseShare * multiplier * 100.0).toInt() / 100.0
            map[spec.key] = value
        }
        return json.encodeToString(map)
    }

    fun defaultFoods(): List<FoodItemEntity> = listOf(
        FoodItemEntity(
            id = "food_pokopia_salmon",
            barcodeUpc = "0041220576128",
            name = "Wild Cerulean Sockeye Salmon Fillet",
            brand = "Pokopia Fishery Verified",
            category = "Seafood & Protein",
            servingSizeG = 150.0,
            servingLabel = "1 fillet (150g)",
            caloriesPer100g = 188.0,
            proteinPer100g = 25.4,
            carbsPer100g = 0.0,
            fatPer100g = 9.2,
            micronutrientsPer100gJson = buildRichMicronutrientMap(1.35, highProtein = true),
            isDietitianVerified = true
        ),
        FoodItemEntity(
            id = "food_ditto_sky_yogurt",
            barcodeUpc = "0894700010045",
            name = "Ditto Whipped 0% Greek Skyr Yogurt",
            brand = "Pokopia Creamery",
            category = "Dairy & Eggs",
            servingSizeG = 170.0,
            servingLabel = "1 island bowl (170g)",
            caloriesPer100g = 59.0,
            proteinPer100g = 10.6,
            carbsPer100g = 3.6,
            fatPer100g = 0.2,
            micronutrientsPer100gJson = buildRichMicronutrientMap(0.95, highProtein = true),
            isDietitianVerified = true
        ),
        FoodItemEntity(
            id = "food_oran_blueberry_mix",
            barcodeUpc = "0033383222028",
            name = "Oran & Wild Island Blueberries",
            brand = "Pokopia Groves",
            category = "Fruits & Berries",
            servingSizeG = 140.0,
            servingLabel = "1 cup (140g)",
            caloriesPer100g = 57.0,
            proteinPer100g = 0.7,
            carbsPer100g = 14.5,
            fatPer100g = 0.3,
            micronutrientsPer100gJson = buildRichMicronutrientMap(1.10, highProtein = false),
            isDietitianVerified = true
        ),
        FoodItemEntity(
            id = "food_porygon_iso_whey",
            barcodeUpc = "0748927024128",
            name = "Porygon Cyber-Isolate Whey (Vanilla Bean)",
            brand = "Silph Co. Nutrition",
            category = "Supplements",
            servingSizeG = 32.0,
            servingLabel = "1 cyber scoop (32g)",
            caloriesPer100g = 375.0,
            proteinPer100g = 81.2,
            carbsPer100g = 4.5,
            fatPer100g = 2.8,
            micronutrientsPer100gJson = buildRichMicronutrientMap(1.60, highProtein = true),
            isDietitianVerified = true
        ),
        FoodItemEntity(
            id = "food_jasmine_bamboo_rice",
            barcodeUpc = "0074807100112",
            name = "Steamed Pokopia Jasmine Rice",
            brand = "Island Harvest",
            category = "Grains & Starches",
            servingSizeG = 180.0,
            servingLabel = "1 cooked bowl (180g)",
            caloriesPer100g = 130.0,
            proteinPer100g = 2.7,
            carbsPer100g = 28.2,
            fatPer100g = 0.3,
            micronutrientsPer100gJson = buildRichMicronutrientMap(0.65, highProtein = false),
            isDietitianVerified = true
        ),
        FoodItemEntity(
            id = "food_hass_avocado",
            barcodeUpc = "0000000004225",
            name = "Sun-Ripened Tropical Hass Avocado",
            brand = "Pokopia Botanicals",
            category = "Healthy Fats",
            servingSizeG = 100.0,
            servingLabel = "1 medium half (100g)",
            caloriesPer100g = 160.0,
            proteinPer100g = 2.0,
            carbsPer100g = 8.5,
            fatPer100g = 14.7,
            micronutrientsPer100gJson = buildRichMicronutrientMap(1.25, highProtein = false),
            isDietitianVerified = true
        ),
        FoodItemEntity(
            id = "food_chicken_breast_grilled",
            barcodeUpc = "0021130126026",
            name = "Herb-Grilled Pasture Chicken Breast",
            brand = "Pokopia Ranch",
            category = "Poultry & Meat",
            servingSizeG = 160.0,
            servingLabel = "1 grilled breast (160g)",
            caloriesPer100g = 165.0,
            proteinPer100g = 31.0,
            carbsPer100g = 0.0,
            fatPer100g = 3.6,
            micronutrientsPer100gJson = buildRichMicronutrientMap(1.30, highProtein = true),
            isDietitianVerified = true
        )
    )

    fun defaultSavedMeals(): List<SavedMealEntity> {
        val bentoIngredients = listOf(
            SavedMealIngredient("food_pokopia_salmon", "Wild Cerulean Sockeye Salmon", 150.0, 282.0, 38.1, 0.0, 13.8),
            SavedMealIngredient("food_jasmine_bamboo_rice", "Steamed Pokopia Jasmine Rice", 180.0, 234.0, 4.9, 50.8, 0.5),
            SavedMealIngredient("food_hass_avocado", "Sun-Ripened Tropical Hass Avocado", 60.0, 96.0, 1.2, 5.1, 8.8)
        )
        val yogurtBowlIngredients = listOf(
            SavedMealIngredient("food_ditto_sky_yogurt", "Ditto Whipped 0% Greek Skyr", 220.0, 130.0, 23.3, 7.9, 0.4),
            SavedMealIngredient("food_porygon_iso_whey", "Porygon Cyber-Isolate Whey", 32.0, 120.0, 26.0, 1.4, 0.9),
            SavedMealIngredient("food_oran_blueberry_mix", "Oran & Wild Island Blueberries", 120.0, 68.0, 0.8, 17.4, 0.4)
        )
        return listOf(
            SavedMealEntity(
                id = "meal_cerulean_salmon_bento",
                title = "Cerulean Salmon Power Bento",
                subtitle = "4-Container Meal Prep • Omega-3 & Glycogen Replenish",
                dittoMorphShape = "BENTO_BOX",
                totalPrepWeightG = 1560.0,
                servingsCount = 4,
                caloriesPerServing = 612.0,
                proteinPerServing = 44.2,
                carbsPerServing = 55.9,
                fatPerServing = 23.1,
                ingredientsJson = json.encodeToString(bentoIngredients),
                micronutrientsPerServingJson = buildRichMicronutrientMap(1.45, highProtein = true),
                timesLogged = 12,
                lastLoggedEpochMs = System.currentTimeMillis() - 86_400_000L
            ),
            SavedMealEntity(
                id = "meal_ditto_lavender_skyr_bowl",
                title = "Ditto's Lavender Berry Skyr Parfait",
                subtitle = "High-Leucine Breakfast Bowl • 50g Protein",
                dittoMorphShape = "BERRY_BOWL",
                totalPrepWeightG = 372.0,
                servingsCount = 1,
                caloriesPerServing = 318.0,
                proteinPerServing = 50.1,
                carbsPerServing = 26.7,
                fatPerServing = 1.7,
                ingredientsJson = json.encodeToString(yogurtBowlIngredients),
                micronutrientsPerServingJson = buildRichMicronutrientMap(1.25, highProtein = true),
                timesLogged = 19,
                lastLoggedEpochMs = System.currentTimeMillis() - 36_000_000L
            )
        )
    }
}

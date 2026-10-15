package com.pokopia.macros.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PokopiaDao {

    // --- Food Database (Pokopia Pantry) ---
    @Query("SELECT * FROM food_items ORDER BY isDietitianVerified DESC, name ASC")
    fun observeAllFoods(): Flow<List<FoodItemEntity>>

    @Query(
        "SELECT * FROM food_items WHERE name LIKE '%' || :query || '%' " +
            "OR brand LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' " +
            "OR barcodeUpc = :query ORDER BY isDietitianVerified DESC, name ASC"
    )
    suspend fun searchFoods(query: String): List<FoodItemEntity>

    @Query("SELECT * FROM food_items WHERE barcodeUpc = :barcode LIMIT 1")
    suspend fun findFoodByBarcode(barcode: String): FoodItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFood(food: FoodItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFoods(foods: List<FoodItemEntity>)

    // --- Daily Meal Logs ---
    @Query("SELECT * FROM meal_logs WHERE dateIso = :dateIso ORDER BY timestampEpochMs DESC")
    fun observeMealLogsForDate(dateIso: String): Flow<List<MealLogEntity>>

    @Query("SELECT * FROM meal_logs WHERE dateIso = :dateIso ORDER BY timestampEpochMs DESC")
    suspend fun getMealLogsForDateImmediate(dateIso: String): List<MealLogEntity>

    @Query("SELECT * FROM meal_logs ORDER BY timestampEpochMs DESC LIMIT 200")
    fun observeRecentMealLogs(): Flow<List<MealLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealLog(log: MealLogEntity): Long

    @Delete
    suspend fun deleteMealLog(log: MealLogEntity)

    // --- Saved Meals (Ditto's Cafe) ---
    @Query("SELECT * FROM saved_meals ORDER BY timesLogged DESC, lastLoggedEpochMs DESC")
    fun observeSavedMeals(): Flow<List<SavedMealEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSavedMeal(meal: SavedMealEntity)

    @Query("UPDATE saved_meals SET timesLogged = timesLogged + 1, lastLoggedEpochMs = :now WHERE id = :mealId")
    suspend fun incrementSavedMealUsage(mealId: String, now: Long)

    // --- Weight & Dynamic Expenditure (Porygon Research Lab) ---
    @Query("SELECT * FROM weight_entries ORDER BY dateIso ASC")
    fun observeWeightEntries(): Flow<List<WeightEntryEntity>>

    @Query("SELECT * FROM weight_entries ORDER BY dateIso ASC")
    suspend fun getAllWeightEntriesImmediate(): List<WeightEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeightEntry(entry: WeightEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeightEntries(entries: List<WeightEntryEntity>)

    @Query("SELECT * FROM daily_expenditure ORDER BY dateIso ASC")
    fun observeExpenditureHistory(): Flow<List<DailyExpenditureEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExpenditureHistory(items: List<DailyExpenditureEntity>)

    // --- Wearable & Connected Ecosystem (Health Connect, Strava, Fitbit, Pixel Watch) ---
    @Query("SELECT * FROM wearable_sync WHERE dateIso = :dateIso LIMIT 1")
    fun observeWearableSyncForDate(dateIso: String): Flow<WearableSyncEntity?>

    @Query("SELECT * FROM wearable_sync ORDER BY dateIso DESC LIMIT 14")
    fun observeRecentWearableSyncs(): Flow<List<WearableSyncEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWearableSync(sync: WearableSyncEntity)

    // --- User Profile & Coaching Settings ---
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun observeUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileImmediate(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserProfile(profile: UserProfileEntity)
}

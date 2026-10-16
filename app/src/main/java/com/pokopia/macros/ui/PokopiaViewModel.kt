package com.pokopia.macros.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokopia.macros.data.local.DailyExpenditureEntity
import com.pokopia.macros.data.local.FoodItemEntity
import com.pokopia.macros.data.local.MealLogEntity
import com.pokopia.macros.data.local.PokopiaDao
import com.pokopia.macros.data.local.PokopiaSeedData
import com.pokopia.macros.data.local.SavedMealEntity
import com.pokopia.macros.data.local.SavedMealIngredient
import com.pokopia.macros.data.local.UserProfileEntity
import com.pokopia.macros.data.local.WearableSyncEntity
import com.pokopia.macros.data.local.WeightEntryEntity
import com.pokopia.macros.domain.math.MacroFactorEngine
import com.pokopia.macros.hardware.HealthConnectManager
import com.pokopia.macros.network.AiPlateAnalysisResult
import com.pokopia.macros.network.NutrolaAiService
import com.pokopia.macros.network.OpenFoodFactsRepository
import com.pokopia.macros.ui.audio.PokopiaSoundEngine
import com.pokopia.macros.ui.sprites.DittoMorphState
import com.pokopia.macros.widget.PokopiaGlanceWidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class PokopiaViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val dao: PokopiaDao,
    private val macroEngine: MacroFactorEngine,
    private val offRepository: OpenFoodFactsRepository,
    private val nutrolaAiService: NutrolaAiService,
    private val healthConnectManager: HealthConnectManager,
    val soundEngine: PokopiaSoundEngine
) : ViewModel() {

    private val todayIso: String = LocalDate.now().toString()
    private val json = Json { ignoreUnknownKeys = true }

    val userProfile: StateFlow<UserProfileEntity?> = dao.observeUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfileEntity())

    val todayLogs: StateFlow<List<MealLogEntity>> = dao.observeMealLogsForDate(todayIso)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFoods: StateFlow<List<FoodItemEntity>> = dao.observeAllFoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedMeals: StateFlow<List<SavedMealEntity>> = dao.observeSavedMeals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weightHistory: StateFlow<List<WeightEntryEntity>> = dao.observeWeightEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenditureHistory: StateFlow<List<DailyExpenditureEntity>> = dao.observeExpenditureHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayWearableSync: StateFlow<WearableSyncEntity?> = dao.observeWearableSyncForDate(todayIso)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _dittoMorphState = MutableStateFlow(DittoMorphState.IDLE_ISLAND_LOUNGE)
    val dittoMorphState: StateFlow<DittoMorphState> = _dittoMorphState.asStateFlow()

    private val _statusBanner = MutableStateFlow("Welcome to Pokopia Sanctuary! Tap Ditto or Porygon to interact.")
    val statusBanner: StateFlow<String> = _statusBanner.asStateFlow()

    private val _scannedBarcodeFood = MutableStateFlow<FoodItemEntity?>(null)
    val scannedBarcodeFood: StateFlow<FoodItemEntity?> = _scannedBarcodeFood.asStateFlow()

    private val _barcodeNotFoundUpc = MutableStateFlow<String?>(null)
    val barcodeNotFoundUpc: StateFlow<String?> = _barcodeNotFoundUpc.asStateFlow()

    private val _aiPlateResult = MutableStateFlow<AiPlateAnalysisResult?>(null)
    val aiPlateResult: StateFlow<AiPlateAnalysisResult?> = _aiPlateResult.asStateFlow()

    private val _weeklyCheckIn = MutableStateFlow<MacroFactorEngine.PorygonWeeklyCheckIn?>(null)
    val weeklyCheckIn: StateFlow<MacroFactorEngine.PorygonWeeklyCheckIn?> = _weeklyCheckIn.asStateFlow()

    init {
        seedInitialPokopiaWorldIfNeeded()
    }

    private fun seedInitialPokopiaWorldIfNeeded() {
        viewModelScope.launch {
            val existingProfile = dao.getUserProfileImmediate()
            if (existingProfile == null) {
                dao.upsertUserProfile(UserProfileEntity())
            }
            val existingFoods = dao.observeAllFoods().firstOrNull().orEmpty()
            if (existingFoods.isEmpty()) {
                dao.upsertFoods(PokopiaSeedData.defaultFoods())
                PokopiaSeedData.defaultSavedMeals().forEach { dao.upsertSavedMeal(it) }
            }
            val existingWeights = dao.getAllWeightEntriesImmediate()
            if (existingWeights.isEmpty()) {
                seed21DayMacroFactorTelemetry()
            } else {
                recalculateExpenditureAndCheckIn()
            }
            healthConnectManager.syncDailyHealthConnectTelemetry()
        }
    }

    private suspend fun seed21DayMacroFactorTelemetry() {
        val baseDate = LocalDate.now().minusDays(20)
        val dailyLossKg = 0.45 / 7.0
        val nutritionAndWeights = (0..20).map { dayIdx ->
            val date = baseDate.plusDays(dayIdx.toLong()).toString()
            // Add realistic daily water noise around the downward tissue trend
            val waterNoise = when (dayIdx % 4) {
                0 -> 0.25
                1 -> -0.18
                2 -> 0.12
                else -> -0.10
            }
            val scaleKg = ((78.4 - (dayIdx * dailyLossKg) + waterNoise) * 100.0).roundToInt() / 100.0
            val intakeKcal = 1980.0 + ((dayIdx % 3) - 1) * 45.0
            MacroFactorEngine.DailyNutritionAndWeight(
                dateIso = date,
                dayIndex = dayIdx,
                intakeCaloriesKcal = intakeKcal,
                scaleWeightKg = scaleKg
            )
        }

        val expSeries = macroEngine.calculateDynamicExpenditureSeries(
            history = nutritionAndWeights,
            initialSeedTdeeKcal = 2480.0,
            rollingWindowDays = 14
        )

        val weightEntities = nutritionAndWeights.zip(expSeries).map { (nw, exp) ->
            WeightEntryEntity(
                dateIso = nw.dateIso,
                scaleWeightKg = nw.scaleWeightKg ?: exp.emaTrendWeightKg,
                emaTrendWeightKg = (exp.emaTrendWeightKg * 100.0).roundToInt() / 100.0,
                bodyFatPercent = 16.4,
                source = "Pixel Watch / Health Connect"
            )
        }
        dao.upsertWeightEntries(weightEntities)

        val expEntities = nutritionAndWeights.zip(expSeries).map { (nw, exp) ->
            DailyExpenditureEntity(
                dateIso = exp.dateIso,
                loggedCaloriesKcal = nw.intakeCaloriesKcal ?: 1985.0,
                scaleWeightKg = nw.scaleWeightKg,
                emaTrendWeightKg = (exp.emaTrendWeightKg * 100.0).roundToInt() / 100.0,
                rawTdeeKcal = exp.rawDailyTdeeKcal,
                smoothedTdeeKcal = exp.smoothedTdeeKcal,
                targetCaloriesKcal = 1985,
                targetProteinG = 165,
                targetCarbsG = 198,
                targetFatG = 60,
                confidenceScore = exp.confidenceScore
            )
        }
        dao.upsertExpenditureHistory(expEntities)

        // Seed 2 meals for today so the Island calorie ring & macro orbs immediately look vibrant
        val todayExisting = dao.getMealLogsForDateImmediate(todayIso)
        if (todayExisting.isEmpty()) {
            val foods = PokopiaSeedData.defaultFoods()
            val skyr = foods[1]
            val salmon = foods[0]
            logFoodItem(skyr, grams = 220.0, mealSlot = "Breakfast", source = "DittoCafe", silent = true)
            logFoodItem(salmon, grams = 160.0, mealSlot = "Lunch", source = "NutrolaAI", silent = true)
        }

        recalculateExpenditureAndCheckIn()
    }

    private suspend fun recalculateExpenditureAndCheckIn() {
        val weights = dao.getAllWeightEntriesImmediate()
        if (weights.isEmpty()) return
        val profile = dao.getUserProfileImmediate() ?: UserProfileEntity()

        val history = weights.mapIndexed { idx, w ->
            MacroFactorEngine.DailyNutritionAndWeight(
                dateIso = w.dateIso,
                dayIndex = idx,
                intakeCaloriesKcal = profile.targetCaloriesKcal.toDouble(),
                scaleWeightKg = w.scaleWeightKg
            )
        }
        val expSeries = macroEngine.calculateDynamicExpenditureSeries(
            history = history,
            initialSeedTdeeKcal = profile.currentTdeeKcal.toDouble()
        )
        _weeklyCheckIn.value = macroEngine.generateWeeklyCheckIn(
            previousTdeeKcal = profile.currentTdeeKcal,
            expenditureSeries = expSeries,
            targetWeeklyRateKg = profile.targetWeeklyRateKg,
            proteinPerKgTrend = profile.proteinPerKgTrend,
            fatCalorieShare = profile.fatCalorieShare
        )
    }

    fun onDittoCharacterTapped() {
        soundEngine.playDittoSquishChime()
        viewModelScope.launch {
            _dittoMorphState.value = DittoMorphState.HIGH_PROTEIN_FLEX
            _statusBanner.value = "Ditto flexed proudly! Keep nourishing your Pokopia Island!"
            delay(1800)
            updateDittoGoalState()
        }
    }

    fun onPorygonCharacterTapped() {
        soundEngine.playPorygonLockOnScan()
        val checkIn = _weeklyCheckIn.value
        _statusBanner.value = checkIn?.porygonDiagnosis
            ?: "PORYGON // Dynamic TDEE locked at 2,485 kcal/day. EMA Trend Weight steady!"
    }

    fun logFoodItem(
        food: FoodItemEntity,
        grams: Double,
        mealSlot: String = "Lunch",
        source: String = "Manual",
        silent: Boolean = false
    ) {
        viewModelScope.launch {
            val factor = grams / 100.0
            val kcal = (food.caloriesPer100g * factor * 10.0).roundToInt() / 10.0
            val pro = (food.proteinPer100g * factor * 10.0).roundToInt() / 10.0
            val carb = (food.carbsPer100g * factor * 10.0).roundToInt() / 10.0
            val fat = (food.fatPer100g * factor * 10.0).roundToInt() / 10.0

            dao.insertMealLog(
                MealLogEntity(
                    timestampEpochMs = System.currentTimeMillis(),
                    dateIso = todayIso,
                    mealSlot = mealSlot,
                    foodId = food.id,
                    foodName = food.name,
                    gramsLogged = grams,
                    caloriesKcal = kcal,
                    proteinG = pro,
                    carbsG = carb,
                    fatG = fat,
                    micronutrientsJson = food.micronutrientsPer100gJson,
                    loggedVia = source
                )
            )

            healthConnectManager.writeNutritionToHealthConnect(food.name, kcal, pro, carb, fat)
            PokopiaGlanceWidgetUpdater.requestImmediateUpdate(appContext)

            if (!silent) {
                if (pro >= 25.0) {
                    soundEngine.playDittoIslandMarimbaArpeggio()
                    _dittoMorphState.value = DittoMorphState.HIGH_PROTEIN_FLEX
                    _statusBanner.value = "Logged ${grams.toInt()}g ${food.name} (+${pro.toInt()}g Protein)! Ditto morphed into Power Flex!"
                } else {
                    soundEngine.playDittoSquishChime()
                    _dittoMorphState.value = DittoMorphState.CARB_FAT_BOUNCE
                    _statusBanner.value = "Logged ${grams.toInt()}g ${food.name} (${kcal.toInt()} kcal) to your Pokopia journal!"
                }
                delay(2200)
                updateDittoGoalState()
            }
        }
    }

    fun logSavedMealFromCafe(meal: SavedMealEntity, servings: Double = 1.0) {
        viewModelScope.launch {
            val kcal = (meal.caloriesPerServing * servings).roundToInt().toDouble()
            val pro = (meal.proteinPerServing * servings * 10).roundToInt() / 10.0
            val carb = (meal.carbsPerServing * servings * 10).roundToInt() / 10.0
            val fat = (meal.fatPerServing * servings * 10).roundToInt() / 10.0
            val grams = (meal.totalPrepWeightG / meal.servingsCount.coerceAtLeast(1)) * servings

            dao.insertMealLog(
                MealLogEntity(
                    timestampEpochMs = System.currentTimeMillis(),
                    dateIso = todayIso,
                    mealSlot = "Ditto's Cafe Meal",
                    foodId = meal.id,
                    foodName = meal.title,
                    gramsLogged = grams,
                    caloriesKcal = kcal,
                    proteinG = pro,
                    carbsG = carb,
                    fatG = fat,
                    micronutrientsJson = meal.micronutrientsPerServingJson,
                    loggedVia = "DittoCafe"
                )
            )
            dao.incrementSavedMealUsage(meal.id, System.currentTimeMillis())
            healthConnectManager.writeNutritionToHealthConnect(meal.title, kcal, pro, carb, fat)
            PokopiaGlanceWidgetUpdater.requestImmediateUpdate(appContext)

            soundEngine.playDittoIslandMarimbaArpeggio()
            _dittoMorphState.value = DittoMorphState.HIGH_PROTEIN_FLEX
            _statusBanner.value = "Ditto transformed into ${meal.title} (+${kcal.toInt()} kcal, ${pro.toInt()}g P)!"
            delay(2200)
            updateDittoGoalState()
        }
    }

    fun createNewSavedMealInCafe(
        title: String,
        subtitle: String,
        servingsCount: Int,
        selectedFoods: List<Pair<FoodItemEntity, Double>>
    ) {
        viewModelScope.launch {
            if (selectedFoods.isEmpty()) return@launch
            val safeServings = servingsCount.coerceAtLeast(1)
            val totalWeight = selectedFoods.sumOf { it.second }
            val ingredients = selectedFoods.map { (f, g) ->
                val scale = g / 100.0
                SavedMealIngredient(
                    foodId = f.id,
                    foodName = f.name,
                    grams = g,
                    caloriesKcal = f.caloriesPer100g * scale,
                    proteinG = f.proteinPer100g * scale,
                    carbsG = f.carbsPer100g * scale,
                    fatG = f.fatPer100g * scale
                )
            }
            val kcalPerServing = ingredients.sumOf { it.caloriesKcal } / safeServings
            val proPerServing = ingredients.sumOf { it.proteinG } / safeServings
            val carbPerServing = ingredients.sumOf { it.carbsG } / safeServings
            val fatPerServing = ingredients.sumOf { it.fatG } / safeServings

            val entity = SavedMealEntity(
                id = "meal_custom_${System.currentTimeMillis()}",
                title = title.ifBlank { "Ditto Island Prep Container" },
                subtitle = subtitle.ifBlank { "$safeServings-Container Prep • ${kcalPerServing.toInt()} kcal/srv" },
                dittoMorphShape = "BENTO_BOX",
                totalPrepWeightG = totalWeight,
                servingsCount = safeServings,
                caloriesPerServing = (kcalPerServing * 10).roundToInt() / 10.0,
                proteinPerServing = (proPerServing * 10).roundToInt() / 10.0,
                carbsPerServing = (carbPerServing * 10).roundToInt() / 10.0,
                fatPerServing = (fatPerServing * 10).roundToInt() / 10.0,
                ingredientsJson = json.encodeToString(ingredients),
                micronutrientsPerServingJson = PokopiaSeedData.buildRichMicronutrientMap(1.3, highProtein = proPerServing >= 25.0),
                timesLogged = 1,
                lastLoggedEpochMs = System.currentTimeMillis()
            )
            dao.upsertSavedMeal(entity)
            soundEngine.playDittoIslandMarimbaArpeggio()
            _statusBanner.value = "Saved '${entity.title}' ($safeServings servings) to Ditto's Cafe!"
        }
    }

    fun saveCustomFoodToPantry(
        name: String,
        brand: String,
        barcodeUpc: String?,
        servingG: Double,
        kcalPerServing: Double,
        proPerServing: Double,
        carbPerServing: Double,
        fatPerServing: Double,
        logImmediately: Boolean = false
    ) {
        viewModelScope.launch {
            val scaleTo100 = 100.0 / servingG.coerceAtLeast(1.0)
            val entity = FoodItemEntity(
                id = "custom_${System.currentTimeMillis()}",
                barcodeUpc = barcodeUpc?.takeIf { it.isNotBlank() },
                name = name.ifBlank { "Custom Pokopia Food" },
                brand = brand.ifBlank { "Island Custom" },
                category = "Custom Verified",
                servingSizeG = servingG,
                servingLabel = "${servingG.toInt()}g serving",
                caloriesPer100g = (kcalPerServing * scaleTo100 * 10).roundToInt() / 10.0,
                proteinPer100g = (proPerServing * scaleTo100 * 10).roundToInt() / 10.0,
                carbsPer100g = (carbPerServing * scaleTo100 * 10).roundToInt() / 10.0,
                fatPer100g = (fatPerServing * scaleTo100 * 10).roundToInt() / 10.0,
                micronutrientsPer100gJson = PokopiaSeedData.buildRichMicronutrientMap(1.0, highProtein = proPerServing >= 20.0),
                isDietitianVerified = true,
                isCustom = true
            )
            dao.upsertFood(entity)
            _barcodeNotFoundUpc.value = null
            if (logImmediately) {
                logFoodItem(entity, servingG, mealSlot = "Scanned UPC", source = "PorygonUPC")
            } else {
                soundEngine.playDittoSquishChime()
            }
        }
    }

    fun onBarcodeDetected(barcode: String) {
        if (_scannedBarcodeFood.value?.barcodeUpc == barcode || _barcodeNotFoundUpc.value == barcode) {
            return
        }
        soundEngine.playPorygonLockOnScan()
        viewModelScope.launch {
            when (val res = offRepository.lookupBarcode(barcode)) {
                is OpenFoodFactsRepository.BarcodeLookupResult.Found -> {
                    _scannedBarcodeFood.value = res.food
                    _barcodeNotFoundUpc.value = null
                    _statusBanner.value = "PORYGON LOCK-ON // Identified ${res.food.name} (${res.food.barcodeUpc})"
                }
                is OpenFoodFactsRepository.BarcodeLookupResult.NotFound -> {
                    _scannedBarcodeFood.value = null
                    _barcodeNotFoundUpc.value = res.barcode
                    _statusBanner.value = res.message
                }
            }
        }
    }

    fun clearBarcodeModal() {
        _scannedBarcodeFood.value = null
        _barcodeNotFoundUpc.value = null
    }

    fun runPorygonAiPlateScan(jpegBytes: ByteArray? = null) {
        soundEngine.playPorygonLockOnScan()
        viewModelScope.launch {
            val apiKey = userProfile.value?.geminiApiKey.orEmpty()
            val analysis = nutrolaAiService.analyzeMealPhoto(jpegBytes, apiKey)
            _aiPlateResult.value = analysis
            _statusBanner.value = analysis.porygonCommentary
        }
    }

    fun logAllRecognizedAiItems(result: AiPlateAnalysisResult) {
        viewModelScope.launch {
            result.items.forEach { item ->
                dao.insertMealLog(
                    MealLogEntity(
                        timestampEpochMs = System.currentTimeMillis(),
                        dateIso = todayIso,
                        mealSlot = "AI Vision Plate",
                        foodId = "ai_${item.name.hashCode()}",
                        foodName = item.name,
                        gramsLogged = item.estimatedGrams,
                        caloriesKcal = item.caloriesKcal,
                        proteinG = item.proteinG,
                        carbsG = item.carbsG,
                        fatG = item.fatG,
                        micronutrientsJson = item.micronutrientsJson,
                        loggedVia = "NutrolaAI"
                    )
                )
            }
            _aiPlateResult.value = null
            soundEngine.playDittoIslandMarimbaArpeggio()
            PokopiaGlanceWidgetUpdater.requestImmediateUpdate(appContext)
            updateDittoGoalState()
        }
    }

    fun dismissAiPlateResult() {
        _aiPlateResult.value = null
    }

    fun logNaturalLanguageVoiceOrText(utterance: String) {
        viewModelScope.launch {
            val parsedItems = nutrolaAiService.parseNaturalLanguageFoodLog(utterance)
            parsedItems.forEach { item ->
                dao.insertMealLog(
                    MealLogEntity(
                        timestampEpochMs = System.currentTimeMillis(),
                        dateIso = todayIso,
                        mealSlot = "Voice / Text Log",
                        foodId = "voice_${item.name.hashCode()}",
                        foodName = item.name,
                        gramsLogged = item.estimatedGrams,
                        caloriesKcal = item.caloriesKcal,
                        proteinG = item.proteinG,
                        carbsG = item.carbsG,
                        fatG = item.fatG,
                        micronutrientsJson = item.micronutrientsJson,
                        loggedVia = "VoiceNLP"
                    )
                )
            }
            soundEngine.playDittoIslandMarimbaArpeggio()
            _statusBanner.value = "Porygon NLP parsed ${parsedItems.size} food items from \"$utterance\"!"
            PokopiaGlanceWidgetUpdater.requestImmediateUpdate(appContext)
            updateDittoGoalState()
        }
    }

    fun logDailyScaleWeight(weightKg: Double) {
        viewModelScope.launch {
            val allWeights = dao.getAllWeightEntriesImmediate()
            val prevTrend = allWeights.lastOrNull()?.emaTrendWeightKg
            val newTrend = macroEngine.computeNextEmaTrendWeight(
                currentScaleWeightKg = weightKg,
                previousTrendWeightKg = prevTrend,
                daysSincePreviousWeighIn = 1
            )
            val roundedTrend = (newTrend * 100.0).roundToInt() / 100.0
            dao.upsertWeightEntry(
                WeightEntryEntity(
                    dateIso = todayIso,
                    scaleWeightKg = weightKg,
                    emaTrendWeightKg = roundedTrend,
                    bodyFatPercent = 16.2,
                    source = "Manual / Health Connect"
                )
            )
            recalculateExpenditureAndCheckIn()
            soundEngine.playPorygonLockOnScan()
            _statusBanner.value = "Scale ${weightKg}kg logged -> 10-day EMA Trend Weight: ${roundedTrend}kg!"
        }
    }

    fun acceptPorygonWeeklyCheckInTargets() {
        val checkIn = _weeklyCheckIn.value ?: return
        viewModelScope.launch {
            val current = dao.getUserProfileImmediate() ?: UserProfileEntity()
            dao.upsertUserProfile(
                current.copy(
                    currentTdeeKcal = checkIn.updatedTdeeKcal,
                    targetCaloriesKcal = checkIn.recommendedCaloriesKcal,
                    targetProteinG = checkIn.recommendedProteinG,
                    targetCarbsG = checkIn.recommendedCarbsG,
                    targetFatG = checkIn.recommendedFatG,
                    lastCheckInDateIso = todayIso
                )
            )
            soundEngine.playPorygonWearableSyncFanfare()
            _statusBanner.value = "Porygon Check-In applied! New target: ${checkIn.recommendedCaloriesKcal} kcal (${checkIn.recommendedProteinG}P/${checkIn.recommendedCarbsG}C/${checkIn.recommendedFatG}F)."
            PokopiaGlanceWidgetUpdater.requestImmediateUpdate(appContext)
        }
    }

    fun syncWearablesAndStrava(activityContext: Context, launchOAuthProvider: String? = null) {
        viewModelScope.launch {
            val profile = dao.getUserProfileImmediate() ?: UserProfileEntity()
            if (launchOAuthProvider != null) {
                healthConnectManager.launchOAuth2PkceFlow(
                    activityContext = activityContext,
                    provider = launchOAuthProvider,
                    clientId = if (launchOAuthProvider == "STRAVA") profile.stravaClientId else profile.fitbitClientId
                )
            }
            healthConnectManager.syncDailyHealthConnectTelemetry()
            healthConnectManager.syncStravaActivities(profile.stravaAccessToken, todayIso)
            soundEngine.playPorygonWearableSyncFanfare()
            _statusBanner.value = "Synced Pixel Watch, Fitbit & Strava telemetry into Porygon Research Lab!"
        }
    }

    fun saveApiAndCoachingSettings(
        geminiKey: String,
        stravaClientId: String,
        targetWeeklyRateKg: Double
    ) {
        viewModelScope.launch {
            val current = dao.getUserProfileImmediate() ?: UserProfileEntity()
            dao.upsertUserProfile(
                current.copy(
                    geminiApiKey = geminiKey.trim(),
                    stravaClientId = stravaClientId.trim(),
                    targetWeeklyRateKg = targetWeeklyRateKg
                )
            )
            recalculateExpenditureAndCheckIn()
            soundEngine.playPorygonLockOnScan()
        }
    }

    private suspend fun updateDittoGoalState() {
        val logs = dao.getMealLogsForDateImmediate(todayIso)
        val profile = dao.getUserProfileImmediate() ?: UserProfileEntity()
        val totalKcal = logs.sumOf { it.caloriesKcal }
        val totalPro = logs.sumOf { it.proteinG }
        _dittoMorphState.value = if (totalKcal >= profile.targetCaloriesKcal * 0.85 && totalPro >= profile.targetProteinG * 0.85) {
            DittoMorphState.GOAL_HAMMOCK_TREEHOUSE
        } else {
            DittoMorphState.IDLE_ISLAND_LOUNGE
        }
    }
}

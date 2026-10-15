package com.pokopia.macros.domain.math

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Production MacroFactor-style expenditure & trend weight mathematics engine.
 *
 * Implements:
 * 1. 10-day Exponential Moving Average (EMA) Trend Weight with missing-day decay handling:
 *    W_trend,t = alpha * W_scale,t + (1 - alpha) * W_trend,t-1
 * 2. Dynamic TDEE Expenditure back-calculation comparing rolling caloric intake against
 *    EMA Trend Weight tissue velocity (3,500 kcal/lb == 7,716.18 kcal/kg), completely
 *    independent of noisy wearable calorie burn estimates.
 * 3. Porygon Weekly Check-In macro and calorie target coaching (adherence-neutral).
 */
class MacroFactorEngine {

    companion object {
        const val DEFAULT_EMA_WINDOW_DAYS = 10
        // Standard 10-day smoothing factor alpha = 2 / (10 + 1) = 0.181818...
        const val DEFAULT_ALPHA = 2.0 / (DEFAULT_EMA_WINDOW_DAYS + 1.0)
        // Energy density of human tissue change: 3,500 kcal per lb = 7,716.18 kcal per kg
        const val KCAL_PER_LB_TISSUE = 3500.0
        const val LBS_PER_KG = 2.20462262
        const val KCAL_PER_KG_TISSUE = KCAL_PER_LB_TISSUE * LBS_PER_KG // ~7716.18 kcal/kg
        // Expenditure damping factor so daily water shifts never cause wild target swings
        const val EXPENDITURE_SMOOTHING_BETA = 0.25
        const val MIN_SAFE_CALORIES = 1200
        const val MAX_SAFE_CALORIES = 6000
    }

    data class DailyWeightSample(
        val dateIso: String,
        val dayIndex: Int, // monotonic day number (0, 1, 2, ...)
        val scaleWeightKg: Double
    )

    data class TrendWeightPoint(
        val dateIso: String,
        val dayIndex: Int,
        val scaleWeightKg: Double,
        val trendWeightKg: Double,
        val weeklyVelocityKg: Double
    )

    data class DailyNutritionAndWeight(
        val dateIso: String,
        val dayIndex: Int,
        val intakeCaloriesKcal: Double?, // null if unlogged day
        val scaleWeightKg: Double?
    )

    data class ExpenditureDayResult(
        val dateIso: String,
        val emaTrendWeightKg: Double,
        val rawDailyTdeeKcal: Double,
        val smoothedTdeeKcal: Double,
        val dailyWeightDeltaKg: Double,
        val confidenceScore: Int
    )

    data class PorygonWeeklyCheckIn(
        val previousTdeeKcal: Int,
        val updatedTdeeKcal: Int,
        val tdeeDeltaKcal: Int,
        val currentTrendWeightKg: Double,
        val sevenDayTrendDeltaKg: Double,
        val recommendedCaloriesKcal: Int,
        val recommendedProteinG: Int,
        val recommendedCarbsG: Int,
        val recommendedFatG: Int,
        val porygonDiagnosis: String,
        val confidencePercent: Int
    )

    /**
     * Computes a single step of the 10-day EMA trend weight:
     * W_trend,t = alpha_effective * W_scale,t + (1 - alpha_effective) * W_trend,t-1
     *
     * Accounts for multi-day gaps between weigh-ins via exponential time decay:
     * alpha_effective = 1 - (1 - alpha)^(daysSincePrevious)
     */
    fun computeNextEmaTrendWeight(
        currentScaleWeightKg: Double,
        previousTrendWeightKg: Double?,
        daysSincePreviousWeighIn: Int = 1,
        alpha: Double = DEFAULT_ALPHA
    ): Double {
        if (previousTrendWeightKg == null || daysSincePreviousWeighIn <= 0) {
            return currentScaleWeightKg
        }
        val clampedGap = daysSincePreviousWeighIn.coerceIn(1, 14)
        val effectiveAlpha = 1.0 - (1.0 - alpha).pow(clampedGap.toDouble())
        return (effectiveAlpha * currentScaleWeightKg) + ((1.0 - effectiveAlpha) * previousTrendWeightKg)
    }

    /**
     * Computes the full EMA Trend Weight series across a chronological list of weigh-ins.
     */
    fun calculateTrendWeightSeries(
        samples: List<DailyWeightSample>,
        alpha: Double = DEFAULT_ALPHA
    ): List<TrendWeightPoint> {
        if (samples.isEmpty()) return emptyList()
        val sorted = samples.sortedBy { it.dayIndex }
        val results = mutableListOf<TrendWeightPoint>()

        var lastTrend: Double? = null
        var lastDayIndex = sorted.first().dayIndex

        for (i in sorted.indices) {
            val sample = sorted[i]
            val gap = if (i == 0) 1 else max(1, sample.dayIndex - lastDayIndex)
            val currentTrend = computeNextEmaTrendWeight(
                currentScaleWeightKg = sample.scaleWeightKg,
                previousTrendWeightKg = lastTrend,
                daysSincePreviousWeighIn = gap,
                alpha = alpha
            )
            // Compute 7-day rolling velocity in kg/week using trend series
            val lookbackIdx = max(0, i - 7)
            val anchor = if (results.isNotEmpty()) results[lookbackIdx] else null
            val elapsedDays = if (anchor != null) max(1, sample.dayIndex - anchor.dayIndex) else 1
            val velocityKgPerWeek = if (anchor != null && i > 0) {
                ((currentTrend - anchor.trendWeightKg) / elapsedDays.toDouble()) * 7.0
            } else {
                0.0
            }

            results.add(
                TrendWeightPoint(
                    dateIso = sample.dateIso,
                    dayIndex = sample.dayIndex,
                    scaleWeightKg = sample.scaleWeightKg,
                    trendWeightKg = currentTrend,
                    weeklyVelocityKg = velocityKgPerWeek
                )
            )
            lastTrend = currentTrend
            lastDayIndex = sample.dayIndex
        }
        return results
    }

    /**
     * Dynamically calculates rolling TDEE expenditure over a timeline of intake and scale weights.
     *
     * Physics relationship:
     *   TDEE_raw = AverageDailyIntake - (DailyTrendWeightChange_kg * 7716.18 kcal/kg)
     *
     * For example, if a user consumes 2,000 kcal/day and their EMA trend weight drops by
     * 1.0 lb (0.453592 kg) over 7 days (-3,500 kcal/week = -500 kcal/day energy balance),
     * their true TDEE is 2,000 - (-500) = 2,500 kcal/day.
     */
    fun calculateDynamicExpenditureSeries(
        history: List<DailyNutritionAndWeight>,
        initialSeedTdeeKcal: Double = 2400.0,
        rollingWindowDays: Int = 14
    ): List<ExpenditureDayResult> {
        if (history.isEmpty()) return emptyList()
        val sorted = history.sortedBy { it.dayIndex }
        val results = mutableListOf<ExpenditureDayResult>()

        var runningTrendWeightKg: Double? = sorted.firstOrNull { it.scaleWeightKg != null }?.scaleWeightKg ?: 75.0
        var lastWeighInDayIndex = sorted.first().dayIndex
        var smoothedTdee = initialSeedTdeeKcal

        val trendHistory = mutableListOf<Double>()

        for (i in sorted.indices) {
            val day = sorted[i]
            val currentTrend = if (day.scaleWeightKg != null) {
                val gap = if (i == 0) 1 else max(1, day.dayIndex - lastWeighInDayIndex)
                lastWeighInDayIndex = day.dayIndex
                computeNextEmaTrendWeight(day.scaleWeightKg, runningTrendWeightKg, gap)
            } else {
                runningTrendWeightKg ?: 75.0
            }
            runningTrendWeightKg = currentTrend
            trendHistory.add(currentTrend)

            // Look back up to rollingWindowDays (default 14 days, minimum 3 days)
            val windowStart = max(0, i - rollingWindowDays + 1)
            val windowSlice = sorted.subList(windowStart, i + 1)
            val loggedDays = windowSlice.mapNotNull { it.intakeCaloriesKcal }

            val avgIntake = if (loggedDays.isNotEmpty()) {
                loggedDays.average()
            } else {
                smoothedTdee
            }

            val daysSpan = max(1, i - windowStart)
            val startTrend = trendHistory[windowStart]
            val warmupBiasCorrection = (1.0 - (1.0 - DEFAULT_ALPHA).pow((i + 1).toDouble())).coerceAtLeast(0.18)
            val dailyTrendDeltaKg = if (i > windowStart) {
                ((currentTrend - startTrend) / daysSpan.toDouble()) / warmupBiasCorrection
            } else {
                0.0
            }

            // Tissue energy equivalent: delta_kg * 7716.18 kcal/kg (3500 kcal/lb)
            val dailyTissueEnergyKcal = dailyTrendDeltaKg * KCAL_PER_KG_TISSUE
            val rawTdee = (avgIntake - dailyTissueEnergyKcal).coerceIn(1000.0, 6500.0)

            // Smooth TDEE with EMA filter so expenditure evolves steadily
            smoothedTdee = if (i == 0) {
                (0.5 * initialSeedTdeeKcal) + (0.5 * rawTdee)
            } else {
                (EXPENDITURE_SMOOTHING_BETA * rawTdee) + ((1.0 - EXPENDITURE_SMOOTHING_BETA) * smoothedTdee)
            }

            // Confidence score based on nutrition logging completeness & weigh-in frequency
            val intakeCompleteness = loggedDays.size.toDouble() / windowSlice.size.toDouble()
            val weighInCompleteness = windowSlice.count { it.scaleWeightKg != null }.toDouble() / windowSlice.size.toDouble()
            val maturityFactor = min(1.0, (i + 1).toDouble() / 14.0)
            val confidence = ((intakeCompleteness * 0.55 + weighInCompleteness * 0.30 + maturityFactor * 0.15) * 100.0)
                .roundToInt()
                .coerceIn(25, 99)

            results.add(
                ExpenditureDayResult(
                    dateIso = day.dateIso,
                    emaTrendWeightKg = currentTrend,
                    rawDailyTdeeKcal = rawTdee,
                    smoothedTdeeKcal = smoothedTdee,
                    dailyWeightDeltaKg = dailyTrendDeltaKg,
                    confidenceScore = confidence
                )
            )
        }

        return results
    }

    /**
     * Generates Porygon's weekly check-in card with adherence-neutral target updates.
     *
     * @param targetWeeklyRateKg negative for weight loss (e.g., -0.45 kg/wk ~ -1 lb/wk),
     *                           0.0 for maintenance, positive for lean gain.
     */
    fun generateWeeklyCheckIn(
        previousTdeeKcal: Int,
        expenditureSeries: List<ExpenditureDayResult>,
        targetWeeklyRateKg: Double,
        proteinPerKgTrend: Double = 2.1,
        fatCalorieShare: Double = 0.27
    ): PorygonWeeklyCheckIn {
        if (expenditureSeries.isEmpty()) {
            return PorygonWeeklyCheckIn(
                previousTdeeKcal = previousTdeeKcal,
                updatedTdeeKcal = previousTdeeKcal,
                tdeeDeltaKcal = 0,
                currentTrendWeightKg = 75.0,
                sevenDayTrendDeltaKg = 0.0,
                recommendedCaloriesKcal = 2000,
                recommendedProteinG = 160,
                recommendedCarbsG = 200,
                recommendedFatG = 60,
                porygonDiagnosis = "PORYGON CYBER-LOG // Initializing baseline metabolic telemetry.",
                confidencePercent = 50
            )
        }

        val latest = expenditureSeries.last()
        val weekAgo = expenditureSeries[max(0, expenditureSeries.size - 8)]
        val updatedTdee = latest.smoothedTdeeKcal.roundToInt()
        val tdeeDelta = updatedTdee - previousTdeeKcal
        val sevenDayTrendDelta = latest.emaTrendWeightKg - weekAgo.emaTrendWeightKg

        // Daily calorie delta required for targetWeeklyRateKg:
        // (targetWeeklyRateKg * 7716.18 kcal/kg) / 7 days
        val dailyGoalDeltaKcal = (targetWeeklyRateKg * KCAL_PER_KG_TISSUE) / 7.0
        val recommendedKcal = (updatedTdee + dailyGoalDeltaKcal)
            .roundToInt()
            .coerceIn(MIN_SAFE_CALORIES, MAX_SAFE_CALORIES)

        // Macro allocation:
        // 1. Protein anchored to EMA Trend Weight (4 kcal/g)
        val proteinG = (latest.emaTrendWeightKg * proteinPerKgTrend).roundToInt().coerceIn(70, 320)
        val proteinKcal = proteinG * 4

        // 2. Dietary fat share of total calories (9 kcal/g), floored at 0.65g/kg for hormonal health
        val minSafeFatG = (latest.emaTrendWeightKg * 0.65).roundToInt()
        val fatG = max(minSafeFatG, ((recommendedKcal * fatCalorieShare) / 9.0).roundToInt())
        val fatKcal = fatG * 9

        // 3. Remaining calories allocated to Carbohydrates (4 kcal/g)
        val remainingKcalForCarbs = max(200, recommendedKcal - proteinKcal - fatKcal)
        val carbsG = (remainingKcalForCarbs / 4.0).roundToInt()

        val directionWord = when {
            abs(tdeeDelta) < 15 -> "held steady"
            tdeeDelta > 0 -> "rose by +$tdeeDelta kcal/day"
            else -> "adapted by $tdeeDelta kcal/day"
        }

        val diagnosis = buildString {
            append("PORYGON LOCK-ON REPORT // Your dynamic expenditure $directionWord ")
            append("(now $updatedTdee kcal/day, ${latest.confidenceScore}% confidence). ")
            append("7-day EMA trend shifted ${"%+.2f".format(sevenDayTrendDelta)} kg. ")
            append("Targets updated adherence-neutrally to $recommendedKcal kcal ")
            append("(${proteinG}P / ${carbsG}C / ${fatG}F).")
        }

        return PorygonWeeklyCheckIn(
            previousTdeeKcal = previousTdeeKcal,
            updatedTdeeKcal = updatedTdee,
            tdeeDeltaKcal = tdeeDelta,
            currentTrendWeightKg = latest.emaTrendWeightKg,
            sevenDayTrendDeltaKg = sevenDayTrendDelta,
            recommendedCaloriesKcal = recommendedKcal,
            recommendedProteinG = proteinG,
            recommendedCarbsG = carbsG,
            recommendedFatG = fatG,
            porygonDiagnosis = diagnosis,
            confidencePercent = latest.confidenceScore
        )
    }
}

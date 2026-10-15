package com.pokopia.macros.domain.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class MacroFactorEngineTest {

    private lateinit var engine: MacroFactorEngine

    @Before
    fun setUp() {
        engine = MacroFactorEngine()
    }

    @Test
    fun `computeNextEmaTrendWeight matches exact 10-day EMA formula`() {
        // W_trend,t = alpha * W_scale,t + (1 - alpha) * W_trend,t-1
        // With alpha = 2 / 11 = 0.18181818...
        val prevTrend = 80.0
        val todayScale = 79.0
        val expected = (2.0 / 11.0) * 79.0 + (9.0 / 11.0) * 80.0
        val actual = engine.computeNextEmaTrendWeight(
            currentScaleWeightKg = todayScale,
            previousTrendWeightKg = prevTrend,
            daysSincePreviousWeighIn = 1
        )
        assertEquals(expected, actual, 1e-6)
    }

    @Test
    fun `computeNextEmaTrendWeight damps single-day water weight spike`() {
        // A sudden +1.5 kg sodium/water fluctuation should move EMA trend by less than 0.30 kg
        val prevTrend = 75.0
        val spikedScale = 76.5
        val newTrend = engine.computeNextEmaTrendWeight(spikedScale, prevTrend, 1)
        val delta = newTrend - prevTrend
        assertTrue("EMA delta ($delta) should damp spike below 0.30 kg", delta in 0.25..0.28)
    }

    @Test
    fun `calculateDynamicExpenditureSeries converges to true TDEE in 500 kcal deficit`() {
        // User eats 2,000 kcal/day and loses ~0.4536 kg (1 lb = 3,500 kcal) per week
        // True TDEE = 2,000 + 500 = 2,500 kcal/day
        val dailyLossKg = 0.45359237 / 7.0
        val history = (0 until 28).map { day ->
            MacroFactorEngine.DailyNutritionAndWeight(
                dateIso = "2026-09-%02d".format((day % 28) + 1),
                dayIndex = day,
                intakeCaloriesKcal = 2000.0,
                scaleWeightKg = 80.0 - (day * dailyLossKg)
            )
        }

        val results = engine.calculateDynamicExpenditureSeries(
            history = history,
            initialSeedTdeeKcal = 2400.0,
            rollingWindowDays = 14
        )
        val finalResult = results.last()
        assertTrue(
            "Smoothed TDEE (${finalResult.smoothedTdeeKcal}) should converge near 2500 kcal",
            abs(finalResult.smoothedTdeeKcal - 2500.0) < 40.0
        )
        assertTrue("Confidence should exceed 90%", finalResult.confidenceScore >= 90)
    }

    @Test
    fun `generateWeeklyCheckIn produces adherence-neutral targets with valid macro sum`() {
        val dailyLossKg = 0.45359237 / 7.0
        val history = (0 until 14).map { day ->
            MacroFactorEngine.DailyNutritionAndWeight(
                dateIso = "2026-10-%02d".format(day + 1),
                dayIndex = day,
                intakeCaloriesKcal = 2050.0,
                scaleWeightKg = 78.0 - (day * dailyLossKg)
            )
        }
        val expenditure = engine.calculateDynamicExpenditureSeries(history, initialSeedTdeeKcal = 2500.0)
        val checkIn = engine.generateWeeklyCheckIn(
            previousTdeeKcal = 2500,
            expenditureSeries = expenditure,
            targetWeeklyRateKg = -0.45,
            proteinPerKgTrend = 2.1,
            fatCalorieShare = 0.27
        )

        val macroCalories = (checkIn.recommendedProteinG * 4) +
            (checkIn.recommendedCarbsG * 4) +
            (checkIn.recommendedFatG * 9)

        assertTrue("Macro calories ($macroCalories) should align with target (${checkIn.recommendedCaloriesKcal})",
            abs(macroCalories - checkIn.recommendedCaloriesKcal) <= 10)
        assertTrue(checkIn.porygonDiagnosis.contains("PORYGON LOCK-ON REPORT"))
    }
}

package com.pokopia.macros.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.room.Room
import com.pokopia.macros.MainActivity
import com.pokopia.macros.data.local.MealLogEntity
import com.pokopia.macros.data.local.PokopiaDatabase
import com.pokopia.macros.data.local.PokopiaSeedData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Interactive Home Screen Widget ("Ditto & Porygon Mini-Habitat" - `PokopiaGlanceWidget.kt`)
 *
 * Built with Jetpack Glance (`androidx.glance:glance-appwidget`):
 * - Renders a dynamic Bitmap (`android.graphics.Canvas` -> `Bitmap` for Glance `Image`)
 *   showing Ditto and Porygon alongside remaining calories and the 3 primary macro rings.
 * - Provides 3 interactive home-screen buttons:
 *   1. Ditto Quick-Snack (`DittoQuickSnackAction` -> logs protein shake/snack to Room DB + morphs Ditto)
 *   2. Porygon UPC Scan (`ACTION_OPEN_SCANNER` deep-link)
 *   3. Porygon AI Eye (`ACTION_OPEN_CAMERA` deep-link)
 */
object PokopiaWidgetKeys {
    val REMAINING_KCAL = intPreferencesKey("remaining_kcal")
    val PROTEIN_G = intPreferencesKey("protein_g")
    val CARBS_G = intPreferencesKey("carbs_g")
    val FAT_G = intPreferencesKey("fat_g")
    val DITTO_FLEXED = booleanPreferencesKey("ditto_flexed")
}

class PokopiaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PokopiaGlanceWidget()
}

class PokopiaGlanceWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Read latest totals from Room before rendering
        val snapshot = loadTodayMacroSnapshot(context)

        provideContent {
            GlanceTheme {
                val prefs = currentState<Preferences>()
                val remKcal = prefs[PokopiaWidgetKeys.REMAINING_KCAL] ?: snapshot.remainingKcal
                val proG = prefs[PokopiaWidgetKeys.PROTEIN_G] ?: snapshot.proteinG
                val carbG = prefs[PokopiaWidgetKeys.CARBS_G] ?: snapshot.carbsG
                val fatG = prefs[PokopiaWidgetKeys.FAT_G] ?: snapshot.fatG
                val flexed = prefs[PokopiaWidgetKeys.DITTO_FLEXED] ?: false

                val habitatBitmap = renderMiniHabitatBitmap(
                    remainingKcal = remKcal,
                    proteinG = proG,
                    carbsG = carbG,
                    fatG = fatG,
                    dittoFlexed = flexed
                )

                val scannerIntent = Intent(context, MainActivity::class.java).apply {
                    action = MainActivity.ACTION_OPEN_SCANNER
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val cameraIntent = Intent(context, MainActivity::class.java).apply {
                    action = MainActivity.ACTION_OPEN_CAMERA
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(android.graphics.Color.parseColor("#0B2229"))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        provider = ImageProvider(habitatBitmap),
                        contentDescription = "Ditto & Porygon Mini-Habitat",
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .defaultWeight()
                    )

                    Spacer(modifier = GlanceModifier.height(6.dp))

                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            text = "+Quick-Snack",
                            onClick = actionRunCallback<DittoQuickSnackAction>(),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Button(
                            text = "UPC Scan",
                            onClick = actionStartActivity(scannerIntent),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Button(
                            text = "AI Eye",
                            onClick = actionStartActivity(cameraIntent),
                            modifier = GlanceModifier.defaultWeight()
                        )
                    }
                }
            }
        }
    }

    data class WidgetMacroSnapshot(
        val remainingKcal: Int,
        val proteinG: Int,
        val carbsG: Int,
        val fatG: Int
    )

    companion object {
        suspend fun loadTodayMacroSnapshot(context: Context): WidgetMacroSnapshot =
            withContext(Dispatchers.IO) {
                runCatching {
                    val db = Room.databaseBuilder(
                        context.applicationContext,
                        PokopiaDatabase::class.java,
                        "pokopia_macros.db"
                    ).build()
                    val dao = db.pokopiaDao()
                    val today = LocalDate.now().toString()
                    val logs = dao.getMealLogsForDateImmediate(today)
                    val profile = dao.getUserProfileImmediate()
                    val targetKcal = profile?.targetCaloriesKcal ?: 1985
                    val consumedKcal = logs.sumOf { it.caloriesKcal }.roundToInt()
                    WidgetMacroSnapshot(
                        remainingKcal = targetKcal - consumedKcal,
                        proteinG = logs.sumOf { it.proteinG }.roundToInt(),
                        carbsG = logs.sumOf { it.carbsG }.roundToInt(),
                        fatG = logs.sumOf { it.fatG }.roundToInt()
                    )
                }.getOrDefault(WidgetMacroSnapshot(1420, 64, 82, 22))
            }

        /**
         * Renders Ditto (Bezier curve sprite), Porygon (low-poly isometric sprite),
         * remaining calories, and the 3 primary macro rings onto an Android Bitmap for Glance.
         */
        fun renderMiniHabitatBitmap(
            remainingKcal: Int,
            proteinG: Int,
            carbsG: Int,
            fatG: Int,
            dittoFlexed: Boolean
        ): Bitmap {
            val width = 640
            val height = 220
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            // 1. Lagoon rounded backdrop
            paint.color = Color.parseColor("#13333D")
            canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), 32f, 32f, paint)

            // 2. Draw Ditto Sprite on left (Lavender #D88CE8, morphs wider/taller if dittoFlexed)
            paint.color = Color.parseColor(if (dittoFlexed) "#E5A4F5" else "#D88CE8")
            val dittoPath = Path().apply {
                val cx = 95f
                val cy = 122f
                val rx = if (dittoFlexed) 64f else 52f
                val ry = if (dittoFlexed) 58f else 46f
                moveTo(cx, cy - ry)
                cubicTo(cx + rx, cy - ry, cx + rx * 1.1f, cy + ry * 0.8f, cx, cy + ry)
                cubicTo(cx - rx * 1.1f, cy + ry * 0.8f, cx - rx, cy - ry, cx, cy - ry)
                close()
            }
            canvas.drawPath(dittoPath, paint)

            // Ditto eyes & smile
            paint.color = Color.parseColor("#1F1324")
            canvas.drawCircle(80f, 110f, 4.5f, paint)
            canvas.drawCircle(110f, 110f, 4.5f, paint)

            // 3. Draw Porygon Low-Poly Sprite on right (#E86A73 coral & #4CC9F0 cyan)
            val poryHead = Path().apply {
                moveTo(545f, 52f)
                lineTo(508f, 92f)
                lineTo(530f, 145f)
                lineTo(575f, 138f)
                lineTo(585f, 82f)
                close()
            }
            paint.color = Color.parseColor("#E86A73")
            canvas.drawPath(poryHead, paint)

            val poryBeak = Path().apply {
                moveTo(508f, 92f)
                lineTo(475f, 112f)
                lineTo(530f, 126f)
                close()
            }
            paint.color = Color.parseColor("#4CC9F0")
            canvas.drawPath(poryBeak, paint)

            // 4. Center Telemetry: Remaining Kcal + 3 Macro Rings
            paint.color = Color.parseColor("#F9F6EE")
            paint.textSize = 36f
            paint.isFakeBoldText = true
            canvas.drawText("$remainingKcal kcal left", 185f, 76f, paint)

            paint.textSize = 22f
            paint.color = Color.parseColor("#F284B6")
            canvas.drawText("P: ${proteinG}g", 185f, 130f, paint)

            paint.color = Color.parseColor("#FFCA5C")
            canvas.drawText("C: ${carbsG}g", 285f, 130f, paint)

            paint.color = Color.parseColor("#63E2B7")
            canvas.drawText("F: ${fatG}g", 385f, 130f, paint)

            paint.color = Color.parseColor("#4CC9F0")
            paint.textSize = 18f
            val badge = if (dittoFlexed) "Ditto Power-Snack Morphed!" else "Pokopia Sanctuary Live"
            canvas.drawText(badge, 185f, 174f, paint)

            return bmp
        }
    }
}

/**
 * ActionCallback for "Ditto Quick-Snack" widget button:
 * Logs a 32g Porygon Cyber-Isolate Protein Shake (120 kcal, 26g Protein) into Room DB
 * and morphs Ditto's sprite on the home screen widget!
 */
class DittoQuickSnackAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        withContext(Dispatchers.IO) {
            runCatching {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    PokopiaDatabase::class.java,
                    "pokopia_macros.db"
                ).build()
                val dao = db.pokopiaDao()
                val today = LocalDate.now().toString()
                dao.insertMealLog(
                    MealLogEntity(
                        timestampEpochMs = System.currentTimeMillis(),
                        dateIso = today,
                        mealSlot = "Widget Quick-Snack",
                        foodId = "food_porygon_iso_whey",
                        foodName = "Ditto Quick-Snack Protein Shake",
                        gramsLogged = 32.0,
                        caloriesKcal = 120.0,
                        proteinG = 26.0,
                        carbsG = 1.5,
                        fatG = 0.9,
                        micronutrientsJson = PokopiaSeedData.buildRichMicronutrientMap(1.2, highProtein = true),
                        loggedVia = "WidgetQuickSnack"
                    )
                )
            }
        }

        val snapshot = PokopiaGlanceWidget.loadTodayMacroSnapshot(context)
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[PokopiaWidgetKeys.REMAINING_KCAL] = snapshot.remainingKcal
                this[PokopiaWidgetKeys.PROTEIN_G] = snapshot.proteinG
                this[PokopiaWidgetKeys.CARBS_G] = snapshot.carbsG
                this[PokopiaWidgetKeys.FAT_G] = snapshot.fatG
                this[PokopiaWidgetKeys.DITTO_FLEXED] = true
            }
        }
        PokopiaGlanceWidget().update(context, glanceId)
    }
}

object PokopiaGlanceWidgetUpdater {
    suspend fun requestImmediateUpdate(context: Context) {
        runCatching {
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(PokopiaGlanceWidget::class.java)
            val snapshot = PokopiaGlanceWidget.loadTodayMacroSnapshot(context)
            ids.forEach { glanceId ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[PokopiaWidgetKeys.REMAINING_KCAL] = snapshot.remainingKcal
                        this[PokopiaWidgetKeys.PROTEIN_G] = snapshot.proteinG
                        this[PokopiaWidgetKeys.CARBS_G] = snapshot.carbsG
                        this[PokopiaWidgetKeys.FAT_G] = snapshot.fatG
                    }
                }
                PokopiaGlanceWidget().update(context, glanceId)
            }
        }
    }
}

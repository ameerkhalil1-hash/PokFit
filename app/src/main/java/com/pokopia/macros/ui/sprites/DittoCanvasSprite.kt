package com.pokopia.macros.ui.sprites

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pokopia.macros.ui.theme.PokopiaColors
import kotlin.math.PI
import kotlin.math.sin

enum class DittoMorphState {
    IDLE_ISLAND_LOUNGE,
    HIGH_PROTEIN_FLEX,
    CARB_FAT_BOUNCE,
    GOAL_HAMMOCK_TREEHOUSE
}

/**
 * Procedural Jetpack Compose Canvas Renderer for Ditto (`DittoCanvasSprite.kt`).
 *
 * Uses animated cubic Bezier curves (`Path.cubicTo`) with soft lavender-pink gradients
 * (`#D88CE8` to `#B865C9`), signature bead eyes, and a simple happy smile.
 * Driven by `animateFloatAsState` and `spring(dampingRatio = Spring.DampingRatioLowBouncy)`.
 */
@Composable
fun DittoCanvasSprite(
    morphState: DittoMorphState,
    onDittoTapped: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ditto_idle_breath")
    val breathPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "breath_phase"
    )

    val flexFactor by animateFloatAsState(
        targetValue = if (morphState == DittoMorphState.HIGH_PROTEIN_FLEX) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ditto_flex_spring"
    )

    val squishFactor by animateFloatAsState(
        targetValue = if (morphState == DittoMorphState.CARB_FAT_BOUNCE) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "ditto_squish_spring"
    )

    val hammockFactor by animateFloatAsState(
        targetValue = if (morphState == DittoMorphState.GOAL_HAMMOCK_TREEHOUSE) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ditto_hammock_spring"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Canvas(
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onDittoTapped
        )
    ) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.56f

        val breathWobble = sin(breathPhase) * (w * 0.03f)
        val squishX = 1f + (squishFactor * 0.22f) - (flexFactor * 0.06f) + (hammockFactor * 0.25f)
        val squishY = 1f - (squishFactor * 0.18f) + (flexFactor * 0.14f) - (hammockFactor * 0.20f)

        val bodyRx = (w * 0.34f) * squishX + breathWobble
        val bodyRy = (h * 0.30f) * squishY - breathWobble * 0.5f

        // 1. Soft tropical sand shadow underneath Ditto
        drawOval(
            color = Color.Black.copy(alpha = 0.24f),
            topLeft = Offset(cx - bodyRx * 0.95f, cy + bodyRy * 0.78f),
            size = Size(bodyRx * 1.9f, bodyRy * 0.42f)
        )

        // 2. If in GOAL_HAMMOCK_TREEHOUSE state, draw palm-tree posts & cozy woven hammock straps
        if (hammockFactor > 0.05f) {
            drawRoundRect(
                color = Color(0xFF8D5B3C).copy(alpha = hammockFactor),
                topLeft = Offset(w * 0.06f, h * 0.22f),
                size = Size(w * 0.06f, h * 0.65f),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawRoundRect(
                color = Color(0xFF8D5B3C).copy(alpha = hammockFactor),
                topLeft = Offset(w * 0.88f, h * 0.22f),
                size = Size(w * 0.06f, h * 0.65f),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawLine(
                color = PokopiaColors.WarmSand.copy(alpha = hammockFactor),
                start = Offset(w * 0.09f, h * 0.36f),
                end = Offset(cx - bodyRx * 0.85f, cy),
                strokeWidth = 4f
            )
            drawLine(
                color = PokopiaColors.WarmSand.copy(alpha = hammockFactor),
                start = Offset(w * 0.91f, h * 0.36f),
                end = Offset(cx + bodyRx * 0.85f, cy),
                strokeWidth = 4f
            )
        }

        // 3. Ditto Organic Bezier Body Path (`Path.cubicTo`)
        val shoulderLift = flexFactor * (h * 0.16f)
        val armBulge = flexFactor * (w * 0.14f)
        val hammockDip = hammockFactor * (h * 0.10f)

        val dittoPath = Path().apply {
            // Top crown of Ditto
            moveTo(cx, cy - bodyRy + hammockDip)

            // Upper-right organic lobe (morphs into muscular bicep when flexFactor > 0)
            cubicTo(
                cx + bodyRx * 0.55f, cy - bodyRy * 1.05f - shoulderLift,
                cx + bodyRx + armBulge, cy - bodyRy * 0.45f - shoulderLift,
                cx + bodyRx + armBulge * 0.6f, cy + bodyRy * 0.10f
            )

            // Lower-right gelatinous base curve
            cubicTo(
                cx + bodyRx * 1.12f, cy + bodyRy * 0.78f,
                cx + bodyRx * 0.55f, cy + bodyRy * 0.96f,
                cx, cy + bodyRy * 0.92f
            )

            // Lower-left gelatinous base curve
            cubicTo(
                cx - bodyRx * 0.55f, cy + bodyRy * 0.96f,
                cx - bodyRx * 1.12f, cy + bodyRy * 0.78f,
                cx - bodyRx - armBulge * 0.6f, cy + bodyRy * 0.10f
            )

            // Upper-left organic lobe
            cubicTo(
                cx - bodyRx - armBulge, cy - bodyRy * 0.45f - shoulderLift,
                cx - bodyRx * 0.55f, cy - bodyRy * 1.05f - shoulderLift,
                cx, cy - bodyRy + hammockDip
            )
            close()
        }

        val dittoBrush = Brush.verticalGradient(
            colors = listOf(
                PokopiaColors.DittoLavenderLight,
                PokopiaColors.DittoLavenderDeep
            ),
            startY = cy - bodyRy,
            endY = cy + bodyRy
        )

        drawPath(path = dittoPath, brush = dittoBrush)
        drawPath(
            path = dittoPath,
            color = Color(0xFF7D3C98).copy(alpha = 0.55f),
            style = Stroke(width = 3.5f)
        )

        // 4. Glossy Jelly Highlight Curve
        val highlightPath = Path().apply {
            moveTo(cx - bodyRx * 0.42f, cy - bodyRy * 0.52f)
            cubicTo(
                cx - bodyRx * 0.20f, cy - bodyRy * 0.75f,
                cx + bodyRx * 0.15f, cy - bodyRy * 0.72f,
                cx + bodyRx * 0.32f, cy - bodyRy * 0.48f
            )
        }
        drawPath(
            path = highlightPath,
            color = Color.White.copy(alpha = 0.42f),
            style = Stroke(width = 5f, cap = StrokeCap.Round)
        )

        // 5. Signature Ditto Bead Eyes, Rosy Cheeks, and Wide Happy Smile
        val eyeY = cy - bodyRy * 0.18f + hammockDip * 0.5f
        val eyeOffsetX = bodyRx * 0.32f
        drawCircle(
            color = Color(0xFF1F1324),
            radius = w * 0.026f,
            center = Offset(cx - eyeOffsetX, eyeY)
        )
        drawCircle(
            color = Color(0xFF1F1324),
            radius = w * 0.026f,
            center = Offset(cx + eyeOffsetX, eyeY)
        )

        // Rosy cheeks
        drawOval(
            color = PokopiaColors.DittoCheekBlush.copy(alpha = 0.65f),
            topLeft = Offset(cx - eyeOffsetX * 1.45f, eyeY + h * 0.03f),
            size = Size(w * 0.07f, h * 0.035f)
        )
        drawOval(
            color = PokopiaColors.DittoCheekBlush.copy(alpha = 0.65f),
            topLeft = Offset(cx + eyeOffsetX * 0.95f, eyeY + h * 0.03f),
            size = Size(w * 0.07f, h * 0.035f)
        )

        // Signature Ditto Smile
        val smilePath = Path().apply {
            moveTo(cx - eyeOffsetX * 0.85f, eyeY + h * 0.07f)
            cubicTo(
                cx - eyeOffsetX * 0.35f, eyeY + h * 0.15f,
                cx + eyeOffsetX * 0.35f, eyeY + h * 0.15f,
                cx + eyeOffsetX * 0.85f, eyeY + h * 0.07f
            )
        }
        drawPath(
            path = smilePath,
            color = Color(0xFF1F1324),
            style = Stroke(width = 3.6f, cap = StrokeCap.Round)
        )
    }
}

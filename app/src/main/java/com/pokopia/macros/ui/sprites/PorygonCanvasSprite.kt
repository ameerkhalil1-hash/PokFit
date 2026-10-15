package com.pokopia.macros.ui.sprites

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.pokopia.macros.ui.theme.PokopiaColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class PorygonCyberMode {
    ISLAND_HOLOGRAM_COMPANION,
    BARCODE_LOCK_ON_HUD,
    CYBER_LAB_CHECKIN
}

/**
 * Procedural Jetpack Compose Canvas Renderer for Porygon (`PorygonCanvasSprite.kt`).
 *
 * Renders Porygon using crisp, faceted isometric 3D-style low-poly polygons (`Path.lineTo`)
 * in coral-pink (`#E86A73`) and cyber-teal (`#4CC9F0`) with sharp geometric shading that
 * subtly hovers and rotates along a sine wave.
 */
@Composable
fun PorygonCanvasSprite(
    cyberMode: PorygonCyberMode,
    onPorygonTapped: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "porygon_cyber_hover")
    val hoverAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "porygon_hover_angle"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Canvas(
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onPorygonTapped
        )
    ) {
        val w = size.width
        val h = size.height
        val hoverOffsetY = sin(hoverAngle) * (h * 0.045f)
        val tiltDegrees = cos(hoverAngle) * 4.5f

        val cx = w * 0.5f
        val cy = h * 0.52f + hoverOffsetY
        val scale = minOf(w, h) * 0.40f

        // 1. Cybernetic Hologram Ring Base under Porygon
        drawOval(
            color = PokopiaColors.PorygonCyberCyan.copy(alpha = 0.35f),
            topLeft = Offset(cx - scale * 0.9f, h * 0.84f),
            size = androidx.compose.ui.geometry.Size(scale * 1.8f, scale * 0.28f),
            style = Stroke(width = 2.5f)
        )

        rotate(degrees = tiltDegrees, pivot = Offset(cx, cy)) {
            // 2. Low-Poly Isometric Chest & Torso Facets (Coral Pink #E86A73 & Dark Coral #C24953)
            val torsoLeftFacet = Path().apply {
                moveTo(cx, cy - scale * 0.15f)
                lineTo(cx - scale * 0.72f, cy + scale * 0.12f)
                lineTo(cx - scale * 0.48f, cy + scale * 0.68f)
                lineTo(cx, cy + scale * 0.58f)
                close()
            }
            drawPath(torsoLeftFacet, color = PokopiaColors.PorygonCoral)

            val torsoRightFacet = Path().apply {
                moveTo(cx, cy - scale * 0.15f)
                lineTo(cx + scale * 0.72f, cy + scale * 0.12f)
                lineTo(cx + scale * 0.48f, cy + scale * 0.68f)
                lineTo(cx, cy + scale * 0.58f)
                close()
            }
            drawPath(torsoRightFacet, color = PokopiaColors.PorygonCoralDark)

            // 3. Cyber-Teal (#4CC9F0) Geometric Belly & Tail Spike
            val bellyShield = Path().apply {
                moveTo(cx, cy + scale * 0.02f)
                lineTo(cx - scale * 0.42f, cy + scale * 0.25f)
                lineTo(cx, cy + scale * 0.64f)
                lineTo(cx + scale * 0.42f, cy + scale * 0.25f)
                close()
            }
            drawPath(bellyShield, color = PokopiaColors.PorygonCyberCyan)

            // Low-poly angular feet
            val leftFoot = Path().apply {
                moveTo(cx - scale * 0.45f, cy + scale * 0.55f)
                lineTo(cx - scale * 0.78f, cy + scale * 0.78f)
                lineTo(cx - scale * 0.28f, cy + scale * 0.82f)
                close()
            }
            val rightFoot = Path().apply {
                moveTo(cx + scale * 0.45f, cy + scale * 0.55f)
                lineTo(cx + scale * 0.78f, cy + scale * 0.78f)
                lineTo(cx + scale * 0.28f, cy + scale * 0.82f)
                close()
            }
            drawPath(leftFoot, color = PokopiaColors.PorygonCyberBlue)
            drawPath(rightFoot, color = PokopiaColors.PorygonCyberCyan)

            // 4. Faceted Polyhedral Head (Coral Pink Crown + Cyber-Teal Duck Bill)
            val headCrown = Path().apply {
                moveTo(cx, cy - scale * 0.92f)
                lineTo(cx - scale * 0.54f, cy - scale * 0.45f)
                lineTo(cx - scale * 0.36f, cy - scale * 0.05f)
                lineTo(cx + scale * 0.36f, cy - scale * 0.05f)
                lineTo(cx + scale * 0.54f, cy - scale * 0.45f)
                close()
            }
            drawPath(headCrown, color = PokopiaColors.PorygonCoral)

            val cyberBill = Path().apply {
                moveTo(cx - scale * 0.36f, cy - scale * 0.14f)
                lineTo(cx - scale * 0.82f, cy + scale * 0.04f)
                lineTo(cx, cy + scale * 0.14f)
                lineTo(cx + scale * 0.36f, cy - scale * 0.14f)
                close()
            }
            drawPath(cyberBill, color = PokopiaColors.PorygonCyberCyan)

            // 5. Crisp White/Cyan Wireframe Polygon Edges
            val wireframeColor = Color.White.copy(alpha = 0.78f)
            val stroke = Stroke(width = 2.4f)
            drawPath(torsoLeftFacet, color = wireframeColor, style = stroke)
            drawPath(torsoRightFacet, color = wireframeColor, style = stroke)
            drawPath(bellyShield, color = wireframeColor, style = stroke)
            drawPath(headCrown, color = wireframeColor, style = stroke)
            drawPath(cyberBill, color = wireframeColor, style = stroke)

            // 6. Angular Octagonal Eyes
            drawCircle(
                color = Color.White,
                radius = scale * 0.13f,
                center = Offset(cx - scale * 0.20f, cy - scale * 0.36f)
            )
            drawCircle(
                color = Color(0xFF101820),
                radius = scale * 0.06f,
                center = Offset(cx - scale * 0.22f, cy - scale * 0.36f)
            )

            // 7. When in BARCODE_LOCK_ON_HUD or CYBER_LAB_CHECKIN mode, project a Lock-On laser reticle
            if (cyberMode != PorygonCyberMode.ISLAND_HOLOGRAM_COMPANION) {
                val beamStart = Offset(cx - scale * 0.22f, cy - scale * 0.36f)
                val beamEnd = Offset(w * 0.05f, h * 0.92f)
                drawLine(
                    color = PokopiaColors.PorygonCyberCyan.copy(alpha = 0.75f),
                    start = beamStart,
                    end = beamEnd,
                    strokeWidth = 3f
                )
            }
        }
    }
}

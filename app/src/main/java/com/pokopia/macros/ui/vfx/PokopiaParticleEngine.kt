package com.pokopia.macros.ui.vfx

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.pokopia.macros.ui.theme.PokopiaColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class PokopiaParticleMode {
    DITTO_JELLY_AND_LEAVES,
    PORYGON_WIREFRAME_AND_TRI_ATTACK
}

private data class ProceduralParticle(
    val seedX: Float,
    val seedY: Float,
    val radius: Float,
    val speed: Float,
    val phaseOffset: Float,
    val kind: Int
)

/**
 * Procedural Jetpack Compose Canvas Particle System (`PokopiaParticleEngine.kt`).
 *
 * Emits:
 * - Ditto Jelly Bubbles & Tropical Island Leaves when logging meals or completing daily goals.
 * - Porygon Low-Poly Wireframe Cubes & Tri-Attack Sparks (red/blue/yellow geometric triangles)
 *   when a barcode locks on, a wearable sync completes, or a weekly TDEE recalculation triggers.
 */
@Composable
fun PokopiaParticleOverlay(
    mode: PokopiaParticleMode,
    intensity: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    val particles = remember {
        val rng = Random(7024)
        List(28) { idx ->
            ProceduralParticle(
                seedX = rng.nextFloat(),
                seedY = rng.nextFloat(),
                radius = 6f + rng.nextFloat() * 14f,
                speed = 0.35f + rng.nextFloat() * 0.85f,
                phaseOffset = rng.nextFloat() * (2f * PI.toFloat()),
                kind = idx % 3
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pokopia_particles")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_phase"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        particles.forEach { p ->
            val progress = (p.seedY - phase * p.speed).let { raw ->
                val wrapped = raw % 1f
                if (wrapped < 0f) wrapped + 1f else wrapped
            }
            val driftX = sin(progress * 2f * PI.toFloat() + p.phaseOffset) * 24f
            val x = (p.seedX * w + driftX).coerceIn(0f, w)
            val y = progress * h
            val alpha = (sin(progress * PI.toFloat()) * 0.65f * intensity).coerceIn(0.05f, 0.85f)

            when (mode) {
                PokopiaParticleMode.DITTO_JELLY_AND_LEAVES -> {
                    if (p.kind != 2) {
                        // Ditto Jelly Bubble with specular highlight
                        drawCircle(
                            color = PokopiaColors.DittoLavenderLight.copy(alpha = alpha * 0.55f),
                            radius = p.radius,
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = alpha * 0.75f),
                            radius = p.radius * 0.28f,
                            center = Offset(x - p.radius * 0.3f, y - p.radius * 0.3f)
                        )
                    } else {
                        // Tropical Pokopia Island Leaf
                        rotate(degrees = (progress * 360f) + p.phaseOffset * 40f, pivot = Offset(x, y)) {
                            val leafPath = Path().apply {
                                moveTo(x - p.radius, y)
                                quadraticTo(x, y - p.radius * 0.85f, x + p.radius, y)
                                quadraticTo(x, y + p.radius * 0.85f, x - p.radius, y)
                                close()
                            }
                            drawPath(
                                path = leafPath,
                                color = PokopiaColors.TropicalFoliage.copy(alpha = alpha * 0.7f)
                            )
                        }
                    }
                }

                PokopiaParticleMode.PORYGON_WIREFRAME_AND_TRI_ATTACK -> {
                    val triColor = when (p.kind) {
                        0 -> PokopiaColors.PorygonCoral.copy(alpha = alpha) // Fire / Red Tri-Attack
                        1 -> PokopiaColors.PorygonCyberCyan.copy(alpha = alpha) // Ice / Cyan Tri-Attack
                        else -> PokopiaColors.TriAttackYellow.copy(alpha = alpha) // Electric / Yellow Tri-Attack
                    }
                    rotate(degrees = (progress * 540f), pivot = Offset(x, y)) {
                        if (p.kind == 1) {
                            // Low-Poly Wireframe Cube
                            drawRect(
                                color = triColor,
                                topLeft = Offset(x - p.radius * 0.7f, y - p.radius * 0.7f),
                                size = Size(p.radius * 1.4f, p.radius * 1.4f),
                                style = Stroke(width = 2.2f)
                            )
                        } else {
                            // Tri-Attack Geometric Spark Triangle
                            val tri = Path().apply {
                                moveTo(x, y - p.radius)
                                lineTo(x + p.radius * 0.86f, y + p.radius * 0.65f)
                                lineTo(x - p.radius * 0.86f, y + p.radius * 0.65f)
                                close()
                            }
                            drawPath(path = tri, color = triColor, style = Stroke(width = 2.4f))
                        }
                    }
                }
            }
        }
    }
}

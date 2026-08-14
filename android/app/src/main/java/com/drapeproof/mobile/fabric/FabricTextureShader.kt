package com.drapeproof.mobile.fabric

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

object FabricTextureShader {

    fun renderFabricDrape(
        scope: DrawScope,
        path: Path,
        fabricId: String,
        baseColor: Color,
        width: Float,
        height: Float,
        neckTopY: Float,
    ) {
        with(scope) {
            // 1. BASE SOLID FOUNDATION (100% Opaque, zero shirt bleed)
            drawPath(
                path = path,
                color = baseColor,
                style = Fill,
            )

            // 2. MATERIAL-SPECIFIC PHYSICALLY-BASED WEAVE & LUSTER SHADER
            when (fabricId) {
                "silk", "satin" -> {
                    // Multi-band anisotropic specular sheen
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.42f),
                                Color.Transparent,
                                Color.White.copy(alpha = 0.30f),
                                Color.Black.copy(alpha = 0.28f),
                                Color.White.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.38f),
                            ),
                            startY = neckTopY,
                            endY = height,
                        ),
                    )

                    // Secondary pearlescent highlights
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.22f),
                                Color.Transparent,
                                Color.White.copy(alpha = 0.18f),
                                Color.Transparent,
                            ),
                        ),
                    )
                }

                "denim" -> {
                    // 45-degree 3x1 twill diagonal ribs
                    val spacing = 8.dp.toPx()
                    var x = -height
                    while (x < width * 2) {
                        drawLine(
                            color = Color.Black.copy(alpha = 0.22f),
                            start = Offset(x, neckTopY),
                            end = Offset(x + height, height),
                            strokeWidth = 2.dp.toPx(),
                        )
                        x += spacing
                    }

                    // Contrast gold double-topstitching at collar seam
                    drawPath(
                        path = path,
                        color = Color(0xFFD4A373).copy(alpha = 0.90f),
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
                        ),
                    )
                }

                "linen" -> {
                    // Organic slub weave crosshatching (Horizontal & Vertical organic fibers)
                    val horizSpacing = 10.dp.toPx()
                    var y = neckTopY
                    var idx = 0
                    while (y < height) {
                        val thickness = if (idx % 3 == 0) 2.2.dp.toPx() else 1.2.dp.toPx()
                        drawLine(
                            color = Color.Black.copy(alpha = if (idx % 3 == 0) 0.18f else 0.10f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = thickness,
                        )
                        y += horizSpacing
                        idx++
                    }

                    val vertSpacing = 12.dp.toPx()
                    var vx = 0f
                    var vidx = 0
                    while (vx < width) {
                        val thickness = if (vidx % 2 == 0) 1.8.dp.toPx() else 1.0.dp.toPx()
                        drawLine(
                            color = Color.Black.copy(alpha = 0.12f),
                            start = Offset(vx, neckTopY),
                            end = Offset(vx, height),
                            strokeWidth = thickness,
                        )
                        vx += vertSpacing
                        vidx++
                    }
                }

                "velvet" -> {
                    // Directional pile nap absorption with velvety edge luster
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.45f),
                                Color.White.copy(alpha = 0.24f),
                                Color.Black.copy(alpha = 0.50f),
                                Color.White.copy(alpha = 0.28f),
                                Color.Black.copy(alpha = 0.45f),
                            ),
                        ),
                    )
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.35f),
                            ),
                            startY = neckTopY,
                            endY = height,
                        ),
                    )
                }

                "knit" -> {
                    // 2x2 Vertical ribbed knit channels
                    val ribSpacing = 12.dp.toPx()
                    var rx = 0f
                    while (rx < width) {
                        drawLine(
                            color = Color.Black.copy(alpha = 0.25f),
                            start = Offset(rx, neckTopY),
                            end = Offset(rx, height),
                            strokeWidth = 3.5.dp.toPx(),
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.18f),
                            start = Offset(rx + 4.dp.toPx(), neckTopY),
                            end = Offset(rx + 4.dp.toPx(), height),
                            strokeWidth = 1.5.dp.toPx(),
                        )
                        rx += ribSpacing
                    }
                }

                "cashmere", "wool" -> {
                    // Dense brushed micro-fiber texture with soft curvature shading
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.28f),
                            ),
                            startY = neckTopY,
                            endY = height,
                        ),
                    )
                    var wx = 0f
                    while (wx < width) {
                        drawLine(
                            color = Color.Black.copy(alpha = 0.08f),
                            start = Offset(wx, neckTopY),
                            end = Offset(wx, height),
                            strokeWidth = 1.dp.toPx(),
                        )
                        wx += 6.dp.toPx()
                    }
                }

                else -> {
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.22f),
                            ),
                            startY = neckTopY,
                            endY = height,
                        ),
                    )
                }
            }

            // 3. TAILORED COLLAR STITCH BORDER
            drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.55f),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}

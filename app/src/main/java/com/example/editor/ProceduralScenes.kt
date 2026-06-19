package com.example.editor

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin

object ProceduralScenes {

    fun drawScene(
        scope: DrawScope,
        sceneType: SceneType,
        currentTimeMs: Long,
        clipSpeed: Float
    ) {
        val width = scope.size.width
        val height = scope.size.height
        if (width <= 0 || height <= 0) return

        // Speed adjusted relative time factor
        val relativeTime = (currentTimeMs * clipSpeed) / 1000f

        when (sceneType) {
            SceneType.NEON_STREET -> drawCyberpunkStreet(scope, width, height, relativeTime)
            SceneType.COSMOS_NEBULA -> drawCosmicNebula(scope, width, height, relativeTime)
            SceneType.TOKYO_SYNTHWAVE -> drawTokyoSynthwave(scope, width, height, relativeTime)
            SceneType.GLITCH_MATRIX -> drawGlitchMatrix(scope, width, height, relativeTime)
            SceneType.CINEMATIC_SUNSET -> drawCinematicSunset(scope, width, height, relativeTime)
        }
    }

    private fun drawCyberpunkStreet(scope: DrawScope, w: Float, h: Float, time: Float) {
        // Deep midnight cyber blue base
        scope.drawRect(
            color = Color(0xFF070714)
        )

        // Draw perspective grid / street lanes
        val horizonY = h * 0.45f
        scope.drawLine(
            color = Color(0xFF1E264E),
            start = Offset(0f, horizonY),
            end = Offset(w, horizonY),
            strokeWidth = 4f
        )

        val laneCount = 12
        for (i in 0..laneCount) {
            val ratio = i.toFloat() / laneCount
            val startX = w * ratio
            val endX = w * 0.5f + (startX - w * 0.5f) * 0.1f
            scope.drawLine(
                color = Color(0xFF3B2FFF),
                start = Offset(startX, h),
                end = Offset(endX, horizonY),
                strokeWidth = 2f
            )
        }

        // Horizontal scrolling road bars
        val horizontalBarCount = 6
        for (i in 0 until horizontalBarCount) {
            val step = (time * 1.5f + i.toFloat() / horizontalBarCount) % 1.0f
            // Map step exponentially to simulate 3D perspective approach
            val easedRatio = step * step
            val barY = horizonY + (h - horizonY) * easedRatio
            val barWidth = 1f + easedRatio * 8f
            scope.drawLine(
                color = Color(0xFFFF007F).copy(alpha = (1.0f - easedRatio) * 0.8f),
                start = Offset(0f, barY),
                end = Offset(w, barY),
                strokeWidth = barWidth
            )
        }

        // Cyber buildings in the background
        val buildingCount = 7
        val seedOffsets = listOf(0.1f, 0.22f, 0.35f, 0.5f, 0.65f, 0.78f, 0.9f)
        val seedWidths = listOf(0.15f, 0.12f, 0.18f, 0.14f, 0.16f, 0.2f, 0.15f)
        val seedHeights = listOf(0.28f, 0.35f, 0.22f, 0.4f, 0.3f, 0.25f, 0.32f)

        for (i in 0 until buildingCount) {
            val bWidth = w * seedWidths[i]
            // Light pan effect with time
            val bX = w * seedOffsets[i] + sin(time * 0.15f + i) * 15f - (bWidth / 2f)
            val bHeight = h * seedHeights[i]
            val bY = horizonY - bHeight

            // Building body
            scope.drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF0F122B), Color(0xFF04050D)),
                    start = Offset(bX, bY),
                    end = Offset(bX, horizonY)
                ),
                topLeft = Offset(bX, bY),
                size = Size(bWidth, bHeight)
            )

            // Neon glowing edges and windows
            scope.drawRect(
                color = Color(0xFF00FFCC).copy(alpha = 0.4f),
                topLeft = Offset(bX, bY),
                size = Size(bWidth, bHeight),
                style = Stroke(width = 1.5f)
            )

            // Dynamic glowing windows
            val rows = 8
            val cols = 4
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val winW = bWidth * 0.12f
                    val winH = bHeight * 0.06f
                    val winX = bX + bWidth * 0.15f + c * (bWidth * 0.18f)
                    val winY = bY + bHeight * 0.15f + r * (bHeight * 0.1f)
                    
                    val flicker = sin(time * 3f + i * 2 + r * 5 + c)
                    val winColor = if (flicker > 0.1f) Color(0xFFFFDD00) else if (flicker < -0.4f) Color(0xFF00FFCC) else Color(0xFF1E264E)
                    scope.drawRect(
                        color = winColor.copy(alpha = 0.5f),
                        topLeft = Offset(winX, winY),
                        size = Size(winW, winH)
                    )
                }
            }
        }

        // Draw a sports car chassis outline in center bottom
        val carScale = h / 720f
        val carW = 220f * carScale
        val carH = 80f * carScale
        // Tiny engine vibration shake
        val shakeX = sin(time * 35f) * 1.5f
        val shakeY = cos(time * 42f) * 1f
        val carX = w / 2f - carW / 2f + shakeX
        val carY = h * 0.85f - carH / 2f + shakeY

        // Glowing spoiler and body
        scope.drawRoundRect(
            color = Color(0xFF121324),
            topLeft = Offset(carX, carY),
            size = Size(carW, carH),
            cornerRadius = CornerRadius(16f * carScale),
        )
        // Red neon rear strip
        scope.drawLine(
            color = Color(0xFFFF0055),
            start = Offset(carX + carW * 0.1f, carY + carH * 0.3f),
            end = Offset(carX + carW * 0.9f, carY + carH * 0.3f),
            strokeWidth = 6f * carScale
        )
        // Neon exhaust flames
        val flameIntensity = (sin(time * 24f) * 0.5f + 0.5f) * carScale * 25f
        scope.drawCircle(
            color = Color(0xFF00FFFF).copy(alpha = 0.8f),
            center = Offset(carX + carW * 0.3f, carY + carH),
            radius = flameIntensity * 0.4f
        )
        scope.drawCircle(
            color = Color(0xFF00FFFF).copy(alpha = 0.8f),
            center = Offset(carX + carW * 0.7f, carY + carH),
            radius = flameIntensity * 0.4f
        )
    }

    private fun drawCosmicNebula(scope: DrawScope, w: Float, h: Float, time: Float) {
        // Starfield dark back block
        scope.drawRect(color = Color(0xFF03020A))

        // Draw Twinkling Star Points
        val stars = 40
        for (i in 0 until stars) {
            val starX = (Math.abs(sin(i.toFloat() * 194.52f)) * w)
            val starY = (Math.abs(cos(i.toFloat() * 582.11f)) * h)
            val twinkle = (sin(time * 1.5f + i) * 0.5f + 0.5f)
            val starSize = (1.5f + (i % 3).toFloat() * 1.5f) * twinkle
            
            scope.drawCircle(
                color = Color.White.copy(alpha = 0.4f + twinkle * 0.6f),
                center = Offset(starX, starY),
                radius = starSize
            )
        }

        // Swirling Purple and Cyan Nebulae
        val nebX1 = w * 0.4f + cos(time * 0.1f) * (w * 0.15f)
        val nebY1 = h * 0.45f + sin(time * 0.15f) * (h * 0.15f)
        val nebR1 = w * 0.45f

        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF8B00FF).copy(alpha = 0.25f), Color.Transparent),
                center = Offset(nebX1, nebY1),
                radius = nebR1
            ),
            center = Offset(nebX1, nebY1),
            radius = nebR1
        )

        val nebX2 = w * 0.6f + sin(time * 0.12f) * (w * 0.12f)
        val nebY2 = h * 0.55f + cos(time * 0.08f) * (h * 0.18f)
        val nebR2 = w * 0.55f

        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF00FFFF).copy(alpha = 0.2f), Color.Transparent),
                center = Offset(nebX2, nebY2),
                radius = nebR2
            ),
            center = Offset(nebX2, nebY2),
            radius = nebR2
        )

        // Swirling Nebula spiral streaks using paths and rotations
        scope.rotate(degrees = time * 8f, pivot = Offset(w*0.5f, h*0.5f)) {
            val spiralColor = Color(0xFFFF007F).copy(alpha = 0.15f)
            scope.drawCircle(
                color = spiralColor,
                center = Offset(w * 0.35f, h * 0.4f),
                radius = w * 0.08f
            )
            scope.drawCircle(
                color = Color(0xFF00FFFF).copy(alpha = 0.12f),
                center = Offset(w * 0.65f, h * 0.6f),
                radius = w * 0.06f
            )
        }

        // Floating Planet in center left
        val planetX = w * 0.5f + cos(time * 0.05f) * (w * 0.1f)
        val planetY = h * 0.5f + sin(time * 0.03f) * (h * 0.05f)
        val planetR = h * 0.12f

        // 3D Planet back shading gradient
        scope.drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF160E36), Color(0xFF05010A)),
                start = Offset(planetX - planetR, planetY - planetR),
                end = Offset(planetX + planetR, planetY + planetR)
            ),
            center = Offset(planetX, planetY),
            radius = planetR
        )
        // Atmosphere ring wrap
        scope.drawCircle(
            color = Color(0xFF2DF3F9).copy(alpha = 0.3f),
            center = Offset(planetX, planetY),
            radius = planetR,
            style = Stroke(width = 4f)
        )

        // Beautiful cosmic tilt rings (CapCut Space aesthetic)
        scope.rotate(degrees = -15f, pivot = Offset(planetX, planetY)) {
            scope.drawOval(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, Color(0xFFFF00D4).copy(alpha = 0.6f), Color.Transparent)
                ),
                topLeft = Offset(planetX - planetR * 1.8f, planetY - planetR * 0.2f),
                size = Size(planetR * 3.6f, planetR * 0.4f),
                style = Stroke(width = 6f)
            )
        }
    }

    private fun drawTokyoSynthwave(scope: DrawScope, w: Float, h: Float, time: Float) {
        // Orange purple vintage gradient twilight sky
        scope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF140D2F), Color(0xFF3B1357), Color(0xFF6F176B))
            )
        )

        // Large glowing retro sun
        val sunR = h * 0.22f
        val sunX = w * 0.5f
        val sunY = h * 0.55f

        scope.drawCircle(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFD100), Color(0xFFFF007F)),
                startY = sunY - sunR,
                endY = sunY + sunR
            ),
            center = Offset(sunX, sunY),
            radius = sunR
        )

        // Draw Retro synth sun horizontal slicing stripes
        val stripeHeight = h * 0.012f
        val spacing = h * 0.018f
        val sunBottomY = sunY + sunR
        val sunTopY = sunY - sunR
        
        var currentStripeY = sunY - h * 0.05f
        var index = 0
        while (currentStripeY < sunBottomY) {
            // Stripes get thicker as they move down
            val sliceWeight = 2f + (index * 1.8f)
            if (currentStripeY > sunTopY) {
                scope.drawRect(
                    color = Color(0xFF140D2F), // Matches sky back to clip
                    topLeft = Offset(sunX - sunR - 10f, currentStripeY),
                    size = Size(sunR * 2f + 20f, sliceWeight)
                )
            }
            currentStripeY += sliceWeight + spacing
            index++
        }

        // Cyber mountains (neon outlines) in the far back
        val mountPath = Path().apply {
            moveTo(0f, sunY + sunR)
            lineTo(w * 0.2f, h * 0.35f)
            lineTo(w * 0.38f, sunY + sunR)
            lineTo(w * 0.55f, h * 0.42f)
            lineTo(w * 0.72f, sunY + sunR)
            lineTo(w * 0.85f, h * 0.3f)
            lineTo(w, sunY + sunR)
        }
        scope.drawPath(
            path = mountPath,
            color = Color(0xFF0F0B1F)
        )
        scope.drawPath(
            path = mountPath,
            color = Color(0xFFFF00D4).copy(alpha = 0.5f),
            style = Stroke(width = 3f)
        )

        // Perspective wireframe floor grid
        val floorY = sunY + sunR - 5f
        val floorH = h - floorY

        scope.drawRect(
            color = Color(0xFF0B0718),
            topLeft = Offset(0f, floorY),
            size = Size(w, floorH)
        )

        // Floor longitudinal grid lines
        val floorLines = 18
        for (i in 0..floorLines) {
            val ratio = i.toFloat() / floorLines
            val botX = w * ratio
            val topX = w * 0.5f + (botX - w * 0.5f) * 0.05f
            scope.drawLine(
                color = Color(0xFF00FFCC).copy(alpha = 0.6f),
                start = Offset(botX, h),
                end = Offset(topX, floorY),
                strokeWidth = 2f
            )
        }

        // Horizontal scrolling transversal lines
        val floorSteps = 8
        for (i in 0 until floorSteps) {
            val progress = (time * 0.8f + i.toFloat() / floorSteps) % 1.0f
            // Exponential expansion for spatial depth
            val yFactor = progress * progress
            val lineY = floorY + floorH * yFactor
            scope.drawLine(
                color = Color(0xFF00FFCC).copy(alpha = (1f - yFactor) * 0.7f),
                start = Offset(0f, lineY),
                end = Offset(w, lineY),
                strokeWidth = 1f + yFactor * 4f
            )
        }
    }

    private fun drawGlitchMatrix(scope: DrawScope, w: Float, h: Float, time: Float) {
        // Black cyber screen background
        scope.drawRect(color = Color(0xFF000000))

        // Matrix background columns
        val columns = 20
        val colWidth = w / columns

        for (c in 0 until columns) {
            // Generate deterministic speed and offsets per column
            val colSpeed = 1.2f + Math.abs(sin(c.toFloat() * 123.4f)) * 2f
            val baseOffset = Math.abs(cos(c.toFloat() * 456.7f)) * h
            val rainY = (baseOffset + time * 120f * colSpeed) % h

            // Draw glowing head point
            scope.drawRect(
                color = Color(0xFFE0FFE0),
                topLeft = Offset(c * colWidth + colWidth * 0.2f, rainY),
                size = Size(colWidth * 0.6f, h * 0.015f)
            )

            // Draw diminishing binary digital trace trailing behind
            val traceLength = 8
            for (t in 0 until traceLength) {
                val segmentY = rainY - (t * h * 0.03f)
                if (segmentY > 0) {
                    val fadeFactor = (1.0f - (t.toFloat() / traceLength))
                    scope.drawRect(
                        color = Color(0xFF00FF41).copy(alpha = fadeFactor * 0.8f),
                        topLeft = Offset(c * colWidth + colWidth * 0.3f, segmentY),
                        size = Size(colWidth * 0.4f, h * 0.02f)
                    )
                }
            }

            // Random digital glitch block overlays (very CapCut Cyberpunk)
            if (sin(time * 8f + c) > 0.88f) {
                val glitchW = colWidth * (2f + (c % 3))
                val glitchH = h * 0.025f
                val glitchX = c * colWidth
                val glitchY = Math.abs(sin(time * 5f + c)) * h * 0.9f
                scope.drawRect(
                    color = Color(0xFF00FFCC).copy(alpha = 0.6f),
                    topLeft = Offset(glitchX, glitchY),
                    size = Size(glitchW, glitchH)
                )
            }
        }

        // Translucent terminal frame grids overlaying
        scope.drawRect(
            color = Color(0xFF00FF41).copy(alpha = 0.12f),
            topLeft = Offset(20f, 20f),
            size = Size(w - 40f, h - 40f),
            style = Stroke(width = 3f)
        )
    }

    private fun drawCinematicSunset(scope: DrawScope, w: Float, h: Float, time: Float) {
        // Warm dusk gradient background
        scope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFE94560), Color(0xFFFC8621), Color(0xFFFFD369))
            )
        )

        // Glowing sun setting parallax
        val sunX = w * 0.75f
        val sunY = h * 0.45f + sin(time * 0.05f) * 20f
        val sunR = h * 0.16f

        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color(0xFFFFD369).copy(alpha = 0.8f), Color.Transparent),
                center = Offset(sunX, sunY),
                radius = sunR * 1.5f
            ),
            center = Offset(sunX, sunY),
            radius = sunR * 1.5f
        )
        scope.drawCircle(
            color = Color.White,
            center = Offset(sunX, sunY),
            radius = sunR
        )

        // Parallax Distant Hills - Layer 1 (Slow speed, lighter tone)
        val hillPath1 = Path().apply {
            moveTo(0f, h * 0.8f)
            cubicTo(w * 0.25f, h * 0.65f, w * 0.5f, h * 0.82f, w * 0.8f, h * 0.70f)
            cubicTo(w * 0.9f, h * 0.66f, w * 0.95f, h * 0.75f, w, h * 0.75f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        scope.drawPath(
            path = hillPath1,
            color = Color(0xFF2E2A39)
        )

        // Parallax Near Hills - Layer 2 (Faster speed, dark silhouette)
        val waveOffset = sin(time * 0.4f) * 12f
        val hillPath2 = Path().apply {
            moveTo(0f, h * 0.88f + waveOffset * 0.5f)
            cubicTo(w * 0.3f, h * 0.78f, w * 0.6f, h * 0.94f, w * 0.75f, h * 0.82f + waveOffset)
            cubicTo(w * 0.88f, h * 0.76f, w * 0.92f, h * 0.92f, w, h * 0.85f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        scope.drawPath(
            path = hillPath2,
            color = Color(0xFF16131F)
        )

        // Flying seagulls animating across the sunset
        val birdCount = 3
        for (i in 0 until birdCount) {
            val birdBaseX = w * 0.2f + (i * w * 0.2f)
            val birdProgress = (time * 0.15f + i * 0.35f) % 1.0f
            val birdX = (birdBaseX + birdProgress * w * 0.7f) % w
            val birdY = h * 0.32f + sin(time * 2f + i) * 15f + (i * h * 0.05f)
            val wingFlap = sin(time * 12f + i) * 6f
            val birdSize = 12f * (1.2f - i * 0.2f)

            val leftWing = Offset(birdX - birdSize, birdY - wingFlap)
            val rightWing = Offset(birdX + birdSize, birdY - wingFlap)
            val bodyCenter = Offset(birdX, birdY + wingFlap * 0.4f)

            // Left wing vector
            scope.drawLine(
                color = Color(0xFF16131F),
                start = bodyCenter,
                end = leftWing,
                strokeWidth = 3f
            )
            // Right wing vector
            scope.drawLine(
                color = Color(0xFF16131F),
                start = bodyCenter,
                end = rightWing,
                strokeWidth = 3f
            )
        }

        // Cinematic Widescreen Letterbox bars (applied natively to source preview)
        val barH = h * 0.08f
        scope.drawRect(
            color = Color.Black,
            topLeft = Offset(0f, 0f),
            size = Size(w, barH)
        )
        scope.drawRect(
            color = Color.Black,
            topLeft = Offset(0f, h - barH),
            size = Size(w, barH)
        )
    }
}

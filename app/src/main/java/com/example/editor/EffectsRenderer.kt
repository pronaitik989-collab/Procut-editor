package com.example.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object EffectsRenderer {

    fun renderSceneWithEffects(
        scope: DrawScope,
        sceneType: SceneType,
        currentTimeMs: Long,
        clipSpeed: Float,
        adjustments: ColorAdjustments,
        activeEffects: List<EffectType>,
        globalOpacity: Float
    ) {
        val w = scope.size.width
        val h = scope.size.height
        if (w <= 0 || h <= 0) return

        val time = (currentTimeMs * clipSpeed) / 1000f

        // 1. Core Geometric Pre-Transforms (Shake & Distort)
        var shakeX = 0f
        var shakeY = 0f
        if (activeEffects.contains(EffectType.SHAKE_DISTORT)) {
            shakeX = sin(time * 38f) * (w * 0.04f)
            shakeY = cos(time * 42f) * (h * 0.03f)
        }

        // Apply global translation for shake
        scope.translate(left = shakeX, top = shakeY) {
            
            // 2. Mirror effect (4-Way Split)
            if (activeEffects.contains(EffectType.SCREEN_MIRROR)) {
                val halfW = w / 2f
                val halfH = h / 2f
                
                // Segment 1: TL
                scope.clipRect(0f, 0f, halfW, halfH) {
                    scope.scale(scaleX = 0.5f, scaleY = 0.5f, pivot = Offset(0f, 0f)) {
                        drawWithColorFilters(scope, sceneType, currentTimeMs, clipSpeed, adjustments, activeEffects, globalOpacity, time)
                    }
                }
                // Segment 2: TR
                scope.clipRect(halfW, 0f, w, halfH) {
                    scope.scale(scaleX = 0.5f, scaleY = 0.5f, pivot = Offset(w, 0f)) {
                        drawWithColorFilters(scope, sceneType, currentTimeMs, clipSpeed, adjustments, activeEffects, globalOpacity, time)
                    }
                }
                // Segment 3: BL
                scope.clipRect(0f, halfH, halfW, h) {
                    scope.scale(scaleX = 0.5f, scaleY = 0.5f, pivot = Offset(0f, h)) {
                        drawWithColorFilters(scope, sceneType, currentTimeMs, clipSpeed, adjustments, activeEffects, globalOpacity, time)
                    }
                }
                // Segment 4: BR
                scope.clipRect(halfW, halfH, w, h) {
                    scope.scale(scaleX = 0.5f, scaleY = 0.5f, pivot = Offset(w, h)) {
                        drawWithColorFilters(scope, sceneType, currentTimeMs, clipSpeed, adjustments, activeEffects, globalOpacity, time)
                    }
                }
            } else if (activeEffects.contains(EffectType.ZOOM_BLUR)) {
                // Render radial blur layers with varying scale & alpha
                val layers = 3
                for (l in 0 until layers) {
                    val scaleFactor = 1.0f + (l * 0.03f)
                    val alpha = 1.0f - (l * 0.35f)
                    scope.scale(scaleX = scaleFactor, scaleY = scaleFactor, pivot = Offset(w / 2f, h / 2f)) {
                        drawWithColorFilters(scope, sceneType, currentTimeMs, clipSpeed, adjustments, activeEffects, globalOpacity * alpha, time)
                    }
                }
            } else {
                // Normal direct render
                drawWithColorFilters(scope, sceneType, currentTimeMs, clipSpeed, adjustments, activeEffects, globalOpacity, time)
            }

            // 3. Post-Render Overlay Effects (VHS, Glitch Artifacts, Lighting, Glows etc.)
            drawOverlayEffects(scope, w, h, activeEffects, time, adjustments)
        }
    }

    private fun drawWithColorFilters(
        scope: DrawScope,
        sceneType: SceneType,
        currentTimeMs: Long,
        clipSpeed: Float,
        adjustments: ColorAdjustments,
        activeEffects: List<EffectType>,
        opacity: Float,
        time: Float
    ) {
        val w = scope.size.width
        val h = scope.size.height

        // Apply basic scene drawing
        ProceduralScenes.drawScene(scope, sceneType, currentTimeMs, clipSpeed)

        // Apply Color grading adjustment overlays (Brightness, Contrast, Saturation, Temp)
        // Brightness: Overlay translucent white or black tint
        if (adjustments.brightness != 0f) {
            val brightnessColor = if (adjustments.brightness > 0) Color.White else Color.Black
            val bAlpha = Math.abs(adjustments.brightness).coerceIn(0f, 0.5f) * opacity
            scope.drawRect(color = brightnessColor.copy(alpha = bAlpha))
        }

        // Contrast: Soften highlights/shadows via simple color multiplication screen overlay
        if (adjustments.contrast != 1f) {
            val contrastFactor = (adjustments.contrast - 1f).coerceIn(-0.5f, 0.5f)
            if (contrastFactor > 0) {
                // Increase contrast by overlaying a hard linear gradient
                scope.drawRect(
                    color = Color.White.copy(alpha = contrastFactor * 0.15f * opacity),
                    blendMode = BlendMode.Overlay
                )
            } else {
                // Decrease contrast by overlaying medium gray
                scope.drawRect(
                    color = Color.Gray.copy(alpha = Math.abs(contrastFactor) * 0.25f * opacity),
                    blendMode = BlendMode.Color
                )
            }
        }

        // Temperature: Yellow warm or blue cool tint
        if (adjustments.temperature != 0.0f) {
            val tempColor = if (adjustments.temperature > 0) Color(0xFFFFCC33) else Color(0xFF33CCFF)
            val tempAlpha = Math.abs(adjustments.temperature).coerceIn(0f, 0.4f) * opacity
            scope.drawRect(color = tempColor.copy(alpha = tempAlpha), blendMode = BlendMode.Color)
        }

        // Saturation: B&W or Vivid Colors overlay
        if (adjustments.saturation < 0.95f) {
            // Desaturate: overlay dark gray in Color blend mode
            val desatAlpha = (1.0f - adjustments.saturation).coerceIn(0f, 1.0f) * opacity
            scope.drawRect(color = Color(0xFF666666).copy(alpha = desatAlpha), blendMode = BlendMode.Color)
        } else if (adjustments.saturation > 1.05f) {
            // Saturation boost: overlay duplicate colors
            val satAlpha = (adjustments.saturation - 1f).coerceIn(0f, 1.0f) * 0.18f * opacity
            scope.drawRect(color = Color(0xFFFF00D4).copy(alpha = satAlpha), blendMode = BlendMode.Overlay)
        }

        // Glitch channel shift effect simulation (draw extra pink/cyan offset lines)
        if (activeEffects.contains(EffectType.GLITCH)) {
            val r = Random(time.hashCode())
            val sliceCount = 4
            for (i in 0 until sliceCount) {
                if (r.nextFloat() > 0.4f) {
                    val sliceY = r.nextFloat() * h
                    val sliceH = (0.04f + r.nextFloat() * 0.08f) * h
                    val xShift = (r.nextFloat() - 0.5f) * w * 0.1f
                    
                    // Draw cyan glitch block in that horizontal band
                    scope.drawRect(
                        color = Color(0xFF00FFFF).copy(alpha = 0.35f),
                        topLeft = Offset(xShift, sliceY),
                        size = Size(w, sliceH),
                        blendMode = BlendMode.Overlay
                    )
                    // Draw magenta glitch block oppositely shifted
                    scope.drawRect(
                        color = Color(0xFFFF00FF).copy(alpha = 0.35f),
                        topLeft = Offset(-xShift, sliceY + r.nextFloat() * 10f),
                        size = Size(w, sliceH),
                        blendMode = BlendMode.Overlay
                    )
                }
            }
        }

        // Cel-shading / Comic outlined effect overlay
        if (activeEffects.contains(EffectType.COMIC_OUTLINE)) {
            // Draw diagonal hatching lines with small spacing to look like comic dots or paper textures
            val hatches = 35
            for (i in -hatches..hatches) {
                val offset = i * (w / hatches) + (time * 15f) % (w / hatches)
                scope.drawLine(
                    color = Color.Black.copy(alpha = 0.15f * opacity),
                    start = Offset(offset, 0f),
                    end = Offset(offset + h, h),
                    strokeWidth = 2.5f
                )
            }
            // Thick pitch black border comic stroke
            scope.drawRect(
                color = Color.Black.copy(alpha = 0.6f * opacity),
                topLeft = Offset(0f, 0f),
                size = scope.size,
                style = Stroke(width = 8f)
            )
        }
    }

    private fun drawOverlayEffects(
        scope: DrawScope,
        w: Float,
        h: Float,
        activeEffects: List<EffectType>,
        time: Float,
        adjustments: ColorAdjustments
    ) {
        // Vignette effect drawing
        val vignetteVal = adjustments.vignette
        if (vignetteVal > 0f) {
            scope.drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = vignetteVal * 0.85f)),
                    center = Offset(w / 2f, h / 2f),
                    radius = w * 0.72f
                )
            )
        }

        // Dreamy Glow
        if (activeEffects.contains(EffectType.RADIAL_GLOW)) {
            val centerGlow = Offset(w / 2f, h / 2f)
            val glowRadius = w * 0.45f
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
                    center = centerGlow,
                    radius = glowRadius
                ),
                center = centerGlow,
                radius = glowRadius
            )
        }

        // Light Leak Flare: warm orange light sweeping diagonally
        if (activeEffects.contains(EffectType.LIGHT_LEAK)) {
            val sweepRatio = (time * 0.4f) % 2.5f - 0.75f
            val startX = w * sweepRatio
            val endX = w * (sweepRatio + 0.4f)
            
            scope.drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFFF5200).copy(alpha = 0.35f),
                        Color(0xFFFFDD00).copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    start = Offset(startX, 0f),
                    end = Offset(endX, h)
                )
            )
        }

        // RGB Strobe Neon Flashes (Party mode)
        if (activeEffects.contains(EffectType.RGB_STROBE)) {
            val stobeCycle = (time * 12f).toInt() % 3
            val strobeColor = when (stobeCycle) {
                0 -> Color(0xFFFF0055).copy(alpha = 0.15f)
                1 -> Color(0xFF00FFCC).copy(alpha = 0.15f)
                else -> Color(0xFFCC00FF).copy(alpha = 0.15f)
            }
            scope.drawRect(color = strobeColor)
        }

        // Rainbow Bokeh Flashes
        if (activeEffects.contains(EffectType.RAINBOW_BOKEH)) {
            val r = Random(12345)
            val count = 12
            for (i in 0 until count) {
                val cycleSpeed = 0.5f + r.nextFloat() * 1.5f
                val life = (time * cycleSpeed + r.nextFloat()) % 1.0f
                val scale = sin(life * Math.PI.toFloat())
                
                val bokehX = r.nextFloat() * w
                val bokehY = r.nextFloat() * h
                val bokehRadius = (35f + r.nextFloat() * 65f) * scale
                
                val colors = listOf(
                    Color(0xFF00FFCC).copy(alpha = 0.35f * scale),
                    Color(0xFFFF3366).copy(alpha = 0.3f * scale),
                    Color(0xFFFFCC00).copy(alpha = 0.25f * scale),
                    Color(0xFF8B00FF).copy(alpha = 0.2f * scale)
                )
                
                scope.drawCircle(
                    color = colors[i % colors.size],
                    center = Offset(bokehX, bokehY),
                    radius = bokehRadius
                )
            }
        }

        // VHS Overlay: Draw VHS analog timestamps, grain and noise lines
        if (activeEffects.contains(EffectType.VHS)) {
            val rand = Random(time.hashCode())
            
            // 1. VHS Grain / noise dots
            val grainCount = 18
            for (i in 0 until grainCount) {
                val gX = rand.nextFloat() * w
                val gY = rand.nextFloat() * h
                scope.drawCircle(
                    color = Color.White.copy(alpha = 0.25f),
                    center = Offset(gX, gY),
                    radius = 1f + rand.nextFloat() * 1.5f
                )
            }

            // 2. Vertical drifting noise bands
            val trackingY = (time * 95f) % h
            scope.drawLine(
                color = Color.White.copy(alpha = 0.22f),
                start = Offset(0f, trackingY),
                end = Offset(w, trackingY),
                strokeWidth = 2.5f
            )
            scope.drawLine(
                color = Color.White.copy(alpha = 0.15f),
                start = Offset(0f, (trackingY + h*0.4f) % h),
                end = Offset(w, (trackingY + h*0.4f) % h),
                strokeWidth = 1f
            )

            // 3. Vintage CRT Scanline screens overlay (thin black bars)
            val scanlineGap = 5f
            var sY = 0f
            while (sY < h) {
                scope.drawLine(
                    color = Color.Black.copy(alpha = 0.08f),
                    start = Offset(0f, sY),
                    end = Offset(w, sY),
                    strokeWidth = 1.2f
                )
                sY += scanlineGap
            }

            // 4. Cinema cropping if Letterbox is not already applied
            if (!activeEffects.contains(EffectType.CINEMATIC_CROP)) {
                // Subtle overlay vignette shading
                scope.drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent, Color.Black.copy(alpha = 0.4f))
                    )
                )
            }
        }

        // 1. LASER_SCAN: Neon scanning laser line
        if (activeEffects.contains(EffectType.LASER_SCAN)) {
            val scanY = ((sin(time * 3f) + 1f) / 2f) * h
            // Sweeping glowing brush gradient
            scope.drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0xFF00F0FF).copy(alpha = 0.4f), Color.Transparent),
                    startY = scanY - 30f,
                    endY = scanY + 30f
                ),
                topLeft = Offset(0f, scanY - 30f),
                size = Size(w, 60f)
            )
            // Center sharp laser core
            scope.drawLine(
                color = Color.White,
                start = Offset(0f, scanY),
                end = Offset(w, scanY),
                strokeWidth = 3f
            )
        }

        // 2. NEON_EDGE: Pulsing neon frame with dynamic chromatic hue
        if (activeEffects.contains(EffectType.NEON_EDGE)) {
            val pulse = (sin(time * 5f) + 1f) / 2f
            val borderThickness = 4f + pulse * 6f
            val glowColor = Color.hsv(hue = (time * 60f) % 360f, saturation = 0.9f, value = 1.0f)
            
            scope.drawRect(
                color = glowColor.copy(alpha = 0.7f),
                topLeft = Offset(borderThickness / 2f, borderThickness / 2f),
                size = Size(w - borderThickness, h - borderThickness),
                style = Stroke(width = borderThickness)
            )
            // Additional softer outer blurred glow
            scope.drawRect(
                color = glowColor.copy(alpha = 0.25f),
                topLeft = Offset(borderThickness, borderThickness),
                size = Size(w - 2f * borderThickness, h - 2f * borderThickness),
                style = Stroke(width = borderThickness * 1.5f)
            )
        }

        // 3. DISINTEGRATE: Matrix rain code lines falling
        if (activeEffects.contains(EffectType.DISINTEGRATE)) {
            val r = Random(999)
            val columns = 16
            for (col in 0 until columns) {
                val speed = 0.6f + r.nextFloat() * 1.2f
                val startY = ((time * speed * 250f + r.nextFloat() * h) % (h + 100f)) - 50f
                val colX = (col.toFloat() / columns) * w + (w / columns) / 2f
                
                // Falling trail
                val length = 8
                for (i in 0 until length) {
                    val charY = startY - (i * 20f)
                    if (charY in 0f..h) {
                        val alpha = (1.0f - (i.toFloat() / length)).coerceIn(0f, 1f)
                        val isBright = (i == 0) // Front particle is bright white
                        val particleColor = if (isBright) Color(0xFF00FF88) else Color(0xFF00AA44)
                        
                        // Draw tiny glowing matrix blocks
                        scope.drawRect(
                            color = particleColor.copy(alpha = alpha * 0.8f),
                            topLeft = Offset(colX - 4f, charY - 6f),
                            size = Size(8f, 12f)
                        )
                        scope.drawCircle(
                            color = Color.White.copy(alpha = alpha * 0.9f),
                            center = Offset(colX, charY),
                            radius = 1.5f
                        )
                    }
                }
            }
        }

        // 4. KALEIDOSCOPE: Professional geometric kaleidoscope mirroring
        if (activeEffects.contains(EffectType.KALEIDOSCOPE)) {
            val center = Offset(w / 2f, h / 2f)
            val maxRadius = minOf(w, h) * 0.5f
            val segments = 8
            val angleStep = 360f / segments
            
            for (i in 0 until segments) {
                val rotAngle = (i * angleStep + (time * 15f))
                scope.rotate(degrees = rotAngle, pivot = center) {
                    // Draw reflective laser/neon slices
                    val color = Color.hsv(hue = (i * 45f + time * 30f) % 360f, saturation = 0.8f, value = 1.0f)
                    
                    // Draw geometric patterns that construct a marvelous kaleidoscope visualization
                    scope.drawLine(
                        color = color.copy(alpha = 0.6f),
                        start = center,
                        end = Offset(center.x, center.y - maxRadius),
                        strokeWidth = 3f
                    )
                    scope.drawCircle(
                        color = color.copy(alpha = 0.3f),
                        center = Offset(center.x, center.y - maxRadius * 0.6f),
                        radius = 12f + sin(time * 3f + i) * 6f
                    )
                    scope.drawRect(
                        color = Color.White.copy(alpha = 0.2f),
                        topLeft = Offset(center.x - 8f, center.y - maxRadius * 0.3f),
                        size = Size(16f, 16f),
                        style = Stroke(width = 2f)
                    )
                }
            }
        }

        // Force cinema crops letterbox if requested
        if (activeEffects.contains(EffectType.CINEMATIC_CROP)) {
            val barH = h * 0.12f
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
}

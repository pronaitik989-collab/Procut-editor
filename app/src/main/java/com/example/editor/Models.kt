package com.example.editor

import androidx.compose.ui.graphics.Color

enum class SceneType {
    NEON_STREET,
    COSMOS_NEBULA,
    TOKYO_SYNTHWAVE,
    GLITCH_MATRIX,
    CINEMATIC_SUNSET
}

enum class EffectType(val label: String, val category: String) {
    GLITCH("Glitch Shift", "Glitch"),
    VHS("VHS 1996", "Retro"),
    CINEMATIC_CROP("Letterbox 16:9", "Cinematic"),
    COMIC_OUTLINE("Cel Shading", "Comic"),
    LIGHT_LEAK("Light Flare", "Lighting"),
    RGB_STROBE("Strobe Neon", "Lighting"),
    SCREEN_MIRROR("4-Way Mirror", "Split Screen"),
    SHAKE_DISTORT("Camera Shake", "Distortion"),
    ZOOM_BLUR("Radial Zoom", "Blur & Focus"),
    RADIAL_GLOW("Dreamy Glow", "Blur & Focus"),
    RAINBOW_BOKEH("Bokeh Flashes", "Lighting"),
    LASER_SCAN("Laser Scanline PRO", "PRO Effects"),
    NEON_EDGE("Neon Edge Glow PRO", "PRO Effects"),
    DISINTEGRATE("Matrix Dissolve PRO", "PRO Effects"),
    KALEIDOSCOPE("Mirror Scope PRO", "PRO Effects")
}

enum class AudioType(val label: String, val author: String) {
    NEON_GRID("Neon Grid (Synthwave)", "Aesthetic Retro"),
    MIDNIGHT_LOFI("Midnight Lofi Beats", "Dreamy Studio"),
    CINEMATIC_RISE("Cinematic Epic Synth", "Hans Wave"),
    CYBERPUNK_BEAT("Digital Rebel Bass", "Cyber Punk"),
    WHOOSH_SFX("Whoosh Transition SFX", "Sound FX"),
    SPARKLE_SFX("Sparkle Magic SFX", "Sound FX"),
    REWIND_SFX("Tape Rewind SFX", "Sound FX"),
    POP_SFX("Bubble Pop SFX", "Sound FX"),
    SHUTTER_SFX("Camera Shutter SFX", "Sound FX"),
    TTS_GENERATED("Generated AI Voice", "Neural Speech"),
    EXTRACTED_AUDIO("Extracted Soundtrack", "Audio Extractor")
}

enum class TextStylePreset(val label: String) {
    NEON_CYAN("Cyan Glow"),
    RETRO_AMBER("Retro Amber"),
    CHROME_SILVER("Chrome Metal"),
    OUTLINE_WHITE("Outline Bold"),
    VINTAGE_GOLD("Gold Vintage"),
    LCD_GREEN("Digital LCD")
}

enum class StickerType(val label: String) {
    GLITCH_CAM("Glitch CAM Overlay"),
    FOCUS_RETICLE("Focus Reticle Frame"),
    VHS_PLAY_ICON("VHS PLAY Indicator"),
    CONFETTI_GRAIN("Sparkling Confetti"),
    HEART_PARTICLES("Floating Hearts"),
    FIRE_SPARKS("Ember Spark Trails"),
    AURORA_GLOW("Aurora Nebula Aura"),
    RAINBOW_AURA("Chroma Rainbow Aura"),
    GOLDEN_AURA("Super Saiyan Gold Aura"),
    ANGELIC_HALO("Angelic Blue Aura"),
    CYBER_SHIELD("Neon Forcefield Aura"),
    VOID_AURA("Abyssal Dark Aura")
}

data class ColorAdjustments(
    val brightness: Float = 0.0f,     // -0.5f to 0.5f
    val contrast: Float = 1.0f,       // 0.5f to 1.5f
    val saturation: Float = 1.0f,     // 0.0f to 2.0f
    val temperature: Float = 0.0f,    // -0.5f to 0.5f
    val vignette: Float = 0.0f        // 0.0f to 1.0f
)

data class VideoClip(
    val id: String,
    val name: String,
    val sceneType: SceneType,
    val durationMs: Long,             // Total active trimmed duration
    val sourceDurationMs: Long = 10000L, // Static available source duration (e.g. 10s)
    val trimStartMs: Long = 0L,       // Start offset inside source clip
    val speed: Float = 1.0f,          // Playback speed speed multiplier (0.5x to 4x)
    val opacity: Float = 1.0f,        // opacity factor (0.0f to 1.0f)
    val adjustments: ColorAdjustments = ColorAdjustments(),
    val localUri: String? = null
) {
    // Calculates timeline presentation width
    fun getEffectiveDurationMs(): Long {
        return (durationMs / speed).toLong()
    }
}

data class EffectTrackItem(
    val id: String,
    val type: EffectType,
    val timelineStartMs: Long,
    val durationMs: Long
)

data class AudioTrackItem(
    val id: String,
    val type: AudioType,
    val timelineStartMs: Long,
    val durationMs: Long,
    val volume: Float = 1.0f,
    val fadeInMs: Long = 500L,
    val fadeOutMs: Long = 500L,
    val voiceChangerEffect: String? = null,
    val isTts: Boolean = false,
    val ttsText: String? = null,
    val ttsVoiceName: String? = null,
    val isTranscribed: Boolean = false,
    val transcription: String? = null
)

data class TextTrackItem(
    val id: String,
    val text: String,
    val timelineStartMs: Long,
    val durationMs: Long,
    val color: Color = Color.White,
    val preset: TextStylePreset = TextStylePreset.OUTLINE_WHITE,
    val scale: Float = 1.0f,
    val posX: Float = 0.5f, // percentage of preview width (0 to 1)
    val posY: Float = 0.7f  // percentage of preview height (0 to 1)
)

data class StickerTrackItem(
    val id: String,
    val type: StickerType,
    val timelineStartMs: Long,
    val durationMs: Long,
    val scale: Float = 0.8f,
    val posX: Float = 0.5f,
    val posY: Float = 0.3f,
    val customEmoji: String? = null,
    val customLabel: String? = null
)

// Represents presets for editing ratios
enum class AspectRatioPreset(val label: String, val ratio: Float) {
    TIKTOK_9_16("9:16 (TikTok)", 9f / 16f),
    YOUTUBE_16_9("16:9 (YouTube)", 16f / 9f),
    INSTAGRAM_1_1("1:1 (Instagram)", 1f / 1f),
    CINEMATIC_21_9("2.35:1 (Cinema)", 21f / 9f)
}

data class ProjectDraft(
    val id: String,
    val name: String,
    val lastModified: String,
    val clips: List<VideoClip>,
    val effects: List<EffectTrackItem>,
    val audios: List<AudioTrackItem>,
    val texts: List<TextTrackItem>,
    val stickers: List<StickerTrackItem>,
    val aspectRatio: AspectRatioPreset = AspectRatioPreset.TIKTOK_9_16
)

fun ColorAdjustments.toMap(): Map<String, Any> {
    return mapOf(
        "brightness" to brightness,
        "contrast" to contrast,
        "saturation" to saturation,
        "temperature" to temperature,
        "vignette" to vignette
    )
}

fun ColorAdjustments_fromMap(map: Map<String, Any>?): ColorAdjustments {
    if (map == null) return ColorAdjustments()
    return ColorAdjustments(
        brightness = (map["brightness"] as? Number)?.toFloat() ?: 0.0f,
        contrast = (map["contrast"] as? Number)?.toFloat() ?: 1.0f,
        saturation = (map["saturation"] as? Number)?.toFloat() ?: 1.0f,
        temperature = (map["temperature"] as? Number)?.toFloat() ?: 0.0f,
        vignette = (map["vignette"] as? Number)?.toFloat() ?: 0.0f
    )
}

fun VideoClip.toMap(): Map<String, Any> {
    return mapOf(
        "id" to id,
        "name" to name,
        "sceneType" to sceneType.name,
        "durationMs" to durationMs,
        "sourceDurationMs" to sourceDurationMs,
        "trimStartMs" to trimStartMs,
        "speed" to speed,
        "opacity" to opacity,
        "adjustments" to adjustments.toMap(),
        "localUri" to (localUri ?: "")
    )
}

fun VideoClip_fromMap(map: Map<String, Any>): VideoClip {
    return VideoClip(
        id = map["id"] as? String ?: "",
        name = map["name"] as? String ?: "",
        sceneType = try { SceneType.valueOf(map["sceneType"] as? String ?: "NEON_STREET") } catch(e: Exception) { SceneType.NEON_STREET },
        durationMs = (map["durationMs"] as? Number)?.toLong() ?: 0L,
        sourceDurationMs = (map["sourceDurationMs"] as? Number)?.toLong() ?: 10000L,
        trimStartMs = (map["trimStartMs"] as? Number)?.toLong() ?: 0L,
        speed = (map["speed"] as? Number)?.toFloat() ?: 1.0f,
        opacity = (map["opacity"] as? Number)?.toFloat() ?: 1.0f,
        adjustments = ColorAdjustments_fromMap(map["adjustments"] as? Map<String, Any>),
        localUri = (map["localUri"] as? String)?.takeIf { it.isNotBlank() }
    )
}

fun EffectTrackItem.toMap(): Map<String, Any> {
    return mapOf(
        "id" to id,
        "type" to type.name,
        "timelineStartMs" to timelineStartMs,
        "durationMs" to durationMs
    )
}

fun EffectTrackItem_fromMap(map: Map<String, Any>): EffectTrackItem {
    return EffectTrackItem(
        id = map["id"] as? String ?: "",
        type = try { EffectType.valueOf(map["type"] as? String ?: "GLITCH") } catch(e: Exception) { EffectType.GLITCH },
        timelineStartMs = (map["timelineStartMs"] as? Number)?.toLong() ?: 0L,
        durationMs = (map["durationMs"] as? Number)?.toLong() ?: 0L
    )
}

fun AudioTrackItem.toMap(): Map<String, Any> {
    return mapOf(
        "id" to id,
        "type" to type.name,
        "timelineStartMs" to timelineStartMs,
        "durationMs" to durationMs,
        "volume" to volume,
        "fadeInMs" to fadeInMs,
        "fadeOutMs" to fadeOutMs,
        "voiceChangerEffect" to (voiceChangerEffect ?: ""),
        "isTts" to isTts,
        "ttsText" to (ttsText ?: ""),
        "ttsVoiceName" to (ttsVoiceName ?: ""),
        "isTranscribed" to isTranscribed,
        "transcription" to (transcription ?: "")
    )
}

fun AudioTrackItem_fromMap(map: Map<String, Any>): AudioTrackItem {
    return AudioTrackItem(
        id = map["id"] as? String ?: "",
        type = try { AudioType.valueOf(map["type"] as? String ?: "NEON_GRID") } catch(e: Exception) { AudioType.NEON_GRID },
        timelineStartMs = (map["timelineStartMs"] as? Number)?.toLong() ?: 0L,
        durationMs = (map["durationMs"] as? Number)?.toLong() ?: 0L,
        volume = (map["volume"] as? Number)?.toFloat() ?: 1.0f,
        fadeInMs = (map["fadeInMs"] as? Number)?.toLong() ?: 500L,
        fadeOutMs = (map["fadeOutMs"] as? Number)?.toLong() ?: 500L,
        voiceChangerEffect = (map["voiceChangerEffect"] as? String)?.takeIf { it.isNotBlank() },
        isTts = map["isTts"] as? Boolean ?: false,
        ttsText = (map["ttsText"] as? String)?.takeIf { it.isNotBlank() },
        ttsVoiceName = (map["ttsVoiceName"] as? String)?.takeIf { it.isNotBlank() },
        isTranscribed = map["isTranscribed"] as? Boolean ?: false,
        transcription = (map["transcription"] as? String)?.takeIf { it.isNotBlank() }
    )
}

fun TextTrackItem.toMap(): Map<String, Any> {
    return mapOf(
        "id" to id,
        "text" to text,
        "timelineStartMs" to timelineStartMs,
        "durationMs" to durationMs,
        "colorValue" to color.value.toLong(),
        "preset" to preset.name,
        "scale" to scale,
        "posX" to posX,
        "posY" to posY
    )
}

fun TextTrackItem_fromMap(map: Map<String, Any>): TextTrackItem {
    return TextTrackItem(
        id = map["id"] as? String ?: "",
        text = map["text"] as? String ?: "",
        timelineStartMs = (map["timelineStartMs"] as? Number)?.toLong() ?: 0L,
        durationMs = (map["durationMs"] as? Number)?.toLong() ?: 0L,
        color = Color((map["colorValue"] as? Number)?.toLong() ?: 0xFFFFFFFF),
        preset = try { TextStylePreset.valueOf(map["preset"] as? String ?: "OUTLINE_WHITE") } catch(e: Exception) { TextStylePreset.OUTLINE_WHITE },
        scale = (map["scale"] as? Number)?.toFloat() ?: 1.0f,
        posX = (map["posX"] as? Number)?.toFloat() ?: 0.5f,
        posY = (map["posY"] as? Number)?.toFloat() ?: 0.7f
    )
}

fun StickerTrackItem.toMap(): Map<String, Any> {
    return mapOf(
        "id" to id,
        "type" to type.name,
        "timelineStartMs" to timelineStartMs,
        "durationMs" to durationMs,
        "scale" to scale,
        "posX" to posX,
        "posY" to posY,
        "customEmoji" to (customEmoji ?: ""),
        "customLabel" to (customLabel ?: "")
    )
}

fun StickerTrackItem_fromMap(map: Map<String, Any>): StickerTrackItem {
    return StickerTrackItem(
        id = map["id"] as? String ?: "",
        type = try { StickerType.valueOf(map["type"] as? String ?: "GLITCH_CAM") } catch(e: Exception) { StickerType.GLITCH_CAM },
        timelineStartMs = (map["timelineStartMs"] as? Number)?.toLong() ?: 0L,
        durationMs = (map["durationMs"] as? Number)?.toLong() ?: 0L,
        scale = (map["scale"] as? Number)?.toFloat() ?: 0.8f,
        posX = (map["posX"] as? Number)?.toFloat() ?: 0.5f,
        posY = (map["posY"] as? Number)?.toFloat() ?: 0.3f,
        customEmoji = (map["customEmoji"] as? String)?.takeIf { it.isNotBlank() },
        customLabel = (map["customLabel"] as? String)?.takeIf { it.isNotBlank() }
    )
}

fun ProjectDraft.toMap(): Map<String, Any> {
    return mapOf(
        "id" to id,
        "name" to name,
        "lastModified" to lastModified,
        "clips" to clips.map { it.toMap() },
        "effects" to effects.map { it.toMap() },
        "audios" to audios.map { it.toMap() },
        "texts" to texts.map { it.toMap() },
        "stickers" to stickers.map { it.toMap() },
        "aspectRatio" to aspectRatio.name
    )
}

@Suppress("UNCHECKED_CAST")
fun ProjectDraft_fromMap(map: Map<String, Any>): ProjectDraft {
    return ProjectDraft(
        id = map["id"] as? String ?: "",
        name = map["name"] as? String ?: "",
        lastModified = map["lastModified"] as? String ?: "",
        clips = (map["clips"] as? List<Map<String, Any>>)?.map { VideoClip_fromMap(it) } ?: emptyList(),
        effects = (map["effects"] as? List<Map<String, Any>>)?.map { EffectTrackItem_fromMap(it) } ?: emptyList(),
        audios = (map["audios"] as? List<Map<String, Any>>)?.map { AudioTrackItem_fromMap(it) } ?: emptyList(),
        texts = (map["texts"] as? List<Map<String, Any>>)?.map { TextTrackItem_fromMap(it) } ?: emptyList(),
        stickers = (map["stickers"] as? List<Map<String, Any>>)?.map { StickerTrackItem_fromMap(it) } ?: emptyList(),
        aspectRatio = try { AspectRatioPreset.valueOf(map["aspectRatio"] as? String ?: "TIKTOK_9_16") } catch(e: Exception) { AspectRatioPreset.TIKTOK_9_16 }
    )
}

data class MediaAsset(
    val id: String,
    val name: String,
    val isVideo: Boolean,
    val durationMs: Long,
    val sceneType: SceneType,
    val emoji: String,
    val tag: String,
    val color: Color
)

data class ImageAnalysis(
    val assetName: String,
    val brightnessStr: String,
    val balanceStr: String,
    val colorsList: List<String>,
    val recommendation: String,
    val aestheticDesc: String
)


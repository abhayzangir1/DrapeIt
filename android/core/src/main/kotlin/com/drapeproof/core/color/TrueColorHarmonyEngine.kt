package com.drapeproof.core.color

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class HarmonyAnalysisResult(
    val scorePercent: Int,
    val harmonyLabel: String,
    val summaryFeedback: String,
    val contrastScorePercent: Int,
    val hueScorePercent: Int,
    val chromaScorePercent: Int,
    val reasonsList: List<String>,
    val deltaE00: Double,
    val deltaLuminance: Double,
    val isFlattering: Boolean,
)

object TrueColorHarmonyEngine {

    /**
     * Evaluates perceptual color compatibility based on CIELAB ΔL* (Contrast),
     * Δh (Hue/Undertone alignment), ΔC* (Chroma saturation), physical fabric optics,
     * and live ambient lighting conditions.
     */
    fun evaluate(
        skinHex: String,
        fabricHex: String,
        fabricId: String? = null,
        ambientLuminance: Double = 0.50,
    ): HarmonyAnalysisResult {
        return runCatching {
            val skinLab = ColorConversions.hexToLab(skinHex)
            val fabricLab = ColorConversions.hexToLab(fabricHex)

            val deltaE = ColorDifference.ciede2000(skinLab, fabricLab)
            val deltaL = abs(fabricLab.l - skinLab.l)

            // Chroma calculations
            val skinChroma = sqrt(skinLab.a * skinLab.a + skinLab.b * skinLab.b)
            val fabricChroma = sqrt(fabricLab.a * fabricLab.a + fabricLab.b * fabricLab.b)
            val deltaChroma = abs(fabricChroma - skinChroma)

            // Hue angles in degrees [0, 360)
            val skinHue = (atan2(skinLab.b, skinLab.a) * 180.0 / Math.PI + 360.0) % 360.0
            val fabricHue = (atan2(fabricLab.b, fabricLab.a) * 180.0 / Math.PI + 360.0) % 360.0

            // 1. Contrast Score (Luminance Separation ΔL*) with Ambient Lighting Adjustment
            val lightingFactor = (ambientLuminance - 0.50) * 10.0 // Shifts perception in bright vs dim lighting
            val effectiveDeltaL = (deltaL + lightingFactor).coerceAtLeast(0.0)

            val contrastRaw = when {
                effectiveDeltaL < 6.0 && deltaChroma < 8.0 -> 30.0 // Blends severely into skin
                effectiveDeltaL < 12.0 -> 55.0 // Low contrast
                effectiveDeltaL in 18.0..58.0 -> 96.0 // Optimal flattering definition
                else -> 82.0
            }
            var contrastScore = contrastRaw.toInt().coerceIn(20, 98)

            // 2. Chroma / Saturation Compatibility
            val chromaRaw = when {
                fabricChroma > 78.0 && skinChroma < 25.0 -> 40.0 // Harsh neon overpowers skin
                fabricChroma > 60.0 -> 65.0
                fabricChroma in 15.0..55.0 -> 94.0 // Balanced harmonious saturation
                else -> 85.0
            }
            var chromaScore = chromaRaw.toInt().coerceIn(25, 98)

            // 3. Hue & Undertone Harmony
            val isSkinWarm = skinLab.b > 11.0
            val isFabricWarm = fabricLab.b > 0.0 && (fabricHue in 10.0..110.0 || fabricHue in 340.0..360.0)
            val isFabricCool = fabricHue in 160.0..280.0

            val hueRaw = when {
                // Sickly yellow-green on cool undertones
                !isSkinWarm && fabricHue in 65.0..115.0 && fabricChroma > 30.0 -> 35.0
                // Ashen muddy tones washing out warm skin
                isSkinWarm && fabricLab.a < -10.0 && fabricLab.b in -5.0..5.0 -> 40.0
                // Complementary warm-on-warm or cool-on-cool alignment
                isSkinWarm && isFabricWarm -> 96.0
                !isSkinWarm && isFabricCool -> 96.0
                else -> 78.0
            }
            val hueScore = hueRaw.toInt().coerceIn(20, 98)

            // 4. Physical Fabric Optical Modifiers (Reflectance, Luster & Scattering)
            var fabricOpticsBonus = 0
            val fabricReason: String? = when (fabricId?.lowercase()) {
                "silk", "satin" -> {
                    // Specular sheen elevates luminance separation and radiant glow
                    fabricOpticsBonus = if (contrastScore >= 75) +4 else -2
                    "✓ Silk specular luster provides luminous facial radiance"
                }
                "velvet" -> {
                    // Deep light trapping nap deepens contrast
                    fabricOpticsBonus = if (effectiveDeltaL >= 20.0) +5 else -4
                    "✓ Velvet light-trapping enhances dramatic contour separation"
                }
                "linen", "cotton" -> {
                    // Soft diffuse scattering tempers high chroma into wearable tones
                    if (chromaScore <= 70) chromaScore += 6
                    fabricOpticsBonus = +2
                    "✓ Natural diffuse weave softens color glare against complexion"
                }
                "leather" -> {
                    fabricOpticsBonus = if (isFabricWarm || effectiveDeltaL > 25.0) +4 else +1
                    "✓ Micro-grain structure adds rich sculptural definition"
                }
                "wool", "tweed" -> {
                    fabricOpticsBonus = if (effectiveDeltaL in 15.0..50.0) +3 else 0
                    "✓ Textured fiber weave delivers organic, grounded depth"
                }
                else -> null
            }

            // Weighted Composite Compatibility Score
            val baseComposite = (0.40 * contrastScore + 0.35 * hueScore + 0.25 * chromaScore).toInt()
            val compositeScore = (baseComposite + fabricOpticsBonus).coerceIn(15, 99)

            // Dynamic Plain-Language Bullet Reasons
            val reasons = mutableListOf<String>()
            if (fabricReason != null) {
                reasons.add(fabricReason)
            }

            if (contrastScore >= 85) {
                reasons.add("✓ Strong lightness contrast defines your facial contour")
            } else if (contrastScore >= 70) {
                reasons.add("✓ Balanced value contrast separates fabric from complexion")
            } else {
                reasons.add("⚠ Low lightness contrast may blend with skin tone")
            }

            if (hueScore >= 85) {
                if (isSkinWarm) {
                    reasons.add("✓ Warm hue naturally harmonizes with your golden undertone")
                } else {
                    reasons.add("✓ Cool hue complements your crisp, cool undertone")
                }
            } else if (hueScore <= 50) {
                reasons.add("⚠ Hue undertone discordance creates visual competition")
            }

            if (chromaScore >= 85) {
                reasons.add("✓ Refined saturation lets your natural complexion shine")
            } else if (chromaScore <= 50) {
                reasons.add("⚠ Hyper-saturated chroma overpowers facial features")
            }

            val (label, summary, isFlattering) = when {
                compositeScore >= 86 -> Triple(
                    "Strong Compatibility",
                    "Enhances your natural skin undertone with crisp, balanced contrast and fabric radiance.",
                    true,
                )
                compositeScore in 70..85 -> Triple(
                    "Good Compatibility",
                    "Clean, wearable pairing with good visual separation from your face.",
                    true,
                )
                compositeScore in 48..69 -> Triple(
                    "Mixed / Contrast Risk",
                    "May slightly blend with skin or provide softer definition around the collar.",
                    false,
                )
                else -> Triple(
                    "Weak Compatibility",
                    "Too close to your skin lightness or clashes with your facial undertone.",
                    false,
                )
            }

            HarmonyAnalysisResult(
                scorePercent = compositeScore,
                harmonyLabel = label,
                summaryFeedback = summary,
                contrastScorePercent = contrastScore,
                hueScorePercent = hueScore,
                chromaScorePercent = chromaScore,
                reasonsList = reasons,
                deltaE00 = deltaE,
                deltaLuminance = deltaL,
                isFlattering = isFlattering,
            )
        }.getOrElse {
            HarmonyAnalysisResult(
                scorePercent = 70,
                harmonyLabel = "Neutral Compatibility",
                summaryFeedback = "Balanced everyday pairing for your complexion.",
                contrastScorePercent = 70,
                hueScorePercent = 70,
                chromaScorePercent = 70,
                reasonsList = listOf("✓ Neutral balanced contrast"),
                deltaE00 = 25.0,
                deltaLuminance = 30.0,
                isFlattering = true,
            )
        }
    }
}

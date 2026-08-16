package com.drapeproof.core.color

import kotlin.math.pow

/** An encoded sRGB color with 8-bit channels. */
data class SrgbColor(
    val red: Int,
    val green: Int,
    val blue: Int,
) {
    init {
        require(red in 0..255 && green in 0..255 && blue in 0..255) {
            "sRGB channels must be between 0 and 255"
        }
    }

    fun toHex(): String = "#%02X%02X%02X".format(red, green, blue)

    companion object {
        fun fromHex(value: String): SrgbColor {
            var normalized = value.trim().removePrefix("#")
            if (normalized.length == 8) {
                // Strip alpha prefix (#AARRGGBB -> RRGGBB)
                normalized = normalized.substring(2)
            } else if (normalized.length == 3) {
                // Expand 3-digit shorthand (#RGB -> RRGGBB)
                normalized = "${normalized[0]}${normalized[0]}${normalized[1]}${normalized[1]}${normalized[2]}${normalized[2]}"
            }
            require(normalized.length == 6 && normalized.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
                "Expected a valid sRGB hex value (e.g. #RRGGBB or #AARRGGBB), but got: $value"
            }
            return SrgbColor(
                red = normalized.substring(0, 2).toInt(16),
                green = normalized.substring(2, 4).toInt(16),
                blue = normalized.substring(4, 6).toInt(16),
            )
        }
    }
}

/** CIE XYZ tristimulus values normalized so reference white Y is 1.0. */
data class XyzColor(
    val x: Double,
    val y: Double,
    val z: Double,
) {
    init {
        require(x.isFinite() && y.isFinite() && z.isFinite()) { "XYZ values must be finite" }
    }
}

/** CIELAB coordinates using the D65 2-degree reference white. */
data class LabColor(
    val l: Double,
    val a: Double,
    val b: Double,
) {
    init {
        require(l.isFinite() && a.isFinite() && b.isFinite()) { "Lab values must be finite" }
    }
}

object ColorConversions {
    private const val D65_X = 0.95047
    private const val D65_Y = 1.00000
    private const val D65_Z = 1.08883
    private const val LAB_EPSILON = 216.0 / 24389.0
    private const val LAB_KAPPA = 24389.0 / 27.0

    fun srgbToXyz(color: SrgbColor): XyzColor {
        val r = linearize(color.red / 255.0)
        val g = linearize(color.green / 255.0)
        val b = linearize(color.blue / 255.0)

        return XyzColor(
            x = r * 0.4124564 + g * 0.3575761 + b * 0.1804375,
            y = r * 0.2126729 + g * 0.7151522 + b * 0.0721750,
            z = r * 0.0193339 + g * 0.1191920 + b * 0.9503041,
        )
    }

    fun xyzToLab(color: XyzColor): LabColor {
        val fx = labTransform(color.x / D65_X)
        val fy = labTransform(color.y / D65_Y)
        val fz = labTransform(color.z / D65_Z)
        return LabColor(
            l = 116.0 * fy - 16.0,
            a = 500.0 * (fx - fy),
            b = 200.0 * (fy - fz),
        )
    }

    fun srgbToLab(color: SrgbColor): LabColor = xyzToLab(srgbToXyz(color))

    fun hexToLab(hex: String): LabColor = srgbToLab(SrgbColor.fromHex(hex))

    private fun linearize(channel: Double): Double =
        if (channel <= 0.04045) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)

    private fun labTransform(value: Double): Double =
        if (value > LAB_EPSILON) value.pow(1.0 / 3.0) else (LAB_KAPPA * value + 16.0) / 116.0
}

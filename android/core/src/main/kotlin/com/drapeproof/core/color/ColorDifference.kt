package com.drapeproof.core.color

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** CIEDE2000 as specified in CIE 015:2018 / CIE 142-2001. */
object ColorDifference {
    fun ciede2000(
        first: LabColor,
        second: LabColor,
        lightnessWeight: Double = 1.0,
        chromaWeight: Double = 1.0,
        hueWeight: Double = 1.0,
    ): Double {
        require(lightnessWeight > 0.0 && chromaWeight > 0.0 && hueWeight > 0.0) {
            "CIEDE2000 weights must be positive"
        }

        val c1 = sqrt(first.a * first.a + first.b * first.b)
        val c2 = sqrt(second.a * second.a + second.b * second.b)
        val meanC = (c1 + c2) / 2.0
        val meanC7 = meanC.pow(7)
        val g = 0.5 * (1.0 - sqrt(meanC7 / (meanC7 + 25.0.pow(7))))

        val a1Prime = (1.0 + g) * first.a
        val a2Prime = (1.0 + g) * second.a
        val c1Prime = sqrt(a1Prime * a1Prime + first.b * first.b)
        val c2Prime = sqrt(a2Prime * a2Prime + second.b * second.b)
        val h1Prime = hueDegrees(first.b, a1Prime)
        val h2Prime = hueDegrees(second.b, a2Prime)

        val deltaLPrime = second.l - first.l
        val deltaCPrime = c2Prime - c1Prime
        val deltaHAngle = when {
            c1Prime * c2Prime == 0.0 -> 0.0
            abs(h2Prime - h1Prime) <= 180.0 -> h2Prime - h1Prime
            h2Prime - h1Prime > 180.0 -> h2Prime - h1Prime - 360.0
            else -> h2Prime - h1Prime + 360.0
        }
        val deltaHPrime = 2.0 * sqrt(c1Prime * c2Prime) * sin(degreesToRadians(deltaHAngle / 2.0))

        val meanLPrime = (first.l + second.l) / 2.0
        val meanCPrime = (c1Prime + c2Prime) / 2.0
        val meanHPrime = when {
            c1Prime * c2Prime == 0.0 -> h1Prime + h2Prime
            abs(h1Prime - h2Prime) <= 180.0 -> (h1Prime + h2Prime) / 2.0
            h1Prime + h2Prime < 360.0 -> (h1Prime + h2Prime + 360.0) / 2.0
            else -> (h1Prime + h2Prime - 360.0) / 2.0
        }

        val t = 1.0 -
            0.17 * cos(degreesToRadians(meanHPrime - 30.0)) +
            0.24 * cos(degreesToRadians(2.0 * meanHPrime)) +
            0.32 * cos(degreesToRadians(3.0 * meanHPrime + 6.0)) -
            0.20 * cos(degreesToRadians(4.0 * meanHPrime - 63.0))
        val deltaTheta = 30.0 * exp(-((meanHPrime - 275.0) / 25.0).pow(2))
        val meanCPrime7 = meanCPrime.pow(7)
        val rC = 2.0 * sqrt(meanCPrime7 / (meanCPrime7 + 25.0.pow(7)))
        val lOffsetSquared = (meanLPrime - 50.0).pow(2)
        val sL = 1.0 + (0.015 * lOffsetSquared) / sqrt(20.0 + lOffsetSquared)
        val sC = 1.0 + 0.045 * meanCPrime
        val sH = 1.0 + 0.015 * meanCPrime * t
        val rT = -sin(degreesToRadians(2.0 * deltaTheta)) * rC

        val lTerm = deltaLPrime / (lightnessWeight * sL)
        val cTerm = deltaCPrime / (chromaWeight * sC)
        val hTerm = deltaHPrime / (hueWeight * sH)
        return sqrt(lTerm * lTerm + cTerm * cTerm + hTerm * hTerm + rT * cTerm * hTerm)
    }

    private fun hueDegrees(b: Double, aPrime: Double): Double {
        if (aPrime == 0.0 && b == 0.0) return 0.0
        val degrees = Math.toDegrees(atan2(b, aPrime))
        return if (degrees >= 0.0) degrees else degrees + 360.0
    }

    private fun degreesToRadians(value: Double): Double = Math.toRadians(value)
}

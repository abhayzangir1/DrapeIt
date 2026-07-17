package com.drapeproof.core.statistics

import com.drapeproof.core.color.ColorDifference
import com.drapeproof.core.color.LabColor
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

data class TemporalColorSummary(
    val median: LabColor,
    /** Median CIEDE2000 distance from each frame to the channel-wise temporal median. */
    val medianDeltaEToMedian: Double,
    val p95DeltaEToMedian: Double,
    val channelMad: LabColor,
    val sampleCount: Int,
)

object RobustStatistics {
    fun median(values: Iterable<Double>): Double {
        val sorted = finiteValues(values).sorted()
        require(sorted.isNotEmpty()) { "At least one value is required" }
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2.0
    }

    /** Raw median absolute deviation. Set [scaleToNormalSigma] for the conventional 1.4826 scaling. */
    fun mad(values: Iterable<Double>, scaleToNormalSigma: Boolean = false): Double {
        val finite = finiteValues(values)
        require(finite.isNotEmpty()) { "At least one value is required" }
        val center = median(finite)
        val raw = median(finite.map { abs(it - center) })
        return if (scaleToNormalSigma) raw * 1.4826 else raw
    }

    fun quantile(values: Iterable<Double>, probability: Double): Double {
        require(probability in 0.0..1.0) { "Quantile probability must be between zero and one" }
        val sorted = finiteValues(values).sorted()
        require(sorted.isNotEmpty()) { "At least one value is required" }
        if (sorted.size == 1) return sorted.first()
        val position = probability * (sorted.lastIndex)
        val lower = floor(position).toInt()
        val upper = ceil(position).toInt()
        if (lower == upper) return sorted[lower]
        return sorted[lower] + (position - lower) * (sorted[upper] - sorted[lower])
    }

    fun medianLab(values: Iterable<LabColor>): LabColor {
        val colors = values.toList()
        require(colors.isNotEmpty()) { "At least one Lab sample is required" }
        return LabColor(
            l = median(colors.map { it.l }),
            a = median(colors.map { it.a }),
            b = median(colors.map { it.b }),
        )
    }

    fun summarizeLab(values: Iterable<LabColor>): TemporalColorSummary {
        val colors = values.toList()
        require(colors.isNotEmpty()) { "At least one Lab sample is required" }
        val center = medianLab(colors)
        val distances = colors.map { ColorDifference.ciede2000(center, it) }
        return TemporalColorSummary(
            median = center,
            medianDeltaEToMedian = median(distances),
            p95DeltaEToMedian = quantile(distances, 0.95),
            channelMad = LabColor(
                l = mad(colors.map { it.l }),
                a = mad(colors.map { it.a }),
                b = mad(colors.map { it.b }),
            ),
            sampleCount = colors.size,
        )
    }

    private fun finiteValues(values: Iterable<Double>): List<Double> = values.toList().also { list ->
        require(list.all(Double::isFinite)) { "Statistics require finite values" }
    }
}

package com.drapeproof.core.statistics

import com.drapeproof.core.color.LabColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RobustStatisticsTest {
    @Test
    fun `median handles odd even and unsorted values`() {
        assertEquals(2.0, RobustStatistics.median(listOf(3.0, 1.0, 2.0)), 0.0)
        assertEquals(2.5, RobustStatistics.median(listOf(4.0, 1.0, 3.0, 2.0)), 0.0)
    }

    @Test
    fun `MAD is robust to a large outlier`() {
        val values = listOf(10.0, 10.0, 11.0, 11.0, 1000.0)
        assertEquals(1.0, RobustStatistics.mad(values), 0.0)
        assertEquals(1.4826, RobustStatistics.mad(values, scaleToNormalSigma = true), 0.000001)
    }

    @Test
    fun `quantile uses linear interpolation`() {
        val values = listOf(0.0, 10.0, 20.0, 30.0, 40.0)
        assertEquals(0.0, RobustStatistics.quantile(values, 0.0), 0.0)
        assertEquals(20.0, RobustStatistics.quantile(values, 0.5), 0.0)
        assertEquals(38.0, RobustStatistics.quantile(values, 0.95), 0.0)
    }

    @Test
    fun `temporal summary uses channel medians and reports spread`() {
        val summary = RobustStatistics.summarizeLab(
            listOf(
                LabColor(50.0, 10.0, 20.0),
                LabColor(51.0, 11.0, 21.0),
                LabColor(49.0, 9.0, 19.0),
                LabColor(90.0, -50.0, 80.0),
            ),
        )
        assertEquals(50.5, summary.median.l, 0.0)
        assertEquals(9.5, summary.median.a, 0.0)
        assertEquals(20.5, summary.median.b, 0.0)
        assertEquals(4, summary.sampleCount)
        assertEquals(1.0, summary.channelMad.l, 0.0)
        assert(summary.p95DeltaEToMedian >= summary.medianDeltaEToMedian)
    }

    @Test
    fun `empty and non-finite input are refused`() {
        assertThrows(IllegalArgumentException::class.java) { RobustStatistics.median(emptyList()) }
        assertThrows(IllegalArgumentException::class.java) {
            RobustStatistics.median(listOf(Double.NaN))
        }
    }
}

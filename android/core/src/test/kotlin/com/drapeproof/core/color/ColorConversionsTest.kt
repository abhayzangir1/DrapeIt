package com.drapeproof.core.color

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ColorConversionsTest {
    @Test
    fun `hex round trip preserves encoded sRGB`() {
        val color = SrgbColor.fromHex("#1aB2fF")
        assertEquals(SrgbColor(26, 178, 255), color)
        assertEquals("#1AB2FF", color.toHex())
    }

    @Test
    fun `invalid hex and channels are refused`() {
        assertThrows(IllegalArgumentException::class.java) { SrgbColor.fromHex("#FFF") }
        assertThrows(IllegalArgumentException::class.java) { SrgbColor(256, 0, 0) }
    }

    @Test
    fun `black white and red match published D65 Lab references`() {
        assertLabClose(LabColor(0.0, 0.0, 0.0), ColorConversions.hexToLab("#000000"), 0.001)
        assertLabClose(LabColor(100.0, 0.0, 0.0), ColorConversions.hexToLab("#FFFFFF"), 0.02)
        assertLabClose(LabColor(53.2408, 80.0925, 67.2032), ColorConversions.hexToLab("#FF0000"), 0.02)
    }

    @Test
    fun `white maps to normalized D65 XYZ reference`() {
        val xyz = ColorConversions.srgbToXyz(SrgbColor(255, 255, 255))
        assertEquals(0.95047, xyz.x, 0.00001)
        assertEquals(1.0, xyz.y, 0.00001)
        assertEquals(1.08883, xyz.z, 0.00001)
    }

    private fun assertLabClose(expected: LabColor, actual: LabColor, tolerance: Double) {
        assertEquals(expected.l, actual.l, tolerance)
        assertEquals(expected.a, actual.a, tolerance)
        assertEquals(expected.b, actual.b, tolerance)
    }
}

package com.drapeproof.mobile.camera

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraPermissionRecoveryTest {
    @Test
    fun `settings recovery is shown only after a request without a rationale`() {
        assertFalse(shouldOpenCameraSettings(false, false, false))
        assertFalse(shouldOpenCameraSettings(true, false, true))
        assertTrue(shouldOpenCameraSettings(true, false, false))
    }

    @Test
    fun `granted permission never shows settings recovery`() {
        assertFalse(shouldOpenCameraSettings(true, true, false))
    }
}

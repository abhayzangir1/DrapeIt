package com.drapeproof.mobile.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudConnectionPolicyTest {
    @Test
    fun `reserved offline and placeholder origins are not configured`() {
        assertFalse(CloudConnectionPolicy.isConfiguredOrigin("https://offline.drapeproof.invalid"))
        assertFalse(CloudConnectionPolicy.isConfiguredOrigin("https://offline.drapeproof.invalid."))
        assertFalse(CloudConnectionPolicy.isConfiguredOrigin("https://api.drapeproof.app"))
    }

    @Test
    fun `real https and emulator loopback origins are configured`() {
        assertTrue(CloudConnectionPolicy.isConfiguredOrigin("https://drapeproof.example.workers.dev"))
        assertTrue(CloudConnectionPolicy.isConfiguredOrigin("http://10.0.2.2:8787"))
    }

    @Test
    fun `credentials paths queries and insecure remote origins are rejected`() {
        assertFalse(CloudConnectionPolicy.isAllowedRuntimeOrigin("https://user:pass@example.com"))
        assertFalse(CloudConnectionPolicy.isAllowedRuntimeOrigin("https://example.com/api"))
        assertFalse(CloudConnectionPolicy.isAllowedRuntimeOrigin("https://example.com?token=value"))
        assertFalse(CloudConnectionPolicy.isAllowedRuntimeOrigin("https://example.com:99999"))
        assertFalse(CloudConnectionPolicy.isAllowedRuntimeOrigin("http://example.com"))
    }

    @Test
    fun `access code requires eight non-whitespace characters and has a safe limit`() {
        assertFalse(CloudConnectionPolicy.isAccessCodeValid("1234567"))
        assertTrue(CloudConnectionPolicy.isAccessCodeValid(" 12345678 "))
        assertFalse(CloudConnectionPolicy.isAccessCodeValid("x".repeat(513)))
    }
}

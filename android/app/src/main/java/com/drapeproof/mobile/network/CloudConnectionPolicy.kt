package com.drapeproof.mobile.network

import java.net.URI

internal object CloudConnectionPolicy {
    private const val PLACEHOLDER_HOST = "api.drapeproof.app"

    fun isAllowedRuntimeOrigin(raw: String): Boolean {
        val uri = parseOrigin(raw) ?: return false
        return uri.scheme.equals("https", ignoreCase = true) ||
            (uri.scheme.equals("http", ignoreCase = true) && uri.host == "10.0.2.2")
    }

    fun isConfiguredOrigin(raw: String): Boolean {
        val uri = parseOrigin(raw) ?: return false
        val host = uri.host.lowercase().trimEnd('.')
        val transportAllowed = uri.scheme.equals("https", ignoreCase = true) ||
            (uri.scheme.equals("http", ignoreCase = true) && host == "10.0.2.2")
        return transportAllowed && !host.endsWith(".invalid") && host != PLACEHOLDER_HOST
    }

    fun isAccessCodeValid(value: String): Boolean = value.trim().length in 8..512

    private fun parseOrigin(raw: String): URI? = runCatching { URI(raw) }.getOrNull()?.takeIf { uri ->
        uri.host != null &&
            uri.userInfo == null &&
            (uri.port == -1 || uri.port in 1..65_535) &&
            (uri.rawPath.isNullOrEmpty() || uri.rawPath == "/") &&
            uri.rawQuery == null &&
            uri.rawFragment == null
    }
}

package com.drapeproof.mobile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.drapeproof.mobile.data.AppSettingsRepository
import com.drapeproof.mobile.data.AppThemeMode
import com.drapeproof.mobile.ui.DrapeProofApp
import com.drapeproof.mobile.ui.theme.DrapeProofTheme

class MainActivity : ComponentActivity() {
    private var sharedImageUri by mutableStateOf<Uri?>(null)
    private var currentThemeMode by mutableStateOf(AppThemeMode.SYSTEM)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        currentThemeMode = AppSettingsRepository.getThemeMode(this)
        sharedImageUri = intent.sharedImage()
        setContent {
            DrapeProofTheme(themeMode = currentThemeMode) {
                DrapeProofApp(
                    sharedImageUri = sharedImageUri,
                    onSharedImageConsumed = { sharedImageUri = null },
                    onThemeChanged = { newMode ->
                        currentThemeMode = newMode
                        AppSettingsRepository.setThemeMode(this, newMode)
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedImageUri = intent.sharedImage()
    }
}

private fun Intent.sharedImage(): Uri? {
    if (action != Intent.ACTION_SEND || type?.startsWith("image/") != true) return null
    return if (android.os.Build.VERSION.SDK_INT >= 33) {
        getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(Intent.EXTRA_STREAM)
    }
}

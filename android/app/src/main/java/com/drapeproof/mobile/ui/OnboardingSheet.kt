package com.drapeproof.mobile.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.drapeproof.mobile.ui.theme.Cobalt
import com.drapeproof.mobile.ui.theme.DrapeCoral
import com.drapeproof.mobile.ui.theme.Moss
import com.drapeproof.mobile.ui.theme.Plum

object OnboardingPreferences {
    private const val PREFS = "drapeit_onboarding"
    private const val KEY_COMPLETED = "has_completed_onboarding"

    fun isCompleted(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_COMPLETED, false)

    fun markCompleted(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_COMPLETED, true).apply()
    }
}

private data class OnboardingSlide(
    val title: String,
    val subtitle: String,
    val body: String,
    val tag: String,
    val tagColor: Color,
)

private val slides = listOf(
    OnboardingSlide(
        title = "Color Evidence, Not Rules",
        subtitle = "A physical measurement approach",
        body = "DrapeIt compares real fabric beside your face under one locked camera session. We do not assign arbitrary seasonal types, attractiveness scores, or beauty filters—we measure physical optical contrast.",
        tag = "PHYSICAL EVIDENCE",
        tagColor = DrapeCoral,
    ),
    OnboardingSlide(
        title = "Three Decision Signals",
        subtitle = "Separation · Definition · Face Shift",
        body = "1. Cloth–skin separation (CIEDE2000 ΔE00)\n2. Feature definition (eyes, brows & lips vs skin)\n3. Apparent face shift (camera-recorded change shown only when controlled capture passes).",
        tag = "TRANSPARENT METRICS",
        tagColor = Moss,
    ),
    OnboardingSlide(
        title = "Privacy & Cloud Control",
        subtitle = "Local measurement, explicit VTO",
        body = "Camera frames stay in volatile memory and are never saved to disk. Optional YouCam Facial Color Tones and Apparel VTO run through our secure server only when you explicitly tap run.",
        tag = "ZERO-LEAK PRIVACY",
        tagColor = Cobalt,
    ),
)

@Composable
fun OnboardingDialog(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var currentSlideIndex by remember { mutableIntStateOf(0) }
    val currentSlide = slides[currentSlideIndex]

    Dialog(onDismissRequest = {
        OnboardingPreferences.markCompleted(context)
        onDismiss()
    }) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        ) {
            Column(Modifier.padding(24.dp)) {
                // Header Tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = currentSlide.tagColor.copy(alpha = 0.14f),
                    ) {
                        Text(
                            currentSlide.tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = currentSlide.tagColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                    TextButton(onClick = {
                        OnboardingPreferences.markCompleted(context)
                        onDismiss()
                    }) {
                        Text("Skip", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Slide Content Card
                Text(currentSlide.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(currentSlide.subtitle, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(14.dp))
                Text(
                    currentSlide.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f),
                )

                Spacer(Modifier.height(28.dp))

                // Indicator Dots & Navigation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        slides.indices.forEach { index ->
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .fillMaxWidth(if (index == currentSlideIndex) 0.15f else 0.05f)
                                    .clip(CircleShape)
                                    .background(if (index == currentSlideIndex) DrapeCoral else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (currentSlideIndex > 0) {
                            OutlinedButton(
                                onClick = { currentSlideIndex -= 1 },
                                shape = RoundedCornerShape(14.dp),
                            ) { Text("Back") }
                        }

                        Button(
                            onClick = {
                                if (currentSlideIndex < slides.lastIndex) {
                                    currentSlideIndex += 1
                                } else {
                                    OnboardingPreferences.markCompleted(context)
                                    onDismiss()
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DrapeCoral),
                        ) {
                            Text(
                                if (currentSlideIndex == slides.lastIndex) "Get Started  →" else "Next",
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }
    }
}

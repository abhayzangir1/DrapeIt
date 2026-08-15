package com.drapeproof.mobile.ui.tutorial

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.sp
import com.drapeproof.mobile.data.AppSettingsRepository
import com.drapeproof.mobile.ui.theme.EditorialGold
import com.drapeproof.mobile.ui.theme.EditorialInk
import com.drapeproof.mobile.ui.theme.EditorialMuted
import com.drapeproof.mobile.ui.theme.EditorialSienna

data class GuideStep(
    val stepNumber: Int,
    val icon: String,
    val targetTab: String,
    val title: String,
    val instruction: String,
    val actionPrompt: String,
)

val guidedTourSteps = listOf(
    GuideStep(
        stepNumber = 1,
        icon = "🪞",
        targetTab = "drape",
        title = "Step 1 • Live Face Drape Studio",
        instruction = "Position your face in the oval. Tap color swatches in the lower carousel to see live harmony scores shift in real-time with your lighting.",
        actionPrompt = "Try Tapping a Color Swatch",
    ),
    GuideStep(
        stepNumber = 2,
        icon = "🧵",
        targetTab = "drape",
        title = "Step 2 • Material & Fabric Sheen",
        instruction = "Tap the 'Fabric' button on the bottom right to switch from Silk to Velvet or Linen. Each fabric has unique optical light scattering!",
        actionPrompt = "Tap 'Fabric' Button to Switch Texture",
    ),
    GuideStep(
        stepNumber = 3,
        icon = "📸",
        targetTab = "drape",
        title = "Step 3 • Snap Curtain Capture",
        instruction = "Press the center shutter ring to snap and freeze your drape portrait. It instantly saves to your Looks tab.",
        actionPrompt = "Tap Shutter to Snap Portrait",
    ),
    GuideStep(
        stepNumber = 4,
        icon = "👗",
        targetTab = "tryon",
        title = "Step 4 • Virtual Try-On Studio",
        instruction = "Upload your portrait or avatar, select apparel or fabrics, and rotate or generate AI virtual try-ons.",
        actionPrompt = "Explore Virtual Try-On",
    ),
    GuideStep(
        stepNumber = 5,
        icon = "👤",
        targetTab = "profile",
        title = "Step 5 • Color Profile & Settings",
        instruction = "Scan your facial skin tone with live biometrics, explore personalized palette recommendations, or adjust dark theme and sound effects.",
        actionPrompt = "Finish & Start Styling!",
    ),
)

@Composable
fun DrapeInteractiveGuide(
    currentTab: String,
    onNavigateToTab: (String) -> Unit,
    onCompleteGuide: () -> Unit,
) {
    val context = LocalContext.current
    var currentStepIndex by remember { mutableIntStateOf(0) }
    val step = guidedTourSteps.getOrElse(currentStepIndex) { guidedTourSteps.first() }
    val isLastStep = currentStepIndex == guidedTourSteps.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        // TOP STEP PILL
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.85f))
                .border(1.dp, EditorialGold.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✨ INTERACTIVE TOUR", color = EditorialGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Spacer(Modifier.width(8.dp))
                Text("${currentStepIndex + 1}/${guidedTourSteps.size}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // FLOATING ACTION CARD AT BOTTOM
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 72.dp) // Sits comfortably above bottom bar
                .border(1.5.dp, EditorialSienna.copy(alpha = 0.6f), RoundedCornerShape(24.dp)),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(step.icon, fontSize = 22.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            step.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Text(
                        "Skip",
                        style = MaterialTheme.typography.labelSmall,
                        color = EditorialMuted,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                AppSettingsRepository.setInteractiveTourDone(context, true)
                                onCompleteGuide()
                            }
                            .padding(4.dp),
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    step.instruction,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                )

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (currentStepIndex > 0) {
                        Button(
                            onClick = {
                                currentStepIndex--
                                onNavigateToTab(guidedTourSteps[currentStepIndex].targetTab)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.weight(0.7f),
                        ) {
                            Text("Back", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Button(
                        onClick = {
                            if (isLastStep) {
                                AppSettingsRepository.setInteractiveTourDone(context, true)
                                onCompleteGuide()
                            } else {
                                currentStepIndex++
                                onNavigateToTab(guidedTourSteps[currentStepIndex].targetTab)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                        modifier = Modifier.weight(1.3f),
                    ) {
                        Text(
                            if (isLastStep) "Got It • Done ✓" else "Next: ${step.actionPrompt} →",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

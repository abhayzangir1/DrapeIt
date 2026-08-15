package com.drapeproof.mobile.ui.tutorial

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.drapeproof.mobile.ui.theme.EditorialCream
import com.drapeproof.mobile.ui.theme.EditorialInk
import com.drapeproof.mobile.ui.theme.EditorialMuted
import com.drapeproof.mobile.ui.theme.EditorialSienna
import com.drapeproof.mobile.ui.theme.EditorialStone

private data class TutorialStep(
    val icon: String,
    val title: String,
    val description: String,
    val featureHighlight: String,
)

private val tutorialSteps = listOf(
    TutorialStep(
        icon = "🪞",
        title = "Live Drape Studio",
        description = "Align your face in the oval to drape virtual fabrics and test color harmony in real time.",
        featureHighlight = "Real-Time Harmony Analysis",
    ),
    TutorialStep(
        icon = "🎨",
        title = "Seasonal Color Profile",
        description = "Discover your skin tone, undertone, and signature seasonal palette with biometric precision.",
        featureHighlight = "Photo Sampling & Live Scan",
    ),
    TutorialStep(
        icon = "👗",
        title = "AI Virtual Try-On",
        description = "Upload clothing or select custom silhouettes and swap colors instantly with neural AI rendering.",
        featureHighlight = "Powered by YouCam AI",
    ),
    TutorialStep(
        icon = "📸",
        title = "Looks & Side-by-Side Compare",
        description = "Save your favorite drape snaps and virtual try-ons, then compare up to 4 looks side-by-side.",
        featureHighlight = "Side-by-Side Evaluation",
    ),
    TutorialStep(
        icon = "✨",
        title = "Curated Seasonal Wardrobe",
        description = "Explore handpicked color palettes and fabric pairings tailored for Everyday, Work, and Evening.",
        featureHighlight = "Personalized Fashion Styling",
    ),
)

@Composable
fun DrapeTutorialModal(
    onDismiss: () -> Unit,
) {
    var currentStepIndex by remember { mutableIntStateOf(0) }
    val step = tutorialSteps[currentStepIndex]
    val isLastStep = currentStepIndex == tutorialSteps.size - 1

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = EditorialCream),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EditorialStone.copy(alpha = 0.40f), RoundedCornerShape(26.dp)),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // TOP BAR: STEP PROGRESS & SKIP
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            tutorialSteps.indices.forEach { index ->
                                val isCurrent = index == currentStepIndex
                                Box(
                                    modifier = Modifier
                                        .height(6.dp)
                                        .width(if (isCurrent) 22.dp else 6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (isCurrent) EditorialSienna else EditorialStone.copy(alpha = 0.5f)),
                                )
                            }
                        }

                        Text(
                            "Skip",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EditorialMuted,
                            modifier = Modifier
                                .clickable { onDismiss() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }

                    Spacer(Modifier.height(28.dp))

                    AnimatedContent(
                        targetState = step,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "tutorialContent",
                    ) { currentStep ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            // ICON CIRCLE
                            Box(
                                modifier = Modifier
                                    .size(86.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.5.dp, EditorialStone.copy(alpha = 0.50f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(currentStep.icon, fontSize = 42.sp)
                            }

                            Spacer(Modifier.height(18.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(EditorialSienna.copy(alpha = 0.10f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    currentStep.featureHighlight,
                                    color = EditorialSienna,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            Text(
                                currentStep.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = EditorialInk,
                                textAlign = TextAlign.Center,
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                currentStep.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = EditorialMuted,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(30.dp))

                    // ACTION BUTTONS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (currentStepIndex > 0) {
                            Button(
                                onClick = { currentStepIndex-- },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .border(1.dp, EditorialStone.copy(alpha = 0.40f), RoundedCornerShape(12.dp)),
                            ) {
                                Text("Back", color = EditorialInk, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Button(
                            onClick = {
                                if (isLastStep) {
                                    onDismiss()
                                } else {
                                    currentStepIndex++
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(if (currentStepIndex > 0) 1.4f else 1f)
                                .height(48.dp),
                        ) {
                            Text(
                                if (isLastStep) "Get Started" else "Next",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

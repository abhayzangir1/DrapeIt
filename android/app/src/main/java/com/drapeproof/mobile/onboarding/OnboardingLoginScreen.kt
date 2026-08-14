package com.drapeproof.mobile.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drapeproof.mobile.silhouette.BodyShape
import com.drapeproof.mobile.silhouette.UserBodyProfile
import com.drapeproof.mobile.silhouette.UserProfileStore
import com.drapeproof.mobile.ui.FabricWaveDrapeView
import com.drapeproof.mobile.ui.theme.EditorialCream
import com.drapeproof.mobile.ui.theme.EditorialSienna

@Composable
fun OnboardingLoginScreen(onComplete: () -> Unit) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = EditorialCream,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Liquid Silk Drapery
            FabricWaveDrapeView(
                modifier = Modifier.fillMaxSize(),
                primaryColor = Color(0xFF5B1226), // Royal Burgundy Silk
                accentColor = EditorialSienna,
            )

            // Dark Vignette Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.40f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f),
                            ),
                        ),
                    ),
            )

            // Content Column
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Top Brand Mark
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text(
                        "DrapeIt",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = (-0.5).sp,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("✨", fontSize = 18.sp)
                }

                // Center Editorial Headline
                Column {
                    Text(
                        "See what truly\nsuits you.",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        lineHeight = 44.sp,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Material-aware color intelligence & YouCam AI Virtual Try-On before you buy or wear.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.80f),
                        lineHeight = 22.sp,
                    )

                    Spacer(Modifier.height(32.dp))

                    // 1-Tap Entry Button
                    Button(
                        onClick = {
                            UserProfileStore.save(
                                context,
                                UserBodyProfile(
                                    name = "Member",
                                    gender = "Unisex",
                                    heightCm = 175,
                                    bodyShape = BodyShape.TRAPEZOID,
                                ),
                            )
                            onComplete()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                    ) {
                        Text(
                            "Get Started  →",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "🔒 100% on-device live color analysis. Photos sent only on Try-On.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.60f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

package com.drapeproof.mobile.silhouette

import android.content.Context
import android.content.SharedPreferences
import com.drapeproof.mobile.fabric.FabricDrape
import com.drapeproof.mobile.fabric.FabricMaterial

enum class BodyShape(val displayName: String, val icon: String, val description: String) {
    HOURGLASS(
        displayName = "Hourglass",
        icon = "⌛",
        description = "Balanced shoulders and hips with a naturally defined waistline.",
    ),
    INVERTED_TRIANGLE(
        displayName = "Inverted Triangle",
        icon = "🔻",
        description = "Broad shoulders and chest tapering to narrower hips (Athletic V-Taper).",
    ),
    RECTANGLE(
        displayName = "Rectangle / Column",
        icon = "▯",
        description = "Balanced shoulders, waist, and hips with a clean, straight silhouette.",
    ),
    PEAR(
        displayName = "Pear / Triangle",
        icon = "🍐",
        description = "Slender upper body with gently fuller hips and thighs.",
    ),
    APPLE(
        displayName = "Apple / Oval",
        icon = "🍎",
        description = "Softer midsection with shapely legs and balanced shoulders.",
    ),
    TRAPEZOID(
        displayName = "Classic / Trapezoid",
        icon = "⏢",
        description = "Naturally proportioned shoulders with moderate chest taper and balanced hips.",
    ),
}

enum class HeightCategory(val displayName: String, val rangeLabel: String) {
    PETITE(displayName = "Petite", rangeLabel = "Under 5'4\" (≤ 162 cm)"),
    REGULAR(displayName = "Regular", rangeLabel = "5'4\" - 5'10\" (163 - 178 cm)"),
    TALL(displayName = "Tall", rangeLabel = "Over 5'10\" (≥ 179 cm)"),
}

enum class TopwearCut(val displayName: String, val icon: String, val description: String) {
    SLIM_FIT(
        displayName = "Slim Fit",
        icon = "👕",
        description = "Tailored close to body lines, accentuating natural waist and chest definition.",
    ),
    BOXY_OVERSIZED(
        displayName = "Boxy Oversized",
        icon = "🧥",
        description = "Structured wide chest cut with clean cropped hem for modern streetwear proportions.",
    ),
    RELAXED_CASUAL(
        displayName = "Relaxed Casual",
        icon = "👚",
        description = "Easy drape with gentle ease across torso, offering breezy comfort and fluidity.",
    ),
    TAILORED_ATHLETIC(
        displayName = "Tailored Athletic",
        icon = "👔",
        description = "Extra shoulder and chest room tapering smoothly to waist for athletic V-taper.",
    ),
    DROP_SHOULDER(
        displayName = "Drop Shoulder",
        icon = "🎽",
        description = "Soft relaxed shoulder seam that balances broad silhouettes and adds effortless style.",
    ),
}

enum class BottomwearCut(val displayName: String, val description: String) {
    HIGH_WAISTED_STRAIGHT(
        displayName = "High-Waisted Straight Leg",
        description = "Elongates leg line and balances torso proportions seamlessly.",
    ),
    TAPERED_CHINOS(
        displayName = "Tapered Chinos",
        description = "Clean modern line narrowing gently from knee to ankle for sharp elegance.",
    ),
    PLEATED_RELAXED_SLACKS(
        displayName = "Pleated Relaxed Slacks",
        description = "Classic single or double pleats offering drape, room, and timeless sophistication.",
    ),
    WIDE_LEG_TROUSERS(
        displayName = "Wide-Leg Trousers",
        description = "Fluid, continuous drape from hip to floor creating dramatic, flattering flow.",
    ),
    SLIM_DENIM(
        displayName = "Slim Dark Denim",
        description = "Versatile clean silhouette that grounds casual and smart-casual tops.",
    ),
}

data class CutRecommendation(
    val recommendedTop: TopwearCut,
    val recommendedBottom: BottomwearCut,
    val bottomColorAdvice: String,
    val stylingTip: String,
)

data class UserBodyProfile(
    val name: String = "Shopper",
    val gender: String = "Unisex",
    val heightCm: Int = 172,
    val bodyShape: BodyShape = BodyShape.TRAPEZOID,
) {
    val heightCategory: HeightCategory
        get() = when {
            heightCm <= 162 -> HeightCategory.PETITE
            heightCm <= 178 -> HeightCategory.REGULAR
            else -> HeightCategory.TALL
        }

    val heightFormatted: String
        get() {
            val totalInches = (heightCm / 2.54).toInt()
            val feet = totalInches / 12
            val inches = totalInches % 12
            return "$feet'$inches\" ($heightCm cm)"
        }
}

object SilhouetteEngine {

    fun recommend(
        shape: BodyShape,
        heightCategory: HeightCategory,
        fabric: FabricMaterial,
    ): CutRecommendation {
        return when (shape) {
            BodyShape.HOURGLASS -> {
                CutRecommendation(
                    recommendedTop = if (fabric.drape == FabricDrape.FLUID) TopwearCut.RELAXED_CASUAL else TopwearCut.SLIM_FIT,
                    recommendedBottom = BottomwearCut.HIGH_WAISTED_STRAIGHT,
                    bottomColorAdvice = "Pair with tonal or high-contrast neutral bottoms (Ecru or Slate).",
                    stylingTip = "Highlight your natural waistline with fitted or belted silhouettes that maintain balanced proportions.",
                )
            }
            BodyShape.INVERTED_TRIANGLE -> {
                CutRecommendation(
                    recommendedTop = TopwearCut.DROP_SHOULDER,
                    recommendedBottom = BottomwearCut.WIDE_LEG_TROUSERS,
                    bottomColorAdvice = "Use lighter or textured bottoms to draw balanced attention downwards.",
                    stylingTip = "Soft drop-shoulder tops soften broad shoulders, while wide-leg or pleated bottoms create perfect symmetry.",
                )
            }
            BodyShape.RECTANGLE -> {
                CutRecommendation(
                    recommendedTop = if (heightCategory == HeightCategory.TALL) TopwearCut.BOXY_OVERSIZED else TopwearCut.RELAXED_CASUAL,
                    recommendedBottom = BottomwearCut.PLEATED_RELAXED_SLACKS,
                    bottomColorAdvice = "Opt for contrasting shades to visually delineate upper and lower body.",
                    stylingTip = "Boxy and relaxed cuts introduce dynamic geometry and depth to lean, straight-column silhouettes.",
                )
            }
            BodyShape.PEAR -> {
                CutRecommendation(
                    recommendedTop = TopwearCut.TAILORED_ATHLETIC,
                    recommendedBottom = BottomwearCut.HIGH_WAISTED_STRAIGHT,
                    bottomColorAdvice = "Darker, streamlined bottomwear grounds your look while highlighting vibrant tops.",
                    stylingTip = "Emphasize your upper body with structured shoulders or vivid colors paired with clean straight lines below.",
                )
            }
            BodyShape.APPLE -> {
                CutRecommendation(
                    recommendedTop = TopwearCut.RELAXED_CASUAL,
                    recommendedBottom = BottomwearCut.TAPERED_CHINOS,
                    bottomColorAdvice = "Tapered bottoms in charcoal or navy create sleek vertical lines.",
                    stylingTip = "Fluid, unconstricted relaxed tops create a smooth, elongated silhouette with zero clinging.",
                )
            }
            BodyShape.TRAPEZOID -> {
                CutRecommendation(
                    recommendedTop = when (fabric.drape) {
                        FabricDrape.STRUCTURED -> TopwearCut.SLIM_FIT
                        FabricDrape.RELAXED -> TopwearCut.BOXY_OVERSIZED
                        FabricDrape.FLUID -> TopwearCut.RELAXED_CASUAL
                        FabricDrape.HEAVY_PLUSH -> TopwearCut.TAILORED_ATHLETIC
                    },
                    recommendedBottom = BottomwearCut.TAPERED_CHINOS,
                    bottomColorAdvice = "Versatile harmony — pairs well with khaki, olive, or classic dark denim.",
                    stylingTip = "Classic balanced frame allows versatile switching between tailored and relaxed streetwear cuts.",
                )
            }
        }
    }
}

object UserProfileStore {
    private const val PREFS_NAME = "drapeit_user_profile"
    private const val KEY_NAME = "profile_name"
    private const val KEY_GENDER = "profile_gender"
    private const val KEY_HEIGHT = "profile_height_cm"
    private const val KEY_SHAPE = "profile_body_shape"
    private const val KEY_ONBOARDED = "profile_is_onboarded"

    fun load(context: Context): UserBodyProfile {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_NAME, "Shopper") ?: "Shopper"
        val gender = prefs.getString(KEY_GENDER, "Unisex") ?: "Unisex"
        val heightCm = prefs.getInt(KEY_HEIGHT, 172)
        val shapeName = prefs.getString(KEY_SHAPE, BodyShape.TRAPEZOID.name) ?: BodyShape.TRAPEZOID.name
        val shape = runCatching { BodyShape.valueOf(shapeName) }.getOrDefault(BodyShape.TRAPEZOID)
        return UserBodyProfile(name, gender, heightCm, shape)
    }

    fun save(context: Context, profile: UserBodyProfile) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_NAME, profile.name)
            .putString(KEY_GENDER, profile.gender)
            .putInt(KEY_HEIGHT, profile.heightCm)
            .putString(KEY_SHAPE, profile.bodyShape.name)
            .putBoolean(KEY_ONBOARDED, true)
            .apply()
    }

    fun isOnboarded(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_ONBOARDED, false)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}

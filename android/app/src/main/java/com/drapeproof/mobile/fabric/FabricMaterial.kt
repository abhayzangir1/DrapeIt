package com.drapeproof.mobile.fabric

import androidx.compose.ui.graphics.Color

enum class FabricLuster {
    MATTE,
    SUBTLE_SHEEN,
    HIGH_GLOSS,
    DIRECTIONAL_VELVET,
}

enum class FabricDrape {
    STRUCTURED,
    RELAXED,
    FLUID,
    HEAVY_PLUSH,
}

data class FabricMaterial(
    val id: String,
    val name: String,
    val icon: String,
    val description: String,
    val luster: FabricLuster,
    val drape: FabricDrape,
    val breathability: String,
    val weaveType: String,
    val textureOpacity: Float = 0.95f,
)

object FabricCatalog {
    val allFabrics: List<FabricMaterial> = listOf(
        FabricMaterial(
            id = "cotton",
            name = "Organic Cotton",
            icon = "🌿",
            description = "Natural soft matte weave, versatile comfort and breathable drape.",
            luster = FabricLuster.MATTE,
            drape = FabricDrape.STRUCTURED,
            breathability = "Very High",
            weaveType = "Plain Weave",
        ),
        FabricMaterial(
            id = "linen",
            name = "Natural Linen",
            icon = "🌾",
            description = "Crisp organic slub texture with relaxed, airy summer drape.",
            luster = FabricLuster.MATTE,
            drape = FabricDrape.RELAXED,
            breathability = "Maximum",
            weaveType = "Slub Weave",
        ),
        FabricMaterial(
            id = "silk",
            name = "Mulberry Silk",
            icon = "✨",
            description = "Luminous fluid drape with pearlescent specular sheen.",
            luster = FabricLuster.HIGH_GLOSS,
            drape = FabricDrape.FLUID,
            breathability = "High",
            weaveType = "Satin Weave",
        ),
        FabricMaterial(
            id = "satin",
            name = "Lustrous Satin",
            icon = "💎",
            description = "Glossy liquid drape with high reflective highlights.",
            luster = FabricLuster.HIGH_GLOSS,
            drape = FabricDrape.FLUID,
            breathability = "Medium",
            weaveType = "High-Luster Filament",
        ),
        FabricMaterial(
            id = "cashmere",
            name = "Pure Cashmere",
            icon = "🧵",
            description = "Ultra-fine cloud-soft luxury with gentle thermal drape.",
            luster = FabricLuster.MATTE,
            drape = FabricDrape.RELAXED,
            breathability = "High",
            weaveType = "Fine Spun",
        ),
        FabricMaterial(
            id = "wool",
            name = "Merino Wool",
            icon = "🐑",
            description = "Brushed fine-gauge knit with tailored warmth and structure.",
            luster = FabricLuster.SUBTLE_SHEEN,
            drape = FabricDrape.STRUCTURED,
            breathability = "High",
            weaveType = "Worsted Weave",
        ),
        FabricMaterial(
            id = "denim",
            name = "Structured Denim",
            icon = "👖",
            description = "Durable diagonal twill weave with firm collar structure.",
            luster = FabricLuster.MATTE,
            drape = FabricDrape.STRUCTURED,
            breathability = "Medium",
            weaveType = "3x1 Twill",
        ),
        FabricMaterial(
            id = "velvet",
            name = "Plush Velvet",
            icon = "🧥",
            description = "Deep directional light absorption with rich dimensional sheen.",
            luster = FabricLuster.DIRECTIONAL_VELVET,
            drape = FabricDrape.HEAVY_PLUSH,
            breathability = "Medium",
            weaveType = "Cut Pile",
        ),
        FabricMaterial(
            id = "knit",
            name = "Ribbed Knit",
            icon = "🧶",
            description = "Dimensional textured rib with body-contouring relaxed drape.",
            luster = FabricLuster.MATTE,
            drape = FabricDrape.RELAXED,
            breathability = "High",
            weaveType = "2x2 Rib Knit",
        ),
        FabricMaterial(
            id = "polyester",
            name = "Tech Polyester",
            icon = "🏃",
            description = "Performance moisture-wicking weave with wrinkle-free drape.",
            luster = FabricLuster.SUBTLE_SHEEN,
            drape = FabricDrape.STRUCTURED,
            breathability = "High",
            weaveType = "Micro-Piqué",
        ),
    )

    val defaultFabric: FabricMaterial = allFabrics[0]

    fun findById(id: String): FabricMaterial {
        return allFabrics.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: defaultFabric
    }
}

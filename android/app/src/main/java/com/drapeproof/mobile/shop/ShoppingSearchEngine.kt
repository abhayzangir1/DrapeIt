package com.drapeproof.mobile.shop

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class ShopProduct(
    val title: String,
    val retailer: String,
    val priceEstimate: String,
    val rating: String,
    val fabric: String,
    val colorName: String,
    val colorHex: String,
    val shoppingUrl: String,
    val tag: String = "Popular Choice",
)

object ShoppingSearchEngine {

    private fun encode(query: String): String {
        return runCatching { URLEncoder.encode(query, StandardCharsets.UTF_8.toString()) }.getOrDefault(query)
    }

    fun buildGoogleShoppingUrl(query: String): String {
        return "https://www.google.com/search?tbm=shop&q=${encode(query)}"
    }

    fun buildAmazonShoppingUrl(query: String): String {
        return "https://www.amazon.com/s?k=${encode(query)}"
    }

    fun buildZaraSearchUrl(query: String): String {
        return "https://www.google.com/search?q=${encode("site:zara.com $query")}"
    }

    fun buildAsosSearchUrl(query: String): String {
        return "https://www.asos.com/search/?q=${encode(query)}"
    }

    fun generateRecommendedProducts(
        fabricName: String,
        colorName: String,
        colorHex: String,
        cutName: String = "Relaxed Fit",
        category: String = "Shirt / Top",
    ): List<ShopProduct> {
        val queryBase = "$colorName $fabricName $cutName $category"
        return listOf(
            ShopProduct(
                title = "$colorName $fabricName $cutName",
                retailer = "Google Shopping Direct",
                priceEstimate = "$38 - $75",
                rating = "★ 4.8 (1.2k)",
                fabric = fabricName,
                colorName = colorName,
                colorHex = colorHex,
                shoppingUrl = buildGoogleShoppingUrl(queryBase),
                tag = "Best Price Match",
            ),
            ShopProduct(
                title = "$fabricName $cutName $category",
                retailer = "Amazon Fashion",
                priceEstimate = "$29 - $49",
                rating = "★ 4.6 (3.4k)",
                fabric = fabricName,
                colorName = colorName,
                colorHex = colorHex,
                shoppingUrl = buildAmazonShoppingUrl(queryBase),
                tag = "Prime Fast Delivery",
            ),
            ShopProduct(
                title = "Minimalist $fabricName $category",
                retailer = "Zara Collection",
                priceEstimate = "$49 - $89",
                rating = "★ 4.9 (850)",
                fabric = fabricName,
                colorName = colorName,
                colorHex = colorHex,
                shoppingUrl = buildZaraSearchUrl(queryBase),
                tag = "Curated Aesthetic",
            ),
            ShopProduct(
                title = "Contemporary $cutName in $colorName",
                retailer = "ASOS Design",
                priceEstimate = "$34 - $65",
                rating = "★ 4.7 (920)",
                fabric = fabricName,
                colorName = colorName,
                colorHex = colorHex,
                shoppingUrl = buildAsosSearchUrl(queryBase),
                tag = "Trending Cut",
            ),
        )
    }

    fun generateVisualSearchProducts(garmentQuery: String): List<ShopProduct> {
        val q = if (garmentQuery.isNotBlank()) garmentQuery else "contemporary fashion apparel"
        return listOf(
            ShopProduct(
                title = "Matched $q",
                retailer = "Google Visual Search",
                priceEstimate = "Compare All Retailers",
                rating = "★ 4.8",
                fabric = "Matched Fabric",
                colorName = "Matched Color",
                colorHex = "#2F51A2",
                shoppingUrl = buildGoogleShoppingUrl(q),
                tag = "Visual Image Match",
            ),
            ShopProduct(
                title = "Similar Style $q",
                retailer = "Amazon Marketplace",
                priceEstimate = "$28 - $58",
                rating = "★ 4.6",
                fabric = "Similar Material",
                colorName = "Similar Color",
                colorHex = "#B85F45",
                shoppingUrl = buildAmazonShoppingUrl(q),
                tag = "Instant Purchase",
            ),
            ShopProduct(
                title = "Designer Match $q",
                retailer = "Zara & ASOS Catalog",
                priceEstimate = "$45 - $95",
                rating = "★ 4.9",
                fabric = "Premium Quality",
                colorName = "Trending",
                colorHex = "#71856E",
                shoppingUrl = buildZaraSearchUrl(q),
                tag = "High Street Match",
            ),
        )
    }
}

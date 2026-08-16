package com.drapeproof.mobile.shop

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class ShopProduct(
    val title: String,
    val retailer: String,
    val priceEstimate: String,
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

    fun generateStyleSuggestions(
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
                priceEstimate = "Approx $38 - $75",
                fabric = fabricName,
                colorName = colorName,
                colorHex = colorHex,
                shoppingUrl = buildGoogleShoppingUrl(queryBase),
                tag = "Best Price Match",
            ),
            ShopProduct(
                title = "$fabricName $cutName $category",
                retailer = "Amazon Fashion",
                priceEstimate = "Approx $29 - $49",
                fabric = fabricName,
                colorName = colorName,
                colorHex = colorHex,
                shoppingUrl = buildAmazonShoppingUrl(queryBase),
                tag = "Prime Fast Delivery",
            ),
            ShopProduct(
                title = "Minimalist $fabricName $category",
                retailer = "Zara Collection",
                priceEstimate = "Approx $49 - $89",
                fabric = fabricName,
                colorName = colorName,
                colorHex = colorHex,
                shoppingUrl = buildZaraSearchUrl(queryBase),
                tag = "Curated Aesthetic",
            ),
            ShopProduct(
                title = "Contemporary $cutName in $colorName",
                retailer = "ASOS Design",
                priceEstimate = "Approx $34 - $65",
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
                priceEstimate = "Variable Pricing",
                fabric = "Matched Fabric",
                colorName = "Matched Color",
                colorHex = "#2F51A2",
                shoppingUrl = buildGoogleShoppingUrl(q),
                tag = "Visual Image Match",
            ),
            ShopProduct(
                title = "Similar Style $q",
                retailer = "Amazon Marketplace",
                priceEstimate = "Approx $28 - $58",
                fabric = "Similar Material",
                colorName = "Similar Color",
                colorHex = "#B85F45",
                shoppingUrl = buildAmazonShoppingUrl(q),
                tag = "Instant Purchase",
            ),
            ShopProduct(
                title = "Designer Match $q",
                retailer = "Zara & ASOS Catalog",
                priceEstimate = "Approx $45 - $95",
                fabric = "Premium Quality",
                colorName = "Trending",
                colorHex = "#71856E",
                shoppingUrl = buildZaraSearchUrl(q),
                tag = "High Street Match",
            ),
        )
    }
}

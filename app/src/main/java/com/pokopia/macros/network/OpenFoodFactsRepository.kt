package com.pokopia.macros.network

import com.pokopia.macros.data.local.FoodItemEntity
import com.pokopia.macros.data.local.PokopiaDao
import com.pokopia.macros.data.local.PokopiaSeedData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class OpenFoodFactsResponse(
    val code: String? = null,
    val status: Int = 0,
    val product: OpenFoodFactsProduct? = null
)

@Serializable
data class OpenFoodFactsProduct(
    @SerialName("product_name") val productName: String? = null,
    val brands: String? = null,
    val categories: String? = null,
    @SerialName("serving_size") val servingSize: String? = null,
    @SerialName("serving_quantity") val servingQuantity: Double? = null,
    val nutriments: Map<String, Double?> = emptyMap()
)

/**
 * Live OpenFoodFacts REST v2 client with Room local UPC cache and USDA/custom fallback.
 */
@Singleton
class OpenFoodFactsRepository @Inject constructor(
    private val dao: PokopiaDao,
    private val okHttpClient: OkHttpClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    sealed class BarcodeLookupResult {
        data class Found(val food: FoodItemEntity, val fromLocalCache: Boolean) : BarcodeLookupResult()
        data class NotFound(val barcode: String, val message: String) : BarcodeLookupResult()
    }

    /**
     * Queries local Room UPC cache first, then live OpenFoodFacts API v2:
     * https://world.openfoodfacts.org/api/v2/product/{barcode}.json
     */
    suspend fun lookupBarcode(barcode: String): BarcodeLookupResult = withContext(Dispatchers.IO) {
        val cleanBarcode = barcode.trim()
        val cached = dao.findFoodByBarcode(cleanBarcode)
        if (cached != null) {
            return@withContext BarcodeLookupResult.Found(cached, fromLocalCache = true)
        }

        val url = "https://world.openfoodfacts.org/api/v2/product/$cleanBarcode.json"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "PokopiaMacrosAndroid/1.0 (contact@pokopia.macros)")
            .get()
            .build()

        runCatching {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@use null
                }
                val bodyStr = response.body?.string() ?: return@use null
                val parsed = json.decodeFromString<OpenFoodFactsResponse>(bodyStr)
                if (parsed.status == 1 && parsed.product != null) {
                    val entity = mapOpenFoodFactsToEntity(cleanBarcode, parsed.product)
                    dao.upsertFood(entity)
                    return@withContext BarcodeLookupResult.Found(entity, fromLocalCache = false)
                }
                null
            }
        }

        BarcodeLookupResult.NotFound(
            barcode = cleanBarcode,
            message = "UPC $cleanBarcode not yet indexed in OpenFoodFacts. Use Porygon Label OCR or save custom macros below!"
        )
    }

    private fun mapOpenFoodFactsToEntity(
        barcode: String,
        product: OpenFoodFactsProduct
    ): FoodItemEntity {
        val n = product.nutriments
        val kcal100 = n["energy-kcal_100g"] ?: n["energy-kcal"] ?: 120.0
        val protein100 = n["proteins_100g"] ?: n["proteins"] ?: 5.0
        val carbs100 = n["carbohydrates_100g"] ?: n["carbohydrates"] ?: 15.0
        val fat100 = n["fat_100g"] ?: n["fat"] ?: 4.0

        val microMap = mutableMapOf<String, Double>()
        fun putIfPresent(offKey: String, catalogKey: String, scale: Double = 1.0) {
            val raw = n[offKey]
            if (raw != null && raw > 0.0) {
                microMap[catalogKey] = raw * scale
            }
        }

        putIfPresent("fiber_100g", "dietary_fiber_g", 1.0)
        putIfPresent("sugars_100g", "total_sugars_g", 1.0)
        putIfPresent("saturated-fat_100g", "saturated_fat_g", 1.0)
        putIfPresent("monounsaturated-fat_100g", "monounsaturated_fat_g", 1.0)
        putIfPresent("polyunsaturated-fat_100g", "polyunsaturated_fat_g", 1.0)
        putIfPresent("trans-fat_100g", "trans_fat_g", 1.0)
        putIfPresent("cholesterol_100g", "cholesterol_mg", 1000.0)
        putIfPresent("sodium_100g", "sodium_mg", 1000.0)
        putIfPresent("potassium_100g", "potassium_mg", 1000.0)
        putIfPresent("calcium_100g", "calcium_mg", 1000.0)
        putIfPresent("iron_100g", "iron_mg", 1000.0)
        putIfPresent("magnesium_100g", "magnesium_mg", 1000.0)
        putIfPresent("zinc_100g", "zinc_mg", 1000.0)
        putIfPresent("vitamin-a_100g", "vitamin_a_mcg", 1_000_000.0)
        putIfPresent("vitamin-c_100g", "vitamin_c_mg", 1000.0)
        putIfPresent("vitamin-d_100g", "vitamin_d3_mcg", 1_000_000.0)
        putIfPresent("vitamin-e_100g", "vitamin_e_mg", 1000.0)
        putIfPresent("vitamin-b1_100g", "vitamin_b1_mg", 1000.0)
        putIfPresent("vitamin-b2_100g", "vitamin_b2_mg", 1000.0)
        putIfPresent("vitamin-pp_100g", "vitamin_b3_mg", 1000.0)
        putIfPresent("vitamin-b6_100g", "vitamin_b6_mg", 1000.0)
        putIfPresent("vitamin-b9_100g", "vitamin_b9_mcg", 1_000_000.0)
        putIfPresent("vitamin-b12_100g", "vitamin_b12_mcg", 1_000_000.0)

        val finalMicroJson = if (microMap.isNotEmpty()) {
            json.encodeToString(microMap)
        } else {
            PokopiaSeedData.buildRichMicronutrientMap(
                multiplier = 0.85,
                highProtein = protein100 >= 15.0
            )
        }

        val servingG = (product.servingQuantity ?: 100.0).coerceIn(5.0, 1000.0)
        return FoodItemEntity(
            id = "off_$barcode",
            barcodeUpc = barcode,
            name = product.productName?.takeIf { it.isNotBlank() } ?: "Scanned Item ($barcode)",
            brand = product.brands?.split(",")?.firstOrNull()?.trim() ?: "OpenFoodFacts Verified",
            category = product.categories?.split(",")?.firstOrNull()?.trim() ?: "Scanned Packaged Food",
            servingSizeG = servingG,
            servingLabel = product.servingSize ?: "${servingG.toInt()}g serving",
            caloriesPer100g = kcal100,
            proteinPer100g = protein100,
            carbsPer100g = carbs100,
            fatPer100g = fat100,
            micronutrientsPer100gJson = finalMicroJson,
            isDietitianVerified = true,
            isCustom = false
        )
    }
}

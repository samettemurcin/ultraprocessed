package com.b2.ultraprocessed.network.usda

class UsdaRepository(
    private val dataSource: UsdaApiDataSource,
    private val cacheTtlMillis: Long = DEFAULT_CACHE_TTL_MILLIS,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val cacheLock = Any()
    private val barcodeCache = LinkedHashMap<String, CacheEntry<UsdaFoodRecord?>>()
    private val queryCache = LinkedHashMap<String, CacheEntry<UsdaFoodRecord?>>()
    private val detailCache = LinkedHashMap<Long, CacheEntry<UsdaFoodDetail?>>()

    suspend fun lookupByBarcode(upc: String): UsdaFoodRecord? {
        val searchDigits = upc.filter { it.isDigit() }
        if (searchDigits.isBlank()) return null
        getCached(barcodeCache, searchDigits)?.let { return it }

        // Query with full scanned digits (keep leading zeros); normalization is only for GTIN comparison.
        val candidates = dataSource.searchFoods(query = searchDigits, pageSize = 25)
        if (candidates.isEmpty()) {
            putCached(barcodeCache, searchDigits, null)
            return null
        }
        val picked = rankBarcodeCandidates(candidates, searchDigits)
        if (picked == null) {
            putCached(barcodeCache, searchDigits, null)
            return null
        }
        val detail = fetchDetailCached(picked.fdcId)
        return toRecord(detail ?: picked.toDetailFallback())
            .also { putCached(barcodeCache, searchDigits, it) }
    }

    suspend fun lookupByQuery(query: String): UsdaFoodRecord? {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return null
        getCached(queryCache, normalizedQuery)?.let { return it }
        val candidates = dataSource.searchFoods(query = normalizedQuery, pageSize = 10)
        val picked = candidates.firstOrNull()
        if (picked == null) {
            putCached(queryCache, normalizedQuery, null)
            return null
        }
        val detail = fetchDetailCached(picked.fdcId)
        return toRecord(detail ?: picked.toDetailFallback())
            .also { putCached(queryCache, normalizedQuery, it) }
    }

    private suspend fun fetchDetailCached(fdcId: Long): UsdaFoodDetail? {
        getCached(detailCache, fdcId)?.let { return it }
        return dataSource.fetchFoodDetail(fdcId)
            .also { putCached(detailCache, fdcId, it) }
    }

    private fun rankBarcodeCandidates(
        candidates: List<UsdaSearchFood>,
        scannedDigits: String,
    ): UsdaSearchFood? {
        val targetNorm = normalizeUpc(scannedDigits)
        if (targetNorm.isBlank()) return null
        // Only accept a row whose GTIN matches the scan — do not return an unrelated branded hit.
        return candidates.firstOrNull { it.gtinUpc.matchesUpc(targetNorm) }
    }

    private fun toRecord(detail: UsdaFoodDetail): UsdaFoodRecord = UsdaFoodRecord(
        fdcId = detail.fdcId,
        productName = detail.description.ifBlank { "USDA product" },
        brandOwner = detail.brandOwner,
        gtinUpc = detail.gtinUpc,
        ingredientsText = detail.ingredients,
    )

    private fun <K, V> getCached(
        cache: LinkedHashMap<K, CacheEntry<V>>,
        key: K,
    ): V? = synchronized(cacheLock) {
        val entry = cache[key] ?: return@synchronized null
        if (entry.expiresAtMillis <= nowMillis()) {
            cache.remove(key)
            return@synchronized null
        }
        entry.value
    }

    private fun <K, V> putCached(
        cache: LinkedHashMap<K, CacheEntry<V>>,
        key: K,
        value: V,
    ) {
        synchronized(cacheLock) {
            cache[key] = CacheEntry(value, nowMillis() + cacheTtlMillis)
            while (cache.size > MAX_CACHE_ENTRIES) {
                val eldest = cache.keys.firstOrNull() ?: break
                cache.remove(eldest)
            }
        }
    }

    private data class CacheEntry<T>(
        val value: T,
        val expiresAtMillis: Long,
    )

    companion object {
        private const val MAX_CACHE_ENTRIES = 128
        private const val DEFAULT_CACHE_TTL_MILLIS = 60L * 60L * 1000L
    }
}

private fun UsdaSearchFood.toDetailFallback(): UsdaFoodDetail = UsdaFoodDetail(
    fdcId = fdcId,
    description = description,
    brandOwner = brandOwner,
    gtinUpc = gtinUpc,
    ingredients = ingredients,
)

private fun normalizeUpc(value: String?): String =
    value.orEmpty().filter(Char::isDigit).trimStart('0')

private fun String?.matchesUpc(normalizedTarget: String): Boolean {
    val mine = normalizeUpc(this)
    return mine == normalizedTarget || mine.endsWith(normalizedTarget) || normalizedTarget.endsWith(mine)
}
